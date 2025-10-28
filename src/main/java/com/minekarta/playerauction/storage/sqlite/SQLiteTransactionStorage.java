package com.minekarta.playerauction.storage.sqlite;

import com.minekarta.playerauction.common.SerializedItem;
import com.minekarta.playerauction.storage.TransactionStorage;
import com.minekarta.playerauction.transaction.model.Transaction;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Executor;

public class SQLiteTransactionStorage implements TransactionStorage {

    private final JavaPlugin plugin;
    private final Executor executor;
    private final String databasePath;

    public SQLiteTransactionStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.executor = Executors.newSingleThreadExecutor();
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + "/auctions.db";
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
    }

    @Override
    public void init() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute(CREATE_TRANSACTIONS_TABLE);
            stmt.execute(CREATE_TRANSACTIONS_INDEX);
            stmt.execute(CREATE_TRANSACTIONS_PLAYER_INDEX);
            stmt.execute(CREATE_TRANSACTIONS_SELLER_INDEX);
            plugin.getLogger().info("SQLite transactions table initialized successfully.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLite transaction storage.");
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> logTransaction(Transaction transaction) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(INSERT_TRANSACTION)) {
                ps.setString(1, transaction.id().toString());
                ps.setString(2, transaction.auctionId().toString());
                ps.setString(3, transaction.actionType());
                ps.setString(4, transaction.actorUuid() != null ? transaction.actorUuid().toString() : null);
                ps.setString(5, transaction.sellerUuid() != null ? transaction.sellerUuid().toString() : null);
                if (transaction.amount() != null) {
                    ps.setDouble(6, transaction.amount());
                } else {
                    ps.setNull(6, Types.DOUBLE);
                }
                ps.setString(7, transaction.details());
                ps.setString(8, transaction.itemSnapshot() != null ? transaction.itemSnapshot().getBase64() : null);
                ps.setLong(9, transaction.timestamp());
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, executor);
    }

    @Override
    public CompletableFuture<List<Transaction>> findTransactionsByPlayer(UUID playerId, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            List<Transaction> transactions = new ArrayList<>();
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(GET_PLAYER_TRANSACTIONS)) {
                ps.setString(1, playerId.toString());
                ps.setString(2, playerId.toString());
                ps.setString(3, playerId.toString());
                ps.setInt(4, limit);
                ps.setInt(5, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        transactions.add(mapRowToTransaction(rs));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return transactions;
        }, executor);
    }

    private Transaction mapRowToTransaction(ResultSet rs) throws SQLException {
        String itemData = rs.getString("item_snapshot");
        SerializedItem itemSnapshot = null;
        if (itemData != null) {
            itemSnapshot = SerializedItem.fromBase64(itemData);
        }

        return new Transaction(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("auction_id")),
            rs.getString("action_type"),
            rs.getString("actor_uuid") != null ? UUID.fromString(rs.getString("actor_uuid")) : null,
            rs.getString("seller_uuid") != null ? UUID.fromString(rs.getString("seller_uuid")) : null,
            rs.getDouble("amount"),
            rs.getString("details"),
            itemSnapshot,
            rs.getLong("timestamp")
        );
    }

    // SQL Statements
    private static final String CREATE_TRANSACTIONS_TABLE = """
        CREATE TABLE IF NOT EXISTS transactions (
          id            TEXT PRIMARY KEY,
          auction_id    TEXT NOT NULL,
          action_type   TEXT NOT NULL,
          actor_uuid    TEXT NULL,
          seller_uuid   TEXT NULL,
          amount        REAL NULL,
          details       TEXT NOT NULL,
          item_snapshot TEXT NULL,
          timestamp     INTEGER NOT NULL
        )""";

    private static final String CREATE_TRANSACTIONS_INDEX = "CREATE INDEX IF NOT EXISTS idx_transactions_auction ON transactions (auction_id, timestamp DESC);";
    private static final String CREATE_TRANSACTIONS_PLAYER_INDEX = "CREATE INDEX IF NOT EXISTS idx_transactions_player ON transactions (actor_uuid, timestamp DESC);";
    private static final String CREATE_TRANSACTIONS_SELLER_INDEX = "CREATE INDEX IF NOT EXISTS idx_transactions_seller ON transactions (seller_uuid, timestamp DESC);";

    private static final String INSERT_TRANSACTION = "INSERT INTO transactions (id, auction_id, action_type, actor_uuid, seller_uuid, amount, details, item_snapshot, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
    private static final String GET_PLAYER_TRANSACTIONS = "SELECT * FROM transactions WHERE actor_uuid = ? OR seller_uuid = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?;";
}