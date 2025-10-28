package com.minekarta.playerauction.transaction;

import com.minekarta.playerauction.auction.model.Auction;
import com.minekarta.playerauction.storage.TransactionStorage;
import com.minekarta.playerauction.transaction.model.Transaction;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TransactionLogger {

    private final TransactionStorage transactionStorage;

    public TransactionLogger(TransactionStorage transactionStorage) {
        this.transactionStorage = transactionStorage;
    }

    public CompletableFuture<Void> log(Auction auction, String status) {
        return log(auction, status, null, auction.price());
    }

    public CompletableFuture<Void> log(Auction auction, String status, UUID buyer, Double price) {
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                auction.id(),
                status,
                buyer,
                auction.seller(),
                price,
                auction.item().toItemStack().getType().toString() + " for " + (price != null ? price : auction.price()),
                auction.item(),
                System.currentTimeMillis()
        );
        return transactionStorage.logTransaction(transaction);
    }

    public CompletableFuture<java.util.List<Transaction>> getHistory(UUID playerId, int page, int pageSize) {
        return transactionStorage.findTransactionsByPlayer(playerId, pageSize, (page - 1) * pageSize);
    }
}
