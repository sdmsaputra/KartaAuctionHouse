package com.minekartastudio.kartaauctionhouse.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public abstract class Gui implements InventoryHolder, Listener {

    protected final JavaPlugin plugin;
    protected final Player player;
    protected Inventory inventory;

    public Gui(JavaPlugin plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    protected abstract String getTitle();
    protected abstract int getSize();
    protected abstract void build();
    protected abstract void onClick(InventoryClickEvent event);

    public void open() {
        inventory = Bukkit.createInventory(this, getSize(), getTitle());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        this.build();
        player.openInventory(inventory);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() != this) return;
        if (!(event.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(player.getUniqueId())) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || event.getClickedInventory().getHolder() != this) {
            return;
        }

        onClick(event);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() == this) {
            HandlerList.unregisterAll(this);
        }
    }
}
