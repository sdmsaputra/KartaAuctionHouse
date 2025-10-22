package com.minekartastudio.kartaauctionhouse.gui.improved;

import com.minekartastudio.kartaauctionhouse.KartaAuctionHouse;
import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.gui.GuiItemBuilder;
import com.minekartastudio.kartaauctionhouse.gui.model.SortOrder;
import com.minekartastudio.kartaauctionhouse.util.TimeUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Improved Main GUI with better performance and user experience
 */
public class ImprovedMainGui {

    private final KartaAuctionHouse plugin;
    private final Player player;
    private final Inventory inventory;
    private final List<Auction> currentAuctions;
    private final Map<UUID, Long> lastClickTimes = new java.util.concurrent.ConcurrentHashMap<>();
    private int page = 1;
    private SortOrder sortOrder = SortOrder.NEWEST;
    private String searchQuery = null;
    private boolean hasNextPage = false;
    private static final int ITEMS_PER_PAGE = 45;
    private static final long CLICK_COOLDOWN_MS = 500;

    public ImprovedMainGui(KartaAuctionHouse plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = plugin.getServer().createInventory(null, 54,
            plugin.getConfigManager().processColors(
                plugin.getConfigManager().getConfig().getString("gui.title-main", "&6Karta Auction House")));
        this.currentAuctions = new ArrayList<>();
    }

    /**
     * Open the GUI with asynchronous loading
     */
    public void open() {
        loadAuctions().thenAccept(v -> {
            buildGui();
            player.openInventory(inventory);
            // Register with event listener
            plugin.getGuiEventListener().registerGui(player, this);
        });
    }

    /**
     * Get the inventory instance for this GUI
     */
    public org.bukkit.inventory.Inventory getInventory() {
        return inventory;
    }

