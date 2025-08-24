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
        // Vault operations should be on the main thread
        return supplyOnMainThread(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            return vault.has(player, amount);
        });
    }

    @Override
    public CompletableFuture<Boolean> withdraw(UUID playerId, double amount, String reason) {
        return supplyOnMainThread(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            EconomyResponse response = vault.withdrawPlayer(player, amount);
            return response.transactionSuccess();
        });
    }

    @Override
    public CompletableFuture<Void> deposit(UUID playerId, double amount, String reason) {
        return runOnMainThread(() -> {
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
        return supplyOnMainThread(() -> {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            return vault.getBalance(player);
        });
    }

    private <T> CompletableFuture<T> supplyOnMainThread(Supplier<T> supplier) {
        // We must ensure that the supplier is not executed on the same thread that calls this method,
        // if the caller is on the main thread, to prevent deadlocks with other plugins that might
        // also be trying to do things on the main thread and waiting.
        // By using supplyAsync with an async executor, we move the execution off the current thread.
        // Then, inside the async task, we use callSyncMethod to ensure the Vault operation
        // itself runs on the main thread, as required by Vault.
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Bukkit.getScheduler().callSyncMethod(plugin, supplier::get).get();
            } catch (Exception e) {
                // Re-throw as an unchecked exception to be caught by the CompletableFuture
                throw new RuntimeException(e);
            }
        }, runnable -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable));
    }

    private CompletableFuture<Void> runOnMainThread(Runnable runnable) {
        // See comment in supplyOnMainThread for the reasoning.
        return CompletableFuture.runAsync(() -> {
            try {
                Bukkit.getScheduler().callSyncMethod(plugin, () -> {
                    runnable.run();
                    return null; // callSyncMethod requires a callable
                }).get();
            } catch (Exception e) {
                // Re-throw as an unchecked exception to be caught by the CompletableFuture
                throw new RuntimeException(e);
            }
        }, runnable1 -> Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable1));
    }
}
