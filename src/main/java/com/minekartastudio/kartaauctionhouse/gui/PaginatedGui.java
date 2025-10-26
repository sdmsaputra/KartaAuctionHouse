package com.minekartastudio.kartaauctionhouse.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class PaginatedGui extends Gui {

    protected int page;
    protected final int itemsPerPage;
    protected boolean hasNextPage = false;

    public PaginatedGui(KartaAuctionHouse plugin, Player player, int page, int itemsPerPage) {
        super(plugin, player);
        this.page = page;
        this.itemsPerPage = itemsPerPage;
    }

    @Override
    public int getSize() {
        return 54; // Standard 6-row inventory
    }

    protected void addControlBar() {
        // Create decorative elements for visual separation
        ItemStack separatorFiller = new GuiItemBuilder(Material.LIGHT_BLUE_STAINED_GLASS_PANE).setName(" ").build();
        ItemStack cornerFiller = new GuiItemBuilder(Material.BLUE_STAINED_GLASS_PANE).setName(" ").build();
        
        // Fill the separator row (slots 36-44) with decorative elements
        for (int i = 36; i < 45; i++) {
            inventory.setItem(i, separatorFiller);
        }
        
        // Decorative corners and sides for visual framing
        inventory.setItem(36, cornerFiller); // Top-left corner of separator
        inventory.setItem(44, cornerFiller); // Top-right corner of separator

        // Previous Page Button (Slot 45) - Leftmost navigation
        if (page > 1) {
            String prevName = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessage("gui.control-items.previous-page");
            String[] prevLore = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessages().getStringList("gui.control-items.previous-page-lore").toArray(new String[0]);
            // Replace placeholders in the lore
            for (int i = 0; i < prevLore.length; i++) {
                prevLore[i] = prevLore[i].replace("{current_page}", String.valueOf(page))
                        .replace("{total_pages}", "N/A");
            }
            inventory.setItem(45, new GuiItemBuilder(Material.ARROW).setName(prevName).setLore(prevLore).build());
        } else {
            // Show disabled arrow when no previous page
            inventory.setItem(45, new GuiItemBuilder(Material.ARROW)
                .setName("&7No Previous Page")
                .setLore("&7You are on the first page")
                .build());
        }

        // Navigation spacer (Slot 46)
        inventory.setItem(46, new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build());

        // Sort Button (Slot 47) - Auction sorting
        String sortName = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessage("gui.control-items.sort");
        String[] sortLore = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessages().getStringList("gui.control-items.sort-lore").toArray(new String[0]);
        inventory.setItem(47, new GuiItemBuilder(Material.HOPPER).setName(sortName).setLore(sortLore).build());

        // Close Button (Slot 48) - Center position for prominence
        String closeName = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessage("gui.control-items.close");
        String[] closeLore = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessages().getStringList("gui.control-items.close-lore").toArray(new String[0]);
        inventory.setItem(48, new GuiItemBuilder(Material.BARRIER).setName(closeName).setLore(closeLore).build());

        // Player Info Item (Slot 49) - Center position for importance
        createPlayerInfoItem().thenAccept(item -> {
            inventory.setItem(49, item);
            // Update inventory for all viewers to ensure the item appears
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                inventory.getViewers().forEach(viewer -> {
                    if (viewer instanceof Player playerViewer) {
                        playerViewer.updateInventory();
                    }
                });
            });
        });

        // Search Button (Slot 50) - Right-side navigation element
        String searchName = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessage("gui.control-items.search");
        String[] searchLore = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessages().getStringList("gui.control-items.search-lore").toArray(new String[0]);
        inventory.setItem(50, new GuiItemBuilder(Material.OAK_SIGN).setName(searchName).setLore(searchLore).build());

        // My Listings Button (Slot 51) - Personal auctions
        String myListingsName = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessage("gui.control-items.my-listings");
        String[] myListingsLore = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessages().getStringList("gui.control-items.my-listings-lore").toArray(new String[0]);
        inventory.setItem(51, new GuiItemBuilder(Material.CHEST).setName(myListingsName).setLore(myListingsLore).build());

        // Mailbox Button (Slot 52) - Player mailbox
        String mailboxName = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessage("gui.control-items.mailbox");
        String[] mailboxLore = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessages().getStringList("gui.control-items.mailbox-lore").toArray(new String[0]);
        inventory.setItem(52, new GuiItemBuilder(Material.WRITABLE_BOOK).setName(mailboxName).setLore(mailboxLore).build());

        // Next Page Button (Slot 53) - Rightmost navigation
        if (hasNextPage) {
            String nextName = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessage("gui.control-items.next-page");
            String[] nextLore = ((com.minekartastudio.kartaauctionhouse.KartaAuctionHouse) plugin).getConfigManager().getMessages().getStringList("gui.control-items.next-page-lore").toArray(new String[0]);
            // Replace placeholders in the lore
            for (int i = 0; i < nextLore.length; i++) {
                nextLore[i] = nextLore[i].replace("{current_page}", String.valueOf(page))
                        .replace("{total_pages}", "N/A");
            }
            inventory.setItem(53, new GuiItemBuilder(Material.ARROW).setName(nextName).setLore(nextLore).build());
        } else {
            // Show disabled arrow when no next page
            inventory.setItem(53, new GuiItemBuilder(Material.ARROW)
                .setName("&7No Next Page")
                .setLore("&7You are on the last page")
                .build());
        }
    }

    protected boolean handleControlBarClick(InventoryClickEvent event) {
        int slot = event.getSlot();
        
        // Navigation controls
        if (slot == 45 && page > 1) {
            openPage(page - 1);
            return true;
        } else if (slot == 53 && hasNextPage) {
            openPage(page + 1);
            return true;
        } else if (slot == 48) {
            // Close button
            player.closeInventory();
            return true;
        } else if (slot == 47) {
            // Sort button - to be implemented by subclasses
            return false;
        } else if (slot == 50) {
            // Search button - to be implemented by subclasses
            return false;
        } else if (slot == 51) {
            // My listings button - to be implemented by subclasses
            return false;
        } else if (slot == 52) {
            // Mailbox button - to be implemented by subclasses
            return false;
        }
        
        // Default handling for other slots
        return false;
    }

    protected abstract void openPage(int newPage);
}
