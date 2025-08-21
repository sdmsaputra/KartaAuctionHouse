package com.minekartastudio.kartaauctionhouse.storage;

import com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxEntry;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MailboxStorage {
    /**
     * Initializes the storage, e.g., creates database tables.
     */
    void init();

    /**
     * Adds a new entry to a player's mailbox.
     */
    CompletableFuture<Void> enqueue(MailboxEntry e);

    /**
     * Retrieves all unclaimed mailbox entries for a specific player.
     */
    CompletableFuture<List<MailboxEntry>> getUnclaimed(UUID owner);

    /**
     * Marks a specific mailbox entry as claimed.
     * @return A future completing with true if the update was successful, false otherwise.
     */
    CompletableFuture<Boolean> markClaimed(UUID entryId);
}
