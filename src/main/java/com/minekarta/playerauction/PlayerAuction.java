package com.minekarta.playerauction;

import com.minekarta.playerauction.auction.AuctionService;
import com.minekarta.playerauction.commands.AuctionCommand;
import com.minekarta.playerauction.commands.AuctionTabCompleter;
import com.minekarta.playerauction.config.ConfigManager;
import com.minekarta.playerauction.economy.EconomyRouter;
import com.minekarta.playerauction.storage.AuctionStorage;
import com.minekarta.playerauction.storage.StorageFactory;
import com.minekarta.playerauction.tasks.AuctionExpirer;
import com.minekarta.playerauction.util.PlayerNameCache;
import com.minekarta.playerauction.notification.NotificationManager;
import com.minekarta.playerauction.transaction.TransactionLogger;
import com.minekarta.playerauction.storage.TransactionStorage;
import com.minekarta.playerauction.players.PlayerSettingsService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class PlayerAuction extends JavaPlugin {

    private ExecutorService asyncExecutor;
    private AuctionService auctionService;
    private EconomyRouter economyRouter;
    private ConfigManager configManager;
    private PlayerNameCache playerNameCache;
    private NotificationManager notificationManager;
    private TransactionLogger transactionLogger;
    private PlayerSettingsService playerSettingsService;

    @Override
    public void onEnable() {
        // 1. Initialize Config
        configManager = new ConfigManager(this);
        configManager.loadConfigs();

        // Initialize Services
        playerSettingsService = new PlayerSettingsService(this);
        notificationManager = new NotificationManager(this, configManager, playerSettingsService);

        // 2. Setup Thread Pool
        asyncExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat("PlayerAuction-Worker-%d").build()
        );

        // 3. Initialize SQLite Database
        AuctionStorage auctionStorage = StorageFactory.createAuctionStorage(this);
        TransactionStorage transactionStorage = StorageFactory.createTransactionStorage(this);

        // Run table creation async
        asyncExecutor.submit(() -> {
            auctionStorage.init();
            transactionStorage.init();
        });

        // 4. Initialize Economy
        economyRouter = new EconomyRouter(this, configManager);
        if (!economyRouter.hasService()) {
            getLogger().severe("Disabling PlayerAuction due to no economy provider being found.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 5. Initialize Caches & Services
        playerNameCache = new PlayerNameCache(asyncExecutor);
        transactionLogger = new TransactionLogger(transactionStorage);
        auctionService = new AuctionService(this, asyncExecutor, auctionStorage, economyRouter, configManager, notificationManager, transactionLogger);

        // 6. Register Commands & Listeners
        AuctionCommand commandExecutor = new AuctionCommand(this, auctionService, configManager, playerSettingsService);
        AuctionTabCompleter tabCompleter = new AuctionTabCompleter(this);

        this.getCommand("ah").setExecutor(commandExecutor);
        this.getCommand("ah").setTabCompleter(tabCompleter);
        this.getCommand("auction").setExecutor(commandExecutor);
        this.getCommand("auction").setTabCompleter(tabCompleter);
        this.getCommand("auctionhouse").setExecutor(commandExecutor);
        this.getCommand("auctionhouse").setTabCompleter(tabCompleter);

        // 7. Start Tasks
        new AuctionExpirer(auctionService).runTaskTimerAsynchronously(this, 20 * 30, 20 * 30); // Every 30 seconds

        getLogger().info("PlayerAuctions has been enabled!");
    }

    @Override
    public void onDisable() {
        if (asyncExecutor != null) {
            asyncExecutor.shutdownNow();
        }
        getLogger().info("PlayerAuctions has been disabled!");
    }

    // Public getters for services if needed by other parts of the plugin (e.g., GUIs)
    public AuctionService getAuctionService() { return auctionService; }
    public EconomyRouter getEconomyRouter() { return economyRouter; }
    public ConfigManager getConfigManager() { return configManager; }
    public PlayerNameCache getPlayerNameCache() { return playerNameCache; }
    public NotificationManager getNotificationManager() { return notificationManager; }
    public TransactionLogger getTransactionLogger() { return transactionLogger; }
    public PlayerSettingsService getPlayerSettingsService() { return playerSettingsService; }
    public ExecutorService getAsyncExecutor() { return asyncExecutor; }
}
