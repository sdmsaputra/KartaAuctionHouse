package com.minekartastudio.kartaauctionhouse.gui;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.auction.model.AuctionStatus;
import com.minekartastudio.kartaauctionhouse.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MyListingsGui extends PaginatedGui {

    private final KartaAuctionHouse kah;
    private List<Auction> auctions;

    public MyListingsGui(KartaAuctionHouse plugin, Player player, int page) {
        super(plugin, player, page, 45);
        this.kah = plugin;
    }

    @Override
    protected String getTitle() {
        return kah.getConfigManager().getMessages().getString("gui.my-listings-title", "&1My Listings");
    }

    @Override
    protected void build() {
        // This is a placeholder as findBySeller is not fully implemented in storage yet.
        // We'll proceed as if it is.
        // kah.getAuctionService().getAuctionsBySeller(player.getUniqueId(), page, itemsPerPage + 1)
        // For now, let's just show an empty screen with controls.

        // Add border/filler items
        ItemStack filler = new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Add back button
        inventory.setItem(49, new GuiItemBuilder(Material.ARROW).setName("&a<- Back").build());

        // For now, let's assume we have auctions to display
        // In a real scenario, this would be populated from the async call
        this.auctions = new ArrayList<>(); // Empty for now
        this.hasNextPage = false;

        // Add pagination controls
        addPaginationControls();
    }

    private ItemStack createAuctionItem(Auction auction) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        List<String> lore = new ArrayList<>();
        lore.add("&7Status: &e" + auction.status().name());

        if (auction.status() == AuctionStatus.ACTIVE) {
            lore.add("&7Time Left: &e" + TimeUtil.formatDuration(auction.endAt() - System.currentTimeMillis()));
        }

        if (auction.currentBid() != null) {
            lore.add("&7Highest Bid: &e" + kah.getEconomyRouter().getService().format(auction.currentBid()));
        } else {
            lore.add("&7Starting Price: &e" + kah.getEconomyRouter().getService().format(auction.startingPrice()));
        }

        lore.add("");
        if (auction.status() == AuctionStatus.ACTIVE && auction.currentBid() == null) {
            lore.add("&cClick to cancel this auction.");
        } else {
            lore.add("&7This auction cannot be cancelled.");
        }

        return builder.setLore(lore).build();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (handlePaginationClick(event)) return;

        if (event.getSlot() == 49) { // Back button
            new MainAuctionGui(kah, player, 1).open();
            return;
        }

        if (event.getSlot() < itemsPerPage && auctions != null && event.getSlot() < auctions.size()) {
            Auction clickedAuction = auctions.get(event.getSlot());
            if (clickedAuction.status() == AuctionStatus.ACTIVE && clickedAuction.currentBid() == null) {
                player.closeInventory();
                kah.getAuctionService().cancelAuction(player, clickedAuction.id()).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(kah.getConfigManager().getPrefixedMessage("info.cancelled", "{item}", clickedAuction.item().toItemStack().getType().toString()));
                    } else {
                        player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.generic-error", "Could not cancel auction."));
                    }
                });
            }
        }
    }

    @Override
    protected void openPage(int newPage) {
        new MyListingsGui(kah, player, newPage).open();
    }
}
