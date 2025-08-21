package com.minekartastudio.kartaauctionhouse.gui;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxEntry;
import com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.stream.Collectors;

public class MailboxGui extends PaginatedGui {

    private final KartaAuctionHouse kah;
    private List<MailboxEntry> entries;

    public MailboxGui(KartaAuctionHouse plugin, Player player, int page) {
        super(plugin, player, page, 45);
        this.kah = plugin;
    }

    @Override
    protected String getTitle() {
        return kah.getConfigManager().getMessages().getString("gui.mailbox-title", "&1My Mailbox");
    }

    @Override
    protected void build() {
        // Add border/filler items
        ItemStack filler = new GuiItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(" ").build();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Add back button
        inventory.setItem(49, new GuiItemBuilder(Material.ARROW).setName("&a<- Back").build());

        // Fetch mailbox entries
        kah.getMailboxService().getUnclaimed(player.getUniqueId())
            .thenAcceptAsync(unclaimedEntries -> {
                // Manual pagination
                int start = (page - 1) * itemsPerPage;
                int end = Math.min(start + itemsPerPage, unclaimedEntries.size());

                this.hasNextPage = unclaimedEntries.size() > end;
                this.entries = unclaimedEntries.stream().skip(start).limit(itemsPerPage).collect(Collectors.toList());

                for (int i = 0; i < this.entries.size(); i++) {
                    MailboxEntry entry = this.entries.get(i);
                    inventory.setItem(i, createMailboxItem(entry));
                }

                addPaginationControls();
            }, runnable -> plugin.getServer().getScheduler().runTask(plugin, runnable));
    }

    private ItemStack createMailboxItem(MailboxEntry entry) {
        if (entry.type() == MailboxType.ITEM) {
            ItemStack item = entry.item().toItemStack();
            return new GuiItemBuilder(item)
                    .setLore("&7Reason: " + entry.note(), "", "&aClick to claim this item!")
                    .build();
        } else { // MONEY
            return new GuiItemBuilder(Material.GOLD_NUGGET)
                    .setName("&e" + kah.getEconomyRouter().getService().format(entry.amount()))
                    .setLore("&7Reason: " + entry.note(), "", "&aClick to claim this money!")
                    .build();
        }
    }

    @Override
    protected void onClick(InventoryClickEvent event) {
        if (handlePaginationClick(event)) return;

        if (event.getSlot() == 49) { // Back button
            new MainAuctionGui(kah, player, 1, com.minekartastudio.kartaauctionhouse.gui.model.AuctionCategory.ALL, com.minekartastudio.kartaauctionhouse.gui.model.SortOrder.NEWEST, null).open();
            return;
        }

        if (event.getSlot() < itemsPerPage && entries != null && event.getSlot() < entries.size()) {
            MailboxEntry clickedEntry = entries.get(event.getSlot());

            player.closeInventory(); // Close GUI to prevent further clicks
            kah.getMailboxService().claimEntry(player, clickedEntry).thenAccept(success -> {
                if (success) {
                    // Refresh the GUI to show the updated mailbox
                    new MailboxGui(kah, player, page).open();
                } else {
                    // Re-open the GUI without refreshing if it failed (e.g., inventory full)
                    new MailboxGui(kah, player, page).open();
                }
            });
        }
    }

    @Override
    protected void openPage(int newPage) {
        new MailboxGui(kah, player, newPage).open();
    }
}
