package com.minekarta.playerauction.auction.model;

import com.minekarta.playerauction.common.SerializedItem;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record Auction(
    UUID id,
    UUID seller,
    SerializedItem item,
    double price,
    @Nullable Double buyNowPrice,
    @Nullable Double reservePrice,
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
        return new Auction(id, seller, item, price, buyNowPrice, reservePrice, createdAt, endAt, newStatus, version);
    }

    /**
     * Creates a new Auction instance with an incremented version.
     * @return A new Auction object with the version incremented by 1.
     */
    public Auction withIncrementedVersion() {
        return new Auction(id, seller, item, price, buyNowPrice, reservePrice, createdAt, endAt, status, version + 1);
    }
}
