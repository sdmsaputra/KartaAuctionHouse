package com.minekartastudio.kartaauctionhouse.gui;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.gui.model.AuctionCategory;
import com.minekartastudio.kartaauctionhouse.gui.model.SortOrder;
import com.minekartastudio.kartaauctionhouse.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MainAuctionGui extends PaginatedGui {

    private final KartaAuctionHouse kah;
    private List<Auction> auctions;
    private final AuctionCategory category;
    private final SortOrder sortOrder;
    private final String searchQuery;

    public MainAuctionGui(KartaAuctionHouse plugin, Player player, int page, AuctionCategory category, SortOrder sortOrder, String searchQuery) {
        super(plugin, player, page, 45);
        this.kah = plugin;
        this.category = category;
        this.sortOrder = sortOrder;
        this.searchQuery = searchQuery;
    }

    @Override
    protected String getTitle() {
        return kah.getConfigManager().getConfig().getString("gui.title-main", "&6KartaAuctionHouse");
    }

    @Override
    protected void build() {
        // Add border/filler items
        ItemStack filler = new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Add control buttons
        addControlItems();

        // Fetch auctions and build page
        kah.getAuctionService().getActiveAuctions(page, itemsPerPage, category, sortOrder, searchQuery)
            .thenCombine(kah.getEconomyRouter().getService().getBalance(player.getUniqueId()), (fetchedAuctions, balance) -> {
                this.hasNextPage = fetchedAuctions.size() > itemsPerPage;
                this.auctions = hasNextPage ? fetchedAuctions.subList(0, itemsPerPage) : fetchedAuctions;

                for (int i = 0; i < auctions.size(); i++) {
                    Auction auction = auctions.get(i);
                    ItemStack displayItem = createAuctionItem(auction, balance);
                    inventory.setItem(i, displayItem);
                }
                return null;
            }).thenRunAsync(() -> addPaginationControls(), runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable)); // Run on main thread
    }

    private void addControlItems() {
        // Category Tabs
        inventory.setItem(45, new GuiItemBuilder(Material.DIAMOND_SWORD).setName("&aWeapons").setLore(category == AuctionCategory.WEAPONS ? "&7(Selected)" : "").build());
        inventory.setItem(46, new GuiItemBuilder(Material.DIAMOND_CHESTPLATE).setName("&aArmor").setLore(category == AuctionCategory.ARMOR ? "&7(Selected)" : "").build());
        inventory.setItem(47, new GuiItemBuilder(Material.GRASS_BLOCK).setName("&aBlocks").setLore(category == AuctionCategory.BLOCKS ? "&7(Selected)" : "").build());
        inventory.setItem(48, new GuiItemBuilder(Material.ELYTRA).setName("&aMisc").setLore(category == AuctionCategory.MISC ? "&7(Selected)" : "").build());

        // Sorting Button
        inventory.setItem(50, new GuiItemBuilder(Material.HOPPER).setName("&aSort By: &e" + sortOrder.getDisplayName()).setLore("&7Click to cycle").build());

        // Search Button
        inventory.setItem(51, new GuiItemBuilder(Material.OAK_SIGN).setName("&aSearch").setLore(searchQuery != null ? "&7Current: &e" + searchQuery : "&7Click to search").build());

        // My Auctions & Mailbox
        inventory.setItem(52, new GuiItemBuilder(Material.CHEST).setName("&aMy Listings").build());
        inventory.setItem(53, new GuiItemBuilder(Material.WRITABLE_BOOK).setName("&aMailbox").build());
    }

    private ItemStack createAuctionItem(Auction auction, double playerBalance) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        List<String> lore = new ArrayList<>();
        lore.add("&7Seller: &e" + kah.getPlayerNameCache().getName(auction.seller()).join());
        lore.add("&7Time Left: &e" + TimeUtil.formatDuration(auction.endAt() - System.currentTimeMillis()));

        double bidPrice = auction.currentBid() != null ? auction.currentBid() : auction.startingPrice();
        String bidColor = playerBalance >= bidPrice ? "&a" : "&c";
        if (auction.currentBid() != null) {
            lore.add(bidColor + "Current Bid: &e" + kah.getEconomyRouter().getService().format(bidPrice));
        } else {
            lore.add(bidColor + "Starting Price: &e" + kah.getEconomyRouter().getService().format(bidPrice));
        }

        if (auction.buyNowPrice() != null) {
            String buyNowColor = playerBalance >= auction.buyNowPrice() ? "&a" : "&c";
            lore.add(buyNowColor + "Buy Now: &e" + kah.getEconomyRouter().getService().format(auction.buyNowPrice()));
        }

        lore.add("");
        lore.add("&aClick to bid or buy!");

        return builder.setLore(lore).build();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (handlePaginationClick(event)) return;

        int slot = event.getSlot();

        if (slot >= 0 && slot < itemsPerPage && auctions != null && slot < auctions.size()) {
            Auction clickedAuction = auctions.get(slot);
            // TODO: Open a detailed GUI for this auction
            player.sendMessage("You clicked on auction: " + clickedAuction.id());
            return;
        }

        switch (slot) {
            case 45: new MainAuctionGui(kah, player, 1, AuctionCategory.WEAPONS, sortOrder, searchQuery).open(); break;
            case 46: new MainAuctionGui(kah, player, 1, AuctionCategory.ARMOR, sortOrder, searchQuery).open(); break;
            case 47: new MainAuctionGui(kah, player, 1, AuctionCategory.BLOCKS, sortOrder, searchQuery).open(); break;
            case 48: new MainAuctionGui(kah, player, 1, AuctionCategory.MISC, sortOrder, searchQuery).open(); break;
            case 50: // Sort
                SortOrder nextSortOrder = sortOrder.next();
                new MainAuctionGui(kah, player, 1, category, nextSortOrder, searchQuery).open();
                break;
            case 51: // Search
                player.closeInventory();
                kah.getSearchInputListener().addPlayerToSearchMode(player.getUniqueId());
                player.sendMessage(kah.getConfigManager().getPrefixedMessage("info.enter-search-query", "Please type your search query in chat. Type 'cancel' to exit."));
                break;
            case 52: new MyListingsGui(kah, player, 1).open(); break;
            case 53: new MailboxGui(kah, player, 1).open(); break;
        }
    }

    @Override
    protected void openPage(int newPage) {
        new MainAuctionGui(kah, player, newPage, category, sortOrder, searchQuery).open();
    }
}
