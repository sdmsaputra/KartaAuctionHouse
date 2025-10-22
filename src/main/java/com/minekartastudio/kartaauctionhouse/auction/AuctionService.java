package com.minekartastudio.kartaauctionhouse.auction;

import com.google.common.util.concurrent.Runnables;
import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.auction.model.AuctionStatus;
import com.minekartastudio.kartaauctionhouse.common.SerializedItem;
import com.minekartastudio.kartaauctionhouse.config.ConfigManager;
import com.minekartastudio.kartaauctionhouse.economy.EconomyRouter;
import com.minekartastudio.kartaauctionhouse.economy.EconomyService;
import com.minekartastudio.kartaauctionhouse.mailbox.MailboxService;
import com.minekartastudio.kartaauctionhouse.notification.NotificationManager;
import com.minekartastudio.kartaauctionhouse.storage.AuctionStorage;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class AuctionService {

    protected final JavaPlugin plugin;
    protected final Executor asyncExecutor;
    protected final AuctionStorage auctionStorage;
    protected final MailboxService mailboxService;
    protected final EconomyRouter economyRouter;
    protected final ConfigManager configManager;
    protected final NotificationManager notificationManager;
    protected final com.minekartastudio.kartaauctionhouse.transaction.TransactionLogger transactionLogger;

    private final ConcurrentHashMap<UUID, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public AuctionService(JavaPlugin plugin, Executor asyncExecutor, AuctionStorage auctionStorage, MailboxService mailboxService, EconomyRouter economyRouter, ConfigManager configManager, NotificationManager notificationManager, com.minekartastudio.kartaauctionhouse.transaction.TransactionLogger transactionLogger) {
        this.plugin = plugin;
        this.asyncExecutor = asyncExecutor;
        this.auctionStorage = auctionStorage;
        this.mailboxService = mailboxService;
        this.economyRouter = economyRouter;
        this.configManager = configManager;
        this.notificationManager = notificationManager;
        this.transactionLogger = transactionLogger;
    }

    public CompletableFuture<Boolean> createListing(Player player, ItemStack item, double price, long durationMillis) {
        SerializedItem serializedItem = SerializedItem.fromItemStack(item);
        Auction auction = new Auction(
                UUID.randomUUID(),
                player.getUniqueId(),
                serializedItem,
                price,
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

                double price = auction.price();

                return economy.withdraw(buyer.getUniqueId(), price, "Purchase from auction " + auction.id())
                        .thenCompose(withdrawn -> {
                            if (!withdrawn) {
                                buyer.sendMessage(configManager.getPrefixedMessage("errors.economy-fail"));
                                return CompletableFuture.completedFuture(false);
                            }

                            double tax = configManager.getConfig().getDouble("economy.tax.percent", 0);
                            double sellerAmount = price * (1 - tax / 100.0);

                            CompletableFuture<Void> sellerDeposit = mailboxService.sendMoney(auction.seller(), sellerAmount, "Sold item " + auction.item().toItemStack().getType());
                            CompletableFuture<Void> itemToBuyer = mailboxService.sendItem(buyer.getUniqueId(), auction.item(), "Purchased item " + auction.item().toItemStack().getType());

                            return CompletableFuture.allOf(sellerDeposit, itemToBuyer).thenCompose(v -> {
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
                                            if(updated) transactionLogger.log(auction, "SOLD", buyer.getUniqueId(), price);
                                            return updated;
                                        });
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

                return mailboxService.sendItem(player.getUniqueId(), auction.item(), "Auction cancelled")
                        .thenCompose(v -> {
                            Auction updatedAuction = auction.withStatus(AuctionStatus.CANCELLED).withIncrementedVersion();
                            return auctionStorage.updateAuctionIfVersionMatches(updatedAuction, auction.version())
                                    .thenApply(updated -> {
                                        if(updated) transactionLogger.log(updatedAuction, "CANCELLED", null, null);
                                        return updated;
                                    });
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
                        mailboxService.sendItem(current.seller(), current.item(), "Auction expired unsold");

                        Player seller = Bukkit.getPlayer(current.seller());
                        if (seller != null) {
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
                ).exceptionally(ex -> {
                    plugin.getLogger().severe("Error processing expired auction " + auction.id());
                    ex.printStackTrace();
                    return null;
                });
            }
        });
    }

    public CompletableFuture<List<Auction>> getActiveAuctions(int page, int pageSize, com.minekartastudio.kartaauctionhouse.gui.model.AuctionCategory category, com.minekartastudio.kartaauctionhouse.gui.model.SortOrder sortOrder, String searchQuery) {
        return auctionStorage.findActive(pageSize, (page - 1) * pageSize, category, sortOrder, searchQuery);
    }

    public CompletableFuture<Integer> countActiveAuctions(Player player) {
        return auctionStorage.countActiveBySeller(player.getUniqueId());
    }

    public CompletableFuture<List<Auction>> getAuctionsBySeller(UUID sellerId, int page, int pageSize) {
        return auctionStorage.findBySeller(sellerId, pageSize, (page - 1) * pageSize);
    }

    private <T> CompletableFuture<T> executeWithLock(UUID auctionId, Supplier<CompletableFuture<T>> action) {
        CompletableFuture<T> future = new CompletableFuture<>();
        asyncExecutor.execute(() -> {
            ReentrantLock lock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());
            lock.lock();
            try {
                action.get().whenComplete((result, ex) -> {
                    if (ex != null) {
                        future.completeExceptionally(ex);
                    } else {
                        future.complete(result);
                    }
                });
            } finally {
                lock.unlock();
                // To prevent memory leak, we can remove locks that are not contended.
                // A better approach would be a cache with weak values or timed eviction.
                // For now, this is a simple implementation.
                if (lock.getQueueLength() == 0) {
                    auctionLocks.remove(auctionId, lock);
                }
            }
        });
        return future;
    }
}
