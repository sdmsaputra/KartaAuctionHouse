package com.minekarta.playerauction.transaction.model;

import com.minekarta.playerauction.auction.model.Auction;
import com.minekarta.playerauction.common.SerializedItem;
import java.util.UUID;

public record Transaction(
    UUID id,
    UUID auctionId,
    String actionType,
    UUID actorUuid,
    UUID sellerUuid,
    Double amount,
    String details,
    SerializedItem itemSnapshot,
    long timestamp
) {

    // Convenience methods for HistoryGui compatibility
    public String status() { return actionType; }
    public UUID buyerUuid() { return actorUuid; }
    public Double finalPrice() { return amount; }
    public UUID transactionId() { return id; }
}