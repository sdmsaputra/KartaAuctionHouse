package com.minekartastudio.kartaauctionhouse.storage.yaml;

import com.minekartastudio.kartaauctionhouse.auction.model.Auction;
import com.minekartastudio.kartaauctionhouse.auction.model.AuctionStatus;
import com.minekartastudio.kartaauctionhouse.auction.model.Bid;
import com.minekartastudio.kartaauctionhouse.gui.model.AuctionCategory;
import com.minekartastudio.kartaauctionhouse.gui.model.SortOrder;
import com.minekartastudio.kartaauctionhouse.storage.AuctionStorage;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class YamlAuctionStorage implements AuctionStorage {

    private final JavaPlugin plugin;
    private final File auctionFile;
    private final Yaml yaml;
    private final Map<UUID, Auction> auctionCache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public YamlAuctionStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.auctionFile = new File(plugin.getDataFolder(), "auctions.yml");
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);
    }

    @Override
    public void init() {
        lock.writeLock().lock();
        try {
            if (!auctionFile.exists()) {
                try {
                    auctionFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not create auctions.yml file!");
                    e.printStackTrace();
                }
            } else {
                loadAuctionsFromFile();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadAuctionsFromFile() {
        try (Reader reader = new FileReader(auctionFile)) {
            Map<String, List<Map<String, Object>>> data = yaml.load(reader);
            if (data != null && data.containsKey("auctions")) {
                List<Map<String, Object>> auctionMaps = data.get("auctions");
                auctionCache.clear();
                for (Map<String, Object> map : auctionMaps) {
                    Auction auction = YamlUtil.auctionFromMap(map);
                    auctionCache.put(auction.id(), auction);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAuctionsToFile() {
        try (Writer writer = new FileWriter(auctionFile)) {
            List<Map<String, Object>> auctionMaps = auctionCache.values().stream()
                    .map(YamlUtil::auctionToMap)
                    .collect(Collectors.toList());
            Map<String, List<Map<String, Object>>> data = new HashMap<>();
            data.put("auctions", auctionMaps);
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Optional<Auction>> findById(UUID id) {
        return CompletableFuture.completedFuture(Optional.ofNullable(auctionCache.get(id)));
    }

    @Override
    public CompletableFuture<List<Auction>> findActive(int limit, int offset, AuctionCategory category, SortOrder sortOrder, String searchQuery) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                List<Auction> activeAuctions = auctionCache.values().stream()
                        .filter((Auction a) -> a.status() == AuctionStatus.ACTIVE)
                        // TODO: Add filtering for category and search query
                        .collect(Collectors.toList());

                // Sorting
                Comparator<Auction> comparator = switch (sortOrder) {
                    case PRICE_ASC -> Comparator.comparing((Auction a) -> a.currentBid() != null ? a.currentBid() : a.startingPrice());
                    case PRICE_DESC -> Comparator.comparing((Auction a) -> a.currentBid() != null ? a.currentBid() : a.startingPrice()).reversed();
                    case NEWEST -> Comparator.comparing(Auction::createdAt).reversed();
                    default -> Comparator.comparing(Auction::endAt); // TIME_LEFT
                };
                activeAuctions.sort(comparator);

                // Pagination
                return activeAuctions.stream()
                        .skip(offset)
                        .limit(limit)
                        .collect(Collectors.toList());
            } finally {
                lock.readLock().unlock();
            }
        });
    }

    @Override
    public CompletableFuture<List<Auction>> findBySeller(UUID seller, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return auctionCache.values().stream()
                        .filter((Auction a) -> a.seller().equals(seller))
                        .sorted(Comparator.comparing(Auction::createdAt).reversed())
                        .skip(offset)
                        .limit(limit)
                        .collect(Collectors.toList());
            } finally {
                lock.readLock().unlock();
            }
        });
    }

    @Override
    public CompletableFuture<Integer> countActiveBySeller(UUID sellerId) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return (int) auctionCache.values().stream()
                        .filter((Auction a) -> a.seller().equals(sellerId) && a.status() == AuctionStatus.ACTIVE)
                        .count();
            } finally {
                lock.readLock().unlock();
            }
        });
    }

    @Override
    public CompletableFuture<Void> insertAuction(Auction a) {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                auctionCache.put(a.id(), a);
                saveAuctionsToFile();
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> updateAuctionIfVersionMatches(Auction a, int expectedVersion) {
        return CompletableFuture.supplyAsync(() -> {
            lock.writeLock().lock();
            try {
                Auction existing = auctionCache.get(a.id());
                if (existing != null && existing.version() == expectedVersion) {
                    auctionCache.put(a.id(), a);
                    saveAuctionsToFile();
                    return true;
                }
                return false;
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    @Override
    public CompletableFuture<Void> insertBid(Bid b) {
        // Bids are part of the Auction object in YAML storage, so this is a no-op.
        // The auction is updated via updateAuctionIfVersionMatches.
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<Auction>> findExpiredUpTo(long nowEpochMillis, int batchSize) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return auctionCache.values().stream()
                        .filter((Auction a) -> a.status() == AuctionStatus.ACTIVE && a.endAt() <= nowEpochMillis)
                        .limit(batchSize)
                        .collect(Collectors.toList());
            } finally {
                lock.readLock().unlock();
            }
        });
    }
}
