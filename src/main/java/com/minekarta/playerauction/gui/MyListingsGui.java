package com.minekarta.playerauction.gui;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.auction.model.Auction;
import com.minekarta.playerauction.auction.model.AuctionStatus;
import com.minekarta.playerauction.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MyListingsGui extends PaginatedGui {

    private final PlayerAuction kah;
    private List<Auction> auctions;

    public MyListingsGui(PlayerAuction plugin, Player player, int page) {
        super(plugin, player, page, 45);
        this.kah = plugin;
    }

    @Override
    protected String getTitle() {
        return kah.getConfigManager().getMessages().getString("gui.my-listings-title", "&1My Listings");
    }

    @Override
    protected void build() {
        // Fetch player's auctions and build page content
        kah.getAuctionService().getPlayerAuctions(player.getUniqueId(), page, itemsPerPage + 1)
            .thenAccept(fetchedAuctions -> {
                // Determine pagination
                this.hasNextPage = fetchedAuctions.size() > itemsPerPage;
                this.auctions = hasNextPage ? fetchedAuctions.subList(0, itemsPerPage) : fetchedAuctions;

                // Populate auction items
                for (int i = 0; i < auctions.size(); i++) {
                    Auction auction = auctions.get(i);
                    ItemStack displayItem = createAuctionItem(auction);
                    inventory.setItem(i, displayItem);
                }

                // Show empty message if no auctions
                if (auctions.isEmpty()) {
                    ItemStack emptyItem = new GuiItemBuilder(Material.BARRIER)
                        .setName("&cNo Active Listings")
                        .setLore(
                            "&7You don't have any active auction listings.",
                            "&8Create a listing to start selling items!",
                            "",
                            "&aClick 'Create Auction' to get started."
                        )
                        .build();
                    inventory.setItem(22, emptyItem); // Center position
                }
            }).thenRunAsync(() -> {
                // Build the static parts of the GUI on the main thread
                addControlBar(); // From PaginatedGui
                addCustomControls(); // Add our specific controls
            }, runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable));
    }

    private void addCustomControls() {
        // Add back button
        String backName = kah.getConfigManager().getMessage("gui.control-items.back");
        List<String> backLore = new ArrayList<>();
        backLore.add("&7Return to the main auction house");
        backLore.add("&8Click to go back");
        inventory.setItem(46, new GuiItemBuilder(Material.SPECTRAL_ARROW).setName("&a" + backName).setLore(backLore).build());
    }

    private ItemStack createAuctionItem(Auction auction) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        List<String> lore = new ArrayList<>();

        // Header information
        lore.add("&8━━━━━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("&f" + item.getType().toString().replace("_", " ").toLowerCase());
        lore.add("&8━━━━━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("");

        // Status with color coding
        String statusText;
        String statusColor;
        switch (auction.status()) {
            case ACTIVE:
                statusColor = "&a";
                statusText = "ACTIVE";
                break;
            case FINISHED:
                statusColor = "&e";
                statusText = "SOLD";
                break;
            case CANCELLED:
                statusColor = "&c";
                statusText = "CANCELLED";
                break;
            case EXPIRED:
                statusColor = "&8";
                statusText = "EXPIRED";
                break;
            default:
                statusColor = "&7";
                statusText = auction.status().name();
                break;
        }
        lore.add("&7➤ &6Status: " + statusColor + statusText);

        // Time left for active auctions
        if (auction.status() == AuctionStatus.ACTIVE) {
            long timeLeft = auction.endAt() - System.currentTimeMillis();
            String timeStr = TimeUtil.formatDuration(timeLeft);
            String timeColor;
            if (timeLeft > 24 * 60 * 60 * 1000) { // More than 1 day
                timeColor = "&a";
            } else if (timeLeft > 60 * 60 * 1000) { // More than 1 hour
                timeColor = "&e";
            } else { // Less than 1 hour
                timeColor = "&c";
            }
            lore.add("&7➤ &6Time Left: " + timeColor + timeStr);
        }

        // Price information
        lore.add("&7➤ &6Listing Price: &e" + kah.getEconomyRouter().getService().format(auction.price()));

        // Additional item details
        if (item.getAmount() > 1) {
            lore.add("&7➤ &6Quantity: &e" + item.getAmount());
        }

        lore.add("");
        lore.add("&8━━━━━━━━━━━━━━━━━━━━━━━━━━");

        // Action buttons based on status
        if (auction.status() == AuctionStatus.ACTIVE) {
            lore.add("&c&l▶ CLICK TO CANCEL LISTING");
            lore.add("&7Remove this item from the auction house");
            lore.add("&8Item will be returned to your inventory");
        } else {
            lore.add("&7&l▶ LISTING " + statusText);
            switch (auction.status()) {
                case FINISHED:
                    lore.add("&7Item was sold to another player");
                    break;
                case CANCELLED:
                    lore.add("&7You cancelled this listing");
                    break;
                case EXPIRED:
                    lore.add("&7Listing expired without sale");
                    break;
                default:
                    lore.add("&7This listing cannot be modified");
                    break;
            }
        }

        return builder.setLore(lore).build();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (handleControlBarClick(event)) return;

        int slot = event.getSlot();

        // Handle custom control clicks
        if (slot == 46) { // Back button
            new MainAuctionGui(kah, player, 1, com.minekarta.playerauction.gui.model.SortOrder.NEWEST, null).open();
            return;
        }

        // Handle clicking on an auction item
        if (slot >= 0 && slot < itemsPerPage && auctions != null && slot < auctions.size()) {
            Auction clickedAuction = auctions.get(slot);

            if (clickedAuction.status() == AuctionStatus.ACTIVE) {
                // Show confirmation before cancelling
                player.closeInventory();
                player.sendMessage(kah.getConfigManager().getPrefixedMessage("info.confirm-cancel",
                    "%item%", clickedAuction.item().toItemStack().getType().toString(),
                    "%price%", kah.getEconomyRouter().getService().format(clickedAuction.price())));

                // Cancel the auction
                kah.getAuctionService().cancelAuction(player, clickedAuction.id()).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(kah.getConfigManager().getPrefixedMessage("auction.cancel-success",
                            "%item%", clickedAuction.item().toItemStack().getType().toString()));
                        // Refresh the GUI after a short delay
                        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                            new MyListingsGui(kah, player, page).open();
                        }, 20L); // 1 second delay
                    } else {
                        player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.cancel-failed",
                            "Could not cancel listing. Please try again."));
                    }
                });
            } else {
                // Show message for non-active auctions
                player.sendMessage(kah.getConfigManager().getPrefixedMessage("errors.auction-not-active",
                    "Only active listings can be cancelled."));
            }
            return;
        }

        // Handle other control buttons
        if (slot == 48) { // My Listings button - refresh current page
            new MyListingsGui(kah, player, page).open();
        } else if (slot == 50) { // History button
            new HistoryGui(kah, player, player.getUniqueId(), 1).open();
        } else if (slot == 51) { // Create Auction button
            player.sendMessage(kah.getConfigManager().getPrefixedMessage("info.create-auction-unavailable",
                "Create auction feature is currently unavailable."));
        }
    }

    @Override
    protected void openPage(int newPage) {
        new MyListingsGui(kah, player, newPage).open();
    }
}
