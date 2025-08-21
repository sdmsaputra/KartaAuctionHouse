package com.minekartastudio.kartaauctionhouse.mailbox;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MailboxManager {
    private final Map<UUID, List<ItemStack>> mailboxes = new ConcurrentHashMap<>();

    public void addItem(UUID playerId, ItemStack item) {
        mailboxes.computeIfAbsent(playerId, k -> new ArrayList<>()).add(item);
    }

    public List<ItemStack> getItems(UUID playerId) {
        return mailboxes.getOrDefault(playerId, new ArrayList<>());
    }

    public void clearMailbox(UUID playerId) {
        mailboxes.remove(playerId);
    }

    public boolean hasMail(UUID playerId) {
        return mailboxes.containsKey(playerId) && !mailboxes.get(playerId).isEmpty();
    }
}
