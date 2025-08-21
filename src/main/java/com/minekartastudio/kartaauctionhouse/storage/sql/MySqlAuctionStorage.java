package com.minekartastudio.kartaauctionhouse.storage.sql;

import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.auction.model.AuctionStatus;
import com.minekartastudio.kartaauctionhouse.auction.model.Bid;
import com.minekartastudio.kartaauctionhouse.common.SerializedItem;
import com.minekartastudio.kartaauctionhouse.storage.AuctionStorage;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySqlAuctionStorage implements AuctionStorage {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;

    //<editor-fold desc="SQL Statements">
    private static final String CREATE_AUCTIONS_TABLE = """
        CREATE TABLE IF NOT EXISTS auctions (
          auction_id       CHAR(36) PRIMARY KEY,
          seller_uuid      CHAR(36) NOT NULL,
          item_base64      LONGTEXT NOT NULL,
          starting_price   DOUBLE NOT NULL,
          current_bid      DOUBLE NULL,
          current_bidder   CHAR(36) NULL,
          buy_now_price    DOUBLE NULL,
          created_at       BIGINT NOT NULL,
          end_at           BIGINT NOT NULL,
          status           VARCHAR(16) NOT NULL,
          version          INT NOT NULL
        );""";
    private static final String CREATE_BIDS_TABLE = """
        CREATE TABLE IF NOT EXISTS bids (
          bid_id           CHAR(36) PRIMARY KEY,
          auction_id       CHAR(36) NOT NULL,
          bidder_uuid      CHAR(36) NOT NULL,
          amount           DOUBLE NOT NULL,
          created_at       BIGINT NOT NULL,
          FOREIGN KEY (auction_id) REFERENCES auctions(auction_id) ON DELETE CASCADE
        );""";
    private static final String CREATE_AUCTIONS_INDEX = "CREATE INDEX IF NOT EXISTS idx_auctions_active ON auctions (status, end_at);";
    private static final String CREATE_BIDS_INDEX = "CREATE INDEX IF NOT EXISTS idx_bids_auction ON bids (auction_id);";

    private static final String INSERT_AUCTION = "INSERT INTO auctions (auction_id, seller_uuid, item_base64, starting_price, current_bid, current_bidder, buy_now_price, created_at, end_at, status, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final String INSERT_BID = "INSERT INTO bids (bid_id, auction_id, bidder_uuid, amount, created_at) VALUES (?, ?, ?, ?, ?);";
    private static final String FIND_BY_ID = "SELECT * FROM auctions WHERE auction_id = ?;";
    private static final String FIND_ACTIVE = "SELECT * FROM auctions WHERE status = 'ACTIVE' ORDER BY end_at ASC LIMIT ? OFFSET ?;";
    private static final String FIND_BY_SELLER = "SELECT * FROM auctions WHERE seller_uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?;";
    private static final String FIND_EXPIRED = "SELECT * FROM auctions WHERE status = 'ACTIVE' AND end_at <= ? LIMIT ?;";
    private static final String UPDATE_AUCTION_VERSIONED = "UPDATE auctions SET current_bid = ?, current_bidder = ?, status = ?, version = ? WHERE auction_id = ? AND version = ?;";
    //</editor-fold>

    public MySqlAuctionStorage(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    @Override
    public void init() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_AUCTIONS_TABLE);
            stmt.execute(CREATE_BIDS_TABLE);
            stmt.execute(CREATE_AUCTIONS_INDEX);
            stmt.execute(CREATE_BIDS_INDEX);
            plugin.getLogger().info("Auctions and Bids tables initialized successfully.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize auction storage tables.");
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Optional<Auction>> findById(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
                ps.setString(1, id.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapRowToAuction(rs));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Optional.empty();
        }, dbManager.getExecutor());
    }

    @Override
    public CompletableFuture<List<Auction>> findActive(int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            List<Auction> auctions = new ArrayList<>();
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(FIND_ACTIVE)) {
                ps.setInt(1, limit);
                ps.setInt(2, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        auctions.add(mapRowToAuction(rs));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return auctions;
        }, dbManager.getExecutor());
    }

    @Override
    public CompletableFuture<List<Auction>> findBySeller(UUID seller, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            List<Auction> auctions = new ArrayList<>();
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(FIND_BY_SELLER)) {
                ps.setString(1, seller.toString());
                ps.setInt(2, limit);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        auctions.add(mapRowToAuction(rs));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return auctions;
        }, dbManager.getExecutor());
    }

    @Override
    public CompletableFuture<Void> insertAuction(Auction a) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(INSERT_AUCTION)) {
                ps.setString(1, a.id().toString());
                ps.setString(2, a.seller().toString());
                ps.setString(3, a.item().getBase64());
                ps.setDouble(4, a.startingPrice());
                setNullableDouble(ps, 5, a.currentBid());
                setNullableUUID(ps, 6, a.currentBidder());
                setNullableDouble(ps, 7, a.buyNowPrice());
                ps.setLong(8, a.createdAt());
                ps.setLong(9, a.endAt());
                ps.setString(10, a.status().name());
                ps.setInt(11, a.version());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, dbManager.getExecutor());
    }

    @Override
    public CompletableFuture<Boolean> updateAuctionIfVersionMatches(Auction a, int expectedVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(UPDATE_AUCTION_VERSIONED)) {
                setNullableDouble(ps, 1, a.currentBid());
                setNullableUUID(ps, 2, a.currentBidder());
                ps.setString(3, a.status().name());
                ps.setInt(4, a.version());
                ps.setString(5, a.id().toString());
                ps.setInt(6, expectedVersion);
                int rowsAffected = ps.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, dbManager.getExecutor());
    }

    @Override
    public CompletableFuture<Void> insertBid(Bid b) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(INSERT_BID)) {
                ps.setString(1, b.id().toString());
                ps.setString(2, b.auctionId().toString());
                ps.setString(3, b.bidder().toString());
                ps.setDouble(4, b.amount());
                ps.setLong(5, b.createdAt());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, dbManager.getExecutor());
    }

    @Override
    public CompletableFuture<List<Auction>> findExpiredUpTo(long nowEpochMillis, int batchSize) {
        return CompletableFuture.supplyAsync(() -> {
            List<Auction> auctions = new ArrayList<>();
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(FIND_EXPIRED)) {
                ps.setLong(1, nowEpochMillis);
                ps.setInt(2, batchSize);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        auctions.add(mapRowToAuction(rs));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return auctions;
        }, dbManager.getExecutor());
    }

    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        return new Auction(
            UUID.fromString(rs.getString("auction_id")),
            UUID.fromString(rs.getString("seller_uuid")),
            SerializedItem.fromBase64(rs.getString("item_base64")),
            rs.getDouble("starting_price"),
            (Double) rs.getObject("current_bid"),
            rs.getString("current_bidder") != null ? UUID.fromString(rs.getString("current_bidder")) : null,
            (Double) rs.getObject("buy_now_price"),
            rs.getLong("created_at"),
            rs.getLong("end_at"),
            AuctionStatus.valueOf(rs.getString("status")),
            rs.getInt("version")
        );
    }

    private void setNullableDouble(PreparedStatement ps, int index, @Nullable Double value) throws SQLException {
        if (value != null) {
            ps.setDouble(index, value);
        } else {
            ps.setNull(index, Types.DOUBLE);
        }
    }

    private void setNullableUUID(PreparedStatement ps, int index, @Nullable UUID value) throws SQLException {
        if (value != null) {
            ps.setString(index, value.toString());
        } else {
            ps.setNull(index, Types.CHAR);
        }
    }
}
