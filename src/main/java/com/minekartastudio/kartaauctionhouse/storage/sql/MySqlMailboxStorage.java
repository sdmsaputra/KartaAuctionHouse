package com.minekartastudio.kartaauctionhouse.storage.sql;

import com.minekartastudio.kartaauctionhouse.common.SerializedItem;
import com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxEntry;
import com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxType;
import com.minekartastudio.kartaauctionhouse.storage.MailboxStorage;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MySqlMailboxStorage implements MailboxStorage {

    private final JavaPlugin plugin;
    private final DatabaseManager dbManager;

    //<editor-fold desc="SQL Statements">
    private static final String CREATE_MAILBOX_TABLE = """
        CREATE TABLE IF NOT EXISTS mailbox (
          entry_id         CHAR(36) PRIMARY KEY,
          owner_uuid       CHAR(36) NOT NULL,
          type             VARCHAR(8) NOT NULL, -- ITEM/MONEY
          item_base64      LONGTEXT NULL,
          amount           DOUBLE NULL,
          note             VARCHAR(255) NOT NULL,
          created_at       BIGINT NOT NULL,
          claimed          TINYINT(1) NOT NULL DEFAULT 0
        );""";
    private static final String CREATE_MAILBOX_INDEX = "CREATE INDEX IF NOT EXISTS idx_mailbox_owner_claimed ON mailbox (owner_uuid, claimed);";

    private static final String ENQUEUE_ENTRY = "INSERT INTO mailbox (entry_id, owner_uuid, type, item_base64, amount, note, created_at, claimed) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
    private static final String GET_UNCLAIMED = "SELECT * FROM mailbox WHERE owner_uuid = ? AND claimed = 0 ORDER BY created_at ASC;";
    private static final String MARK_CLAIMED = "UPDATE mailbox SET claimed = 1 WHERE entry_id = ?;";
    //</editor-fold>

    public MySqlMailboxStorage(JavaPlugin plugin, DatabaseManager dbManager) {
        this.plugin = plugin;
        this.dbManager = dbManager;
    }

    @Override
    public void init() {
        try (Connection conn = dbManager.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(CREATE_MAILBOX_TABLE);
            stmt.execute(CREATE_MAILBOX_INDEX);
            plugin.getLogger().info("Mailbox table initialized successfully.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize mailbox table.");
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> enqueue(MailboxEntry e) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(ENQUEUE_ENTRY)) {
                ps.setString(1, e.id().toString());
                ps.setString(2, e.owner().toString());
                ps.setString(3, e.type().name());
                setNullableString(ps, 4, e.item() != null ? e.item().getBase64() : null);
                setNullableDouble(ps, 5, e.amount());
                ps.setString(6, e.note());
                ps.setLong(7, e.createdAt());
                ps.setBoolean(8, e.claimed());
                ps.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }, dbManager.getExecutor());
    }

    @Override
    public CompletableFuture<List<MailboxEntry>> getUnclaimed(UUID owner) {
        return CompletableFuture.supplyAsync(() -> {
            List<MailboxEntry> entries = new ArrayList<>();
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(GET_UNCLAIMED)) {
                ps.setString(1, owner.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(mapRowToMailboxEntry(rs));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return entries;
        }, dbManager.getExecutor());
    }

    @Override
    public CompletableFuture<Boolean> markClaimed(UUID entryId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = dbManager.getConnection(); PreparedStatement ps = conn.prepareStatement(MARK_CLAIMED)) {
                ps.setString(1, entryId.toString());
                int rowsAffected = ps.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }, dbManager.getExecutor());
    }

    private MailboxEntry mapRowToMailboxEntry(ResultSet rs) throws SQLException {
        String itemBase64 = rs.getString("item_base64");
        return new MailboxEntry(
            UUID.fromString(rs.getString("entry_id")),
            UUID.fromString(rs.getString("owner_uuid")),
            MailboxType.valueOf(rs.getString("type")),
            itemBase64 != null ? SerializedItem.fromBase64(itemBase64) : null,
            (Double) rs.getObject("amount"),
            rs.getString("note"),
            rs.getLong("created_at"),
            rs.getBoolean("claimed")
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
}
