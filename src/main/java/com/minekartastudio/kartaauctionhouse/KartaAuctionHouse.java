package com.minekartastudio.kartaauctionhouse;

import com.minekartastudio.kartaauctionhouse.auction.AuctionManager;
import com.minekartastudio.kartaauctionhouse.commands.AuctionCommand;
import com.minekartastudio.kartaauctionhouse.mailbox.MailboxManager;
import com.minekartastudio.kartaauctionhouse.tasks.AuctionExpirer;
import org.bukkit.plugin.java.JavaPlugin;

public class KartaAuctionHouse extends JavaPlugin {

    private AuctionManager auctionManager;
    private MailboxManager mailboxManager;

    @Override
    public void onEnable() {
        this.auctionManager = new AuctionManager();
        this.mailboxManager = new MailboxManager();
        this.getCommand("ah").setExecutor(new AuctionCommand(auctionManager, mailboxManager));

        new AuctionExpirer(this, auctionManager, mailboxManager).runTaskTimer(this, 0L, 1200L);

        getLogger().info("KartaAuctionHouse has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("KartaAuctionHouse has been disabled!");
    }
}
