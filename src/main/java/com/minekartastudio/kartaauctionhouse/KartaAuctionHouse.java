package com.minekartastudio.kartaauctionhouse;

import com.minekartastudio.kartaauctionhouse.auction.AuctionService;
import com.minekartastudio.kartaauctionhouse.commands.AuctionCommand;
import com.minekartastudio.kartaauctionhouse.config.ConfigManager;
import com.minekartastudio.kartaauctionhouse.economy.EconomyRouter;
import com.minekartastudio.kartaauctionhouse.mailbox.MailboxService;
import com.minekartastudio.kartaauctionhouse.storage.AuctionStorage;
import com.minekartastudio.kartaauctionhouse.storage.MailboxStorage;
import com.minekartastudio.kartaauctionhouse.storage.sql.DatabaseManager;
import com.minekartastudio.kartaauctionhouse.storage.sql.MySqlAuctionStorage;
import com.minekartastudio.kartaauctionhouse.storage.sql.MySqlMailboxStorage;
import com.minekartastudio.kartaauctionhouse.storage.sql.MySqlTransactionStorage;
import com.minekartastudio.kartaauctionhouse.tasks.AuctionExpirer;
import com.minekartastudio.kartaauctionhouse.util.PlayerNameCache;
import com.minekartastudio.kartaauctionhouse.notification.NotificationManager;
import com.minekartastudio.kartaauctionhouse.transaction.TransactionLogger;
import com.minekartastudio.kartaauctionhouse.storage.TransactionStorage;
import com.minekartastudio.kartaauctionhouse.gui.SearchInputListener;
import com.minekartastudio.kartaauctionhouse.players.PlayerSettingsService;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class KartaAuctionHouse extends JavaPlugin {

    private ExecutorService asyncExecutor;
    private DatabaseManager databaseManager;
    private AuctionService auctionService;
    private MailboxService mailboxService;
    private EconomyRouter economyRouter;
    private ConfigManager configManager;
    private PlayerNameCache playerNameCache;
    private NotificationManager notificationManager;
    private TransactionLogger transactionLogger;
    private SearchInputListener searchInputListener;
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
            new ThreadFactoryBuilder().setNameFormat("KartaAuctionHouse-Worker-%d").build()
        );

        // 3. Initialize Database
        databaseManager = new DatabaseManager(this, configManager);
        AuctionStorage auctionStorage = new MySqlAuctionStorage(this, databaseManager);
        MailboxStorage mailboxStorage = new MySqlMailboxStorage(this, databaseManager);
        TransactionStorage transactionStorage = new MySqlTransactionStorage(this, databaseManager);

        // Run table creation async
        asyncExecutor.submit(() -> {
            auctionStorage.init();
            mailboxStorage.init();
            transactionStorage.init();
        });

        // 4. Initialize Economy
        economyRouter = new EconomyRouter(this, configManager);
        if (!economyRouter.hasService()) {
            getLogger().severe("Disabling KartaAuctionHouse due to no economy provider being found.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 5. Initialize Caches & Services
        playerNameCache = new PlayerNameCache(asyncExecutor);
        mailboxService = new MailboxService(this, mailboxStorage, economyRouter, configManager);
        transactionLogger = new TransactionLogger(transactionStorage);
        auctionService = new AuctionService(this, asyncExecutor, auctionStorage, mailboxService, economyRouter, configManager, notificationManager, transactionLogger);

        // 6. Register Commands & Listeners
        this.getCommand("ah").setExecutor(new AuctionCommand(this, auctionService, mailboxService, configManager, playerSettingsService));
        this.searchInputListener = new SearchInputListener(this);

        // 7. Start Tasks
        new AuctionExpirer(auctionService).runTaskTimerAsynchronously(this, 20 * 30, 20 * 30); // Every 30 seconds

        getLogger().info("KartaAuctionHouse has been enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        if (asyncExecutor != null) {
            asyncExecutor.shutdownNow();
        }
        getLogger().info("KartaAuctionHouse has been disabled!");
    }

    // Public getters for services if needed by other parts of the plugin (e.g., GUIs)
    public AuctionService getAuctionService() { return auctionService; }
    public MailboxService getMailboxService() { return mailboxService; }
    public EconomyRouter getEconomyRouter() { return economyRouter; }
    public ConfigManager getConfigManager() { return configManager; }
    public PlayerNameCache getPlayerNameCache() { return playerNameCache; }
    public NotificationManager getNotificationManager() { return notificationManager; }
    public TransactionLogger getTransactionLogger() { return transactionLogger; }
    public SearchInputListener getSearchInputListener() { return searchInputListener; }
    public PlayerSettingsService getPlayerSettingsService() { return playerSettingsService; }
}
