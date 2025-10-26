package com.minekartastudio.kartaauctionhouse.gui.iframework;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.gui.GuiItemBuilder;
import com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxEntry;
import com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * Mailbox GUI implementation using the Inventory Framework
 */
public class MailboxIFGui extends BaseIFGui {
    
    private final KartaAuctionHouse kah;
    private List<MailboxEntry> entries;
    private final int page;
    private boolean hasNextPage = false;
    private static final int ITEMS_PER_PAGE = 45; // 5 rows
    
    public MailboxIFGui(KartaAuctionHouse plugin, Player player, int page) {
        super(plugin, player);
        this.kah = plugin;
        this.page = page;
    }
    
    @Override
    protected String getTitle() {
        return kah.getConfigManager().getMessage("gui.mailbox-title");
    }
    
    @Override
    protected void build() {
        OutlinePane itemPane = new OutlinePane(0, 0, 9, 5);
        
        // Get mailbox entries for the current page
        kah.getMailboxService().getUnclaimed(player.getUniqueId())
            .thenAccept(unclaimedEntries -> {
                // Manual pagination for this example since MailboxService returns the full list
                int start = (page - 1) * ITEMS_PER_PAGE;
                int end = Math.min(start + ITEMS_PER_PAGE, unclaimedEntries.size());
                
                this.hasNextPage = unclaimedEntries.size() > end;
                this.entries = hasNextPage ? unclaimedEntries.subList(start, end) : unclaimedEntries;
                
                // Add items to the pane
                for (MailboxEntry entry : this.entries) {
                    ItemStack displayItem = createMailboxItem(entry);
                    itemPane.addItem(new GuiItem(displayItem, event -> onMailboxItemClick(event, entry)));
                }
                
                addPane(itemPane);
                addControlBar();
            });
    }
    
    private ItemStack createMailboxItem(MailboxEntry entry) {
        if (entry.type() == MailboxType.ITEM) {
            ItemStack item = entry.item().toItemStack();
            return new GuiItemBuilder(item)
                    .setLore("&7Type: &eItem", 
                             "&7Reason: &f" + entry.note(), 
                             "", 
                             "&aClick to claim this item!")
                    .build();
        } else { // MONEY
            return new GuiItemBuilder(Material.GOLD_NUGGET)
                    .setName("&e" + kah.getEconomyRouter().getService().format(entry.amount()))
                    .setLore("&7Type: &eMoney", 
                             "&7Reason: &f" + entry.note(), 
                             "", 
                             "&aClick to claim this money!")
                    .build();
        }
    }
    
    private void onMailboxItemClick(InventoryClickEvent event, MailboxEntry entry) {
        // Claim the entry
        player.closeInventory(); // Close GUI to prevent further clicks
        kah.getMailboxService().claimEntry(player, entry).thenAccept(success -> {
            if (success) {
                // Refresh the GUI to show the updated mailbox
                new MailboxIFGui(kah, player, page).open();
            } else {
                // Re-open the GUI without refreshing if it failed (e.g., inventory full)
                new MailboxIFGui(kah, player, page).open();
            }
        });
    }
    
    private void addControlBar() {
        StaticPane controlPane = new StaticPane(0, 5, 9, 1);
        
        // Previous Page Button
        if (page > 1) {
            ItemStack prevPageItem = new GuiItemBuilder(Material.ARROW)
                .setName("&a<- Previous Page")
                .setLore("&7Go to page " + (page - 1))
                .build();
            controlPane.addItem(new GuiItem(prevPageItem, event -> openPage(page - 1)), 0, 0);
        }
        
        // Close Button
        ItemStack closeItem = new GuiItemBuilder(Material.BARRIER).setName("&cClose").build();
        controlPane.addItem(new GuiItem(closeItem, event -> player.closeInventory()), 3, 0);
        
        // Player Info Item
        createPlayerInfoItem().thenAccept(item -> {
            controlPane.addItem(new GuiItem(item), 4, 0);
        });
        
        // Next Page Button
        if (hasNextPage) {
            ItemStack nextPageItem = new GuiItemBuilder(Material.ARROW)
                .setName("&aNext Page ->")
                .setLore("&7Go to page " + (page + 1))
                .build();
            controlPane.addItem(new GuiItem(nextPageItem, event -> openPage(page + 1)), 8, 0);
        }
        
        addPane(controlPane);
    }
    
    private void openPage(int newPage) {
        new MailboxIFGui(kah, player, newPage).open();
    }
    
    @Override
    protected void onClick(InventoryClickEvent event) {
        // Control bar clicks are handled by individual GuiItem click handlers
        // This method will be called for clicks in the main GUI area
        // Specific item clicks are handled by individual GuiItem click handlers
    }
}