package com.minekartastudio.kartaauctionhouse.auction;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private final Map<UUID, AuctionItem> activeAuctions = new ConcurrentHashMap<>();

    public void addAuction(AuctionItem auction) {
        activeAuctions.put(auction.getId(), auction);
    }

    public void removeAuction(UUID auctionId) {
        activeAuctions.remove(auctionId);
    }

    public AuctionItem getAuction(UUID auctionId) {
        return activeAuctions.get(auctionId);
    }

    public Collection<AuctionItem> getActiveAuctions() {
        return Collections.unmodifiableCollection(activeAuctions.values());
    }
}
