package com.minekartastudio.kartaauctionhouse.storage.sql;

import com.minekartastudio.kartaauctionhouse.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class DatabaseManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;
    private final ExecutorService executor;

    public DatabaseManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;

        ThreadFactory threadFactory = new ThreadFactoryBuilder()
            .setNameFormat("KartaAuctionHouse-DB-%d")
            .setDaemon(true)
            .build();
        this.executor = Executors.newFixedThreadPool(configManager.getConfig().getInt("database.pool-size", 10), threadFactory);

        connect();
    }

    private void connect() {
        try {
            ConfigurationSection dbConfig = configManager.getConfig().getConfigurationSection("database");
            if (dbConfig == null) {
                plugin.getLogger().severe("Database configuration section is missing from config.yml!");
                return;
            }

            HikariConfig config = new HikariConfig();
            config.setPoolName("KartaAuctionHouse-Pool");

            // Set the driver class name for MySQL
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("MySQL JDBC driver not found. Make sure it's included in the plugin JAR.");
                return;
            }

            String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=%b",
                dbConfig.getString("host", "localhost"),
                dbConfig.getInt("port", 3306),
                dbConfig.getString("database", "auctionhouse"),
                dbConfig.getBoolean("use-ssl", false)
            );

            config.setJdbcUrl(jdbcUrl);
            config.setUsername(dbConfig.getString("username"));
            config.setPassword(dbConfig.getString("password"));
            config.setMaximumPoolSize(dbConfig.getInt("pool-size", 10));
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);

            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Database connection pool successfully initialized.");
        } catch (Exception e) {
            plugin.getLogger().severe("Could not initialize database connection pool! Check your config.yml.");
            e.printStackTrace();
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        executor.shutdown();
    }

    public Connection getConnection() throws SQLException {
        if (!isDatabaseConnected()) {
            throw new SQLException("Database source is not available.");
        }
        return dataSource.getConnection();
    }

    public boolean isDatabaseConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
