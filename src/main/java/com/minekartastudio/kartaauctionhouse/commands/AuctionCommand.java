package com.minekartastudio.kartaauctionhouse.commands;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.auction.AuctionService;
import com.minekartastudio.kartaauctionhouse.config.ConfigManager;
import com.minekartastudio.kartaauctionhouse.gui.improved.ImprovedMainGui;
import com.minekartastudio.kartaauctionhouse.gui.model.SortOrder;
import com.minekartastudio.kartaauctionhouse.mailbox.MailboxService;
import com.minekartastudio.kartaauctionhouse.util.DurationParser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AuctionCommand implements CommandExecutor {

    private final KartaAuctionHouse plugin;
    private final AuctionService auctionService;
    private final MailboxService mailboxService;
    private final ConfigManager configManager;
    private final com.minekartastudio.kartaauctionhouse.players.PlayerSettingsService playerSettingsService;

    public AuctionCommand(KartaAuctionHouse plugin, AuctionService auctionService, MailboxService mailboxService, ConfigManager configManager, com.minekartastudio.kartaauctionhouse.players.PlayerSettingsService playerSettingsService) {
        this.plugin = plugin;
        this.auctionService = auctionService;
        this.mailboxService = mailboxService;
        this.configManager = configManager;
        this.playerSettingsService = playerSettingsService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            new ImprovedMainGui(plugin, player).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "sell" -> handleSell(player, args);
            case "mailbox" -> new ImprovedMainGui(plugin, player).open();
            case "listings", "myauctions" -> new ImprovedMainGui(plugin, player).open();
            case "reload" -> handleReload(player);
            case "search" -> handleSearch(player, args);
            case "notify" -> handleNotify(player, args);
            case "history" -> handleHistory(player, args);
            default -> new ImprovedMainGui(plugin, player).open();
        }

        return true;
    }

    private void handleSell(Player player, String[] args) {
        if (!player.hasPermission("kartaauctionhouse.sell")) {
            player.sendMessage(configManager.getPrefixedMessage("errors.no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(configManager.getPrefixedMessage("errors.usage-sell", "{usage}", "/ah sell <price> [duration]"));
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

        String durationStr = args.length > 2 ? args[2] : configManager.getConfig().getString("auction.defaults.duration", "24h");

        long durationMillis = DurationParser.parse(durationStr).orElse(0L);
        if (durationMillis <= 0) {
            player.sendMessage(configManager.getPrefixedMessage("errors.duration-out-of-range", "{min}", "1s", "{max}", "infinite"));
            return;
        }

        int maxAuctions = configManager.getConfig().getInt("auction.max-auctions-per-player", 5);
        auctionService.countActiveAuctions(player).thenAccept(count -> {
            if (count >= maxAuctions) {
                player.sendMessage(configManager.getPrefixedMessage("errors.listing-limit-reached", "{limit}", String.valueOf(maxAuctions)));
                return;
            }

            ItemStack toSell = itemInHand.clone();
            player.getInventory().setItemInMainHand(null); // Remove item from hand

            auctionService.createListing(player, toSell, price, durationMillis).thenAccept(success -> {
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
        if (!player.hasPermission("kartaauctionhouse.reload")) {
            player.sendMessage(configManager.getPrefixedMessage("errors.no-permission"));
            return;
        }
        configManager.loadConfigs();
        player.sendMessage(configManager.getPrefixedMessage("info.reload-success"));
    }

    private void handleSearch(Player player, String[] args) {
        if (!player.hasPermission("kartaauctionhouse.search")) {
            player.sendMessage(configManager.getPrefixedMessage("errors.no-permission"));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(configManager.getPrefixedMessage("errors.usage-search", "{usage}", "/ah search <keyword>"));
            return;
        }

        String searchQuery = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        ImprovedMainGui searchGui = new ImprovedMainGui(plugin, player);
        searchGui.setSearchQuery(searchQuery);
        searchGui.open();
    }

    private void handleNotify(Player player, String[] args) {
        if (!player.hasPermission("kartaauctionhouse.notify")) {
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
        if (!player.hasPermission("kartaauctionhouse.history")) {
            player.sendMessage(configManager.getPrefixedMessage("errors.no-permission"));
            return;
        }

        // TODO: Implement HistoryGui with Inventory Framework
        player.sendMessage(configManager.getPrefixedMessage("errors.feature-not-available", "&cHistory feature is coming soon!"));
    }
}
