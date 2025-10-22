package com.minekartastudio.kartaauctionhouse.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        config = loadConfig("config.yml");
        messages = loadConfig("messages.yml");
    }

    private FileConfiguration loadConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public String getMessage(String path, String... replacements) {
        String message = messages.getString(path, "&cMissing message: " + path);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                String placeholder = replacements[i];
                String value = replacements[i + 1];
                if (placeholder != null && value != null) {
                    message = message.replace(placeholder, value);
                }
            }
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getPrefixedMessage(String path, String... replacements) {
        String prefix = messages.getString("prefix", "&7[&6KAH&7] ");
        String message = getMessage(path, replacements);
        return ChatColor.translateAlternateColorCodes('&', prefix) + message;
    }

    /**
     * Process color codes in a string
     */
    public String processColors(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
