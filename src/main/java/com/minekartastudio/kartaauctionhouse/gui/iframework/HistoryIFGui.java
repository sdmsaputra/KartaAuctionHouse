package com.minekartastudio.kartaauctionhouse.gui.iframework;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.gui.GuiItemBuilder;
import com.minekartastudio.kartaauctionhouse.transaction.model.Transaction;
import com.minekartastudio.kartaauctionhouse.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * History GUI implementation using the Inventory Framework
 */
public class HistoryIFGui extends BaseIFGui {
    
    private final KartaAuctionHouse kah;
    private List<Transaction> transactions;
    private final UUID targetPlayerId;
    private final int page;
    private boolean hasNextPage = false;
    private static final int ITEMS_PER_PAGE = 45; // 5 rows
    
    public HistoryIFGui(KartaAuctionHouse plugin, Player player, UUID targetPlayerId, int page) {
        super(plugin, player);
        this.kah = plugin;
        this.targetPlayerId = targetPlayerId;
        this.page = page;
    }
    
    @Override
    protected String getTitle() {
        return kah.getConfigManager().getMessage("gui.history-title");
    }
    
    @Override
    protected void build() {
        OutlinePane itemPane = new OutlinePane(0, 0, 9, 5);
        
        // Get transaction history for the current page
        kah.getTransactionLogger().getHistory(targetPlayerId, page, ITEMS_PER_PAGE + 1)
            .thenAccept(fetchedTransactions -> {
                // Determine pagination
                this.hasNextPage = fetchedTransactions.size() > ITEMS_PER_PAGE;
                this.transactions = hasNextPage ? fetchedTransactions.subList(0, ITEMS_PER_PAGE) : fetchedTransactions;
                
                // Add transactions to the pane
                for (Transaction transaction : this.transactions) {
                    ItemStack displayItem = createTransactionItem(transaction);
                    itemPane.addItem(new GuiItem(displayItem, event -> onTransactionClick(event, transaction)));
                }
                
                addPane(itemPane);
                addControlBar();
            });
    }
    
    private ItemStack createTransactionItem(Transaction transaction) {
        ItemStack item = transaction.itemSnapshot().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);
        
        // Add transaction details to the lore
        List<String> lore = new ArrayList<>();
        lore.add("&7Status: &e" + transaction.status());
        lore.add("&7Date: &e" + new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm").format(new java.util.Date(transaction.timestamp())));
        
        boolean isSeller = player.getUniqueId().equals(transaction.sellerUuid());
        
        if (transaction.status().equals("SOLD")) {
            if (isSeller) {
                lore.add("&7Sold to: &e" + kah.getPlayerNameCache().getName(transaction.buyerUuid()).join());
                lore.add("&7Earned: &a+" + kah.getEconomyRouter().getService().format(transaction.finalPrice()));
            } else {
                lore.add("&7Bought from: &e" + kah.getPlayerNameCache().getName(transaction.sellerUuid()).join());
                lore.add("&7Paid: &c-" + kah.getEconomyRouter().getService().format(transaction.finalPrice()));
            }
        } else if (transaction.status().equals("EXPIRED")) {
            lore.add("&7This item expired unsold.");
            if (isSeller) {
                lore.add("&7Item returned to your mailbox.");
            }
        } else if (transaction.status().equals("CANCELLED")) {
            lore.add("&7You cancelled this auction.");
            lore.add("&7Item returned to your mailbox.");
        }
        
        lore.add("");
        lore.add("&7&oTransaction ID: &r&e" + transaction.transactionId());
        
        return builder.setLore(lore.toArray(new String[0])).build();
    }
    
    private void onTransactionClick(InventoryClickEvent event, Transaction transaction) {
        // For now, just show a message - in a full implementation you might open a detailed view
        player.sendMessage("Transaction ID: " + transaction.transactionId());
    }
    
    private void addControlBar() {
        StaticPane controlPane = new StaticPane(0, 5, 9, 1);
        
        // Previous Page Button
        if (page > 1) {
            ItemStack prevPageItem = new GuiItemBuilder(Material.ARROW)
                .setName("&a<- Previous Page")
                .setLore("&7Go to page " + (page - 1))
                .build();
            controlPane.addItem(new GuiItem(prevPageItem, event -> openPage(page - 1)), 0, 0);
        }
        
        // Close Button
        ItemStack closeItem = new GuiItemBuilder(Material.BARRIER).setName("&cClose").build();
        controlPane.addItem(new GuiItem(closeItem, event -> player.closeInventory()), 3, 0);
        
        // Player Info Item
        createPlayerInfoItem().thenAccept(item -> {
            controlPane.addItem(new GuiItem(item), 4, 0);
        });
        
        // Next Page Button
        if (hasNextPage) {
            ItemStack nextPageItem = new GuiItemBuilder(Material.ARROW)
                .setName("&aNext Page ->")
                .setLore("&7Go to page " + (page + 1))
                .build();
            controlPane.addItem(new GuiItem(nextPageItem, event -> openPage(page + 1)), 8, 0);
        }
        
        addPane(controlPane);
    }
    
    private void openPage(int newPage) {
        new HistoryIFGui(kah, player, targetPlayerId, newPage).open();
    }
    
    @Override
    protected void onClick(InventoryClickEvent event) {
        // Control bar clicks are handled by individual GuiItem click handlers
    }
}