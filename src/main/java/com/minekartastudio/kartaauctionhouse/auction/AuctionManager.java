package com.minekartastudio.kartaauctionhouse.auction;

import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.auction.model.AuctionStatus;
import com.minekartastudio.kartaauctionhouse.auction.model.Bid;
import com.minekartastudio.kartaauctionhouse.config.ConfigManager;
import com.minekartastudio.kartaauctionhouse.economy.EconomyRouter;
import com.minekartastudio.kartaauctionhouse.mailbox.MailboxService;
import com.minekartastudio.kartaauctionhouse.notification.NotificationManager;
import com.minekartastudio.kartaauctionhouse.storage.AuctionStorage;
import com.minekartastudio.kartaauctionhouse.transaction.TransactionLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Advanced Auction Manager that handles complex auction operations and logic
 */
public class AuctionManager {
    
    private final AuctionService auctionService;
    private final AuctionStorage auctionStorage;
    private final MailboxService mailboxService;
    private final EconomyRouter economyRouter;
    private final ConfigManager configManager;
    private final NotificationManager notificationManager;
    private final TransactionLogger transactionLogger;
    private final JavaPlugin plugin;
    private final Executor asyncExecutor;
    
    public AuctionManager(AuctionService auctionService, 
                         AuctionStorage auctionStorage,
                         MailboxService mailboxService,
                         EconomyRouter economyRouter,
                         ConfigManager configManager,
                         NotificationManager notificationManager,
                         TransactionLogger transactionLogger,
                         JavaPlugin plugin,
                         Executor asyncExecutor) {
        this.auctionService = auctionService;
        this.auctionStorage = auctionStorage;
        this.mailboxService = mailboxService;
        this.economyRouter = economyRouter;
        this.configManager = configManager;
        this.notificationManager = notificationManager;
        this.transactionLogger = transactionLogger;
        this.plugin = plugin;
        this.asyncExecutor = asyncExecutor;
    }
    
    /**
     * Creates an auction with enhanced validation and processing
     */
    public CompletableFuture<Boolean> createEnhancedListing(Player player, org.bukkit.inventory.ItemStack item, 
                                                           double startingPrice, Double buyNowPrice, 
                                                           Double reservePrice, long durationMillis) {
        // Validation logic
        if (!validateAuctionParameters(startingPrice, buyNowPrice, reservePrice, durationMillis)) {
            player.sendMessage(configManager.getPrefixedMessage("errors.invalid-auction-params"));
            return CompletableFuture.completedFuture(false);
        }
        
        // Check if player is at auction limit
        int maxAuctions = configManager.getConfig().getInt("auction.max-auctions-per-player", 5);
        return auctionService.countActiveAuctions(player).thenCompose(count -> {
            if (count >= maxAuctions) {
                player.sendMessage(configManager.getPrefixedMessage("errors.listing-limit-reached", 
                    "{limit}", String.valueOf(maxAuctions)));
                return CompletableFuture.completedFuture(false);
            }
            
            // Create the listing
            return auctionService.createListing(player, item, startingPrice, buyNowPrice, reservePrice, durationMillis);
        });
    }
    
    /**
     * Places a bid with enhanced validation and anti-sniping
     */
    public CompletableFuture<Boolean> placeEnhancedBid(Player bidder, UUID auctionId, double bidAmount) {
        // For backward compatibility, assume no debug mode if AdminCommand isn't provided
        return placeEnhancedBid(bidder, auctionId, bidAmount, (KartaAuctionHouse) null);
    }
    
    /**
     * Places a bid with enhanced validation and anti-sniping
     */
    public CompletableFuture<Boolean> placeEnhancedBid(Player bidder, UUID auctionId, double bidAmount, com.minekartastudio.kartaauctionhouse.commands.AdminCommand adminCommand) {
        // For backward compatibility with AdminCommand
        boolean isInDebugMode = false;
        if (adminCommand != null && plugin instanceof KartaAuctionHouse) {
            // Use the main plugin's debug check
            isInDebugMode = ((KartaAuctionHouse) plugin).isInDebugMode(bidder);
        }
        
        return placeEnhancedBid(bidder, auctionId, bidAmount, isInDebugMode ? (KartaAuctionHouse) plugin : null);
    }
    
