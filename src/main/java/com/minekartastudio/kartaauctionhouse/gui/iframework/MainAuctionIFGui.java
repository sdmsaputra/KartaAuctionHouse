package com.minekartastudio.kartaauctionhouse.gui.iframework;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.auction.model.AuctionStatus;
import com.minekartastudio.kartaauctionhouse.gui.GuiItemBuilder;
import com.minekartastudio.kartaauctionhouse.gui.model.AuctionCategory;
import com.minekartastudio.kartaauctionhouse.gui.model.SortOrder;
import com.minekartastudio.kartaauctionhouse.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Main Auction GUI implementation using the Inventory Framework
 */
public class MainAuctionIFGui extends BaseIFGui {
    
    private final KartaAuctionHouse kah;
    private List<Auction> auctions;
    private final SortOrder sortOrder;
    private final String searchQuery;
    private final int page;
    private boolean hasNextPage = false;
    private static final int ITEMS_PER_PAGE = 36; // 4 rows of items (excluding control bar)
    
    public MainAuctionIFGui(KartaAuctionHouse plugin, Player player, int page, SortOrder sortOrder, String searchQuery) {
        super(plugin, player);
        this.kah = plugin;
        this.page = page;
        this.sortOrder = sortOrder;
        this.searchQuery = searchQuery;
    }
    
    @Override
    protected String getTitle() {
        return kah.getConfigManager().getMessage("gui.main-title");
    }
    
