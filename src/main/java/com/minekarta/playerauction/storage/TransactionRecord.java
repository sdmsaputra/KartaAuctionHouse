package com.minekarta.playerauction.storage;

import java.util.UUID;

public record TransactionRecord(
    UUID id,
    UUID auctionId,
    String actionType,
    UUID actorUuid,
    Double amount,
    String details,
    long timestamp
) {}