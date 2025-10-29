package net.godlycow.io.vaultleaderboards;

import org.bstats.bukkit.Metrics;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class VaultLeaderboards extends JavaPlugin {

    private static VaultLeaderboards instance;
    private Economy economy;
    private LeaderboardManager leaderboardManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        if (!setupEconomy()) {
            getLogger().severe("Vault or an economy plugin not found! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        leaderboardManager = new LeaderboardManager(this, economy);
        leaderboardManager.updateLeaderboard();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new LeaderboardPlaceholder(this, leaderboardManager).register();
            getLogger().info("Registered PlaceholderAPI placeholders.");
        } else {
            getLogger().warning("PlaceholderAPI not found — placeholders won't work.");
        }

        long refreshSeconds = getConfig().getLong("settings.refresh-interval", 60L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, leaderboardManager::updateLeaderboard, 20L, refreshSeconds * 20L);

        getCommand("vaultleaderboards").setExecutor((sender, command, label, args) -> {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("vaultleaderboards.reload")) {
                    sender.sendMessage("§cYou lack permission to reload VaultLeaderboards.");
                    return true;
                }
                reloadConfig();
                leaderboardManager.updateLeaderboard();
                sender.sendMessage("§aVaultLeaderboards config reloaded!");
                return true;
            }
            sender.sendMessage("§eVaultLeaderboards by _GodlyCow");
            sender.sendMessage("§eUse /vaultleaderboards reload");
            return true;
        });

        int pluginId = 27346;
        new Metrics(this, pluginId);

        getLogger().info("VaultLeaderboards enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("VaultLeaderboards disabled.");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null)
            return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
            return false;

        economy = rsp.getProvider();
        return economy != null;
    }

    public static VaultLeaderboards getInstance() {
        return instance;
    }
}
