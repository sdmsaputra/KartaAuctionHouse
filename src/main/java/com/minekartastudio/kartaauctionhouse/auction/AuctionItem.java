package com.minekartastudio.kartaauctionhouse.auction;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class AuctionItem {
    private final UUID id;
    private final UUID seller;
    private final ItemStack item;
    private final double price;
    private final long expiration;

    public AuctionItem(UUID seller, ItemStack item, double price, long expiration) {
        this.id = UUID.randomUUID();
        this.seller = seller;
        this.item = item;
        this.price = price;
        this.expiration = expiration;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSeller() {
        return seller;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public long getExpiration() {
        return expiration;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiration;
    }
}
