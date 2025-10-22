package com.minekartastudio.kartaauctionhouse.gui;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.gui.improved.ImprovedMainGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Event listener for handling GUI interactions
 */
public class GuiEventListener implements Listener {

    private final KartaAuctionHouse plugin;
    private final Map<UUID, ImprovedMainGui> activeGuis = new HashMap<>();

    public GuiEventListener(KartaAuctionHouse plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Handle inventory clicks in GUIs
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Check if this is one of our GUIs
        ImprovedMainGui gui = activeGuis.get(player.getUniqueId());
        if (gui != null && gui.getInventory().equals(event.getClickedInventory())) {
            // Handle the click
            boolean handled = gui.handleClick(event);
            if (handled) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Handle inventory close to clean up GUI instances
     */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            // Clean up GUI instance when player closes it
            ImprovedMainGui gui = activeGuis.get(player.getUniqueId());
            if (gui != null && gui.getInventory().equals(event.getInventory())) {
                activeGuis.remove(player.getUniqueId());
            }
        }
    }

    /**
     * Prevent item dragging in GUIs
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ImprovedMainGui gui = activeGuis.get(player.getUniqueId());
        if (gui != null && gui.getInventory().equals(event.getInventory())) {
            // Prevent dragging in our GUIs
            if (event.getRawSlots().stream().anyMatch(slot -> slot < gui.getInventory().getSize())) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Register a GUI instance
     */
    public void registerGui(Player player, ImprovedMainGui gui) {
        activeGuis.put(player.getUniqueId(), gui);
    }

    /**
     * Unregister a GUI instance
     */
    public void unregisterGui(Player player) {
        activeGuis.remove(player.getUniqueId());
    }

    /**
     * Check if a player has an active GUI
     */
    public boolean hasActiveGui(Player player) {
        return activeGuis.containsKey(player.getUniqueId());
    }

    /**
     * Get the active GUI for a player
     */
    public ImprovedMainGui getActiveGui(Player player) {
        return activeGuis.get(player.getUniqueId());
    }

    /**
     * Close all active GUIs (useful on plugin disable)
     */
    public void closeAllGuis() {
        for (Map.Entry<UUID, ImprovedMainGui> entry : activeGuis.entrySet()) {
            Player player = plugin.getServer().getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.closeInventory();
            }
        }
        activeGuis.clear();
    }
}