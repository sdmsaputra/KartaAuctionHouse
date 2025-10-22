package com.minekartastudio.kartaauctionhouse.gui.improved;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.auction.AuctionService;
import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.auction.model.AuctionStatus;
import com.minekartastudio.kartaauctionhouse.config.ConfigManager;
import com.minekartastudio.kartaauctionhouse.economy.EconomyRouter;
import com.minekartastudio.kartaauctionhouse.mailbox.MailboxService;
import com.minekartastudio.kartaauctionhouse.notification.NotificationManager;
import com.minekartastudio.kartaauctionhouse.storage.AuctionStorage;
import com.minekartastudio.kartaauctionhouse.transaction.TransactionLogger;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Improved AuctionService with better error handling, caching, and performance optimizations
 */
public class ImprovedAuctionService extends AuctionService {

    // Cache for frequently accessed auctions
    private final ConcurrentHashMap<UUID, Auction> auctionCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> cacheTimestamps = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRY_MS = 30000; // 30 seconds

    // Rate limiting for purchases
    private final ConcurrentHashMap<UUID, Long> purchaseCooldowns = new ConcurrentHashMap<>();
    private static final long PURCHASE_COOLDOWN_MS = 1000; // 1 second

    // Metrics
    private volatile long totalPurchases = 0;
    private volatile long successfulPurchases = 0;
    private volatile long failedPurchases = 0;

    public ImprovedAuctionService(KartaAuctionHouse plugin, Executor asyncExecutor, AuctionStorage auctionStorage,
                                 MailboxService mailboxService, EconomyRouter economyRouter,
                                 ConfigManager configManager, NotificationManager notificationManager,
                                 TransactionLogger transactionLogger) {
        super(plugin, asyncExecutor, auctionStorage, mailboxService, economyRouter, configManager, notificationManager, transactionLogger);
    }

    /**
     * Enhanced buyItem method with rate limiting and validation
     */
    @Override
    public CompletableFuture<Boolean> buyItem(Player buyer, UUID auctionId) {
        // Rate limiting check
        Long lastPurchase = purchaseCooldowns.get(buyer.getUniqueId());
        long currentTime = System.currentTimeMillis();
        if (lastPurchase != null && (currentTime - lastPurchase) < PURCHASE_COOLDOWN_MS) {
            buyer.sendMessage(configManager.getPrefixedMessage("errors.purchase-too-fast",
                "&cPlease wait before making another purchase."));
            return CompletableFuture.completedFuture(false);
        }

        // Update cooldown
        purchaseCooldowns.put(buyer.getUniqueId(), currentTime);
        totalPurchases++;

        // Use cached auction if available and fresh
        Auction cachedAuction = getCachedAuction(auctionId);
        if (cachedAuction != null) {
            return processPurchase(buyer, cachedAuction);
        }

        // Fallback to original method
        return super.buyItem(buyer, auctionId).whenComplete((result, throwable) -> {
            if (throwable != null) {
                failedPurchases++;
                plugin.getLogger().warning("Purchase failed for auction " + auctionId + ": " + throwable.getMessage());
            } else if (result) {
                successfulPurchases++;
                // Update cache
                invalidateCache(auctionId);
            } else {
                failedPurchases++;
            }
        });
    }

    /**
     * Create listing with enhanced validation
     */
    @Override
    public CompletableFuture<Boolean> createListing(Player player, ItemStack item, double price, long durationMillis) {
        // Enhanced validation
        if (price <= 0) {
            player.sendMessage(configManager.getPrefixedMessage("errors.invalid-price", "&cPrice must be greater than 0."));
            return CompletableFuture.completedFuture(false);
        }

        if (price > configManager.getConfig().getDouble("auction.max-price", 1000000.0)) {
            player.sendMessage(configManager.getPrefixedMessage("errors.price-too-high",
                "&cPrice cannot exceed {max}.", "{max}", String.valueOf(configManager.getConfig().getDouble("auction.max-price"))));
            return CompletableFuture.completedFuture(false);
        }

        if (durationMillis < configManager.getConfig().getLong("auction.min-duration", 60000)) { // 1 minute minimum
            player.sendMessage(configManager.getPrefixedMessage("errors.duration-too-short", "&cDuration is too short."));
            return CompletableFuture.completedFuture(false);
        }

        if (durationMillis > configManager.getConfig().getLong("auction.max-duration", 604800000)) { // 7 days maximum
            player.sendMessage(configManager.getPrefixedMessage("errors.duration-too-long", "&cDuration is too long."));
            return CompletableFuture.completedFuture(false);
        }

        // Check if player is blacklisted
        if (isPlayerBlacklisted(player.getUniqueId())) {
            player.sendMessage(configManager.getPrefixedMessage("errors.blacklisted", "&cYou are not allowed to create auctions."));
            return CompletableFuture.completedFuture(false);
        }

        return super.createListing(player, item, price, durationMillis);
    }

