package com.minekartastudio.kartaauctionhouse.commands;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.auction.AuctionService;
import com.minekartastudio.kartaauctionhouse.config.ConfigManager;
import com.minekartastudio.kartaauctionhouse.gui.MainAuctionGui;
import com.minekartastudio.kartaauctionhouse.gui.MailboxGui;
import com.minekartastudio.kartaauctionhouse.gui.MyListingsGui;
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

    public AuctionCommand(KartaAuctionHouse plugin, AuctionService auctionService, MailboxService mailboxService, ConfigManager configManager) {
        this.plugin = plugin;
        this.auctionService = auctionService;
        this.mailboxService = mailboxService;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            new MainAuctionGui(plugin, player, 1).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "sell" -> handleSell(player, args);
            case "mailbox" -> new MailboxGui(plugin, player, 1).open();
            case "listings", "myauctions" -> new MyListingsGui(plugin, player, 1).open();
            case "reload" -> handleReload(player);
            // Bidding and buying are handled via GUI, but you could add command versions
            // case "bid" -> handleBid(player, args);
            // case "cancel" -> handleCancel(player, args);
            default -> new MainAuctionGui(plugin, player, 1).open();
        }

        return true;
    }

    private void handleSell(Player player, String[] args) {
        if (!player.hasPermission("kartaauctionhouse.sell")) {
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

        ItemStack toSell = itemInHand.clone();
        player.getInventory().setItemInMainHand(null); // Remove item from hand

        auctionService.createListing(player, toSell, price, buyNow, durationMillis).thenAccept(success -> {
            if (success) {
                player.sendMessage(configManager.getPrefixedMessage("info.listed", "{item}", toSell.getType().toString(), "{price}", String.valueOf(price)));
            } else {
                player.sendMessage(configManager.getPrefixedMessage("errors.generic-error", "Failed to list item."));
                player.getInventory().addItem(toSell); // Return item on failure
            }
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
}
