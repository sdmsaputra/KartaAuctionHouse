package com.minekartastudio.kartaauctionhouse.auction;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AuctionManagerTest {

    private AuctionManager auctionManager;

    @BeforeEach
    void setUp() {
        auctionManager = new AuctionManager();
    }

    @Test
    void addAndGetAuction() {
        AuctionItem auctionItem = new AuctionItem(UUID.randomUUID(), mock(ItemStack.class), 100.0, System.currentTimeMillis() + 1000);
        auctionManager.addAuction(auctionItem);

        assertEquals(auctionItem, auctionManager.getAuction(auctionItem.getId()));
        assertTrue(auctionManager.getActiveAuctions().contains(auctionItem));
    }

    @Test
    void removeAuction() {
        AuctionItem auctionItem = new AuctionItem(UUID.randomUUID(), mock(ItemStack.class), 100.0, System.currentTimeMillis() + 1000);
        auctionManager.addAuction(auctionItem);
        auctionManager.removeAuction(auctionItem.getId());

        assertNull(auctionManager.getAuction(auctionItem.getId()));
        assertFalse(auctionManager.getActiveAuctions().contains(auctionItem));
    }
}
