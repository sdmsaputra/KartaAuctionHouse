package com.minekartastudio.kartaauctionhouse.storage;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.config.ConfigManager;
import com.minekartastudio.kartaauctionhouse.storage.sql.DatabaseManager;
import com.minekartastudio.kartaauctionhouse.storage.sql.MySqlAuctionStorage;
import com.minekartastudio.kartaauctionhouse.storage.sql.MySqlMailboxStorage;
import com.minekartastudio.kartaauctionhouse.storage.sql.MySqlTransactionStorage;
import com.minekartastudio.kartaauctionhouse.storage.yaml.YamlAuctionStorage;
import com.minekartastudio.kartaauctionhouse.storage.yaml.YamlMailboxStorage;
import com.minekartastudio.kartaauctionhouse.storage.yaml.YamlTransactionStorage;
import org.bukkit.plugin.java.JavaPlugin;

public class StorageFactory {

    public static AuctionStorage createAuctionStorage(JavaPlugin plugin, ConfigManager configManager, DatabaseManager dbManager) {
        String storageType = configManager.getConfig().getString("database.type", "YAML").toUpperCase();
        return switch (storageType) {
            case "MYSQL" -> new MySqlAuctionStorage(plugin, dbManager);
            case "YAML" -> new YamlAuctionStorage(plugin);
            default -> {
                plugin.getLogger().warning("Invalid storage type '" + storageType + "', defaulting to YAML.");
                yield new YamlAuctionStorage(plugin);
            }
        };
    }

    public static MailboxStorage createMailboxStorage(JavaPlugin plugin, ConfigManager configManager, DatabaseManager dbManager) {
        String storageType = configManager.getConfig().getString("database.type", "YAML").toUpperCase();
        return switch (storageType) {
            case "MYSQL" -> new MySqlMailboxStorage(plugin, dbManager);
            case "YAML" -> new YamlMailboxStorage(plugin);
            default -> {
                plugin.getLogger().warning("Invalid storage type '" + storageType + "', defaulting to YAML.");
                yield new YamlMailboxStorage(plugin);
            }
        };
    }

    public static TransactionStorage createTransactionStorage(JavaPlugin plugin, ConfigManager configManager, DatabaseManager dbManager) {
        String storageType = configManager.getConfig().getString("database.type", "YAML").toUpperCase();
        return switch (storageType) {
            case "MYSQL" -> new MySqlTransactionStorage((KartaAuctionHouse) plugin, dbManager);
            case "YAML" -> new YamlTransactionStorage(plugin);
            default -> {
                plugin.getLogger().warning("Invalid storage type '" + storageType + "', defaulting to YAML.");
                yield new YamlTransactionStorage(plugin);
            }
        };
    }
}
