package com.minekartastudio.kartaauctionhouse.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class VaultEconomyService implements EconomyService {

    private final JavaPlugin plugin;
    private final Economy vault;

    public VaultEconomyService(JavaPlugin plugin, Economy vault) {
        this.plugin = plugin;
        this.vault = vault;
    }

    @Override
    public String getName() {
        return "Vault (" + vault.getName() + ")";
    }

    @Override
    public CompletableFuture<Boolean> has(UUID playerId, double amount) {
        return supplyAsynchronously(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            return vault.has(player, amount);
        });
    }

    @Override
    public CompletableFuture<Boolean> withdraw(UUID playerId, double amount, String reason) {
        return supplyAsynchronously(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            EconomyResponse response = vault.withdrawPlayer(player, amount);
            return response.transactionSuccess();
        });
    }

    @Override
    public CompletableFuture<Void> deposit(UUID playerId, double amount, String reason) {
        return runAsynchronously(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            vault.depositPlayer(player, amount);
        });
    }

    @Override
    public String format(double amount) {
        return vault.format(amount);
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID playerId) {
        return supplyAsynchronously(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            return vault.getBalance(player);
        });
    }

    private <T> CompletableFuture<T> supplyAsynchronously(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                future.complete(supplier.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private CompletableFuture<Void> runAsynchronously(Runnable runnable) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
