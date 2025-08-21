package com.minekartastudio.kartaauctionhouse.auction;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class AuctionItemTest {

    @Test
    void isExpired() {
        long now = System.currentTimeMillis();
        ItemStack itemStack = mock(ItemStack.class);

        // Test with an expiration time in the future
        AuctionItem notExpiredItem = new AuctionItem(UUID.randomUUID(), itemStack, 100.0, now + 1000);
        assertFalse(notExpiredItem.isExpired());

        // Test with an expiration time in the past
        AuctionItem expiredItem = new AuctionItem(UUID.randomUUID(), itemStack, 100.0, now - 1000);
        assertTrue(expiredItem.isExpired());
    }
}
