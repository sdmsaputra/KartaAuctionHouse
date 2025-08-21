package com.minekartastudio.kartaauctionhouse.gui;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MainAuctionGui extends PaginatedGui {

    private final KartaAuctionHouse kah;
    private List<Auction> auctions;

    public MainAuctionGui(KartaAuctionHouse plugin, Player player, int page) {
        super(plugin, player, page, 45);
        this.kah = plugin;
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
        inventory.setItem(49, new GuiItemBuilder(Material.CHEST).setName("&aMy Listings").build());
        inventory.setItem(50, new GuiItemBuilder(Material.WRITABLE_BOOK).setName("&aMailbox").build());

        // Fetch auctions and build page
        kah.getAuctionService().getActiveAuctions(page, itemsPerPage + 1)
            .thenAcceptAsync(fetchedAuctions -> {
                this.hasNextPage = fetchedAuctions.size() > itemsPerPage;
                this.auctions = hasNextPage ? fetchedAuctions.subList(0, itemsPerPage) : fetchedAuctions;

                for (int i = 0; i < auctions.size(); i++) {
                    Auction auction = auctions.get(i);
                    ItemStack displayItem = createAuctionItem(auction);
                    inventory.setItem(i, displayItem);
                }

                addPaginationControls(); // Add controls after fetching data
            }, runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable)); // Run on main thread
    }

    private ItemStack createAuctionItem(Auction auction) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        List<String> lore = new ArrayList<>();
        lore.add("&7Seller: &e" + kah.getPlayerNameCache().getName(auction.seller()).join());
        lore.add("&7Time Left: &e" + TimeUtil.formatDuration(auction.endAt() - System.currentTimeMillis()));

        if (auction.currentBid() != null) {
            lore.add("&7Current Bid: &e" + kah.getEconomyRouter().getService().format(auction.currentBid()));
        } else {
            lore.add("&7Starting Price: &e" + kah.getEconomyRouter().getService().format(auction.startingPrice()));
        }

        if (auction.buyNowPrice() != null) {
            lore.add("&7Buy Now: &e" + kah.getEconomyRouter().getService().format(auction.buyNowPrice()));
        }

        lore.add("");
        lore.add("&aClick to bid or buy!");

        return builder.setLore(lore).build();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (handlePaginationClick(event)) return;

        if (event.getSlot() == 49) { // My Listings
            new MyListingsGui(kah, player, 1).open();
            return;
        }

        if (event.getSlot() == 50) { // Mailbox
            new MailboxGui(kah, player, 1).open();
            return;
        }

        if (event.getSlot() < itemsPerPage && auctions != null && event.getSlot() < auctions.size()) {
            Auction clickedAuction = auctions.get(event.getSlot());
            // TODO: Open a detailed GUI for this auction
            player.sendMessage("You clicked on auction: " + clickedAuction.id());
        }
    }

    @Override
    protected void openPage(int newPage) {
        new MainAuctionGui(kah, player, newPage).open();
    }
}
