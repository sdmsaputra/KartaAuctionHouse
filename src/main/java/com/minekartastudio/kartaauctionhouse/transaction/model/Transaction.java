package com.minekartastudio.kartaauctionhouse.transaction.model;

import com.minekartastudio.kartaauctionhouse.common.SerializedItem;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record Transaction(
    UUID transactionId,
    UUID auctionId,
    UUID sellerUuid,
    @Nullable UUID buyerUuid,
    SerializedItem itemSnapshot,
    @Nullable Double finalPrice,
    String status,
    long timestamp
) {
}
