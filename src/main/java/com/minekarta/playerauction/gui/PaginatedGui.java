package com.minekarta.playerauction.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import com.minekarta.playerauction.PlayerAuction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class PaginatedGui extends Gui {

    protected int page;
    protected final int itemsPerPage;
    protected boolean hasNextPage = false;

    public PaginatedGui(PlayerAuction plugin, Player player, int page, int itemsPerPage) {
        super(plugin, player);
        this.page = page;
        this.itemsPerPage = itemsPerPage;
    }

    @Override
    public int getSize() {
        return 54; // Standard 6-row inventory for better layout with borders
    }

    protected void addControlBar() {
        // Get border configuration
        boolean borderEnabled = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getConfig().getBoolean("gui.border.enabled", true);
        Material borderMaterial = Material.getMaterial(((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getConfig().getString("gui.border.material", "BLACK_STAINED_GLASS_PANE"));
        String borderName = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getConfig().getString("gui.border.name", " ");
        java.util.List<String> borderLore = ((com.minekarta.playerauction.PlayerAuction) plugin).getConfigManager().getConfig().getStringList("gui.border.lore");

        ItemStack borderFiller = new GuiItemBuilder(borderMaterial != null ? borderMaterial : Material.BLACK_STAINED_GLASS_PANE).setName(borderName).setLore(borderLore).build();
        ItemStack accentFiller = new GuiItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE).setName(" ").build();

        // Add border if enabled
        if (borderEnabled) {
            // Top and bottom borders
            for (int i = 0; i < 9; i++) {
                inventory.setItem(i, borderFiller); // Top row
                inventory.setItem(i + 45, borderFiller); // Bottom row
            }
            // Side borders
            for (int i = 1; i < 5; i++) {
                inventory.setItem(i * 9, borderFiller); // Left side
                inventory.setItem(i * 9 + 8, borderFiller); // Right side
            }
        } else {
            // Create decorative border for bottom control area only
            for (int i = 36; i <= 44; i++) {
                inventory.setItem(i, borderFiller);
            }
        }

        // Corner accents for visual appeal
        inventory.setItem(borderEnabled ? 0 : 36, borderFiller); // Bottom-left corner
        inventory.setItem(borderEnabled ? 8 : 44, borderFiller); // Bottom-right corner

        // Add side accents for visual framing
        for (int i = 45; i <= 53; i++) {
            if (i % 2 == 1) {
                inventory.setItem(i, accentFiller);
            }
        }

        // Clean layout with essential controls only
        // Previous Page Button (Slot 46) - Left side
        if (page > 1) {
            inventory.setItem(46, new GuiItemBuilder(Material.ARROW)
                .setName("§ePrevious Page")
                .setLore("§7Page " + (page - 1))
                .build());
        }

        // Sort Button (Slot 47) - Left side
        inventory.setItem(47, new GuiItemBuilder(Material.COMPARATOR)
            .setName("§aSort: §f" + getCurrentSortOrder())
            .setLore("§7Click to change", "§8Newest → Oldest → Price")
            .build());

        // Player Info Item (Slot 49) - Center position
        createPlayerInfoItem().thenAccept(item -> {
            inventory.setItem(49, item);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                inventory.getViewers().forEach(viewer -> {
                    if (viewer instanceof Player playerViewer) {
                        playerViewer.updateInventory();
                    }
                });
            });
        });

        // Next Page Button (Slot 52) - Right side
        if (hasNextPage) {
            inventory.setItem(52, new GuiItemBuilder(Material.ARROW)
                .setName("§eNext Page")
                .setLore("§7Page " + (page + 1))
                .build());
        }
    }

    protected boolean handleControlBarClick(InventoryClickEvent event) {
        int slot = event.getSlot();

        // Navigation controls
        if (slot == 46 && page > 1) {
            // Previous page
            openPage(page - 1);
            return true;
        } else if (slot == 52 && hasNextPage) {
            // Next page
            openPage(page + 1);
            return true;
        } else if (slot == 47) {
            // Sort button - to be implemented by subclasses
            return false;
        }

        // Default handling for other slots
        return false;
    }

    protected String getCurrentSortOrder() {
        return "Newest"; // Default implementation, can be overridden by subclasses
    }

    protected abstract void openPage(int newPage);
}
