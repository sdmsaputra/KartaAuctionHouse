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
    private final SortOrder sortOrder;
    private final String searchQuery;

    public MainAuctionGui(KartaAuctionHouse plugin, Player player, int page, SortOrder sortOrder, String searchQuery) {
        super(plugin, player, page, 45);
        this.kah = plugin;
        this.sortOrder = sortOrder;
        this.searchQuery = searchQuery;
    }

    @Override
    protected String getTitle() {
        return kah.getConfigManager().getConfig().getString("gui.title-main", "&6KartaAuctionHouse");
    }

    @Override
    protected void build() {
        // Fetch auctions and build page content first
        kah.getAuctionService().getActiveAuctions(page, itemsPerPage, AuctionCategory.ALL, sortOrder, searchQuery)
            .thenCombine(kah.getEconomyRouter().getService().getBalance(player.getUniqueId()), (fetchedAuctions, balance) -> {
                // Determine pagination
                this.hasNextPage = fetchedAuctions.size() > itemsPerPage;
                this.auctions = hasNextPage ? fetchedAuctions.subList(0, itemsPerPage) : fetchedAuctions;

                // Populate auction items
                for (int i = 0; i < auctions.size(); i++) {
                    Auction auction = auctions.get(i);
                    ItemStack displayItem = createAuctionItem(auction, balance);
                    inventory.setItem(i, displayItem);
                }
                return null;
            }).thenRunAsync(() -> {
                // Build the static parts of the GUI on the main thread
                addControlBar(); // From PaginatedGui
                addCustomControls(); // Add our specific controls
            }, runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable));
    }

    private void addCustomControls() {
        // Sorting Button
        inventory.setItem(47, new GuiItemBuilder(Material.HOPPER).setName("&aSort By: &e" + sortOrder.getDisplayName()).setLore("&7Click to cycle").build());

        // Search Button
        inventory.setItem(50, new GuiItemBuilder(Material.OAK_SIGN).setName("&aSearch").setLore(searchQuery != null ? "&7Current: &e" + searchQuery : "&7Click to search").build());

        // My Auctions & Mailbox
        inventory.setItem(51, new GuiItemBuilder(Material.CHEST).setName("&aMy Listings").build());
        inventory.setItem(52, new GuiItemBuilder(Material.WRITABLE_BOOK).setName("&aMailbox").build());
    }

    private ItemStack createAuctionItem(Auction auction, double playerBalance) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        List<String> lore = new ArrayList<>();
        lore.add("&7Seller: &e" + kah.getPlayerNameCache().getName(auction.seller()).join());
        lore.add("&7Time Left: &e" + TimeUtil.formatDuration(auction.endAt() - System.currentTimeMillis()));

        double price = auction.price();
        String priceColor = playerBalance >= price ? "&a" : "&c";
        lore.add(priceColor + "Price: &e" + kah.getEconomyRouter().getService().format(price));

        lore.add("");
        lore.add("&aClick to purchase!");

        return builder.setLore(lore).build();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        // Handle pagination, close, and player info clicks from the parent
        if (handleControlBarClick(event)) return;

        int slot = event.getSlot();

        // Handle clicking on an auction item
        if (slot >= 0 && slot < itemsPerPage && auctions != null && slot < auctions.size()) {
            Auction clickedAuction = auctions.get(slot);
            // Direct purchase
            kah.getAuctionService().buyItem(player, clickedAuction.id()).thenAccept(success -> {
                if (success) {
                    player.sendMessage(kah.getConfigManager().getPrefixedMessage("info.purchase-success",
                        "{item}", clickedAuction.item().toItemStack().getType().toString()));
                    // Refresh the GUI
                    new MainAuctionGui(kah, player, page, sortOrder, searchQuery).open();
                } else {
                    player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.purchase-failed"));
                }
            });
            return;
        }

        // Handle custom control clicks
        switch (slot) {
            case 47: // Sort
                SortOrder nextSortOrder = sortOrder.next();
                new MainAuctionGui(kah, player, 1, nextSortOrder, searchQuery).open();
                break;
            case 50: // Search
                player.closeInventory();
                kah.getSearchInputListener().addPlayerToSearchMode(player.getUniqueId());
                player.sendMessage(kah.getConfigManager().getPrefixedMessage("info.enter-search-query", "Please type your search query in chat. Type 'cancel' to exit."));
                break;
            case 51: new MyListingsGui(kah, player, 1).open(); break;
            case 52: new MailboxGui(kah, player, 1).open(); break;
        }
    }

    @Override
    protected void openPage(int newPage) {
        new MainAuctionGui(kah, player, newPage, sortOrder, searchQuery).open();
    }
}
