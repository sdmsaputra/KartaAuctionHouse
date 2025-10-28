package com.minekarta.playerauction.storage;

import com.minekarta.playerauction.transaction.model.Transaction;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TransactionStorage {
    /**
     * Initializes the storage medium (e.g., creates database tables).
     */
    void init();

    /**
     * Logs a transaction to the storage.
     * @param transaction The transaction to log.
     * @return A CompletableFuture that completes when the operation is finished.
     */
    CompletableFuture<Void> logTransaction(Transaction transaction);

    /**
     * Finds a list of transactions for a specific player (as either seller or buyer).
     * @param playerId The UUID of the player.
     * @param limit The maximum number of transactions to return.
     * @param offset The offset to start from (for pagination).
     * @return A future completing with the list of transactions.
     */
    CompletableFuture<List<Transaction>> findTransactionsByPlayer(UUID playerId, int limit, int offset);
}
