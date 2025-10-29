package net.godlycow.io.vaultleaderboards;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.*;
import java.util.stream.Collectors;

public class LeaderboardManager {

    private final net.godlycow.io.vaultleaderboards.VaultLeaderboards plugin;
    private final Economy economy;
    private final MiniMessage mini = MiniMessage.miniMessage();

    private final Map<UUID, Double> balances = new HashMap<>();

    public LeaderboardManager(net.godlycow.io.vaultleaderboards.VaultLeaderboards plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
    }

    public void updateLeaderboard() {
        balances.clear();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                double balance = economy.getBalance(player);
                balances.put(player.getUniqueId(), balance);

                if (plugin.getConfig().getBoolean("settings.debug", false)) {
                    plugin.getLogger().info("Loaded " + player.getName() + " → " + balance);
                }
            }
        });
    }

    public Component getTopPlayerComponent(int rank) {
        List<Map.Entry<UUID, Double>> sorted = balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .limit(plugin.getConfig().getInt("settings.top-limit", 10))
                .collect(Collectors.toList());

        if (rank <= 0 || rank > sorted.size()) {
            return mini.deserialize("<red>N/A</red>");
        }

        Map.Entry<UUID, Double> entry = sorted.get(rank - 1);
        OfflinePlayer player = Bukkit.getOfflinePlayer(entry.getKey());
        String name = player.getName() != null ? player.getName() : "Unknown";

        String template = plugin.getConfig().getString("placeholders.top", "<yellow><rank>. <green><player></green> - <aqua>$<balance>");
        String result = template
                .replace("<rank>", String.valueOf(rank))
                .replace("<player>", name)
                .replace("<balance>", formatBalance(entry.getValue()));

        return mini.deserialize(result);
    }

    public Component getPlayerRankComponent(String name) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(name);
        if (!balances.containsKey(player.getUniqueId())) {
            return mini.deserialize("<red>N/A</red>");
        }

        List<Map.Entry<UUID, Double>> sorted = balances.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        int rank = 1;
        for (Map.Entry<UUID, Double> entry : sorted) {
            if (entry.getKey().equals(player.getUniqueId())) break;
            rank++;
        }

        String template = plugin.getConfig().getString("placeholders.player", "<yellow><player></yellow> is <green>#<rank></green> with <aqua>$<balance>");
        String result = template
                .replace("<player>", name)
                .replace("<rank>", String.valueOf(rank))
                .replace("<balance>", formatBalance(balances.get(player.getUniqueId())));

        return mini.deserialize(result);
    }

    private String formatBalance(double balance) {
        if (balance >= 1_000_000_000) return String.format("%.2fB", balance / 1_000_000_000);
        if (balance >= 1_000_000) return String.format("%.2fM", balance / 1_000_000);
        if (balance >= 1_000) return String.format("%.2fk", balance / 1_000);
        return String.format("%.2f", balance);
    }
}
