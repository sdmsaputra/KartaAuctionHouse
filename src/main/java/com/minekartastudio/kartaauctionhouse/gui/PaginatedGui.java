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
        ItemStack filler = new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Previous Page Button
        if (page > 1) {
            inventory.setItem(45, new GuiItemBuilder(Material.ARROW).setName("&a<- Previous Page").setLore("&7Go to page " + (page - 1)).build());
        }

        // Close Button
        inventory.setItem(48, new GuiItemBuilder(Material.BARRIER).setName("&cClose").build());

        // Player Info Item
        createPlayerInfoItem().thenAccept(item -> inventory.setItem(49, item));

        // Next Page Button
        if (hasNextPage) {
            inventory.setItem(53, new GuiItemBuilder(Material.ARROW).setName("&aNext Page ->").setLore("&7Go to page " + (page + 1)).build());
        }
    }

    protected boolean handleControlBarClick(InventoryClickEvent event) {
        if (event.getSlot() == 45 && page > 1) {
            openPage(page - 1);
            return true;
        } else if (event.getSlot() == 53 && hasNextPage) {
            openPage(page + 1);
            return true;
        } else if (event.getSlot() == 48) {
            player.closeInventory();
            return true;
        }
        return false;
    }

    protected abstract void openPage(int newPage);
}
