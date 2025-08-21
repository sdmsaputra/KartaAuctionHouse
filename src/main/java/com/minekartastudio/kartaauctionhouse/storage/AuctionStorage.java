package com.minekartastudio.kartaauctionhouse.storage;

import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.auction.model.Bid;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AuctionStorage {
    /**
     * Initializes the storage, e.g., creates database tables.
     */
    void init();

    /**
     * Finds an auction by its unique ID.
     */
    CompletableFuture<Optional<Auction>> findById(UUID id);

    /**
     * Finds a list of active auctions, for pagination.
     */
    CompletableFuture<List<Auction>> findActive(int limit, int offset);

    /**
     * Finds auctions listed by a specific seller.
     */
    CompletableFuture<List<Auction>> findBySeller(UUID seller, int limit, int offset);

    /**
     * Inserts a new auction into the storage.
     */
    CompletableFuture<Void> insertAuction(Auction a);

    /**
     * Updates an auction only if the provided version matches the one in storage.
     * This is for optimistic concurrency control.
     * @return A future completing with true if the update was successful, false otherwise.
     */
    CompletableFuture<Boolean> updateAuctionIfVersionMatches(Auction a, int expectedVersion);

    /**
     * Inserts a new bid record into the storage.
     */
    CompletableFuture<Void> insertBid(Bid b);

    /**
     * Finds a batch of auctions that have expired as of a given timestamp.
     */
    CompletableFuture<List<Auction>> findExpiredUpTo(long nowEpochMillis, int batchSize);
}
