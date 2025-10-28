package com.minekarta.playerauction.storage.sqlite;

import com.minekarta.playerauction.auction.model.Auction;
import com.minekarta.playerauction.auction.model.AuctionStatus;
import com.minekarta.playerauction.common.SerializedItem;
import com.minekarta.playerauction.gui.model.AuctionCategory;
import com.minekarta.playerauction.gui.model.SortOrder;
import com.minekarta.playerauction.storage.AuctionStorage;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class SQLiteAuctionStorage implements AuctionStorage {

    private final JavaPlugin plugin;
    private final Executor executor;
    private final String databasePath;

    public SQLiteAuctionStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor();
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + "/auctions.db";
        createDatabaseDirectory();
    }

    private void createDatabaseDirectory() {
        java.io.File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    @Override
    public void init() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute(CREATE_AUCTIONS_TABLE);
            stmt.execute(CREATE_AUCTIONS_INDEX);
            plugin.getLogger().info("SQLite auctions table initialized successfully.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLite auction storage.");
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Optional<Auction>> findById(UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(FIND_BY_ID)) {
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
        }, executor);
    }

    @Override
    public CompletableFuture<List<Auction>> findActive(int limit, int offset, AuctionCategory category, SortOrder sortOrder, String searchQuery) {
        return CompletableFuture.supplyAsync(() -> {
            List<Auction> auctions = new ArrayList<>();

            StringBuilder sql = new StringBuilder("SELECT * FROM auctions WHERE status = 'ACTIVE'");
            List<Object> params = new ArrayList<>();

            // Add category filter if specified
            if (category != AuctionCategory.ALL) {
                sql.append(" AND item_type = ?");
                params.add(category.name());
            }

            // Add search filter if specified
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                sql.append(" AND (item_type LIKE ? OR item_name LIKE ?)");
                String searchPattern = "%" + searchQuery.toLowerCase() + "%";
                params.add(searchPattern);
                params.add(searchPattern);
            }

            // Add sorting
            sql.append(" ORDER BY ");
            switch (sortOrder) {
                case PRICE_ASC:
                    sql.append("price ASC");
                    break;
                case PRICE_DESC:
                    sql.append("price DESC");
                    break;
                case NEWEST:
                    sql.append("created_at DESC");
                    break;
                default: // TIME_LEFT
                    sql.append("end_at ASC");
            }

            // Add pagination
            sql.append(" LIMIT ? OFFSET ?");
            params.add(limit);
            params.add(offset);

            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {

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
        }, executor);
    }

    @Override
    public CompletableFuture<List<Auction>> findActiveAuctions(int page, int limit, AuctionCategory category, SortOrder sortOrder, String searchQuery) {
        return findActive(limit, (page - 1) * limit, category, sortOrder, searchQuery);
    }

    @Override
    public CompletableFuture<List<Auction>> findBySeller(UUID seller, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            List<Auction> auctions = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(FIND_BY_SELLER)) {
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
        }, executor);
    }

    @Override
    public CompletableFuture<List<Auction>> findPlayerHistory(UUID playerId, int page, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<Auction> auctions = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(FIND_PLAYER_HISTORY)) {
                ps.setString(1, playerId.toString());
                ps.setInt(2, limit);
                ps.setInt(3, (page - 1) * limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        auctions.add(mapRowToAuction(rs));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return auctions;
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> countActiveBySeller(UUID sellerId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(COUNT_ACTIVE_BY_SELLER)) {
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
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> countActiveAuctionsByPlayer(UUID playerId) {
        return countActiveBySeller(playerId);
    }

    @Override
    public CompletableFuture<Void> insertAuction(Auction a) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT_AUCTION)) {
                ps.setString(1, a.id().toString());
                ps.setString(2, a.seller().toString());
                ps.setString(3, a.item().getBase64());
                ps.setString(4, a.item().toItemStack().getType().name());
                ps.setString(5, a.item().toItemStack().hasItemMeta() && a.item().toItemStack().getItemMeta().hasDisplayName() ?
                        a.item().toItemStack().getItemMeta().getDisplayName() : null);
                ps.setDouble(6, a.price());
                setNullableDouble(ps, 7, a.buyNowPrice());
                setNullableDouble(ps, 8, a.reservePrice());
                ps.setLong(9, a.createdAt());
                ps.setLong(10, a.endAt());
                ps.setString(11, a.status().name());
                ps.setInt(12, a.version());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Boolean> updateAuctionIfVersionMatches(Auction a, int expectedVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(UPDATE_AUCTION_VERSIONED)) {
                ps.setString(1, a.status().name());
                ps.setInt(2, a.version());
                ps.setString(3, a.id().toString());
                ps.setInt(4, expectedVersion);
                int rowsAffected = ps.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<Auction>> findExpiredUpTo(long nowEpochMillis, int batchSize) {
        return CompletableFuture.supplyAsync(() -> {
            List<Auction> auctions = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(FIND_EXPIRED)) {
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
        }, executor);
    }

    private Auction mapRowToAuction(ResultSet rs) throws SQLException {
        return new Auction(
            UUID.fromString(rs.getString("auction_id")),
            UUID.fromString(rs.getString("seller_uuid")),
            SerializedItem.fromBase64(rs.getString("item_base64")),
            rs.getDouble("price"),
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

    private void setNullableString(PreparedStatement ps, int index, @Nullable String value) throws SQLException {
        if (value != null) {
            ps.setString(index, value);
        } else {
            ps.setNull(index, Types.VARCHAR);
        }
    }

    // SQL Statements
    private static final String CREATE_AUCTIONS_TABLE = """
        CREATE TABLE IF NOT EXISTS auctions (
          auction_id       TEXT PRIMARY KEY,
          seller_uuid      TEXT NOT NULL,
          item_base64      TEXT NOT NULL,
          item_type        TEXT NOT NULL,
          item_name        TEXT NULL,
          price            REAL NOT NULL,
          buy_now_price    REAL NULL,
          reserve_price    REAL NULL,
          created_at       INTEGER NOT NULL,
          end_at           INTEGER NOT NULL,
          status           TEXT NOT NULL,
          version          INTEGER NOT NULL
        )""";

    private static final String CREATE_AUCTIONS_INDEX = "CREATE INDEX IF NOT EXISTS idx_auctions_active ON auctions (status, end_at);";

    private static final String INSERT_AUCTION = "INSERT INTO auctions (auction_id, seller_uuid, item_base64, item_type, item_name, price, buy_now_price, reserve_price, created_at, end_at, status, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final String FIND_BY_ID = "SELECT * FROM auctions WHERE auction_id = ?;";
    private static final String FIND_BY_SELLER = "SELECT * FROM auctions WHERE seller_uuid = ? ORDER BY created_at DESC LIMIT ? OFFSET ?;";
    private static final String FIND_PLAYER_HISTORY = "SELECT * FROM auctions WHERE seller_uuid = ? AND status != 'ACTIVE' ORDER BY created_at DESC LIMIT ? OFFSET ?;";
    private static final String COUNT_ACTIVE_BY_SELLER = "SELECT COUNT(*) FROM auctions WHERE seller_uuid = ? AND status = 'ACTIVE';";
    private static final String FIND_EXPIRED = "SELECT * FROM auctions WHERE status = 'ACTIVE' AND end_at <= ? LIMIT ?;";
    private static final String UPDATE_AUCTION_VERSIONED = "UPDATE auctions SET status = ?, version = ? WHERE auction_id = ? AND version = ?;";
}