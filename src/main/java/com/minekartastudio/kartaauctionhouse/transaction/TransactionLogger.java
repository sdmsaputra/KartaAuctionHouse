package com.minekartastudio.kartaauctionhouse.transaction;

import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.storage.TransactionStorage;
import com.minekartastudio.kartaauctionhouse.transaction.model.Transaction;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TransactionLogger {

    private final TransactionStorage transactionStorage;

    public TransactionLogger(TransactionStorage transactionStorage) {
        this.transactionStorage = transactionStorage;
    }

    public CompletableFuture<Void> log(Auction auction, String status) {
        return log(auction, status, auction.currentBidder(), auction.currentBid());
    }

    public CompletableFuture<Void> log(Auction auction, String status, UUID buyer, Double price) {
        Transaction transaction = new Transaction(
                UUID.randomUUID(),
                auction.id(),
                auction.seller(),
                buyer,
                auction.item(),
                price,
                status,
                System.currentTimeMillis()
        );
        return transactionStorage.logTransaction(transaction);
    }

    public CompletableFuture<java.util.List<Transaction>> getHistory(UUID playerId, int page, int pageSize) {
        return transactionStorage.findTransactionsByPlayer(playerId, pageSize, (page - 1) * pageSize);
    }
}
