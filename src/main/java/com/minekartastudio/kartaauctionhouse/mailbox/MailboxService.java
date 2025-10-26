package com.minekartastudio.kartaauctionhouse.mailbox;

import com.minekartastudio.kartaauctionhouse.common.SerializedItem;
import com.minekartastudio.kartaauctionhouse.config.ConfigManager;
import com.minekartastudio.kartaauctionhouse.economy.EconomyRouter;
import com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxEntry;
import com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxType;
import com.minekartastudio.kartaauctionhouse.storage.MailboxStorage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MailboxService {

    private final JavaPlugin plugin;
    private final MailboxStorage mailboxStorage;
    private final EconomyRouter economyRouter;
    private final ConfigManager configManager;

    public MailboxService(JavaPlugin plugin, MailboxStorage mailboxStorage, EconomyRouter economyRouter, ConfigManager configManager) {
        this.plugin = plugin;
        this.mailboxStorage = mailboxStorage;
        this.economyRouter = economyRouter;
        this.configManager = configManager;
    }

    public CompletableFuture<Void> sendItem(UUID player, SerializedItem item, String note) {
        MailboxEntry entry = new MailboxEntry(
                UUID.randomUUID(),
                player,
                MailboxType.ITEM,
                item,
                null,
                note,
                System.currentTimeMillis(),
                false
        );
        return mailboxStorage.enqueue(entry);
    }

    public CompletableFuture<Void> sendMoney(UUID player, double amount, String note) {
        MailboxEntry entry = new MailboxEntry(
                UUID.randomUUID(),
                player,
                MailboxType.MONEY,
                null,
                amount,
                note,
                System.currentTimeMillis(),
                false
        );
        return mailboxStorage.enqueue(entry);
    }

    public CompletableFuture<List<MailboxEntry>> getUnclaimed(UUID player) {
        return mailboxStorage.getUnclaimed(player);
    }
    
    public CompletableFuture<Integer> getMailboxCount(UUID player) {
        return mailboxStorage.countUnclaimed(player);
    }

    public CompletableFuture<Boolean> claimEntry(Player player, MailboxEntry entryToClaim) {
        if (entryToClaim.type() == MailboxType.MONEY) {
            return claimMoney(player, entryToClaim);
        } else {
            return claimItem(player, entryToClaim);
        }
    }

    private CompletableFuture<Boolean> claimMoney(Player player, MailboxEntry entry) {
        return economyRouter.getService().deposit(player.getUniqueId(), entry.amount(), entry.note())
                .thenCompose(v -> mailboxStorage.markClaimed(entry.id()))
                .thenApply(success -> {
                    if (success) {
                        player.sendMessage(configManager.getPrefixedMessage("mailbox.claimed-money", "{amount}", economyRouter.getService().format(entry.amount())));
                    }
                    return success;
                }).exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to claim money for " + player.getName() + ": " + ex.getMessage());
                    player.sendMessage(configManager.getPrefixedMessage("errors.economy-fail"));
                    return false;
                });
    }

    private CompletableFuture<Boolean> claimItem(Player player, MailboxEntry entry) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack itemStack = entry.item().toItemStack();
            Map<Integer, ItemStack> returned = player.getInventory().addItem(itemStack);

            if (returned.isEmpty()) {
                // Successfully added to inventory, now mark as claimed
                mailboxStorage.markClaimed(entry.id()).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(configManager.getPrefixedMessage("mailbox.claimed-item", "{item}", itemStack.getType().name()));
                        future.complete(true);
                    } else {
                        // This is a bad state, we gave the item but failed to mark it claimed.
                        // A robust solution would try to revert or use a two-phase commit.
                        plugin.getLogger().severe("CRITICAL: Gave item " + itemStack.getType() + " to " + player.getName() + " but FAILED to mark mailbox entry " + entry.id() + " as claimed.");
                        player.getInventory().removeItem(itemStack); // Attempt to revert
                        player.sendMessage(configManager.getPrefixedMessage("errors.generic-error", "An error occurred, the item was not claimed."));
                        future.complete(false);
                    }
                });
            } else {
                // Inventory is full
                player.sendMessage(configManager.getPrefixedMessage("errors.inventory-full"));
                future.complete(false);
            }
        });
        return future;
    }
}
