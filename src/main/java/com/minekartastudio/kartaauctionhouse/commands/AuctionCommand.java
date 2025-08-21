package com.minekartastudio.kartaauctionhouse.commands;

import com.minekartastudio.kartaauctionhouse.auction.AuctionItem;
import com.minekartastudio.kartaauctionhouse.auction.AuctionManager;
import com.minekartastudio.kartaauctionhouse.mailbox.MailboxManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class AuctionCommand implements CommandExecutor {

    private final AuctionManager auctionManager;
    private final MailboxManager mailboxManager;

    public AuctionCommand(AuctionManager auctionManager, MailboxManager mailboxManager) {
        this.auctionManager = auctionManager;
        this.mailboxManager = mailboxManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            // Handle /ah command - for now, just a message
            player.sendMessage(ChatColor.GOLD + "Welcome to KartaAuctionHouse!");
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "sell":
                handleSellCommand(player, args);
                break;
            case "listings":
                player.sendMessage(ChatColor.YELLOW + "Your listings are not yet implemented.");
                break;
            case "mailbox":
                handleMailboxCommand(player);
                break;
            case "bid":
                // To be implemented
                player.sendMessage(ChatColor.YELLOW + "Bidding is not yet implemented.");
                break;
            case "buy":
                // To be implemented
                player.sendMessage(ChatColor.YELLOW + "Buying is not yet implemented.");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /ah sell, /ah bid, or /ah buy.");
                break;
        }

        return true;
    }

    private void handleMailboxCommand(Player player) {
        if (!mailboxManager.hasMail(player.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You have no mail.");
            return;
        }

        List<ItemStack> items = mailboxManager.getItems(player.getUniqueId());
        player.sendMessage(ChatColor.GOLD + "Your mailbox contains:");
        for (ItemStack item : items) {
            player.sendMessage(ChatColor.GRAY + "- " + item.getAmount() + "x " + item.getType().name());
        }

        // For now, we will clear the mailbox after viewing.
        // In the future, this will be handled by a GUI.
        mailboxManager.clearMailbox(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Your mailbox has been cleared.");
    }

    private void handleSellCommand(Player player, String[] args) {
        if (!player.hasPermission("kartaauctionshouse.sell")) {
            player.sendMessage(ChatColor.RED + "You don't have permission to sell items.");
            return;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /ah sell <price>");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid price specified.");
            return;
        }

        if (price <= 0) {
            player.sendMessage(ChatColor.RED + "Price must be positive.");
            return;
        }

        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType().isAir()) {
            player.sendMessage(ChatColor.RED + "You must be holding an item to sell.");
            return;
        }

        // Default expiration: 24 hours from now
        long expiration = System.currentTimeMillis() + (24 * 60 * 60 * 1000);

        AuctionItem auctionItem = new AuctionItem(player.getUniqueId(), itemInHand.clone(), price, expiration);
        auctionManager.addAuction(auctionItem);

        player.getInventory().setItemInMainHand(null);
        player.sendMessage(ChatColor.GREEN + "You have listed your item for sale for " + price + "!");
    }
}
