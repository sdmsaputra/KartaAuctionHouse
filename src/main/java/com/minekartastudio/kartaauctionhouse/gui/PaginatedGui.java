package com.minekartastudio.kartaauctionhouse.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class PaginatedGui extends Gui {

    protected int page;
    protected final int itemsPerPage;
    protected boolean hasNextPage = false;

    public PaginatedGui(JavaPlugin plugin, Player player, int page, int itemsPerPage) {
        super(plugin, player);
        this.page = page;
        this.itemsPerPage = itemsPerPage;
    }

    @Override
    public int getSize() {
        return 54; // Standard 6-row inventory
    }

    protected void addPaginationControls() {
        // Previous Page Button
        if (page > 1) {
            inventory.setItem(48, new GuiItemBuilder(Material.ARROW).setName("&a<- Previous Page").setLore("&7Go to page " + (page - 1)).build());
        } else {
            inventory.setItem(48, new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build());
        }

        // Page Info
        inventory.setItem(49, new GuiItemBuilder(Material.PAPER).setName("&ePage " + page).build());

        // Next Page Button
        if (hasNextPage) {
            inventory.setItem(50, new GuiItemBuilder(Material.ARROW).setName("&aNext Page ->").setLore("&7Go to page " + (page + 1)).build());
        } else {
            inventory.setItem(50, new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build());
        }
    }

    protected boolean handlePaginationClick(InventoryClickEvent event) {
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return false;

        if (event.getSlot() == 48 && page > 1) {
            openPage(page - 1);
            return true;
        } else if (event.getSlot() == 50 && hasNextPage) {
            openPage(page + 1);
            return true;
        }
        return false;
    }

    protected abstract void openPage(int newPage);
}
