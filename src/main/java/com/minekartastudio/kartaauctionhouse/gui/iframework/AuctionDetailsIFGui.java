package com.minekartastudio.kartaauctionhouse.gui.iframework;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.auction.model.AuctionStatus;
import com.minekartastudio.kartaauctionhouse.gui.GuiItemBuilder;
import com.minekartastudio.kartaauctionhouse.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Detailed Auction View GUI using the Inventory Framework
 * Provides detailed information about an auction and allows bidding/buying
 */
public class AuctionDetailsIFGui extends BaseIFGui {
    
    private final KartaAuctionHouse kah;
    private final Auction auction;
    
    public AuctionDetailsIFGui(KartaAuctionHouse plugin, Player player, Auction auction) {
        super(plugin, player);
        this.kah = plugin;
        this.auction = auction;
    }
    
    @Override
    protected String getTitle() {
        return kah.getConfigManager().getMessage("gui.auction-details-title");
    }
    
    @Override
    protected int getRows() {
        return 6; // Standard 6-row inventory
    }
    
    @Override
    protected void build() {
        // Create the main pane for auction details (slots 0-44)
        OutlinePane detailPane = new OutlinePane(0, 0, 9, 5);
        
        // Add the auction item to the center of the GUI
        ItemStack auctionItem = auction.item().toItemStack();
        GuiItemBuilder itemBuilder = new GuiItemBuilder(auctionItem);
        
        // Add detailed lore to the auction item
        List<String> itemLore = new ArrayList<>();
        itemLore.add("&7&m-------------------------");
        itemLore.add("&7Seller: &e" + kah.getPlayerNameCache().getName(auction.seller()).join());
        itemLore.add("&7Time Left: &e" + TimeUtil.formatDuration(auction.endAt() - System.currentTimeMillis()));
        
        if (auction.currentBid() != null) {
            itemLore.add("&7Current Bid: &e" + kah.getEconomyRouter().getService().format(auction.currentBid()));
        } else {
            itemLore.add("&7Starting Price: &e" + kah.getEconomyRouter().getService().format(auction.startingPrice()));
        }
        
        if (auction.buyNowPrice() != null) {
            itemLore.add("&7Buy Now: &c" + kah.getEconomyRouter().getService().format(auction.buyNowPrice()));
        }
        
        if (auction.reservePrice() != null) {
            itemLore.add("&7Reserve Price: &b" + kah.getEconomyRouter().getService().format(auction.reservePrice()));
        }
        
        itemLore.add("&7&m-------------------------");
        itemLore.add("");
        itemLore.add("&a&oClick to bid or buy!");
        
        itemBuilder.setLore(itemLore.toArray(new String[0]));
        detailPane.addItem(new GuiItem(itemBuilder.build(), event -> handleItemClick(event))); // Center position
        
        addPane(detailPane);
        
        // Add control bar (slots 45-53)
        addControlBar();
    }
    