    /**
     * Process purchase with enhanced error handling
     */
    private CompletableFuture<Boolean> processPurchase(Player buyer, Auction auction) {
        // Additional validation
        if (auction.status() != AuctionStatus.ACTIVE) {
            buyer.sendMessage(configManager.getPrefixedMessage("errors.auction-not-active", "&cThis auction is no longer active."));
            return CompletableFuture.completedFuture(false);
        }

        if (auction.seller().equals(buyer.getUniqueId())) {
            buyer.sendMessage(configManager.getPrefixedMessage("errors.cannot-buy-own", "&cYou cannot buy your own auction."));
            return CompletableFuture.completedFuture(false);
        }

        if (System.currentTimeMillis() > auction.endAt()) {
            buyer.sendMessage(configManager.getPrefixedMessage("errors.auction-expired", "&cThis auction has expired."));
            return CompletableFuture.completedFuture(false);
        }

        // Check buyer's balance more efficiently
        double buyerBalance = economyRouter.getService().getBalance(buyer.getUniqueId()).join();
        if (buyerBalance < auction.price()) {
            buyer.sendMessage(configManager.getPrefixedMessage("errors.insufficient-funds",
                "&cYou don't have enough money. Need: {needed}, Have: {have}",
                "{needed}", economyRouter.getService().format(auction.price()),
                "{have}", economyRouter.getService().format(buyerBalance)));
            return CompletableFuture.completedFuture(false);
        }

        // Process the purchase
        return super.buyItem(buyer, auction.id());
    }

    /**
     * Get auction with caching
     */
    private Auction getCachedAuction(UUID auctionId) {
        Long timestamp = cacheTimestamps.get(auctionId);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_EXPIRY_MS) {
            return auctionCache.get(auctionId);
        }
        return null;
    }

    /**
     * Invalidate cache for specific auction
     */
    private void invalidateCache(UUID auctionId) {
        auctionCache.remove(auctionId);
        cacheTimestamps.remove(auctionId);
    }

    /**
     * Check if player is blacklisted
     */
    private boolean isPlayerBlacklisted(UUID playerId) {
        return configManager.getConfig().getStringList("blacklisted-players").contains(playerId.toString());
    }

    /**
     * Get performance metrics
     */
    public Map<String, Long> getMetrics() {
        return Map.of(
            "total_purchases", totalPurchases,
            "successful_purchases", successfulPurchases,
            "failed_purchases", failedPurchases,
            "success_rate", totalPurchases > 0 ? (successfulPurchases * 100 / totalPurchases) : 0L
        );
    }

    /**
     * Clear old cache entries
     */
    public void cleanupCache() {
        long currentTime = System.currentTimeMillis();
        cacheTimestamps.entrySet().removeIf(entry ->
            (currentTime - entry.getValue()) > CACHE_EXPIRY_MS);
        auctionCache.entrySet().removeIf(entry ->
            !cacheTimestamps.containsKey(entry.getKey()));
    }

    /**
     * Preload popular auctions into cache
     */
    public CompletableFuture<Void> preloadCache() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Load recent auctions into cache
                List<Auction> recentAuctions = auctionStorage.findActive(20, 0, null, null, null).join();
                for (Auction auction : recentAuctions) {
                    auctionCache.put(auction.id(), auction);
                    cacheTimestamps.put(auction.id(), System.currentTimeMillis());
                }
                plugin.getLogger().info("Preloaded " + recentAuctions.size() + " auctions into cache");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to preload auction cache: " + e.getMessage());
            }
        }, asyncExecutor);
    }
}