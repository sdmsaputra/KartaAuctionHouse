package com.minekartastudio.kartaauctionhouse.gui.iframework;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.gui.GuiItemBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;

/**
 * Base class for all IF-based GUIs in the KartaAuctionHouse plugin
 */
public abstract class BaseIFGui {
    
    protected final KartaAuctionHouse plugin;
    protected final Player player;
    protected ChestGui chestGui;
    
    public BaseIFGui(KartaAuctionHouse plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }
    
    /**
     * Builds the GUI layout using the Inventory Framework
     */
    protected abstract void build();
    
    /**
     * Gets the title for this GUI
     */
    protected abstract String getTitle();
    
    /**
     * Handles click events in the GUI
     */
    protected abstract void onClick(InventoryClickEvent event);
    
    /**
     * Opens the GUI for the player
     */
    public void open() {
        int rows = getRows(); // Default to 6 rows if not overridden
        chestGui = new ChestGui(rows, getTitle());
        
        // Set the click handler for the GUI
        chestGui.setOnTopClick(event -> {
            event.setCancelled(true);
            onClick(event);
        });
        
        build();
        chestGui.show(player);
    }
    
    /**
     * Gets the number of rows for the GUI. Override to specify custom size.
     */
    protected int getRows() {
        return 6; // Default to 6 rows (54 slots)
    }
    
    /**
     * Adds a GUI item to a specific slot
     */
    protected void addItem(int slot, GuiItem item) {
        StaticPane pane = new StaticPane(9, getRows());
        pane.addItem(item, slot % 9, slot / 9);
        chestGui.addPane(pane);
    }
    
    /**
     * Adds a GUI item to multiple slots
     */
    protected void addItem(int row, int column, GuiItem item) {
        StaticPane pane = new StaticPane(9, getRows());
        pane.addItem(item, column, row);
        chestGui.addPane(pane);
    }
    
    /**
     * Adds a pane to the GUI
     */
    protected void addPane(Pane pane) {
        chestGui.addPane(pane);
    }
    
    /**
     * Updates the GUI after changes
     */
    protected void update() {
        if (chestGui != null) {
            chestGui.update();
        }
    }
    
    /**
     * Helper method to create a GuiItem from an ItemStack
     */
    protected GuiItem createGuiItem(ItemStack itemStack) {
        return new GuiItem(itemStack, event -> onClick(event));
    }
    
    /**
     * Helper method to create a GuiItem from an ItemStack with a custom click handler
     */
    protected GuiItem createGuiItem(ItemStack itemStack, org.bukkit.event.inventory.InventoryClickEvent event) {
        return new GuiItem(itemStack, event1 -> onClick(event));
    }
    
    /**
     * Creates a player info item showing the player's head and balance.
     * @return A CompletableFuture that resolves to the player info item.
     */
    protected CompletableFuture<ItemStack> createPlayerInfoItem() {
        if (!(plugin instanceof KartaAuctionHouse kah)) {
            // Fallback for safety, though it should always be a KartaAuctionHouse instance
            return CompletableFuture.completedFuture(
                new GuiItemBuilder(Material.PLAYER_HEAD)
                    .setSkullOwner(player.getName())
                    .setName("&e" + player.getName())
                    .build()
            );
        }

        return kah.getEconomyRouter().getService().getBalance(player.getUniqueId()).thenApply(balance -> {
            String formattedBalance = kah.getEconomyRouter().getService().format(balance);
            GuiItemBuilder builder = new GuiItemBuilder(Material.PLAYER_HEAD)
                .setSkullOwner(player.getName())
                .setName("&a" + player.getName())
                .setLore("&7Balance: &e" + formattedBalance);

            return builder.build();
        });
    }
}