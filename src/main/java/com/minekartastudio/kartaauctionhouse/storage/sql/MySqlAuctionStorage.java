package com.minekartastudio.kartaauctionhouse.storage.sql;

import com.google.common.collect.ImmutableList;
import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.auction.model.AuctionStatus;
import com.minekartastudio.kartaauctionhouse.auction.model.Bid;
import com.minekartastudio.kartaauctionhouse.common.SerializedItem;
import com.minekartastudio.kartaauctionhouse.gui.model.AuctionCategory;
import com.minekartastudio.kartaauctionhouse.gui.model.SortOrder;
import com.minekartastudio.kartaauctionhouse.storage.AuctionStorage;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class MySqlAuctionStorage implements AuctionStorage {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;

    //<editor-fold desc="SQL Statements">
    private static final String CREATE_AUCTIONS_TABLE = """
        CREATE TABLE IF NOT EXISTS auctions (
          auction_id       CHAR(36) PRIMARY KEY,
          seller_uuid      CHAR(36) NOT NULL,
          item_base64      LONGTEXT NOT NULL,
          item_type        VARCHAR(64) NOT NULL,
          item_name        VARCHAR(255) NULL,
          starting_price   DOUBLE NOT NULL,
          current_bid      DOUBLE NULL,
          current_bidder   CHAR(36) NULL,
          buy_now_price    DOUBLE NULL,
          reserve_price    DOUBLE NULL,
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

    private static final String INSERT_AUCTION = "INSERT INTO auctions (auction_id, seller_uuid, item_base64, item_type, item_name, starting_price, current_bid, current_bidder, buy_now_price, reserve_price, created_at, end_at, status, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final String INSERT_BID = "INSERT INTO bids (bid_id, auction_id, bidder_uuid, amount, created_at) VALUES (?, ?, ?, ?, ?);";
    private static final String FIND_BY_ID = "SELECT * FROM auctions WHERE auction_id = ?;";
    private static final String FIND_BY_SELLER = "SELECT * FROM auctions WHERE seller_uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?;";
    private static final String COUNT_ACTIVE_BY_SELLER = "SELECT COUNT(*) FROM auctions WHERE seller_uuid = ? AND status = 'ACTIVE';";
    private static final String FIND_EXPIRED = "SELECT * FROM auctions WHERE status = 'ACTIVE' AND end_at <= ? LIMIT ?;";
    private static final String UPDATE_AUCTION_VERSIONED = "UPDATE auctions SET current_bid = ?, current_bidder = ?, status = ?, version = ?, end_at = ? WHERE auction_id = ? AND version = ?;";
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
    public CompletableFuture<List<Auction>> findActive(int limit, int offset, AuctionCategory category, SortOrder sortOrder, String searchQuery) {
        return CompletableFuture.supplyAsync(() -> {
            List<Auction> auctions = new ArrayList<>();
            StringBuilder sql = new StringBuilder("SELECT * FROM auctions WHERE status = 'ACTIVE'");
            List<Object> params = new ArrayList<>();

            // Category Filter
            if (category != null && category != AuctionCategory.ALL) {
                List<String> materials = getMaterialsForCategory(category).stream().map(Material::name).collect(Collectors.toList());
                if (!materials.isEmpty()) {
                    sql.append(" AND item_type IN (");
                    sql.append(String.join(",", java.util.Collections.nCopies(materials.size(), "?")));
                    sql.append(")");
                    params.addAll(materials);
                }
            }

            // Search Query Filter
            if (searchQuery != null && !searchQuery.isBlank()) {
                sql.append(" AND item_name LIKE ?");
                params.add("%" + searchQuery + "%");
            }

            // Sorting
            sql.append(" ORDER BY ");
            switch (sortOrder) {
                case PRICE_ASC -> sql.append("COALESCE(current_bid, starting_price) ASC");
                case PRICE_DESC -> sql.append("COALESCE(current_bid, starting_price) DESC");
                case NEWEST -> sql.append("created_at DESC");
                default -> sql.append("end_at ASC"); // TIME_LEFT
            }

            // Pagination
            sql.append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add(offset);

            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                for (int i = 0; i < params.size(); i++) {
                    ps.setObject(i + 1, params.get(i));
                }
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
    public CompletableFuture<Integer> countActiveBySeller(UUID sellerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(COUNT_ACTIVE_BY_SELLER)) {
                ps.setString(1, sellerId.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return 0;
        }, dbManager.getExecutor());
    }

    @Override
    public CompletableFuture<List<Auction>> findBySeller(UUID seller, int limit, int offset) {
        // This method signature is not in the interface, but let's keep it for now
        // as it might be used by MyListingsGui
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
                ps.setString(4, a.item().toItemStack().getType().name());
                ps.setString(5, a.item().toItemStack().hasItemMeta() && a.item().toItemStack().getItemMeta().hasDisplayName() ? a.item().toItemStack().getItemMeta().getDisplayName() : null);
                ps.setDouble(6, a.startingPrice());
                setNullableDouble(ps, 7, a.currentBid());
                setNullableUUID(ps, 8, a.currentBidder());
                setNullableDouble(ps, 9, a.buyNowPrice());
                setNullableDouble(ps, 10, a.reservePrice());
                ps.setLong(11, a.createdAt());
                ps.setLong(12, a.endAt());
                ps.setString(13, a.status().name());
                ps.setInt(14, a.version());
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
                ps.setLong(5, a.endAt());
                ps.setString(6, a.id().toString());
                ps.setInt(7, expectedVersion);
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
            (Double) rs.getObject("reserve_price"),
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

    private List<Material> getMaterialsForCategory(AuctionCategory category) {
        // This is a simplified mapping. A more robust solution might use item tags or a configurable list.
        return switch (category) {
            case WEAPONS -> ImmutableList.of(Material.DIAMOND_SWORD, Material.IRON_SWORD, Material.BOW, Material.CROSSBOW, Material.TRIDENT);
            case ARMOR -> ImmutableList.of(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, Material.ELYTRA);
            case BLOCKS -> ImmutableList.of(Material.DIRT, Material.COBBLESTONE, Material.OAK_LOG, Material.DIAMOND_BLOCK); // Example blocks
            case MISC -> ImmutableList.of(Material.TOTEM_OF_UNDYING, Material.ENCHANTED_BOOK, Material.POTION); // Example misc
            default -> ImmutableList.of();
        };
    }
}
