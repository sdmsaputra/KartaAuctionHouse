package com.minekartastudio.kartaauctionhouse.auction.model;

import java.util.UUID;

public record Bid(
    UUID id,
    UUID auctionId,
    UUID bidder,
    double amount,
    long createdAt
) {}
