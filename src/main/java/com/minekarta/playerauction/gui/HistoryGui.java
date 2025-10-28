package com.minekarta.playerauction.gui;

import com.minekarta.playerauction.PlayerAuction;
import com.minekarta.playerauction.transaction.model.Transaction;
import com.minekarta.playerauction.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HistoryGui extends PaginatedGui {

    private final PlayerAuction kah;
    private final UUID targetPlayerId;
    private List<Transaction> transactions;

    public HistoryGui(PlayerAuction plugin, Player player, UUID targetPlayerId, int page) {
        super(plugin, player, page, 45);
        this.kah = plugin;
        this.targetPlayerId = targetPlayerId;
    }

    @Override
    protected String getTitle() {
        return kah.getConfigManager().getMessage("gui.history-title");
    }

    @Override
    protected void build() {
        // Fetch transactions and build page
        kah.getTransactionLogger().getHistory(targetPlayerId, page, itemsPerPage + 1)
            .thenAcceptAsync(fetchedTransactions -> {
                this.hasNextPage = fetchedTransactions.size() > itemsPerPage;
                this.transactions = hasNextPage ? fetchedTransactions.subList(0, itemsPerPage) : fetchedTransactions;

                // Populate transaction items
                for (int i = 0; i < transactions.size(); i++) {
                    Transaction transaction = transactions.get(i);
                    ItemStack displayItem = createHistoryItem(transaction);
                    inventory.setItem(i, displayItem);
                }

                // Show empty message if no transactions
                if (transactions.isEmpty()) {
                    ItemStack emptyItem = new GuiItemBuilder(Material.WRITABLE_BOOK)
                        .setName("&6No Transaction History")
                        .setLore(
                            "&7You don't have any auction transactions yet.",
                            "&8Start buying or selling items to see your history!",
                            "",
                            "&aBrowse the auction house to get started."
                        )
                        .build();
                    inventory.setItem(22, emptyItem); // Center position
                }
            }).thenRunAsync(() -> {
                // Run on main thread to build the static parts of the GUI
                addControlBar();
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

    private ItemStack createHistoryItem(Transaction transaction) {
        ItemStack item = transaction.itemSnapshot().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        List<String> lore = new ArrayList<>();

        // Header information
        lore.add("&8━━━━━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("&f" + item.getType().toString().replace("_", " ").toLowerCase());
        lore.add("&8━━━━━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("");

        // Status with color coding and icon
        String statusText;
        String statusIcon;
        String statusColor;
        switch (transaction.status()) {
            case "SOLD":
                statusIcon = "✓";
                statusColor = "&a";
                statusText = "SOLD";
                break;
            case "EXPIRED":
                statusIcon = "⏰";
                statusColor = "&e";
                statusText = "EXPIRED";
                break;
            case "CANCELLED":
                statusIcon = "✗";
                statusColor = "&c";
                statusText = "CANCELLED";
                break;
            default:
                statusIcon = "•";
                statusColor = "&7";
                statusText = transaction.status();
                break;
        }
        lore.add("&7➤ &6Status: " + statusColor + statusIcon + " " + statusText);

        // Date and time
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm");
        lore.add("&7➤ &6Date: &e" + dateFormat.format(new java.util.Date(transaction.timestamp())));

        // Determine user role in transaction
        boolean isSeller = player.getUniqueId().equals(transaction.sellerUuid());
        boolean isBuyer = transaction.buyerUuid() != null && player.getUniqueId().equals(transaction.buyerUuid());

        // Transaction details based on status
        if (transaction.status().equals("SOLD")) {
            if (isSeller) {
                // User was the seller
                String buyerName = transaction.buyerUuid() != null ?
                    kah.getPlayerNameCache().getName(transaction.buyerUuid()).join() : "Unknown";
                lore.add("&7➤ &6Sold to: &e" + buyerName);
                lore.add("&7➤ &6Earned: &a+" + kah.getEconomyRouter().getService().format(transaction.finalPrice()));
            } else if (isBuyer) {
                // User was the buyer
                String sellerName = transaction.sellerUuid() != null ?
                    kah.getPlayerNameCache().getName(transaction.sellerUuid()).join() : "Unknown";
                lore.add("&7➤ &6Bought from: &e" + sellerName);
                lore.add("&7➤ &6Paid: &c-" + kah.getEconomyRouter().getService().format(transaction.finalPrice()));
            } else {
                // User is just viewing history
                lore.add("&7➤ &6Price: &e" + kah.getEconomyRouter().getService().format(transaction.finalPrice()));
            }
        } else if (transaction.status().equals("EXPIRED")) {
            lore.add("&7➤ &6Result: &eItem expired without sale");
            if (isSeller) {
                lore.add("&7➤ &6Outcome: &aItem returned to inventory");
            }
        } else if (transaction.status().equals("CANCELLED")) {
            lore.add("&7➤ &6Result: &eAuction was cancelled");
            if (isSeller) {
                lore.add("&7➤ &6Outcome: &aItem returned to inventory");
            }
        }

        // Additional item details
        if (item.getAmount() > 1) {
            lore.add("&7➤ &6Quantity: &e" + item.getAmount());
        }

        lore.add("");
        lore.add("&8━━━━━━━━━━━━━━━━━━━━━━━━━━");
        lore.add("&8Transaction ID: &7" + transaction.transactionId().toString().substring(0, 8) + "...");

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

        // Handle clicking on transaction items - show more details
        if (slot >= 0 && slot < itemsPerPage && transactions != null && slot < transactions.size()) {
            Transaction clickedTransaction = transactions.get(slot);

            // Create detailed info message
            List<String> details = new ArrayList<>();
            details.add("&6=== Transaction Details ===");
            details.add("&7Transaction ID: &e" + clickedTransaction.transactionId().toString());
            details.add("&7Status: " + clickedTransaction.status());
            details.add("&7Date: &e" + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(clickedTransaction.timestamp())));
            details.add("&7Item: &e" + clickedTransaction.itemSnapshot().toItemStack().getType().toString());

            if (clickedTransaction.finalPrice() != null) {
                details.add("&7Amount: &e" + kah.getEconomyRouter().getService().format(clickedTransaction.finalPrice()));
            }

            details.add("&7" + clickedTransaction.details());

            // Send details to player
            for (String line : details) {
                player.sendMessage(line);
            }
            return;
        }

        // Handle other control buttons
        if (slot == 48) { // My Listings button
            new MyListingsGui(kah, player, 1).open();
        } else if (slot == 50) { // History button - refresh current page
            new HistoryGui(kah, player, targetPlayerId, page).open();
        } else if (slot == 51) { // Create Auction button
            player.sendMessage(kah.getConfigManager().getPrefixedMessage("info.create-auction-unavailable",
                "Create auction feature is currently unavailable."));
        }
    }

    @Override
    protected void openPage(int newPage) {
        new HistoryGui(kah, player, targetPlayerId, newPage).open();
    }
}
