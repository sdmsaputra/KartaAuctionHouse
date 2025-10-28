package com.minekarta.playerauction.storage;

import com.minekarta.playerauction.auction.model.Auction;
import com.minekarta.playerauction.gui.model.AuctionCategory;
import com.minekarta.playerauction.gui.model.SortOrder;

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
     * Finds a list of active auctions with filtering and sorting.
     */
    CompletableFuture<List<Auction>> findActive(int limit, int offset, AuctionCategory category, SortOrder sortOrder, String searchQuery);

    /**
     * Finds a list of active auctions with pagination support.
     */
    CompletableFuture<List<Auction>> findActiveAuctions(int page, int limit, AuctionCategory category, SortOrder sortOrder, String searchQuery);

    /**
     * Finds auctions listed by a specific seller.
     */
    CompletableFuture<List<Auction>> findBySeller(UUID seller, int limit, int offset);

    /**
     * Finds player's auction history.
     */
    CompletableFuture<List<Auction>> findPlayerHistory(UUID playerId, int page, int limit);

    /**
     * Counts the number of active auctions for a specific seller.
     */
    CompletableFuture<Integer> countActiveBySeller(UUID sellerId);

    /**
     * Counts the number of active auctions for a specific seller (alternative method name).
     */
    CompletableFuture<Integer> countActiveAuctionsByPlayer(UUID playerId);

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
     * Finds a batch of auctions that have expired as of a given timestamp.
     */
    CompletableFuture<List<Auction>> findExpiredUpTo(long nowEpochMillis, int batchSize);
}
