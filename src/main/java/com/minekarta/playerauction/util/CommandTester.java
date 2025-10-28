package com.minekarta.playerauction.util;

import com.minekarta.playerauction.PlayerAuction;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Utility class to test and validate command registration
 */
public class CommandTester {

    private final PlayerAuction plugin;

    public CommandTester(PlayerAuction plugin) {
        this.plugin = plugin;
    }

    /**
     * Test all command registrations and log results
     */
    public void testAllCommands() {
        plugin.getLogger().info("=== Testing Command Registration ===");

        // Test main AH command
        testCommand("ah", "AuctionCommand");

        // Test debug command (if enabled)
        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            testCommand("kahdebug", "DebugCommands");
        }

        plugin.getLogger().info("=== Command Testing Complete ===");
    }

    private void testCommand(String commandName, String expectedExecutor) {
        try {
            PluginCommand command = plugin.getCommand(commandName);
            if (command != null) {
                CommandExecutor executor = command.getExecutor();
                if (executor != null) {
                    String executorName = executor.getClass().getSimpleName();
                    if (executorName.contains(expectedExecutor) || expectedExecutor.contains(executorName)) {
                        plugin.getLogger().info("✓ /" + commandName + " registered with " + executorName);
                    } else {
                        plugin.getLogger().warning("/" + commandName + " registered with unexpected executor: " + executorName);
                    }
                } else {
                    plugin.getLogger().severe("✗ /" + commandName + " has no executor!");
                }
            } else {
                plugin.getLogger().severe("✗ /" + commandName + " not found!");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error testing /" + commandName, e);
        }
    }

    /**
     * Log all registered commands
     */
    public void logAllCommands() {
        plugin.getLogger().info("=== Registered Commands ===");

        String[] commandNames = {"ah", "kahdebug"};
        for (String commandName : commandNames) {
            PluginCommand command = plugin.getCommand(commandName);
            if (command != null) {
                plugin.getLogger().info("Command: /" + command.getName());
                plugin.getLogger().info("  Description: " + command.getDescription());
                plugin.getLogger().info("  Usage: " + command.getUsage());
                plugin.getLogger().info("  Aliases: " + command.getAliases());
                plugin.getLogger().info("  Permission: " + command.getPermission());
            }
        }
    }
}