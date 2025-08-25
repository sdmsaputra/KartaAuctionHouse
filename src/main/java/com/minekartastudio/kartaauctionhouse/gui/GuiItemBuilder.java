package com.minekartastudio.kartaauctionhouse.gui;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.List;
import java.util.stream.Collectors;

public class GuiItemBuilder {
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public GuiItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public GuiItemBuilder(ItemStack itemStack) {
        this.itemStack = itemStack.clone();
        this.itemMeta = this.itemStack.getItemMeta();
    }

    public GuiItemBuilder setName(String name) {
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        return this;
    }

    public GuiItemBuilder setLore(String... lore) {
        itemMeta.setLore(List.of(lore).stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList()));
        return this;
    }

    public GuiItemBuilder setLore(List<String> lore) {
        itemMeta.setLore(lore.stream()
                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                .collect(Collectors.toList()));
        return this;
    }

    public GuiItemBuilder setAmount(int amount) {
        itemStack.setAmount(amount);
        return this;
    }

    public GuiItemBuilder addLore(String line) {
        List<String> lore = itemMeta.getLore();
        if (lore == null) {
            lore = new java.util.ArrayList<>();
        }
        lore.add(ChatColor.translateAlternateColorCodes('&', line));
        itemMeta.setLore(lore);
        return this;
    }

    public GuiItemBuilder setSkullOwner(String ownerName) {
        if (itemStack.getType() == Material.PLAYER_HEAD && itemMeta instanceof org.bukkit.inventory.meta.SkullMeta) {
            org.bukkit.inventory.meta.SkullMeta skullMeta = (org.bukkit.inventory.meta.SkullMeta) itemMeta;
            skullMeta.setOwningPlayer(org.bukkit.Bukkit.getOfflinePlayer(ownerName));
        }
        return this;
    }

    public ItemStack build() {
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