    @Override
    protected void build() {
        // Create the main pane for auction items
        OutlinePane auctionItemsPane = new OutlinePane(0, 0, 9, 4);
        
        // Fetch auctions and populate the GUI
        kah.getAuctionService().getActiveAuctions(page, ITEMS_PER_PAGE, AuctionCategory.ALL, sortOrder, searchQuery)
            .thenCombine(kah.getEconomyRouter().getService().getBalance(player.getUniqueId()), (fetchedAuctions, balance) -> {
                // Determine pagination
                this.hasNextPage = fetchedAuctions.size() > ITEMS_PER_PAGE;
                this.auctions = hasNextPage ? fetchedAuctions.subList(0, ITEMS_PER_PAGE) : fetchedAuctions;
                
                // Add auction items to the pane
                for (Auction auction : this.auctions) {
                    ItemStack displayItem = createAuctionItem(auction, balance);
                    GuiItem guiItem = new GuiItem(displayItem, event -> onAuctionItemClick(event, auction));
                    auctionItemsPane.addItem(guiItem);
                }
                
                return null;
            }).thenRunAsync(() -> {
                // Build the static parts of the GUI on the main thread
                addControlBar();
                
                // Add the auction items pane to the GUI
                addPane(auctionItemsPane);
                
                // Update the GUI to reflect changes
                update();
            }, runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable));
    }
    
    private void addControlBar() {
        StaticPane controlPane = new StaticPane(0, 4, 9, 2);
        
        // Filler items for the control bar
        ItemStack filler = new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 0; i < 9; i++) {
            controlPane.addItem(new GuiItem(filler), i, 0);
        }
        
        // Previous Page Button
        if (page > 1) {
            ItemStack prevPageItem = new GuiItemBuilder(Material.ARROW)
                .setName("&a<- Previous Page")
                .setLore("&7Go to page " + (page - 1))
                .build();
            controlPane.addItem(new GuiItem(prevPageItem, event -> openPage(page - 1)), 0, 1);
        }
        
        // Close Button
        ItemStack closeItem = new GuiItemBuilder(Material.BARRIER).setName("&cClose").build();
        controlPane.addItem(new GuiItem(closeItem, event -> player.closeInventory()), 3, 1);
        
        // Player Info Item
        createPlayerInfoItem().thenAccept(item -> {
            controlPane.addItem(new GuiItem(item), 4, 1);
        });
        
        // Next Page Button
        if (hasNextPage) {
            ItemStack nextPageItem = new GuiItemBuilder(Material.ARROW)
                .setName("&aNext Page ->")
                .setLore("&7Go to page " + (page + 1))
                .build();
            controlPane.addItem(new GuiItem(nextPageItem, event -> openPage(page + 1)), 8, 1);
        }
        
        // Additional controls
        // Sorting Button
        String sortName = kah.getConfigManager().getMessage("gui.control-items.sort", "{sort_order}", sortOrder.getDisplayName());
        String[] sortLore = kah.getConfigManager().getMessages().getStringList("gui.control-items.sort-lore").toArray(new String[0]);
        ItemStack sortItem = new GuiItemBuilder(Material.HOPPER).setName(sortName).setLore(sortLore).build();
        controlPane.addItem(new GuiItem(sortItem, event -> cycleSortOrder()), 5, 1);
        
        // Search Button
        String searchName = kah.getConfigManager().getMessage("gui.control-items.search");
        String[] searchLore = kah.getConfigManager().getMessages().getStringList("gui.control-items.search-lore")
                .toArray(new String[0]);
        // Replace the {query} placeholder in the lore
        for (int i = 0; i < searchLore.length; i++) {
            searchLore[i] = searchLore[i].replace("{query}", searchQuery != null ? searchQuery : "None");
        }
        ItemStack searchItem = new GuiItemBuilder(Material.OAK_SIGN).setName(searchName).setLore(searchLore).build();
        controlPane.addItem(new GuiItem(searchItem, event -> enterSearchMode()), 6, 1);
        
        // My Auctions
        String myAuctionsName = kah.getConfigManager().getMessage("gui.control-items.my-listings");
        String[] myAuctionsLore = kah.getConfigManager().getMessages().getStringList("gui.control-items.my-listings-lore")
                .toArray(new String[0]);
        // Replace placeholders in the lore
        kah.getAuctionService().countActiveAuctions(player).thenAccept(count -> {
            int maxListings = kah.getConfigManager().getConfig().getInt("auction.max-auctions-per-player", 5);
            for (int i = 0; i < myAuctionsLore.length; i++) {
                myAuctionsLore[i] = myAuctionsLore[i].replace("{count}", String.valueOf(count))
                        .replace("{max}", String.valueOf(maxListings));
            }
            ItemStack myAuctionsItem = new GuiItemBuilder(Material.CHEST).setName(myAuctionsName).setLore(myAuctionsLore).build();
            // We need to update this asynchronously, so we'll add a placeholder here and update later
            // For now, just add the item normally
        });
        ItemStack myAuctionsItem = new GuiItemBuilder(Material.CHEST).setName(myAuctionsName).build();
        controlPane.addItem(new GuiItem(myAuctionsItem, event -> openMyListings()), 7, 1);
        
        addPane(controlPane);
    }
    
    private ItemStack createAuctionItem(Auction auction, double playerBalance) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        // Get the template lore from messages config
        List<String> templateLore = kah.getConfigManager().getMessages().getStringList("gui.item-lore");
        List<String> lore = new ArrayList<>();

        for (String loreLine : templateLore) {
            // Replace placeholders in each line
            String processedLine = loreLine
                    .replace("{seller}", kah.getPlayerNameCache().getName(auction.seller()).join())
                    .replace("{time_left}", TimeUtil.formatDuration(auction.endAt() - System.currentTimeMillis()))
                    .replace("{starting_price}", kah.getEconomyRouter().getService().format(auction.startingPrice()))
                    .replace("{current_bid}", auction.currentBid() != null ? 
                            kah.getEconomyRouter().getService().format(auction.currentBid()) : "N/A")
                    .replace("{buy_now_price}", auction.buyNowPrice() != null ? 
                            kah.getEconomyRouter().getService().format(auction.buyNowPrice()) : "N/A")
                    .replace("{reserve_price}", auction.reservePrice() != null ? 
                            kah.getEconomyRouter().getService().format(auction.reservePrice()) : "N/A");

            // Apply bid/buy now affordability color
            if (loreLine.contains("{current_bid}") || loreLine.contains("{starting_price}")) {
                double bidPrice = auction.currentBid() != null ? auction.currentBid() : auction.startingPrice();
                String bidColor = playerBalance >= bidPrice ? "&a" : "&c";
                processedLine = processedLine.replace("{current_bid}", bidColor + kah.getEconomyRouter().getService().format(bidPrice));
                processedLine = processedLine.replace("{starting_price}", bidColor + kah.getEconomyRouter().getService().format(bidPrice));
            } else if (loreLine.contains("{buy_now_price}") && auction.buyNowPrice() != null) {
                String buyNowColor = playerBalance >= auction.buyNowPrice() ? "&a" : "&c";
                processedLine = processedLine.replace("{buy_now_price}", buyNowColor + kah.getEconomyRouter().getService().format(auction.buyNowPrice()));
            }

            // Replace status with appropriate color
            if (loreLine.contains("{status}")) {
                String status = kah.getConfigManager().getMessage("gui.status.active"); // Default to active
                String statusColor = "&a"; // Default color
                processedLine = processedLine.replace("{status_color}", statusColor).replace("{status}", status);
            } else {
                processedLine = processedLine.replace("{status_color}", "").replace("{status}", "");
            }

            lore.add(processedLine);
        }

        return builder.setLore(lore.toArray(new String[0])).build();
    }
    
    private void onAuctionItemClick(InventoryClickEvent event, Auction auction) {
        // Check if player is clicking on their own auction and not in debug mode
        if (auction.seller().equals(player.getUniqueId()) && 
            !isPlayerInDebugMode(player)) {
            // Show message and don't allow interaction
            player.sendMessage(kah.getConfigManager().getPrefixedMessage("admin.cannot-interact-with-own"));
            return;
        }
        
        // Check if auction is still active
        if (auction.status() != AuctionStatus.ACTIVE) {
            player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.auction-not-found"));
            return;
        }
        
        // Check if auction has expired
        if (System.currentTimeMillis() >= auction.endAt()) {
            player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.auction-expired"));
            return;
        }
        
        // Determine action based on click type
        if (event.isLeftClick() || event.getClick().isRightClick()) {
            // Handle left/right click for bid/buy now
            if (auction.buyNowPrice() != null && event.isRightClick()) {
                // Buy Now with right click
                handleBuyNow(auction);
            } else {
                // Place bid with left click (or left click when no buy now available)
                handlePlaceBid(auction);
            }
        } else {
            // Open detailed view for other click types (optional)
            openAuctionDetails(auction);
        }
    }
    
    /**
     * Handles placing a bid on an auction
     * @param auction The auction to bid on
     */
    private void handlePlaceBid(Auction auction) {
        kah.getEconomyRouter().getService().getBalance(player.getUniqueId()).thenAccept(playerBalance -> {
            double currentPrice = auction.currentBid() != null ? auction.currentBid() : auction.startingPrice();
            
            // Calculate minimum bid increment
            String incrementStr = kah.getConfigManager().getConfig().getString("auction.bid-increment", "5%");
            double minBid;
            
            if (incrementStr.endsWith("%")) {
                double percent = Double.parseDouble(incrementStr.substring(0, incrementStr.length() - 1));
                minBid = currentPrice * (1 + percent / 100.0);
            } else {
                double flatAmount = Double.parseDouble(incrementStr);
                minBid = currentPrice + flatAmount;
            }
            
            // Check if player has enough money
            if (playerBalance < minBid) {
                player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.not-enough-money", 
                    "{amount}", kah.getEconomyRouter().getService().format(minBid)));
                return;
            }
            
            // Place the bid
            player.closeInventory(); // Close GUI to prevent double clicking
            kah.getAuctionService().placeBid(player, auction.id(), minBid, kah).thenAccept(success -> {
                if (success) {
                    player.sendMessage(kah.getConfigManager().getPrefixedMessage("info.bid-placed", 
                        "{amount}", kah.getEconomyRouter().getService().format(minBid),
                        "{item}", auction.item().toItemStack().getType().toString()));
                    
                    // Refresh the GUI to show updated auction information
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        new MainAuctionIFGui(kah, player, page, sortOrder, searchQuery).open();
                    }, 20L); // Delay to allow transaction to complete
                } else {
                    player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.generic-error", 
                        "Failed to place bid. Please try again."));
                }
            });
        });
    }
    
    /**
     * Handles buying an item immediately through Buy Now option
     * @param auction The auction to buy now
     */
    private void handleBuyNow(Auction auction) {
        if (auction.buyNowPrice() == null) {
            player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.buy-now-unavailable"));
            return;
        }
        
        kah.getEconomyRouter().getService().getBalance(player.getUniqueId()).thenAccept(playerBalance -> {
            // Check if player has enough money
            if (playerBalance < auction.buyNowPrice()) {
                player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.not-enough-money", 
                    "{amount}", kah.getEconomyRouter().getService().format(auction.buyNowPrice())));
                return;
            }
            
            // Check if there are already bids (buy now should not be available if there are bids)
            if (auction.currentBid() != null) {
                player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.buy-now-fail", 
                    "Cannot use Buy Now on an auction that already has bids."));
                return;
            }
            
            // Buy now the item
            player.closeInventory(); // Close GUI to prevent double clicking
            kah.getAuctionService().buyNow(player, auction.id(), kah).thenAccept(success -> {
                if (success) {
                    player.sendMessage(kah.getConfigManager().getPrefixedMessage("info.bought", 
                        "{item}", auction.item().toItemStack().getType().toString(),
                        "{amount}", kah.getEconomyRouter().getService().format(auction.buyNowPrice())));
                } else {
                    player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.generic-error", 
                        "Failed to purchase item. Please try again."));
                }
            });
        });
    }
    
    /**
     * Opens the detailed auction view (optional feature)
     * @param auction The auction to view details for
     */
    private void openAuctionDetails(Auction auction) {
        // For now, we can open a detailed view or just show more information
        player.sendMessage("Detailed view for auction: " + auction.id());
        // In a full implementation, you might open an AuctionDetailsIFGui
        // new AuctionDetailsIFGui(kah, player, auction).open();
    }
    
    /**
     * Checks if a player is in debug mode
     * @param player The player to check
     * @return true if the player is in debug mode, false otherwise
     */
    private boolean isPlayerInDebugMode(Player player) {
        // Check if player is in debug mode
        return kah.isInDebugMode(player);
    }
    
    private void cycleSortOrder() {
        SortOrder nextSortOrder = sortOrder.next();
        new MainAuctionIFGui(kah, player, 1, nextSortOrder, searchQuery).open();
    }
    
    private void enterSearchMode() {
        player.closeInventory();
        kah.getSearchInputListener().addPlayerToSearchMode(player.getUniqueId());
        player.sendMessage(kah.getConfigManager().getPrefixedMessage("info.enter-search-query", 
            "Please type your search query in chat. Type 'cancel' to exit."));
    }
    
    private void openMyListings() {
        new MyListingsIFGui(kah, player, 1).open();
    }
    
    private void openPage(int newPage) {
        new MainAuctionIFGui(kah, player, newPage, sortOrder, searchQuery).open();
    }
    
    @Override
    protected void onClick(InventoryClickEvent event) {
        // This method will be called for clicks in the main GUI area
        // Specific item clicks are handled by individual GuiItem click handlers
    }
}