    /**
     * Load auctions asynchronously with caching
     */
    private CompletableFuture<?> loadAuctions() {
        return plugin.getAuctionService().getActiveAuctions(page, ITEMS_PER_PAGE, null, sortOrder, searchQuery)
                .thenCombine(plugin.getEconomyRouter().getService().getBalance(player.getUniqueId()),
                    (auctions, balance) -> {
                        currentAuctions.clear();
                        this.hasNextPage = auctions.size() > ITEMS_PER_PAGE;

                        // Add items with pagination
                        for (int i = 0; i < Math.min(auctions.size(), ITEMS_PER_PAGE); i++) {
                            currentAuctions.add(auctions.get(i));
                        }
                        return null;
                    })
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Failed to load auctions: " + throwable.getMessage());

                    // Send error message to player
                    player.sendMessage(plugin.getConfigManager().getPrefixedMessage("errors.load-failed",
                        "&cFailed to load auctions. Please try again."));

                    // Close inventory on error
                    player.closeInventory();
                    return null;
                });
    }

    /**
     * Build the GUI with improved layout
     */
    private void buildGui() {
        inventory.clear();

        // Add auction items
        for (int i = 0; i < currentAuctions.size(); i++) {
            if (i < 45) { // Only fill first 5 rows
                inventory.setItem(i, createAuctionItem(currentAuctions.get(i)));
            }
        }

        // Add control panel (row 6)
        addControlPanel();
    }

    /**
     * Create auction item with enhanced information
     */
    private ItemStack createAuctionItem(Auction auction) {
        ItemStack item = auction.item().toItemStack();
        GuiItemBuilder builder = new GuiItemBuilder(item);

        // Get player balance for color coding
        double playerBalance = plugin.getEconomyRouter().getService().getBalance(player.getUniqueId()).join();

        // Build comprehensive lore
        builder.addLore("&7Seller: &e" + plugin.getPlayerNameCache().getName(auction.seller()).join());

        long timeLeft = auction.endAt() - System.currentTimeMillis();
        if (timeLeft > 0) {
            builder.addLore("&7Time Left: &e" + TimeUtil.formatDuration(timeLeft));

            // Add urgency indicator
            if (timeLeft < 300000) { // Less than 5 minutes
                builder.addLore("&c⚠ Ending Soon!");
            } else if (timeLeft < 3600000) { // Less than 1 hour
                builder.addLore("&e⚠ Ending in less than an hour!");
            }
        } else {
            builder.addLore("&c&lEXPIRED");
        }

        double price = auction.price();
        String priceColor = playerBalance >= price ? "&a" : "&c";
        builder.addLore(priceColor + "Price: &e" + plugin.getEconomyRouter().getService().format(price));

        // Add affordability indicator
        if (playerBalance >= price) {
            builder.addLore("&a✓ You can afford this!");
        } else {
            builder.addLore("&c✗ Insufficient funds");
        }

        builder.addLore("");
        builder.addLore("&aClick to purchase!");

        return builder.build();
    }

    /**
     * Add control panel with intuitive navigation
     */
    private void addControlPanel() {
        // Navigation controls (slots 45-53)
        if (page > 1) {
            inventory.setItem(45, createNavigationItem(Material.ARROW, "&ePrevious Page",
                "&7Go to page " + (page - 1), "prev_page"));
        }

        // Sort control
        inventory.setItem(46, createNavigationItem(Material.HOPPER, "&eSort: " + sortOrder.getDisplayName(),
            "&7Current: " + sortOrder.getDisplayName(),
            "&7Click to change sorting", "sort"));

        // Search control
        inventory.setItem(47, createNavigationItem(Material.OAK_SIGN, "&eSearch",
            "&7Click to search items", "search"));

        // My listings
        inventory.setItem(48, createNavigationItem(Material.CHEST, "&eMy Listings",
            "&7View your auctions", "my_listings"));

        // Mailbox
        inventory.setItem(49, createNavigationItem(Material.WRITABLE_BOOK, "&eMailbox",
            "&7Check your mailbox", "mailbox"));

        // Page info
        inventory.setItem(50, createNavigationItem(Material.PAPER, "&ePage " + page,
            "&7Showing " + currentAuctions.size() + " items", "info"));

        if (hasNextPage) {
            inventory.setItem(53, createNavigationItem(Material.ARROW, "&eNext Page",
                "&7Go to page " + (page + 1), "next_page"));
        }
    }

    /**
     * Create navigation item with consistent styling
     */
    private ItemStack createNavigationItem(Material material, String name, String... lore) {
        GuiItemBuilder builder = new GuiItemBuilder(material)
            .setName(name);

        for (String line : lore) {
            builder.addLore(line);
        }

        return builder.build();
    }

    /**
     * Handle inventory clicks with improved validation and cooldown
     */
    public boolean handleClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inventory)) return false;

        event.setCancelled(true);

        // Click cooldown to prevent double-clicking
        long currentTime = System.currentTimeMillis();
        Long lastClick = lastClickTimes.get(player.getUniqueId());
        if (lastClick != null && (currentTime - lastClick) < CLICK_COOLDOWN_MS) {
            return true;
        }
        lastClickTimes.put(player.getUniqueId(), currentTime);

        int slot = event.getSlot();

        // Handle auction item clicks (0-44)
        if (slot >= 0 && slot < 45 && slot < currentAuctions.size()) {
            handleAuctionClick(currentAuctions.get(slot));
            return true;
        }

        // Handle control panel clicks (45-53)
        handleControlClick(slot);
        return true;
    }

    /**
     * Handle auction item purchase with validation
     */
    private void handleAuctionClick(Auction auction) {
        // Double-check auction validity
        if (System.currentTimeMillis() > auction.endAt()) {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("errors.auction-expired",
                "&cThis auction has expired."));
            return;
        }

        if (auction.seller().equals(player.getUniqueId())) {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("errors.cannot-buy-own",
                "&cYou cannot buy your own auction."));
            return;
        }

        // Check balance again
        double playerBalance = plugin.getEconomyRouter().getService().getBalance(player.getUniqueId()).join();
        if (playerBalance < auction.price()) {
            player.sendMessage(plugin.getConfigManager().getPrefixedMessage("errors.insufficient-funds",
                "&cYou don't have enough money. Need: &e" + plugin.getEconomyRouter().getService().format(auction.price())));
            return;
        }

        // Process purchase
        player.sendMessage(plugin.getConfigManager().getPrefixedMessage("info.processing-purchase",
            "&eProcessing purchase..."));

        plugin.getAuctionService().buyItem(player, auction.id()).thenAccept(success -> {
            if (success) {
                player.sendMessage(plugin.getConfigManager().getPrefixedMessage("info.purchase-success",
                    "&aSuccessfully purchased &e" + auction.item().toItemStack().getType().toString() +
                    "&a for &e" + plugin.getEconomyRouter().getService().format(auction.price()) + "&a!"));

                // Play success sound
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 2.0f);

                // Refresh the GUI
                open();
            } else {
                player.sendMessage(plugin.getConfigManager().getPrefixedMessage("errors.purchase-failed",
                    "&cFailed to purchase item. It may have been sold already."));

                // Play error sound
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
        });
    }

    /**
     * Handle control panel clicks
     */
    private void handleControlClick(int slot) {
        switch (slot) {
            case 45 -> { // Previous page
                if (page > 1) {
                    page--;
                    open();
                }
            }
            case 46 -> { // Sort
                sortOrder = sortOrder.next();
                open();
            }
            case 47 -> { // Search
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getPrefixedMessage("info.enter-search-query",
                    "&ePlease type your search query in chat. Type 'cancel' to exit."));
                plugin.getSearchInputListener().addPlayerToSearchMode(player.getUniqueId());
            }
            case 48 -> { // My listings
                player.sendMessage(plugin.getConfigManager().getPrefixedMessage("info.coming-soon", "&eMy Listings feature coming soon!"));
            }
            case 49 -> { // Mailbox
                player.sendMessage(plugin.getConfigManager().getPrefixedMessage("info.coming-soon", "&eMailbox feature coming soon!"));
            }
            case 53 -> { // Next page
                if (hasNextPage) {
                    page++;
                    open();
                }
            }
        }
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
        this.page = 1;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        this.page = 1;
    }
}