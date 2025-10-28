package com.minekarta.playerauction.commands;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.config.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Admin command handler for PlayerAuction
 * Handles commands like /ah admin debug on/off
 */
public class AdminCommand implements CommandExecutor {
    
    private final PlayerAuction plugin;
    private final ConfigManager configManager;
    
    // Keep track of players in debug mode
    private final Set<UUID> debugPlayers = new HashSet<>();
    
    public AdminCommand(PlayerAuction plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }
        
        // Currently, AdminCommand doesn't handle any commands, so show a message
        player.sendMessage("The /ahadmin command does not support any subcommands.");
        return true;
    }
    
    /**
     * Check if a player is in debug mode
     * @param player The player to check
     * @return true if the player is in debug mode, false otherwise
     */
    public boolean isInDebugMode(Player player) {
        return debugPlayers.contains(player.getUniqueId());
    }
    
    /**
     * Check if a player is in debug mode by UUID
     * @param playerId The player UUID to check
     * @return true if the player is in debug mode, false otherwise
     */
    public boolean isInDebugMode(UUID playerId) {
        return debugPlayers.contains(playerId);
    }
    
    /**
     * Add a player to debug mode
     * @param playerId The player UUID to add
     */
    public void addPlayerToDebugMode(UUID playerId) {
        debugPlayers.add(playerId);
    }
    
    /**
     * Remove a player from debug mode
     * @param playerId The player UUID to remove
     */
    public void removePlayerFromDebugMode(UUID playerId) {
        debugPlayers.remove(playerId);
    }
}