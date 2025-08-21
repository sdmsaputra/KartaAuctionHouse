package com.minekartastudio.kartaauctionhouse.auction.model;

import com.minekartastudio.kartaauctionhouse.common.SerializedItem;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record Auction(
    UUID id,
    UUID seller,
    SerializedItem item,
    double startingPrice,
    @Nullable Double currentBid,
    @Nullable UUID currentBidder,
    @Nullable Double buyNowPrice,
    long createdAt,
    long endAt,
    AuctionStatus status,
    int version
) {
    /**
     * Creates a new Auction instance with an updated status.
     * @param newStatus The new status.
     * @return A new Auction object with the updated status.
     */
    public Auction withStatus(AuctionStatus newStatus) {
        return new Auction(id, seller, item, startingPrice, currentBid, currentBidder, buyNowPrice, createdAt, endAt, newStatus, version);
    }

    /**
     * Creates a new Auction instance with a new bid.
     * @param bidAmount The new bid amount.
     * @param bidder The new bidder's UUID.
     * @return A new Auction object with the updated bid information.
     */
    public Auction withNewBid(double bidAmount, UUID bidder) {
        return new Auction(id, seller, item, startingPrice, bidAmount, bidder, buyNowPrice, createdAt, endAt, status, version);
    }

    /**
     * Creates a new Auction instance with an incremented version.
     * @return A new Auction object with the version incremented by 1.
     */
    public Auction withIncrementedVersion() {
        return new Auction(id, seller, item, startingPrice, currentBid, currentBidder, buyNowPrice, createdAt, endAt, status, version + 1);
    }
}
