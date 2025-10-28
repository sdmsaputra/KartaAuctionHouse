package com.minekarta.playerauction.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class PlayerNameCache {

    private final Cache<UUID, String> nameCache;
    private final Executor asyncExecutor;

    public PlayerNameCache(Executor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
        this.nameCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();
    }

    /**
     * Asynchronously gets the name of a player from a UUID.
     * Tries the cache first, then looks up the offline player.
     *
     * @param uuid The UUID of the player.
     * @return A CompletableFuture that will complete with the player's name,
     *         or a shortened UUID if the name cannot be found.
     */
    public CompletableFuture<String> getName(@NotNull UUID uuid) {
        String cachedName = nameCache.getIfPresent(uuid);
        if (cachedName != null) {
            return CompletableFuture.completedFuture(cachedName);
        }

        return CompletableFuture.supplyAsync(() -> {
            // This can be a blocking operation, so it's run async
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
            String name = player.getName();

            if (name != null) {
                nameCache.put(uuid, name);
                return name;
            } else {
                // Fallback for players who have never joined or whose profiles can't be fetched
                return uuid.toString().substring(0, 8);
            }
        }, asyncExecutor);
    }

    /**
     * Proactively fetches and caches a player's name.
     *
     * @param uuid The UUID of the player to prefetch.
     */
    public void prefetchName(@NotNull UUID uuid) {
        if (nameCache.getIfPresent(uuid) == null) {
            getName(uuid); // The result is intentionally not used, just populates the cache
        }
    }

    /**
     * Removes a player's name from the cache.
     *
     * @param uuid The UUID of the player to invalidate.
     */
    public void invalidate(@NotNull UUID uuid) {
        nameCache.invalidate(uuid);
    }
}
