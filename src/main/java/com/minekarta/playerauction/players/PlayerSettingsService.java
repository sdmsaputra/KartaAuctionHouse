package com.minekarta.playerauction.players;

import com.minekarta.playerauction.PlayerAuction;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerSettingsService {

    private final PlayerAuction plugin;
    private final NamespacedKey notificationsEnabledKey;

    public PlayerSettingsService(PlayerAuction plugin) {
        this.plugin = plugin;
        this.notificationsEnabledKey = new NamespacedKey(plugin, "notifications_enabled");
    }

    public boolean getNotificationsEnabled(Player player) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        // Default to true (notifications on) if the key doesn't exist
        return data.getOrDefault(notificationsEnabledKey, PersistentDataType.BYTE, (byte) 1) == (byte) 1;
    }

    public void setNotificationsEnabled(Player player, boolean enabled) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        data.set(notificationsEnabledKey, PersistentDataType.BYTE, enabled ? (byte) 1 : (byte) 0);
    }
}
