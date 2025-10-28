package com.minekarta.playerauction.commands;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.auction.AuctionService;
import com.minekarta.playerauction.config.ConfigManager;
import com.minekarta.playerauction.gui.HistoryGui;
import com.minekarta.playerauction.gui.MainAuctionGui;
import com.minekarta.playerauction.gui.MyListingsGui;
import com.minekarta.playerauction.gui.model.SortOrder;
import com.minekarta.playerauction.util.DurationParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AuctionCommand implements CommandExecutor {

    private final PlayerAuction plugin;
    private final AuctionService auctionService;
    private final ConfigManager configManager;
    private final com.minekarta.playerauction.players.PlayerSettingsService playerSettingsService;

    public AuctionCommand(PlayerAuction plugin, AuctionService auctionService, ConfigManager configManager, com.minekarta.playerauction.players.PlayerSettingsService playerSettingsService) {
        this.plugin = plugin;
        this.auctionService = auctionService;
        this.configManager = configManager;
        this.playerSettingsService = playerSettingsService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(configManager.getPrefixedMessage("errors.player-only"));
            return true;
        }

        if (args.length == 0) {
            new MainAuctionGui(plugin, player, 1, SortOrder.NEWEST, null).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "help" -> handleHelp(player);
            case "sell" -> handleSell(player, args);
            case "listings", "myauctions" -> {
                if (!player.hasPermission("playerauctions.use")) {
                    player.sendMessage(configManager.getPrefixedMessage("errors.no-permission"));
                    return true;
                }
                new MyListingsGui(plugin, player, 1).open();
            }
            case "reload" -> handleReload(player);
            case "search" -> handleSearch(player, args);
            case "notify" -> handleNotify(player, args);
            case "history" -> handleHistory(player, args);
            default -> {
                player.sendMessage(configManager.getPrefixedMessage("errors.unknown-command",
                    "{command}", subCommand,
                    "{usage}", "/" + label + " help"));
                new MainAuctionGui(plugin, player, 1, SortOrder.NEWEST, null).open();
            }
        }

        return true;
    }

    private void handleSell(Player player, String[] args) {
        if (!player.hasPermission("playerauctions.sell")) {
            player.sendMessage(configManager.getPrefixedMessage("errors.no-permission")); // Assumes no-permission message exists
            return;
        }

        if (args.length < 2) {
            player.sendMessage(configManager.getPrefixedMessage("errors.usage-sell", "{usage}", "/ah sell <price> [buyNow] [duration]"));
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType().isAir()) {
            player.sendMessage(configManager.getPrefixedMessage("errors.no-item-in-hand"));
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(configManager.getPrefixedMessage("errors.not-a-number"));
            return;
        }

        if (price < configManager.getConfig().getDouble("auction.min-price", 1.0)) {
             player.sendMessage(configManager.getPrefixedMessage("errors.price-too-low", "{min}", String.valueOf(configManager.getConfig().getDouble("auction.min-price", 1.0))));
             return;
        }

        Double buyNow = args.length > 2 ? Double.parseDouble(args[2]) : null;
        String durationStr = args.length > 3 ? args[3] : configManager.getConfig().getString("auction.defaults.duration", "24h");

        long durationMillis = DurationParser.parse(durationStr).orElse(0L);
        if (durationMillis <= 0) {
            player.sendMessage(configManager.getPrefixedMessage("errors.duration-out-of-range", "{min}", "1s", "{max}", "infinite")); // Placeholder
            return;
        }

        int maxAuctions = configManager.getConfig().getInt("auction.max-auctions-per-player", 5);
        auctionService.getPlayerActiveAuctionCount(player.getUniqueId()).thenAccept(count -> {
            if (count >= maxAuctions) {
                player.sendMessage(configManager.getPrefixedMessage("errors.listing-limit-reached", "{limit}", String.valueOf(maxAuctions)));
                return;
            }

            ItemStack toSell = itemInHand.clone();
            player.getInventory().setItemInMainHand(null); // Remove item from hand

            auctionService.createListing(player, toSell, price, buyNow, null, durationMillis).thenAccept(success -> {
                if (success) {
                    player.sendMessage(configManager.getPrefixedMessage("info.listed", "{item}", toSell.getType().toString(), "{price}", String.valueOf(price)));
                } else {
                    player.sendMessage(configManager.getPrefixedMessage("errors.generic-error", "Failed to list item."));
                    player.getInventory().addItem(toSell); // Return item on failure
                }
            });
        });
    }

    private void handleReload(Player player) {
        if (!player.hasPermission("playerauctions.reload")) {
            player.sendMessage(configManager.getPrefixedMessage("errors.no-permission"));
            return;
        }
        configManager.loadConfigs();
        player.sendMessage(configManager.getPrefixedMessage("info.reload-success"));
    }

    private void handleSearch(Player player, String[] args) {
        if (!player.hasPermission("playerauctions.search")) {
            player.sendMessage(configManager.getPrefixedMessage("errors.no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(configManager.getPrefixedMessage("errors.usage-search", "{usage}", "/ah search <keyword>"));
            return;
        }

        String searchQuery = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        new MainAuctionGui(plugin, player, 1, SortOrder.NEWEST, searchQuery).open();
    }

    private void handleNotify(Player player, String[] args) {
        if (!player.hasPermission("playerauctions.notify")) {
            player.sendMessage(configManager.getPrefixedMessage("errors.no-permission"));
            return;
        }

        if (args.length < 2 || (!args[1].equalsIgnoreCase("on") && !args[1].equalsIgnoreCase("off"))) {
            player.sendMessage(configManager.getPrefixedMessage("errors.usage-notify", "{usage}", "/ah notify <on/off>"));
            return;
        }

        boolean enabled = args[1].equalsIgnoreCase("on");
        playerSettingsService.setNotificationsEnabled(player, enabled);

        if (enabled) {
            player.sendMessage(configManager.getPrefixedMessage("info.notifications-on", "Auction notifications have been enabled."));
        } else {
            player.sendMessage(configManager.getPrefixedMessage("info.notifications-off", "Auction notifications have been disabled."));
        }
    }

    private void handleHistory(Player player, String[] args) {
        if (!player.hasPermission("playerauctions.history")) {
            player.sendMessage(configManager.getPrefixedMessage("errors.no-permission"));
            return;
        }

        if (args.length > 1) {
            if (!player.hasPermission("playerauctions.history.others")) {
                player.sendMessage(configManager.getPrefixedMessage("errors.no-permission"));
                return;
            }
            org.bukkit.OfflinePlayer targetPlayer = org.bukkit.Bukkit.getOfflinePlayer(args[1]);
            if (!targetPlayer.hasPlayedBefore() && targetPlayer.getUniqueId() == null) {
                player.sendMessage(configManager.getPrefixedMessage("errors.player-not-found", "{player}", args[1]));
                return;
            }
            new HistoryGui(plugin, player, targetPlayer.getUniqueId(), 1).open();
        } else {
            new HistoryGui(plugin, player, player.getUniqueId(), 1).open();
        }
    }

    private void handleHelp(Player player) {
        if (!player.hasPermission("playerauctions.use")) {
            player.sendMessage(configManager.getPrefixedMessage("errors.no-permission"));
            return;
        }

        player.sendMessage("§6§lPlayerAuctions Help");
        player.sendMessage("§7─────────────────────────");
        player.sendMessage("§e/ah §7- Opens the auction house GUI");

        if (player.hasPermission("playerauctions.sell")) {
            player.sendMessage("§e/ah sell <price> [buy_now] [duration] §7- Sell item in hand");
            player.sendMessage("§7  §8Price: Starting bid amount");
            player.sendMessage("§7  §8Buy Now: Instant purchase price (optional)");
            player.sendMessage("§7  §8Duration: 1h, 6h, 12h, 24h, 48h, 72h (default: 24h)");
        }

        if (player.hasPermission("playerauctions.search")) {
            player.sendMessage("§e/ah search <keyword> §7- Search for auction items");
        }

        if (player.hasPermission("playerauctions.use")) {
            player.sendMessage("§e/ah listings §7- View your auction listings");
            player.sendMessage("§e/ah myauctions §7- View your auction listings");
        }

        if (player.hasPermission("playerauctions.notify")) {
            player.sendMessage("§e/ah notify <on/off> §7- Toggle auction notifications");
        }

        if (player.hasPermission("playerauctions.history")) {
            player.sendMessage("§e/ah history [player] §7- View auction history");
        }

        if (player.hasPermission("playerauctions.reload")) {
            player.sendMessage("§e/ah reload §7- Reload plugin configuration");
        }

        player.sendMessage("§7─────────────────────────");
        player.sendMessage("§6Aliases: §f/ah, /auction, /auctionhouse");
    }
}
