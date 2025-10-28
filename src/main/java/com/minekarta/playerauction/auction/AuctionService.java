package com.minekarta.playerauction.auction;

import com.minekarta.playerauction.auction.model.Auction;
import com.minekarta.playerauction.auction.model.AuctionStatus;
import com.minekarta.playerauction.common.SerializedItem;
import com.minekarta.playerauction.config.ConfigManager;
import com.minekarta.playerauction.economy.EconomyRouter;
import com.minekarta.playerauction.economy.EconomyService;
import com.minekarta.playerauction.notification.NotificationManager;
import com.minekarta.playerauction.storage.AuctionStorage;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class AuctionService {

    private final JavaPlugin plugin;
    private final Executor asyncExecutor;
    private final AuctionStorage auctionStorage;
    private final EconomyRouter economyRouter;
    private final ConfigManager configManager;
    private final NotificationManager notificationManager;
    private final com.minekarta.playerauction.transaction.TransactionLogger transactionLogger;

    private final ConcurrentHashMap<UUID, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public AuctionService(JavaPlugin plugin, Executor asyncExecutor, AuctionStorage auctionStorage, EconomyRouter economyRouter, ConfigManager configManager, NotificationManager notificationManager, com.minekarta.playerauction.transaction.TransactionLogger transactionLogger) {
        this.plugin = plugin;
        this.asyncExecutor = asyncExecutor;
        this.auctionStorage = auctionStorage;
        this.economyRouter = economyRouter;
        this.configManager = configManager;
        this.notificationManager = notificationManager;
        this.transactionLogger = transactionLogger;
    }

    public CompletableFuture<Boolean> createListing(Player player, ItemStack item, double price, Double buyNowPrice, Double reservePrice, long durationMillis) {
        SerializedItem serializedItem = SerializedItem.fromItemStack(item);
        Auction auction = new Auction(
                UUID.randomUUID(),
                player.getUniqueId(),
                serializedItem,
                price,
                buyNowPrice,
                reservePrice,
                System.currentTimeMillis(),
                System.currentTimeMillis() + durationMillis,
                AuctionStatus.ACTIVE,
                1 // Initial version
        );

        return auctionStorage.insertAuction(auction).thenApply(v -> true).exceptionally(ex -> {
            plugin.getLogger().warning("Failed to create auction for " + player.getName());
            ex.printStackTrace();
            return false;
        });
    }

    public CompletableFuture<Boolean> buyItem(Player buyer, UUID auctionId) {
        return executeWithLock(auctionId, () -> {
            EconomyService economy = economyRouter.getService();
            return auctionStorage.findById(auctionId).thenCompose(optAuction -> {
                if (optAuction.isEmpty() || optAuction.get().status() != AuctionStatus.ACTIVE) {
                    buyer.sendMessage(configManager.getPrefixedMessage("errors.auction-not-found"));
                    return CompletableFuture.completedFuture(false);
                }

                Auction auction = optAuction.get();

                if (auction.seller().equals(buyer.getUniqueId())) {
                    buyer.sendMessage(configManager.getPrefixedMessage("errors.cannot-buy-own"));
                    return CompletableFuture.completedFuture(false);
                }

                double buyPrice = auction.price();

                return economy.withdraw(buyer.getUniqueId(), buyPrice, "Purchase item " + auction.id())
                        .thenCompose(withdrawn -> {
                            if (!withdrawn) {
                                buyer.sendMessage(configManager.getPrefixedMessage("errors.economy-fail"));
                                return CompletableFuture.completedFuture(false);
                            }

                            double tax = configManager.getConfig().getDouble("auction.tax-percentage", 0);
                            double sellerAmount = buyPrice * (1 - tax / 100.0);

                            // Give money to seller directly
                            return economy.deposit(auction.seller(), sellerAmount, "Sold item " + auction.item().toItemStack().getType())
                                    .thenCompose(v -> {
                                        // Give item to buyer directly
                                        ItemStack itemToGive = auction.item().toItemStack();
                                        if (buyer.getInventory().firstEmpty() == -1) {
                                            // Inventory full, drop item at player location
                                            buyer.getWorld().dropItem(buyer.getLocation(), itemToGive);
                                            buyer.sendMessage(configManager.getPrefixedMessage("inventory-full", "Inventory full, item dropped on ground"));
                                        } else {
                                            buyer.getInventory().addItem(itemToGive);
                                        }

                                        Player seller = Bukkit.getPlayer(auction.seller());
                                        if (seller != null) {
                                            notificationManager.sendNotification(seller, "auction.sold", Map.of(
                                                "%item%", auction.item().toItemStack().getType().toString(),
                                                "%price%", economy.format(sellerAmount)
                                            ));
                                        }

                                        Auction updatedAuction = auction.withStatus(AuctionStatus.FINISHED).withIncrementedVersion();
                                        return auctionStorage.updateAuctionIfVersionMatches(updatedAuction, auction.version())
                                                .thenApply(updated -> {
                                                    if(updated) transactionLogger.log(auction, "SOLD", buyer.getUniqueId(), buyPrice);
                                                    return updated;
                                                });
                                    })
                                    .exceptionally(ex -> {
                                        // Refund buyer if seller deposit failed
                                        return economy.deposit(buyer.getUniqueId(), buyPrice, "Refund - seller deposit failed")
                                                .thenCompose(v -> CompletableFuture.completedFuture(false))
                                                .join();
                                    });
                        });
            });
        });
    }

    public CompletableFuture<Boolean> cancelAuction(Player player, UUID auctionId) {
        return executeWithLock(auctionId, () ->
            auctionStorage.findById(auctionId).thenCompose(optAuction -> {
                if (optAuction.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }
                Auction auction = optAuction.get();
                if (!auction.seller().equals(player.getUniqueId())) {
                    player.sendMessage(configManager.getPrefixedMessage("errors.not-your-auction"));
                    return CompletableFuture.completedFuture(false);
                }
                if (auction.status() != AuctionStatus.ACTIVE) {
                    player.sendMessage(configManager.getPrefixedMessage("errors.auction-not-active"));
                    return CompletableFuture.completedFuture(false);
                }

                // Return item directly to seller
                ItemStack itemToReturn = auction.item().toItemStack();
                if (player.getInventory().firstEmpty() == -1) {
                    // Inventory full, drop item at player location
                    player.getWorld().dropItem(player.getLocation(), itemToReturn);
                    player.sendMessage(configManager.getPrefixedMessage("inventory-full", "Inventory full, item dropped on ground"));
                } else {
                    player.getInventory().addItem(itemToReturn);
                }

                Auction updatedAuction = auction.withStatus(AuctionStatus.CANCELLED).withIncrementedVersion();
                return auctionStorage.updateAuctionIfVersionMatches(updatedAuction, auction.version())
                        .thenApply(updated -> {
                            if(updated) transactionLogger.log(updatedAuction, "CANCELLED", null, null);
                            return updated;
                        });
            })
        );
    }

    public void processExpiredAuctions() {
        int batchSize = 200;
        auctionStorage.findExpiredUpTo(System.currentTimeMillis(), batchSize).thenAccept(expiredAuctions -> {
            if (!expiredAuctions.isEmpty()) {
                plugin.getLogger().info("Processing " + expiredAuctions.size() + " expired auctions...");
            }
            for (Auction auction : expiredAuctions) {
                executeWithLock(auction.id(), () ->
                    // Re-fetch to ensure it's still valid to process
                    auctionStorage.findById(auction.id()).thenCompose(optAuction -> {
                        if (optAuction.isEmpty() || optAuction.get().status() != AuctionStatus.ACTIVE) {
                            return CompletableFuture.completedFuture(false); // Already processed
                        }

                        Auction current = optAuction.get();

                        // Expired - return item to seller
                        Player seller = Bukkit.getPlayer(current.seller());
                        if (seller != null && seller.isOnline()) {
                            ItemStack itemToReturn = current.item().toItemStack();
                            if (seller.getInventory().firstEmpty() == -1) {
                                // Inventory full, drop item at seller location
                                seller.getWorld().dropItem(seller.getLocation(), itemToReturn);
                                seller.sendMessage(configManager.getPrefixedMessage("inventory-full", "Inventory full, expired item dropped on ground"));
                            } else {
                                seller.getInventory().addItem(itemToReturn);
                            }
                            notificationManager.sendNotification(seller, "auction.expired", Map.of(
                                "%item%", current.item().toItemStack().getType().toString()
                            ));
                        }

                        Auction updated = current.withStatus(AuctionStatus.EXPIRED).withIncrementedVersion();
                        return auctionStorage.updateAuctionIfVersionMatches(updated, current.version())
                                .thenApply(isUpdated -> {
                                    if (isUpdated) {
                                        transactionLogger.log(updated, "EXPIRED");
                                    }
                                    return isUpdated;
                                });
                    })
                );
            }
        });
    }

    // Utility methods
    private <T> CompletableFuture<T> executeWithLock(UUID auctionId, Supplier<CompletableFuture<T>> operation) {
        ReentrantLock lock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
        return CompletableFuture.supplyAsync(() -> {
            lock.lock();
            try {
                return operation.get().join();
            } finally {
                lock.unlock();
                auctionLocks.remove(auctionId, lock);
            }
        }, asyncExecutor);
    }

    // Getters
    public CompletableFuture<List<Auction>> getActiveAuctions(int page, int limit, com.minekarta.playerauction.gui.model.AuctionCategory category, com.minekarta.playerauction.gui.model.SortOrder sortOrder, String searchQuery) {
        return auctionStorage.findActiveAuctions(page, limit, category, sortOrder, searchQuery);
    }

    public CompletableFuture<List<Auction>> getPlayerAuctions(UUID playerId, int page, int limit) {
        return auctionStorage.findBySeller(playerId, page, limit);
    }

    public CompletableFuture<List<Auction>> getPlayerHistory(UUID playerId, int page, int limit) {
        return auctionStorage.findPlayerHistory(playerId, page, limit);
    }

    public CompletableFuture<Integer> getPlayerActiveAuctionCount(UUID playerId) {
        return auctionStorage.countActiveAuctionsByPlayer(playerId);
    }

    // Public getters for fields needed by other classes
    public ConfigManager getConfigManager() { return configManager; }
    public EconomyRouter getEconomyRouter() { return economyRouter; }
    public Executor getAsyncExecutor() { return asyncExecutor; }
    public AuctionStorage getAuctionStorage() { return auctionStorage; }
    public JavaPlugin getPlugin() { return plugin; }
}