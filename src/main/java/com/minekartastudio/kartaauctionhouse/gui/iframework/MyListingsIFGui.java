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

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * My Listings GUI implementation using the Inventory Framework
 */
public class MyListingsIFGui extends BaseIFGui {
    
    private final KartaAuctionHouse kah;
    private List<Auction> auctions;
    private final int page;
    private boolean hasNextPage = false;
    private static final int ITEMS_PER_PAGE = 36; // 4 rows of items (excluding control bar)
    
    public MyListingsIFGui(KartaAuctionHouse plugin, Player player, int page) {
        super(plugin, player);
        this.kah = plugin;
        this.page = page;
    }
    
    @Override
    protected String getTitle() {
        return kah.getConfigManager().getMessage("gui.my-listings-title");
    }
    
    @Override
    protected void build() {
        // Create the main pane for auction items
        OutlinePane auctionItemsPane = new OutlinePane(0, 0, 9, 4);
        
        // Fetch player's auctions and populate the GUI
        kah.getAuctionService().getPlayerAuctions(player.getUniqueId(), page, ITEMS_PER_PAGE)
            .thenAccept(fetchedAuctions -> {
                // Determine pagination
                this.hasNextPage = fetchedAuctions.size() > ITEMS_PER_PAGE;
                this.auctions = hasNextPage ? fetchedAuctions.subList(0, ITEMS_PER_PAGE) : fetchedAuctions;
                
                // Add auction items to the pane
                for (Auction auction : this.auctions) {
                    ItemStack displayItem = createAuctionItem(auction);
                    GuiItem guiItem = new GuiItem(displayItem, event -> onAuctionItemClick(event, auction));
                    auctionItemsPane.addItem(guiItem);
                }
                
                // Build the static parts of the GUI
                addControlBar();
                
                // Add the auction items pane to the GUI
                addPane(auctionItemsPane);
                
                // Update the GUI to reflect changes
                update();
            });
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
        
        // Back to Main Auction House Button
        String backName = kah.getConfigManager().getMessage("gui.control-items.back");
        String[] backLore = kah.getConfigManager().getMessages().getStringList("gui.control-items.back-lore").toArray(new String[0]);
        ItemStack backButton = new GuiItemBuilder(Material.OAK_DOOR).setName(backName).setLore(backLore).build();
        controlPane.addItem(new GuiItem(backButton, event -> openMainAuction()), 7, 1);
        
        addPane(controlPane);
    }
    
    private ItemStack createAuctionItem(Auction auction) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        // Get the template lore from messages config
        List<String> templateLore = kah.getConfigManager().getMessages().getStringList("gui.my-listings-lore");
        List<String> lore = new ArrayList<>();

        String statusKey = "gui.status." + auction.status().name().toLowerCase();
        String statusMsg = kah.getConfigManager().getMessage(statusKey);
        if (statusMsg == null || statusMsg.startsWith("&cMissing message:")) {
            statusMsg = "&e" + auction.status().name(); // Fallback
        }

        long timeLeft = auction.endAt() - System.currentTimeMillis();
        String timeLeftStr = auction.status() == AuctionStatus.ACTIVE ? 
                TimeUtil.formatDuration(timeLeft) : "N/A";

        int bidderCount = auction.currentBidder() != null ? 1 : 0; // This is a simplified count

        for (String loreLine : templateLore) {
            // Replace placeholders in each line
            String processedLine = loreLine
                    .replace("{item_name}", item.getType().toString())
                    .replace("{starting_price}", kah.getEconomyRouter().getService().format(auction.startingPrice()))
                    .replace("{current_bid}", auction.currentBid() != null ? 
                            kah.getEconomyRouter().getService().format(auction.currentBid()) : "N/A")
                    .replace("{buy_now_price}", auction.buyNowPrice() != null ? 
                            kah.getEconomyRouter().getService().format(auction.buyNowPrice()) : "N/A")
                    .replace("{time_left}", timeLeftStr)
                    .replace("{duration}", kah.getConfigManager().getConfig().getString("auction.defaults.duration", "N/A"))
                    .replace("{bidder_count}", String.valueOf(bidderCount))
                    .replace("{status}", statusMsg);

            lore.add(processedLine);
        }

        return builder.setLore(lore.toArray(new String[0])).build();
    }
    
    private void onAuctionItemClick(InventoryClickEvent event, Auction auction) {
        // For now, just show a message - in a full implementation you might open a management GUI
        player.sendMessage("Managing auction: " + auction.id());
    }
    
    private void openPage(int newPage) {
        new MyListingsIFGui(kah, player, newPage).open();
    }
    
    private void openMainAuction() {
        // Assuming there's a default sort order and no search query when returning to main
        new MainAuctionIFGui(kah, player, 1, com.minekartastudio.kartaauctionhouse.gui.model.SortOrder.NEWEST, null).open();
    }
    
    @Override
    protected void onClick(InventoryClickEvent event) {
        // This method will be called for clicks in the main GUI area
        // Specific item clicks are handled by individual GuiItem click handlers
    }
}