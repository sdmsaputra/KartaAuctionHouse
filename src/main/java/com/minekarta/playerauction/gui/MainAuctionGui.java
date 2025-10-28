package com.minekarta.playerauction.gui;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.auction.model.Auction;
import com.minekarta.playerauction.gui.model.AuctionCategory;
import com.minekarta.playerauction.gui.model.SortOrder;
import com.minekarta.playerauction.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MainAuctionGui extends PaginatedGui {

    private final PlayerAuction kah;
    private List<Auction> auctions;
    private final SortOrder sortOrder;
    private final String searchQuery;

    public MainAuctionGui(PlayerAuction plugin, Player player, int page, SortOrder sortOrder, String searchQuery) {
        super(plugin, player, page, 45);
        this.kah = plugin;
        this.sortOrder = sortOrder;
        this.searchQuery = searchQuery;
    }

    @Override
    protected String getTitle() {
        return kah.getConfigManager().getConfig().getString("gui.title-main", "&6PlayerAuctions").replace("&", "§");
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
        // Update sort button with current sort order
        String sortName = kah.getConfigManager().getMessage("gui.control-items.sort");
        List<String> sortLore = new ArrayList<>();
        sortLore.add("§7Current: §e" + sortOrder.getDisplayName());
        sortLore.add("§7Click to cycle through options");
        sortLore.add("§8Available:");
        for (com.minekarta.playerauction.gui.model.SortOrder order : com.minekarta.playerauction.gui.model.SortOrder.values()) {
            sortLore.add("§8  • " + order.getDisplayName());
        }
        inventory.setItem(46, new GuiItemBuilder(Material.COMPARATOR).setName("§a" + sortName).setLore(sortLore).build());

        // Update search button with current search status
        String searchName = kah.getConfigManager().getMessage("gui.control-items.search");
        List<String> searchLore = new ArrayList<>();
        if (searchQuery != null && !searchQuery.isEmpty()) {
            searchLore.add("§7Currently searching for:");
            searchLore.add("§e\"" + searchQuery + "\"");
            searchLore.add("§7Click to modify search");
        } else {
            searchLore.add("§7Click to search for items");
            searchLore.add("§8Type keywords to find");
        }
        inventory.setItem(47, new GuiItemBuilder(Material.ENDER_EYE).setName("§a" + searchName).setLore(searchLore).build());
    }

    private ItemStack createAuctionItem(Auction auction, double playerBalance) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        List<String> lore = new ArrayList<>();

        // Header information
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("§f" + item.getType().toString().replace("_", " ").toLowerCase());
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("");

        // Seller information
        lore.add("§7➤ §6Seller: §e" + kah.getPlayerNameCache().getName(auction.seller()).join());

        // Time left with color coding
        long timeLeft = auction.endAt() - System.currentTimeMillis();
        String timeStr = TimeUtil.formatDuration(timeLeft);
        String timeColor;
        if (timeLeft > 24 * 60 * 60 * 1000) { // More than 1 day
            timeColor = "§a";
        } else if (timeLeft > 60 * 60 * 1000) { // More than 1 hour
            timeColor = "§e";
        } else { // Less than 1 hour
            timeColor = "§c";
        }
        lore.add("§7➤ §6Time Left: " + timeColor + timeStr);

        // Price with affordability indicator
        double price = auction.price();
        String priceColor = playerBalance >= price ? "§a" : "§c";
        String affordText = playerBalance >= price ? "§a(✓ Affordable)" : "§c(✗ Insufficient)";
        lore.add("§7➤ §6Price: " + priceColor + kah.getEconomyRouter().getService().format(price) + " §7" + affordText);

        // Additional item details
        if (item.getAmount() > 1) {
            lore.add("§7➤ §6Quantity: §e" + item.getAmount());
        }

        lore.add("");
        lore.add("§8━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Action buttons
        if (playerBalance >= price) {
            lore.add("§a§l▶ CLICK TO PURCHASE");
            lore.add("§7Buy this item instantly");
        } else {
            lore.add("§c§l▶ INSUFFICIENT FUNDS");
            lore.add("§7You need more money to buy this");
        }

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
            // Direct purchase with confirmation
            double price = clickedAuction.price();
            kah.getEconomyRouter().getService().getBalance(player.getUniqueId()).thenAccept(balance -> {
                if (balance >= price) {
                    // Sufficient funds, proceed with purchase
                    kah.getAuctionService().buyItem(player, clickedAuction.id()).thenAccept(success -> {
                        if (success) {
                            player.sendMessage(kah.getConfigManager().getPrefixedMessage("auction.purchase-success",
                                "%item%", clickedAuction.item().toItemStack().getType().toString(),
                                "%price%", kah.getEconomyRouter().getService().format(clickedAuction.price())));
                            // Refresh the GUI
                            new MainAuctionGui(kah, player, page, sortOrder, searchQuery).open();
                        }
                    });
                } else {
                    // Insufficient funds
                    player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.insufficient-funds",
                        "%needed%", kah.getEconomyRouter().getService().format(price - balance),
                        "%balance%", kah.getEconomyRouter().getService().format(balance)));
                }
            });
            return;
        }

        // Handle custom control clicks (these are the ones not handled by parent class)
        if (slot == 47) { // Sort button
            SortOrder nextSortOrder = sortOrder.next();
            new MainAuctionGui(kah, player, 1, nextSortOrder, searchQuery).open();
        }
    }

    @Override
    protected void openPage(int newPage) {
        new MainAuctionGui(kah, player, newPage, sortOrder, searchQuery).open();
    }

    @Override
    protected String getCurrentSortOrder() {
        return sortOrder.getDisplayName();
    }
}
