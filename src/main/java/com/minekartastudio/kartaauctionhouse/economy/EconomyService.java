package com.minekartastudio.kartaauctionhouse.economy;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface EconomyService {
    /**
     * @return The name of the economy provider (e.g., "Vault", "KartaEmeraldCurrency").
     */
    String getName();

    /**
     * Checks if a player has at least a certain amount of money.
     */
    CompletableFuture<Boolean> has(UUID player, double amount);

    /**
     * Withdraws money from a player's account.
     * @return A future completing with true if the withdrawal was successful, false otherwise.
     */
    CompletableFuture<Boolean> withdraw(UUID player, double amount, String reason);

    /**
     * Deposits money into a player's account.
     */
    CompletableFuture<Void> deposit(UUID player, double amount, String reason);

    /**
     * Formats a double amount into a currency string (e.g., "$1,234.56").
     */
    String format(double amount);
}
