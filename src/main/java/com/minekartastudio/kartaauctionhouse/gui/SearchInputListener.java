package com.minekartastudio.kartaauctionhouse.gui;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.gui.model.AuctionCategory;
import com.minekartastudio.kartaauctionhouse.gui.model.SortOrder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SearchInputListener implements Listener {

    private final KartaAuctionHouse plugin;
    private final Set<UUID> playersInSearchMode = ConcurrentHashMap.newKeySet();

    public SearchInputListener(KartaAuctionHouse plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void addPlayerToSearchMode(UUID playerId) {
        playersInSearchMode.add(playerId);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!playersInSearchMode.contains(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        playersInSearchMode.remove(player.getUniqueId());

        String searchQuery = event.getMessage();

        // Re-open the GUI with the search query on the main thread
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            new MainAuctionGui(plugin, player, 1, AuctionCategory.ALL, SortOrder.NEWEST, searchQuery).open();
        });
    }
}
