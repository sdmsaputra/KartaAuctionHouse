package com.minekartastudio.kartaauctionhouse.storage.sql;

import com.google.gson.Gson;
import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.common.SerializedItem;
import com.minekartastudio.kartaauctionhouse.storage.TransactionStorage;
import com.minekartastudio.kartaauctionhouse.transaction.model.Transaction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

public class MySqlTransactionStorage implements TransactionStorage {

    private final KartaAuctionHouse plugin;
    private final DatabaseManager dbManager;
    private final Gson gson = new Gson();

    public MySqlTransactionStorage(KartaAuctionHouse plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    @Override
    public void init() {
        String createTableSql = "CREATE TABLE IF NOT EXISTS karta_auction_transactions (" +
                "transaction_id VARCHAR(36) NOT NULL PRIMARY KEY," +
                "auction_id VARCHAR(36) NOT NULL," +
                "seller_uuid VARCHAR(36) NOT NULL," +
                "buyer_uuid VARCHAR(36)," +
                "item_snapshot TEXT NOT NULL," +
                "final_price DECIMAL(19, 4)," +
                "status VARCHAR(20) NOT NULL," +
                "timestamp BIGINT NOT NULL" +
                ");";
        try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(createTableSql)) {
            ps.execute();
        } catch (SQLException e) {
            plugin.getLogger().severe("Could not create transaction log table!");
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> logTransaction(Transaction transaction) {
        return CompletableFuture.runAsync(() -> {
            String insertSql = "INSERT INTO karta_auction_transactions (transaction_id, auction_id, seller_uuid, buyer_uuid, item_snapshot, final_price, status, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(insertSql)) {
                ps.setString(1, transaction.transactionId().toString());
                ps.setString(2, transaction.auctionId().toString());
                ps.setString(3, transaction.sellerUuid().toString());
                ps.setString(4, transaction.buyerUuid() != null ? transaction.buyerUuid().toString() : null);
                ps.setString(5, gson.toJson(transaction.itemSnapshot()));
                if (transaction.finalPrice() != null) {
                    ps.setDouble(6, transaction.finalPrice());
                } else {
                    ps.setNull(6, java.sql.Types.DECIMAL);
                }
                ps.setString(7, transaction.status());
                ps.setLong(8, transaction.timestamp());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Could not log transaction " + transaction.transactionId());
                e.printStackTrace();
            }
        }, plugin.getServer().getScheduler().getMainThreadExecutor(plugin));
    }

    @Override
    public CompletableFuture<List<Transaction>> findTransactionsByPlayer(java.util.UUID playerId, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            List<Transaction> transactions = new java.util.ArrayList<>();
            String sql = "SELECT * FROM karta_auction_transactions WHERE seller_uuid = ? OR buyer_uuid = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?;";
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, playerId.toString());
                ps.setString(2, playerId.toString());
                ps.setInt(3, limit);
                ps.setInt(4, offset);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        transactions.add(mapRowToTransaction(rs));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return transactions;
        }, dbManager.getExecutor());
    }

    private Transaction mapRowToTransaction(java.sql.ResultSet rs) throws SQLException {
        return new Transaction(
                java.util.UUID.fromString(rs.getString("transaction_id")),
                java.util.UUID.fromString(rs.getString("auction_id")),
                java.util.UUID.fromString(rs.getString("seller_uuid")),
                rs.getString("buyer_uuid") != null ? java.util.UUID.fromString(rs.getString("buyer_uuid")) : null,
                gson.fromJson(rs.getString("item_snapshot"), SerializedItem.class),
                (Double) rs.getObject("final_price"),
                rs.getString("status"),
                rs.getLong("timestamp")
        );
    }
}
