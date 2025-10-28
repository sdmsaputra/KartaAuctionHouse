package com.minekarta.playerauction.tasks;

import com.minekarta.playerauction.auction.AuctionService;
import org.bukkit.scheduler.BukkitRunnable;

public class AuctionExpirer extends BukkitRunnable {

    private final AuctionService auctionService;

    public AuctionExpirer(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public void run() {
        auctionService.processExpiredAuctions();
    }
}
