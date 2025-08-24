package com.minekartastudio.kartaauctionhouse.storage.yaml;

import com.minekartastudio.kartaauctionhouse.mailbox.model.MailboxEntry;
import com.minekartastudio.kartaauctionhouse.storage.MailboxStorage;
import org.bukkit.plugin.java.JavaPlugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class YamlMailboxStorage implements MailboxStorage {

    private final JavaPlugin plugin;
    private final File mailboxFile;
    private final Yaml yaml;
    private final Map<UUID, MailboxEntry> mailboxCache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public YamlMailboxStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mailboxFile = new File(plugin.getDataFolder(), "mailbox.yml");
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        this.yaml = new Yaml(options);
    }

    @Override
    public void init() {
        lock.writeLock().lock();
        try {
            if (!mailboxFile.exists()) {
                try {
                    mailboxFile.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Could not create mailbox.yml file!");
                    e.printStackTrace();
                }
            } else {
                loadMailboxFromFile();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void loadMailboxFromFile() {
        try (Reader reader = new FileReader(mailboxFile)) {
            Map<String, List<Map<String, Object>>> data = yaml.load(reader);
            if (data != null && data.containsKey("mailbox_entries")) {
                List<Map<String, Object>> entryMaps = data.get("mailbox_entries");
                mailboxCache.clear();
                for (Map<String, Object> map : entryMaps) {
                    MailboxEntry entry = YamlUtil.mailboxFromMap(map);
                    mailboxCache.put(entry.id(), entry);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveMailboxToFile() {
        try (Writer writer = new FileWriter(mailboxFile)) {
            List<Map<String, Object>> entryMaps = mailboxCache.values().stream()
                    .map(YamlUtil::mailboxToMap)
                    .collect(Collectors.toList());
            Map<String, List<Map<String, Object>>> data = new HashMap<>();
            data.put("mailbox_entries", entryMaps);
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public CompletableFuture<Void> enqueue(MailboxEntry e) {
        return CompletableFuture.runAsync(() -> {
            lock.writeLock().lock();
            try {
                mailboxCache.put(e.id(), e);
                saveMailboxToFile();
            } finally {
                lock.writeLock().unlock();
            }
        });
    }

    @Override
    public CompletableFuture<List<MailboxEntry>> getUnclaimed(UUID owner) {
        return CompletableFuture.supplyAsync(() -> {
            lock.readLock().lock();
            try {
                return mailboxCache.values().stream()
                        .filter(e -> e.owner().equals(owner) && !e.claimed())
                        .sorted(Comparator.comparing(MailboxEntry::createdAt).reversed())
                        .collect(Collectors.toList());
            } finally {
                lock.readLock().unlock();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> markClaimed(UUID entryId) {
        return CompletableFuture.supplyAsync(() -> {
            lock.writeLock().lock();
            try {
                MailboxEntry entry = mailboxCache.get(entryId);
                if (entry != null && !entry.claimed()) {
                    MailboxEntry updatedEntry = entry.withClaimed(true);
                    mailboxCache.put(entryId, updatedEntry);
                    saveMailboxToFile();
                    return true;
                }
                return false;
            } finally {
                lock.writeLock().unlock();
            }
        });
    }
}
