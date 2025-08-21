package com.minekartastudio.kartaauctionhouse.gui.model;

public enum SortOrder {
    TIME_LEFT("Time Left"),
    PRICE_ASC("Price (Low to High)"),
    PRICE_DESC("Price (High to Low)"),
    NEWEST("Recently Listed");

    private final String displayName;
    SortOrder(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    public SortOrder next() { return values()[(this.ordinal() + 1) % values().length]; }
}
