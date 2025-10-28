package com.minekarta.playerauction.commands;

import com.minekarta.playerauction.PlayerAuction;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AuctionTabCompleter implements TabCompleter {

    private final PlayerAuction plugin;

    public AuctionTabCompleter(PlayerAuction plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Main subcommands
            if (sender.hasPermission("playerauctions.use")) {
                completions.add("help");
            }
            if (sender.hasPermission("playerauctions.sell")) {
                completions.add("sell");
            }
            if (sender.hasPermission("playerauctions.search")) {
                completions.add("search");
            }
            if (sender.hasPermission("playerauctions.notify")) {
                completions.add("notify");
            }
            if (sender.hasPermission("playerauctions.history")) {
                completions.add("history");
            }
            if (sender.hasPermission("playerauctions.reload")) {
                completions.add("reload");
            }

            // Add alternative commands
            if (sender.hasPermission("playerauctions.use")) {
                completions.add("listings");
                completions.add("myauctions");
            }
        } else if (args.length == 2) {
            String subcommand = args[0].toLowerCase();

            switch (subcommand) {
                case "notify":
                    if (sender.hasPermission("playerauctions.notify")) {
                        completions.add("on");
                        completions.add("off");
                    }
                    break;

                case "search":
                    if (sender.hasPermission("playerauctions.search")) {
                        // Add search suggestions for common items
                        completions.add("diamond");
                        completions.add("gold");
                        completions.add("iron");
                        completions.add("emerald");
                        completions.add("sword");
                        completions.add("pickaxe");
                        completions.add("armor");
                    }
                    break;

                case "history":
                    if (sender.hasPermission("playerauctions.history.others")) {
                        // Add online player names
                        for (Player player : plugin.getServer().getOnlinePlayers()) {
                            completions.add(player.getName());
                        }
                    }
                    break;

                case "sell":
                    if (sender.hasPermission("playerauctions.sell")) {
                        completions.add("100");
                        completions.add("500");
                        completions.add("1000");
                        completions.add("5000");
                        completions.add("10000");
                    }
                    break;
            }
        } else if (args.length == 3) {
            String subcommand = args[0].toLowerCase();

            if (subcommand.equals("sell") && sender.hasPermission("playerauctions.sell")) {
                // Buy now price suggestions
                completions.add("200");
                completions.add("1000");
                completions.add("2000");
                completions.add("10000");
                completions.add("20000");
            }
        } else if (args.length == 4) {
            String subcommand = args[0].toLowerCase();

            if (subcommand.equals("sell") && sender.hasPermission("playerauctions.sell")) {
                // Duration suggestions
                completions.add("1h");
                completions.add("6h");
                completions.add("12h");
                completions.add("24h");
                completions.add("48h");
                completions.add("72h");
            }
        }

        return filterCompletions(completions, args[args.length - 1]);
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        List<String> filtered = new ArrayList<>();

        for (String completion : completions) {
            if (completion.toLowerCase().startsWith(input.toLowerCase())) {
                filtered.add(completion);
            }
        }

        return filtered;
    }
}