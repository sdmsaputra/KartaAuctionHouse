package com.minekartastudio.kartaauctionhouse.tasks;

import com.minekartastudio.kartaauctionhouse.auction.AuctionItem;
import com.minekartastudio.kartaauctionhouse.auction.AuctionManager;
import com.minekartastudio.kartaauctionhouse.mailbox.MailboxManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class AuctionExpirer extends BukkitRunnable {

    private final AuctionManager auctionManager;
    private final MailboxManager mailboxManager;
    private final JavaPlugin plugin;

    public AuctionExpirer(JavaPlugin plugin, AuctionManager auctionManager, MailboxManager mailboxManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
        this.mailboxManager = mailboxManager;
    }

    @Override
    public void run() {
        for (AuctionItem auction : auctionManager.getActiveAuctions()) {
            if (auction.isExpired()) {
                auctionManager.removeAuction(auction.getId());
                mailboxManager.addItem(auction.getSeller(), auction.getItem());
                plugin.getLogger().info("Auction " + auction.getId() + " has expired and been moved to the seller's mailbox.");
            }
        }
    }
}