    private void addControlBar() {
        StaticPane controlPane = new StaticPane(0, 5, 9, 1);
        
        // Create decorative elements for visual separation
        ItemStack separatorFiller = new GuiItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setName(" ").build();
        ItemStack cornerFiller = new GuiItemBuilder(Material.BLUE_STAINED_GLASS_PANE).setName(" ").build();
        
        // Fill the separator row (slots 36-44) with decorative elements
        for (int i = 36; i < 45; i++) {
            controlPane.addItem(new GuiItem(separatorFiller), i % 9, 0);
        }
        
        // Decorative corners and sides for visual framing
        controlPane.addItem(new GuiItem(cornerFiller), 0, 0); // Top-left corner of separator
        controlPane.addItem(new GuiItem(cornerFiller), 8, 0); // Top-right corner of separator

        // Back Button (Slot 45) - Leftmost navigation
        String backName = kah.getConfigManager().getMessage("gui.control-items.back");
        String[] backLore = kah.getConfigManager().getMessages().getStringList("gui.control-items.back-lore").toArray(new String[0]);
        controlPane.addItem(new GuiItem(new GuiItemBuilder(Material.ARROW).setName(backName).setLore(backLore).build(), 
            event -> openMainAuction()), 0, 0);

        // Navigation spacer (Slot 46)
        controlPane.addItem(new GuiItem(new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build()), 1, 0);

        // Place Bid Button (Slot 47) - Allow players to place a bid
        if (auction.status() == AuctionStatus.ACTIVE && System.currentTimeMillis() < auction.endAt()) {
            String bidName = kah.getConfigManager().getMessage("gui.control-items.place-bid");
            String[] bidLore = kah.getConfigManager().getMessages().getStringList("gui.control-items.place-bid-lore").toArray(new String[0]);
            controlPane.addItem(new GuiItem(new GuiItemBuilder(Material.GOLD_INGOT).setName(bidName).setLore(bidLore).build(), 
                event -> handlePlaceBid()), 2, 0);
        } else {
            // Disabled bid button
            String bidName = kah.getConfigManager().getMessage("gui.control-items.place-bid");
            String[] bidLore = {"&c&oAuction is no longer active"};
            controlPane.addItem(new GuiItem(new GuiItemBuilder(Material.GOLD_INGOT).setName(bidName).setLore(bidLore).build()), 2, 0);
        }

        // Close Button (Slot 48) - Center position for prominence
        String closeName = kah.getConfigManager().getMessage("gui.control-items.close");
        String[] closeLore = kah.getConfigManager().getMessages().getStringList("gui.control-items.close-lore").toArray(new String[0]);
        controlPane.addItem(new GuiItem(new GuiItemBuilder(Material.BARRIER).setName(closeName).setLore(closeLore).build(), 
            event -> player.closeInventory()), 3, 0);

        // Player Info Item (Slot 49) - Center position for importance
        createPlayerInfoItem().thenAccept(item -> {
            controlPane.addItem(new GuiItem(item), 4, 0);
        });

        // Buy Now Button (Slot 50) - Right-side navigation element
        if (auction.buyNowPrice() != null && auction.status() == AuctionStatus.ACTIVE && 
            System.currentTimeMillis() < auction.endAt() && auction.currentBid() == null) {
            String buyNowName = kah.getConfigManager().getMessage("gui.control-items.buy-now");
            String[] buyNowLore = kah.getConfigManager().getMessages().getStringList("gui.control-items.buy-now-lore").toArray(new String[0]);
            controlPane.addItem(new GuiItem(new GuiItemBuilder(Material.EMERALD).setName(buyNowName).setLore(buyNowLore).build(), 
                event -> handleBuyNow()), 5, 0);
        } else {
            // Disabled buy now button
            String buyNowName = kah.getConfigManager().getMessage("gui.control-items.buy-now");
            String[] buyNowLore = {"&c&oBuy Now unavailable"};
            controlPane.addItem(new GuiItem(new GuiItemBuilder(Material.EMERALD).setName(buyNowName).setLore(buyNowLore).build()), 5, 0);
        }

        // My Listings Button (Slot 51) - Personal auctions
        String myListingsName = kah.getConfigManager().getMessage("gui.control-items.my-listings");
        String[] myListingsLore = kah.getConfigManager().getMessages().getStringList("gui.control-items.my-listings-lore").toArray(new String[0]);
        controlPane.addItem(new GuiItem(new GuiItemBuilder(Material.CHEST).setName(myListingsName).setLore(myListingsLore).build(), 
            event -> openMyListings()), 6, 0);

        // Mailbox Button (Slot 52) - Player mailbox
        String mailboxName = kah.getConfigManager().getMessage("gui.control-items.mailbox");
        String[] mailboxLore = kah.getConfigManager().getMessages().getStringList("gui.control-items.mailbox-lore").toArray(new String[0]);
        controlPane.addItem(new GuiItem(new GuiItemBuilder(Material.WRITABLE_BOOK).setName(mailboxName).setLore(mailboxLore).build(), 
            event -> openMailbox()), 7, 0);

        // Next Page Button (Slot 53) - Rightmost navigation
        // For auction details, this will go back to the main auction GUI
        String nextName = kah.getConfigManager().getMessage("gui.control-items.next-page");
        String[] nextLore = kah.getConfigManager().getMessages().getStringList("gui.control-items.next-page-lore").toArray(new String[0]);
        controlPane.addItem(new GuiItem(new GuiItemBuilder(Material.ARROW).setName(nextName).setLore(nextLore).build(), 
            event -> openMainAuction()), 8, 0);
        
        addPane(controlPane);
    }
    
    /**
     * Handles clicking on the auction item in the GUI
     */
    private void handleItemClick(InventoryClickEvent event) {
        // For now, just show a message - in a full implementation you might open a detailed view or handle bid/buy
        player.sendMessage("You clicked on auction item: " + auction.id());
    }
    
    /**
     * Handles placing a bid on the auction
     */
    private void handlePlaceBid() {
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
                        new MainAuctionIFGui(kah, player, 1, com.minekartastudio.kartaauctionhouse.gui.model.SortOrder.NEWEST, null).open();
                    }, 20L); // Delay to allow transaction to complete
                } else {
                    player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.generic-error", 
                        "Failed to place bid. Please try again."));
                }
            });
        });
    }
    
    /**
     * Handles buying the item immediately through Buy Now option
     */
    private void handleBuyNow() {
        if (auction.buyNowPrice() == null) {
            player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.buy-now-unavailable"));
            return;
        }
        
        // Check if player is clicking on their own auction and not in debug mode
        if (auction.seller().equals(player.getUniqueId()) && 
            !isPlayerInDebugMode(player)) {
            // Show message and don't allow interaction
            player.sendMessage(kah.getConfigManager().getPrefixedMessage("admin.cannot-interact-with-own"));
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
                    
                    // Refresh the GUI to show updated auction information
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                        new MainAuctionIFGui(kah, player, 1, com.minekartastudio.kartaauctionhouse.gui.model.SortOrder.NEWEST, null).open();
                    }, 20L); // Delay to allow transaction to complete
                } else {
                    player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.generic-error", 
                        "Failed to purchase item. Please try again."));
                }
            });
        });
    }
    
    /**
     * Opens the main auction GUI
     */
    private void openMainAuction() {
        new MainAuctionIFGui(kah, player, 1, com.minekartastudio.kartaauctionhouse.gui.model.SortOrder.NEWEST, null).open();
    }
    
    /**
     * Opens the my listings GUI
     */
    private void openMyListings() {
        new MyListingsIFGui(kah, player, 1).open();
    }
    
    /**
     * Opens the mailbox GUI
     */
    private void openMailbox() {
        new MailboxIFGui(kah, player, 1).open();
    }
    
    /**
     * Checks if a player is in debug mode
     * @param player The player to check
     * @return true if the player is in debug mode, false otherwise
     */
    private boolean isPlayerInDebugMode(Player player) {
        return kah.isInDebugMode(player);
    }
    
    @Override
    protected void onClick(InventoryClickEvent event) {
        // All clicks are handled by individual GuiItem click handlers
        // This method will be called for clicks in the main GUI area
    }
}