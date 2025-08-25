package com.minekartastudio.kartaauctionhouse.gui;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.transaction.model.Transaction;
import com.minekartastudio.kartaauctionhouse.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class HistoryGui extends PaginatedGui {

    private final KartaAuctionHouse kah;
    private final UUID targetPlayerId;
    private List<Transaction> transactions;

    public HistoryGui(KartaAuctionHouse plugin, Player player, UUID targetPlayerId, int page) {
        super(plugin, player, page, 45);
        this.kah = plugin;
        this.targetPlayerId = targetPlayerId;
    }

    @Override
    protected String getTitle() {
        return "&1Auction History";
    }

    @Override
    protected void build() {
        // Fetch transactions and build page
        kah.getTransactionLogger().getHistory(targetPlayerId, page, itemsPerPage + 1)
            .thenAcceptAsync(fetchedTransactions -> {
                this.hasNextPage = fetchedTransactions.size() > itemsPerPage;
                this.transactions = hasNextPage ? fetchedTransactions.subList(0, itemsPerPage) : fetchedTransactions;

                for (int i = 0; i < transactions.size(); i++) {
                    Transaction transaction = transactions.get(i);
                    ItemStack displayItem = createHistoryItem(transaction);
                    inventory.setItem(i, displayItem);
                }

                // Run on main thread to build the static parts of the GUI
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    addControlBar();
                    // Add a back button specific to this GUI
                    inventory.setItem(46, new GuiItemBuilder(Material.ARROW).setName("&aBack to AH").build());
                });
            }, kah.getAsyncExecutor()); // Use the plugin's executor
    }

    private ItemStack createHistoryItem(Transaction transaction) {
        ItemStack item = transaction.itemSnapshot().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        List<String> lore = new ArrayList<>();
        lore.add("&7Status: &e" + transaction.status());
        lore.add("&7Date: &e" + new java.util.Date(transaction.timestamp()).toString());

        boolean isSeller = player.getUniqueId().equals(transaction.sellerUuid());

        if (transaction.status().equals("SOLD")) {
            if (isSeller) {
                lore.add("&7Sold to: &e" + kah.getPlayerNameCache().getName(transaction.buyerUuid()).join());
                lore.add("&7Price: &a+" + kah.getEconomyRouter().getService().format(transaction.finalPrice()));
            } else {
                lore.add("&7Bought from: &e" + kah.getPlayerNameCache().getName(transaction.sellerUuid()).join());
                lore.add("&7Price: &c-" + kah.getEconomyRouter().getService().format(transaction.finalPrice()));
            }
        } else if (transaction.status().equals("EXPIRED")) {
            lore.add("&7This item expired unsold.");
        } else if (transaction.status().equals("CANCELLED")) {
            lore.add("&7You cancelled this auction.");
        }

        return builder.setLore(lore).build();
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (handleControlBarClick(event)) return;

        if (event.getSlot() == 46) { // Back button
            new MainAuctionGui(kah, player, 1, com.minekartastudio.kartaauctionhouse.gui.model.SortOrder.NEWEST, null).open();
        }
    }

    @Override
    protected void openPage(int newPage) {
        new HistoryGui(kah, player, targetPlayerId, newPage).open();
    }
}