    /**
     * Places a bid with enhanced validation and anti-sniping
     */
    public CompletableFuture<Boolean> placeEnhancedBid(Player bidder, UUID auctionId, double bidAmount, KartaAuctionHouse plugin) {
        return auctionService.executeWithLock(auctionId, () -> {
            return auctionStorage.findById(auctionId).thenCompose(optAuction -> {
                if (optAuction.isEmpty() || optAuction.get().status() != AuctionStatus.ACTIVE) {
                    bidder.sendMessage(configManager.getPrefixedMessage("errors.auction-not-found"));
                    return CompletableFuture.completedFuture(false);
                }

                Auction auction = optAuction.get();

                if (auction.seller().equals(bidder.getUniqueId()) && (plugin == null || !plugin.isInDebugMode(bidder))) {
                    bidder.sendMessage(configManager.getPrefixedMessage("errors.cannot-bid-own"));
                    return CompletableFuture.completedFuture(false);
                }
                
                // Validate bid amount against minimum increment
                double minBid = calculateMinimumBid(auction);
                if (bidAmount < minBid) {
                    bidder.sendMessage(configManager.getPrefixedMessage("errors.bid-too-low", 
                        "{min_bid}", economyRouter.getService().format(minBid)));
                    return CompletableFuture.completedFuture(false);
                }
                
                // Check if bidder has enough money
                return economyRouter.getService().getBalance(bidder.getUniqueId()).thenCompose(bidderBalance -> {
                    if (bidderBalance < bidAmount) {
                        bidder.sendMessage(configManager.getPrefixedMessage("errors.not-enough-money"));
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    // All validations passed, place the bid
                    return executeBid(auction, bidder, bidAmount);
                });
            });
        });
    }
    
    /**
     * Executes a bid after all validations pass
     */
    private CompletableFuture<Boolean> executeBid(Auction auction, Player bidder, double bidAmount) {
        return economyRouter.getService().withdraw(bidder.getUniqueId(), bidAmount, 
                "Bid on auction " + auction.id()).thenCompose(withdrawn -> {
            if (!withdrawn) {
                bidder.sendMessage(configManager.getPrefixedMessage("errors.economy-fail"));
                return CompletableFuture.completedFuture(false);
            }

            CompletableFuture<Void> refundFuture = CompletableFuture.completedFuture(null);
            if (auction.currentBidder() != null) {
                // Refund previous bidder
                Player previousBidder = Bukkit.getPlayer(auction.currentBidder());
                if (previousBidder != null) {
                    notificationManager.sendNotification(previousBidder, "auction.outbid", Map.of(
                        "%player%", bidder.getName(),
                        "%item%", auction.item().toItemStack().getType().toString()
                    ));
                }
                refundFuture = mailboxService.sendMoney(auction.currentBidder(), auction.currentBid(), 
                    "Outbid on auction for " + auction.item().toItemStack().getType());
            }

            return refundFuture.thenCompose(v -> {
                // Anti-Sniping Logic
                long antiSnipingMillis = com.minekartastudio.kartaauctionhouse.util.DurationParser
                    .parse(configManager.getConfig().getString("auction.anti-sniping-extension", "30s"))
                    .orElse(0L);
                long newEndAt = auction.endAt();
                long timeRemaining = auction.endAt() - System.currentTimeMillis();
                
                if (antiSnipingMillis > 0 && timeRemaining < antiSnipingMillis) {
                    newEndAt = auction.endAt() + antiSnipingMillis;
                    // Notify about extension
                    bidder.sendMessage(configManager.getPrefixedMessage("info.anti_sniping_extended", 
                        "{extension}", 
                        com.minekartastudio.kartaauctionhouse.util.TimeUtil.formatDuration(antiSnipingMillis)));
                }

                // Create and save bid
                Bid newBid = new Bid(UUID.randomUUID(), auction.id(), bidder.getUniqueId(), bidAmount, 
                    System.currentTimeMillis());
                Auction updatedAuction = auction.withNewBid(bidAmount, bidder.getUniqueId())
                    .withNewEndAt(newEndAt).withIncrementedVersion();

                return auctionStorage.updateAuctionIfVersionMatches(updatedAuction, auction.version())
                    .thenCompose(updated -> {
                        if (!updated) {
                            // Optimistic lock failed, refund the new bidder and abort
                            return economyRouter.getService().deposit(bidder.getUniqueId(), bidAmount, 
                                "Refund for failed bid").thenApply(v2 -> false);
                        }
                        return auctionStorage.insertBid(newBid).thenApply(v2 -> true);
                    });
            });
        });
    }
    
    /**
     * Calculates the minimum acceptable bid based on current bid and increment settings
     */
    private double calculateMinimumBid(Auction auction) {
        String incrementStr = configManager.getConfig().getString("auction.bid-increment", "5%");
        double currentBid = auction.currentBid() != null ? auction.currentBid() : auction.startingPrice();
        
        if (incrementStr.endsWith("%")) {
            double percent = Double.parseDouble(incrementStr.substring(0, incrementStr.length() - 1));
            return currentBid * (1 + percent / 100.0);
        } else {
            double flatAmount = Double.parseDouble(incrementStr);
            return currentBid + flatAmount;
        }
    }
    
    /**
     * Validates auction parameters
     */
    private boolean validateAuctionParameters(double startingPrice, Double buyNowPrice, 
                                             Double reservePrice, long durationMillis) {
        double minPrice = configManager.getConfig().getDouble("auction.min-price", 1.0);
        
        if (startingPrice < minPrice) {
            return false;
        }
        
        if (buyNowPrice != null && buyNowPrice < startingPrice) {
            return false; // Buy now must be higher than starting price
        }
        
        if (reservePrice != null && reservePrice < startingPrice) {
            return false; // Reserve price must be higher than starting price
        }
        
        long maxDuration = com.minekartastudio.kartaauctionhouse.util.DurationParser
            .parse(configManager.getConfig().getString("auction.max-duration", "30d")).orElse(30 * 24 * 60 * 60 * 1000L);
        
        if (durationMillis <= 0 || durationMillis > maxDuration) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Retrieves auction history for a player with enhanced filtering options
     */
    public CompletableFuture<List<Auction>> getAuctionHistory(UUID playerId, 
                                                             String statusFilter, // ACTIVE, FINISHED, CANCELLED, EXPIRED
                                                             int page, 
                                                             int pageSize) {
        // This would require a method in the storage layer to retrieve historical auctions
        // For now, we'll return completed auctions
        return auctionStorage.getAuctionsByPlayer(playerId, page, pageSize);
    }
    
    /**
     * Gets detailed auction information including bid history
     */
    public CompletableFuture<AuctionDetails> getAuctionDetails(UUID auctionId) {
        return auctionStorage.findById(auctionId).thenCompose(optAuction -> {
            if (optAuction.isEmpty()) {
                return CompletableFuture.completedFuture(null);
            }
            
            Auction auction = optAuction.get();
            
            // Get bid history
            return auctionStorage.getBidsForAuction(auctionId).thenApply(bids -> {
                return new AuctionDetails(auction, bids);
            });
        });
    }
    
    /**
     * Cancels an auction with enhanced validation
     */
    public CompletableFuture<Boolean> cancelAuction(Player player, UUID auctionId) {
        return auctionService.executeWithLock(auctionId, () ->
            auctionStorage.findById(auctionId).thenCompose(optAuction -> {
                if (optAuction.isEmpty()) {
                    return CompletableFuture.completedFuture(false);
                }
                
                Auction auction = optAuction.get();
                
                // Allow cancellation if the player is the seller and the auction is active with no bids
                if (!auction.seller().equals(player.getUniqueId())) {
                    player.sendMessage(configManager.getPrefixedMessage("errors.not-your-auction"));
                    return CompletableFuture.completedFuture(false);
                }
                
                if (auction.status() != AuctionStatus.ACTIVE) {
                    player.sendMessage(configManager.getPrefixedMessage("errors.cannot-cancel-status"));
                    return CompletableFuture.completedFuture(false);
                }
                
                if (auction.currentBidder() != null) {
                    player.sendMessage(configManager.getPrefixedMessage("errors.no-bids-to-cancel"));
                    return CompletableFuture.completedFuture(false);
                }
                
                // Cancel the auction
                return mailboxService.sendItem(player.getUniqueId(), auction.item(), "Auction cancelled")
                    .thenCompose(v -> {
                        Auction updatedAuction = auction.withStatus(AuctionStatus.CANCELLED)
                            .withIncrementedVersion();
                        return auctionStorage.updateAuctionIfVersionMatches(updatedAuction, auction.version())
                            .thenApply(updated -> {
                                if (updated) {
                                    transactionLogger.log(updatedAuction, "CANCELLED", null, null);
                                }
                                return updated;
                            });
                    });
            })
        );
    }
    
    /**
     * Gets all active auctions with advanced filtering
     */
    public CompletableFuture<List<Auction>> getActiveAuctions(int page, 
                                                             int pageSize, 
                                                             String category, 
                                                             String sortOrder, 
                                                             String searchQuery) {
        // These would be mapped to the existing GUI models, but we'll make them more flexible
        com.minekartastudio.kartaauctionhouse.gui.model.AuctionCategory cat = 
            com.minekartastudio.kartaauctionhouse.gui.model.AuctionCategory.valueOf(category.toUpperCase());
        com.minekartastudio.kartaauctionhouse.gui.model.SortOrder order = 
            com.minekartastudio.kartaauctionhouse.gui.model.SortOrder.valueOf(sortOrder.toUpperCase());
        
        return auctionService.getActiveAuctions(page, pageSize, cat, order, searchQuery);
    }
    
    // Helper class for detailed auction information
    public static class AuctionDetails {
        private final Auction auction;
        private final List<Bid> bidHistory;
        
        public AuctionDetails(Auction auction, List<Bid> bidHistory) {
            this.auction = auction;
            this.bidHistory = bidHistory;
        }
        
        public Auction getAuction() {
            return auction;
        }
        
        public List<Bid> getBidHistory() {
            return bidHistory;
        }
        
        public int getBidCount() {
            return bidHistory.size();
        }
    }
}