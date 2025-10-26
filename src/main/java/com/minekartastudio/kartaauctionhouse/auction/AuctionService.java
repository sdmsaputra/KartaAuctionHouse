package com.minekartastudio.kartaauctionhouse.auction;

import com.google.common.util.concurrent.Runnables;
import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.auction.model.AuctionStatus;
import com.minekartastudio.kartaauctionhouse.auction.model.Bid;
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

    private final JavaPlugin plugin;
    private final Executor asyncExecutor;
    private final AuctionStorage auctionStorage;
    private final MailboxService mailboxService;
    private final EconomyRouter economyRouter;
    private final ConfigManager configManager;
    private final NotificationManager notificationManager;
    private final com.minekartastudio.kartaauctionhouse.transaction.TransactionLogger transactionLogger;

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

    public CompletableFuture<Boolean> createListing(Player player, ItemStack item, double startingPrice, Double buyNowPrice, Double reservePrice, long durationMillis) {
        SerializedItem serializedItem = SerializedItem.fromItemStack(item);
        Auction auction = new Auction(
                UUID.randomUUID(),
                player.getUniqueId(),
                serializedItem,
                startingPrice,
                null,
                null,
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

    public CompletableFuture<Boolean> placeBid(Player bidder, UUID auctionId, double bidAmount) {
        return executeWithLock(auctionId, () -> {
            EconomyService economy = economyRouter.getService();
            return auctionStorage.findById(auctionId).thenCompose(optAuction -> {
                if (optAuction.isEmpty() || optAuction.get().status() != AuctionStatus.ACTIVE) {
                    bidder.sendMessage(configManager.getPrefixedMessage("errors.auction-not-found"));
                    return CompletableFuture.completedFuture(false);
                }

                Auction auction = optAuction.get();

                if (auction.seller().equals(bidder.getUniqueId())) {
                    bidder.sendMessage(configManager.getPrefixedMessage("errors.cannot-bid-own"));
                    return CompletableFuture.completedFuture(false);
                }

                // Minimum Increment Bidding
                String incrementStr = configManager.getConfig().getString("auction.bid-increment", "5%");
                double minBid;
                double currentBid = auction.currentBid() != null ? auction.currentBid() : auction.startingPrice();

                if (incrementStr.endsWith("%")) {
                    double percent = Double.parseDouble(incrementStr.substring(0, incrementStr.length() - 1));
                    minBid = currentBid * (1 + percent / 100.0);
                } else {
                    double flatAmount = Double.parseDouble(incrementStr);
                    minBid = currentBid + flatAmount;
                }

                if (bidAmount < minBid) {
                    bidder.sendMessage(configManager.getPrefixedMessage("errors.bid-too-low", "{min_bid}", economy.format(minBid)));
                    return CompletableFuture.completedFuture(false);
                }

                return economy.withdraw(bidder.getUniqueId(), bidAmount, "Bid on auction " + auction.id())
                        .thenCompose(withdrawn -> {
                            if (!withdrawn) {
                                bidder.sendMessage(configManager.getPrefixedMessage("errors.economy-fail"));
                                return CompletableFuture.completedFuture(false);
                            }

                            CompletableFuture<Void> refundFuture = CompletableFuture.completedFuture(null);
                            if (auction.currentBidder() != null) {
                                Player previousBidder = Bukkit.getPlayer(auction.currentBidder());
                                if (previousBidder != null) {
                                    notificationManager.sendNotification(previousBidder, "auction.outbid", Map.of(
                                        "%player%", bidder.getName(),
                                        "%item%", auction.item().toItemStack().getType().toString()
                                    ));
                                }
                                refundFuture = mailboxService.sendMoney(auction.currentBidder(), auction.currentBid(), "Outbid on auction for " + auction.item().toItemStack().getType());
                            }

                            return refundFuture.thenCompose(v -> {
                                // Anti-Sniping Logic
                                long antiSnipingMillis = com.minekartastudio.kartaauctionhouse.util.DurationParser.parse(configManager.getConfig().getString("auction.anti-sniping-extension", "30s")).orElse(0L);
                                long newEndAt = auction.endAt();
                                if (antiSnipingMillis > 0 && (auction.endAt() - System.currentTimeMillis()) < antiSnipingMillis) {
                                    newEndAt = auction.endAt() + antiSnipingMillis;
                                }

                                Bid newBid = new Bid(UUID.randomUUID(), auctionId, bidder.getUniqueId(), bidAmount, System.currentTimeMillis());
                                Auction updatedAuction = auction.withNewBid(bidAmount, bidder.getUniqueId()).withNewEndAt(newEndAt).withIncrementedVersion();

                                return auctionStorage.updateAuctionIfVersionMatches(updatedAuction, auction.version())
                                        .thenCompose(updated -> {
                                            if (!updated) {
                                                // Optimistic lock failed, refund the new bidder and abort
                                                return economy.deposit(bidder.getUniqueId(), bidAmount, "Refund for failed bid").thenApply(v2 -> false);
                                            }
                                            return auctionStorage.insertBid(newBid).thenApply(v2 -> true);
                                        });
                            });
                        });
            });
        });
    }

    public CompletableFuture<Boolean> buyNow(Player buyer, UUID auctionId) {
        return executeWithLock(auctionId, () -> {
            EconomyService economy = economyRouter.getService();
            return auctionStorage.findById(auctionId).thenCompose(optAuction -> {
                if (optAuction.isEmpty() || optAuction.get().status() != AuctionStatus.ACTIVE || optAuction.get().buyNowPrice() == null) {
                    buyer.sendMessage(configManager.getPrefixedMessage("errors.auction-not-found"));
                    return CompletableFuture.completedFuture(false);
                }

                Auction auction = optAuction.get();

                if (auction.currentBidder() != null) {
                    buyer.sendMessage(configManager.getPrefixedMessage("errors.buy-now-fail", "Cannot use Buy Now on an auction that already has bids."));
                    return CompletableFuture.completedFuture(false);
                }

                double buyPrice = auction.buyNowPrice();

                return economy.withdraw(buyer.getUniqueId(), buyPrice, "BuyNow auction " + auction.id())
                        .thenCompose(withdrawn -> {
                            if (!withdrawn) {
                                buyer.sendMessage(configManager.getPrefixedMessage("errors.economy-fail"));
                                return CompletableFuture.completedFuture(false);
                            }

                            double tax = configManager.getConfig().getDouble("economy.tax.percent", 0);
                            double sellerAmount = buyPrice * (1 - tax / 100.0);

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
                                            if(updated) transactionLogger.log(auction, "SOLD", buyer.getUniqueId(), buyPrice);
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
                if (auction.status() != AuctionStatus.ACTIVE || auction.currentBidder() != null) {
                    player.sendMessage(configManager.getPrefixedMessage("errors.no-bids-to-cancel"));
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
                        Auction updated;
                        boolean reserveMet = current.currentBid() != null && (current.reservePrice() == null || current.currentBid() >= current.reservePrice());

                        if (current.currentBidder() != null && reserveMet) {
                            // Settle the auction - WINNER
                            double tax = configManager.getConfig().getDouble("auction.tax-percentage", 0);
                            double sellerAmount = current.currentBid() * (1 - tax / 100.0);
                            mailboxService.sendMoney(current.seller(), sellerAmount, "Sold item " + current.item().toItemStack().getType());
                            mailboxService.sendItem(current.currentBidder(), current.item(), "Won auction for " + current.item().toItemStack().getType());

                            Player winner = Bukkit.getPlayer(current.currentBidder());
                            if (winner != null) {
                                notificationManager.sendNotification(winner, "auction.win", Map.of(
                                    "%item%", current.item().toItemStack().getType().toString(),
                                    "%price%", economyRouter.getService().format(current.currentBid())
                                ));
                            }
                            Player seller = Bukkit.getPlayer(current.seller());
                            if (seller != null) {
                                notificationManager.sendNotification(seller, "auction.sold", Map.of(
                                    "%item%", current.item().toItemStack().getType().toString(),
                                    "%price%", economyRouter.getService().format(sellerAmount)
                                ));
                            }

                            updated = current.withStatus(AuctionStatus.FINISHED).withIncrementedVersion();
                        } else {
                            // Expired - either no bids, or reserve not met
                            if (current.currentBidder() != null) {
                                // Refund bidder because reserve was not met
                                mailboxService.sendMoney(current.currentBidder(), current.currentBid(), "Auction ended (reserve not met) for " + current.item().toItemStack().getType());
                            }
                            // Return item to seller
                            mailboxService.sendItem(current.seller(), current.item(), "Auction expired unsold (reserve not met or no bids)");

                            Player seller = Bukkit.getPlayer(current.seller());
                            if (seller != null) {
                                notificationManager.sendNotification(seller, "auction.expired", Map.of(
                                    "%item%", current.item().toItemStack().getType().toString()
                                ));
                            }

                            updated = current.withStatus(AuctionStatus.EXPIRED).withIncrementedVersion();
                        }
                        return auctionStorage.updateAuctionIfVersionMatches(updated, current.version())
                                .thenApply(isUpdated -> {
                                    if (isUpdated) {
                                        if (updated.status() == AuctionStatus.FINISHED) {
                                            transactionLogger.log(updated, "SOLD");
                                        } else {
                                            transactionLogger.log(updated, "EXPIRED");
                                        }
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
