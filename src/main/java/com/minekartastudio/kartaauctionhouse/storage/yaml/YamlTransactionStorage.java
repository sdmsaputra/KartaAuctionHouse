package com.minekartastudio.kartaauctionhouse.storage.yaml;

import com.minekartastudio.kartaauctionhouse.storage.TransactionStorage;
import com.minekartastudio.kartaauctionhouse.transaction.model.Transaction;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class YamlTransactionStorage implements TransactionStorage {

    private final JavaPlugin plugin;
    private final File transactionFile;
    private final Yaml yaml;
    private final List<Transaction> transactionLog = Collections.synchronizedList(new ArrayList<>());
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public YamlTransactionStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.transactionFile = new File(plugin.getDataFolder(), "transactions.yml");
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);
    }

    @Override
    public void init() {
        lock.writeLock().lock();
        try {
            if (!transactionFile.exists()) {
                try {
                    transactionFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not create transactions.yml file!");
                    e.printStackTrace();
                }
            } else {
                loadTransactionsFromFile();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadTransactionsFromFile() {
        try (Reader reader = new FileReader(transactionFile)) {
            Map<String, List<Map<String, Object>>> data = yaml.load(reader);
            if (data != null && data.containsKey("transactions")) {
                List<Map<String, Object>> transactionMaps = data.get("transactions");
                transactionLog.clear();
                for (Map<String, Object> map : transactionMaps) {
                    Transaction transaction = YamlUtil.transactionFromMap(map);
                    transactionLog.add(transaction);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveTransactionsToFile() {
        try (Writer writer = new FileWriter(transactionFile)) {
            List<Map<String, Object>> transactionMaps = transactionLog.stream()
                    .map(YamlUtil::transactionToMap)
                    .collect(Collectors.toList());
            Map<String, List<Map<String, Object>>> data = new HashMap<>();
            data.put("transactions", transactionMaps);
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public CompletableFuture<Void> logTransaction(Transaction transaction) {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                transactionLog.add(transaction);
                saveTransactionsToFile();
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    @Override
    public CompletableFuture<List<Transaction>> findTransactionsByPlayer(UUID playerId, int limit, int offset) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return transactionLog.stream()
                        .filter(t -> t.sellerUuid().equals(playerId) || (t.buyerUuid() != null && t.buyerUuid().equals(playerId)))
                        .sorted(Comparator.comparing(Transaction::timestamp).reversed())
                        .skip(offset)
                        .limit(limit)
                        .collect(Collectors.toList());
            } finally {
                lock.readLock().unlock();
            }
        });
    }
}
