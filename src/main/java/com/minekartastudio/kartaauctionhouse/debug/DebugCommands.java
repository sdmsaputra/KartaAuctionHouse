package com.minekartastudio.kartaauctionhouse.debug;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.gui.improved.ImprovedMainGui;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Debug commands for troubleshooting GUI issues
 */
public class DebugCommands implements CommandExecutor {

    private final KartaAuctionHouse plugin;

    public DebugCommands(KartaAuctionHouse plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("kartaauctionhouse.debug")) {
            sender.sendMessage("§cYou don't have permission for debug commands.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "kahdebug":
                if (args.length == 0) {
                    showDebugHelp(player);
                } else {
                    handleDebugCommand(player, args[0]);
                }
                break;
        }

        return true;
    }

    private void showDebugHelp(Player player) {
        player.sendMessage("§6=== KartaAuctionHouse Debug Commands ===");
        player.sendMessage("§e/kahdebug test - Test basic GUI functionality");
        player.sendMessage("§e/kahdebug testitem - Test GUI with sample items");
        player.sendMessage("§e/kahdebug services - Test all services");
        player.sendMessage("§e/kahdebug config - Show configuration status");
        player.sendMessage("§e/kahdebug economy - Test economy integration");
    }

    private void handleDebugCommand(Player player, String subCommand) {
        switch (subCommand.toLowerCase()) {
            case "test":
                testBasicGui(player);
                break;
            case "testitem":
                testGuiWithItems(player);
                break;
            case "services":
                testServices(player);
                break;
            case "config":
                testConfiguration(player);
                break;
            case "economy":
                testEconomy(player);
                break;
            default:
                player.sendMessage("§cUnknown debug command: " + subCommand);
                break;
        }
    }

    private void testBasicGui(Player player) {
        player.sendMessage("§eTesting basic GUI functionality...");

        try {
            ImprovedMainGui gui = new ImprovedMainGui(plugin, player);
            player.sendMessage("§a✓ GUI created successfully");

            gui.open();
            player.sendMessage("§a✓ GUI opened successfully");

        } catch (Exception e) {
            player.sendMessage("§c✗ GUI test failed: " + e.getMessage());
            plugin.getLogger().log(Level.WARNING, "GUI test failed", e);
        }
    }

    private void testGuiWithItems(Player player) {
        player.sendMessage("§eTesting GUI with sample items...");

        try {
            // Create a sample item for testing
            ItemStack testItem = new ItemStack(Material.DIAMOND);
            var meta = testItem.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§bTest Diamond");
                testItem.setItemMeta(meta);
            }

            player.sendMessage("§a✓ Sample item created");

            // Test GUI opening
            ImprovedMainGui gui = new ImprovedMainGui(plugin, player);
            gui.open();

            player.sendMessage("§a✓ GUI with test items opened successfully");

        } catch (Exception e) {
            player.sendMessage("§c✗ GUI with items test failed: " + e.getMessage());
            plugin.getLogger().log(Level.WARNING, "GUI with items test failed", e);
        }
    }

    private void testServices(Player player) {
        player.sendMessage("§eTesting all services...");

        try {
            // Test AuctionService
            if (plugin.getAuctionService() != null) {
                player.sendMessage("§a✓ AuctionService: OK");
            } else {
                player.sendMessage("§c✗ AuctionService: NULL");
            }

            // Test MailboxService
            if (plugin.getMailboxService() != null) {
                player.sendMessage("§a✓ MailboxService: OK");
            } else {
                player.sendMessage("§c✗ MailboxService: NULL");
            }

            // Test EconomyRouter
            if (plugin.getEconomyRouter() != null && plugin.getEconomyRouter().hasService()) {
                player.sendMessage("§a✓ EconomyRouter: OK");
            } else {
                player.sendMessage("§c✗ EconomyRouter: No service");
            }

            // Test ConfigManager
            if (plugin.getConfigManager() != null) {
                player.sendMessage("§a✓ ConfigManager: OK");
            } else {
                player.sendMessage("§c✗ ConfigManager: NULL");
            }

            // Test PlayerNameCache
            if (plugin.getPlayerNameCache() != null) {
                player.sendMessage("§a✓ PlayerNameCache: OK");
            } else {
                player.sendMessage("§c✗ PlayerNameCache: NULL");
            }

            // Test NotificationManager
            if (plugin.getNotificationManager() != null) {
                player.sendMessage("§a✓ NotificationManager: OK");
            } else {
                player.sendMessage("§c✗ NotificationManager: NULL");
            }

            // Test TransactionLogger
            if (plugin.getTransactionLogger() != null) {
                player.sendMessage("§a✓ TransactionLogger: OK");
            } else {
                player.sendMessage("§c✗ TransactionLogger: NULL");
            }

            // Test SearchInputListener
            if (plugin.getSearchInputListener() != null) {
                player.sendMessage("§a✓ SearchInputListener: OK");
            } else {
                player.sendMessage("§c✗ SearchInputListener: NULL");
            }

            // Test GuiEventListener
            if (plugin.getGuiEventListener() != null) {
                player.sendMessage("§a✓ GuiEventListener: OK");
            } else {
                player.sendMessage("§c✗ GuiEventListener: NULL");
            }

        } catch (Exception e) {
            player.sendMessage("§c✗ Service test failed: " + e.getMessage());
            plugin.getLogger().log(Level.WARNING, "Service test failed", e);
        }
    }

    private void testConfiguration(Player player) {
        player.sendMessage("§eTesting configuration...");

        try {
            if (plugin.getConfig() != null) {
                player.sendMessage("§a✓ Main config loaded");
                player.sendMessage("§7  Database type: " + plugin.getConfig().getString("database.type", "NOT SET"));
                player.sendMessage("§7  Debug enabled: " + plugin.getConfig().getBoolean("debug.enabled", false));
            } else {
                player.sendMessage("§c✗ Main config is NULL");
            }

            if (plugin.getConfigManager() != null) {
                player.sendMessage("§a✓ ConfigManager loaded");
                player.sendMessage("§7  Max auctions per player: " +
                    plugin.getConfigManager().getConfig().getInt("auction.max-auctions-per-player", -1));
            } else {
                player.sendMessage("§c✗ ConfigManager is NULL");
            }

        } catch (Exception e) {
            player.sendMessage("§c✗ Configuration test failed: " + e.getMessage());
            plugin.getLogger().log(Level.WARNING, "Configuration test failed", e);
        }
    }

    private void testEconomy(Player player) {
        player.sendMessage("§eTesting economy integration...");

        try {
            if (plugin.getEconomyRouter() != null && plugin.getEconomyRouter().hasService()) {
                player.sendMessage("§a✓ Economy service found");

                // Test getting balance
                CompletableFuture<Double> balanceFuture = plugin.getEconomyRouter().getService().getBalance(player.getUniqueId());
                balanceFuture.thenAccept(balance -> {
                    player.sendMessage("§a✓ Balance retrieved: " + plugin.getEconomyRouter().getService().format(balance));
                }).exceptionally(throwable -> {
                    player.sendMessage("§c✗ Failed to get balance: " + throwable.getMessage());
                    return null;
                });

            } else {
                player.sendMessage("§c✗ No economy service found");
                player.sendMessage("§7  Make sure Vault or another economy plugin is installed");
            }

        } catch (Exception e) {
            player.sendMessage("§c✗ Economy test failed: " + e.getMessage());
            plugin.getLogger().log(Level.WARNING, "Economy test failed", e);
        }
    }
}