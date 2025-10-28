package com.minekarta.playerauction.storage;

import com.minekarta.playerauction.storage.sqlite.SQLiteAuctionStorage;
import com.minekarta.playerauction.storage.sqlite.SQLiteTransactionStorage;
import org.bukkit.plugin.java.JavaPlugin;

public class StorageFactory {

    public static AuctionStorage createAuctionStorage(JavaPlugin plugin) {
        return new SQLiteAuctionStorage(plugin);
    }

    public static TransactionStorage createTransactionStorage(JavaPlugin plugin) {
        return new SQLiteTransactionStorage(plugin);
    }
}
