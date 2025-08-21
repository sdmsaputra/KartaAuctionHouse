package com.minekartastudio.kartaauctionhouse.mailbox;

import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class MailboxManagerTest {

    private MailboxManager mailboxManager;

    @BeforeEach
    void setUp() {
        mailboxManager = new MailboxManager();
    }

    @Test
    void addItemAndGetItems() {
        UUID playerId = UUID.randomUUID();
        ItemStack item = mock(ItemStack.class);
        mailboxManager.addItem(playerId, item);

        assertTrue(mailboxManager.hasMail(playerId));
        assertEquals(1, mailboxManager.getItems(playerId).size());
        assertEquals(item, mailboxManager.getItems(playerId).get(0));
    }

    @Test
    void clearMailbox() {
        UUID playerId = UUID.randomUUID();
        ItemStack item = mock(ItemStack.class);
        mailboxManager.addItem(playerId, item);
        mailboxManager.clearMailbox(playerId);

        assertFalse(mailboxManager.hasMail(playerId));
        assertTrue(mailboxManager.getItems(playerId).isEmpty());
    }

    @Test
    void hasMail() {
        UUID playerId = UUID.randomUUID();
        assertFalse(mailboxManager.hasMail(playerId));

        ItemStack item = mock(ItemStack.class);
        mailboxManager.addItem(playerId, item);
        assertTrue(mailboxManager.hasMail(playerId));
    }
}
