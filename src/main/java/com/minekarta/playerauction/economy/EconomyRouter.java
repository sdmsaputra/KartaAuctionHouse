package com.minekarta.playerauction.economy;

import com.minekarta.playerauction.config.ConfigManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public class EconomyRouter {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private EconomyService activeService;

    public EconomyRouter(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        setupEconomy();
    }

    private void setupEconomy() {
        String preferredProvider = configManager.getConfig().getString("economy.preferred", "VAULT").toUpperCase();

        KartaEmeraldEconomyService kecService = new KartaEmeraldEconomyService(plugin);
        VaultEconomyService vaultService = setupVault();

        if (Objects.equals(preferredProvider, "KARTAEMERALDCURRENCY") && kecService.isEnabled()) {
            this.activeService = kecService;
            plugin.getLogger().info("Hooked into preferred economy: KartaEmeraldCurrency.");
        } else if (vaultService != null) {
            this.activeService = vaultService;
            plugin.getLogger().info("Hooked into economy: " + vaultService.getName());
        } else if (kecService.isEnabled()) {
            // Fallback to KEC if Vault wasn't found/hooked
            this.activeService = kecService;
            plugin.getLogger().info("Hooked into fallback economy: KartaEmeraldCurrency.");
        } else {
            plugin.getLogger().severe("No compatible economy plugin found! PlayerAuction will not function correctly.");
            this.activeService = null;
        }
    }

    private VaultEconomyService setupVault() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return null;
        }
        return new VaultEconomyService(plugin, rsp.getProvider());
    }

    public EconomyService getService() {
        if (activeService == null) {
            throw new IllegalStateException("Economy service is not available, but was requested. The plugin should have handled this gracefully.");
        }
        return activeService;
    }

    public boolean hasService() {
        return activeService != null;
    }
}
