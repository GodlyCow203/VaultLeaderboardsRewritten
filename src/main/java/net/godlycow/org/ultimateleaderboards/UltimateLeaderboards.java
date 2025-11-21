package net.godlycow.org.ultimateleaderboards;

import net.godlycow.org.ultimateleaderboards.manager.LeaderboardManager;
import net.godlycow.org.ultimateleaderboards.placeholder.UltimateLeaderboardsExpansion;
import net.godlycow.org.ultimateleaderboards.util.UpdateChecker;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class UltimateLeaderboards extends JavaPlugin implements Listener {
    private static UltimateLeaderboards instance;
    private Economy economy;
    private LeaderboardManager leaderboardManager;
    private UpdateChecker updateChecker;

    public static final String PREFIX = "<dark_gray>[<gold>UL</gold>]</dark_gray>";
    private static final String MODRINTH_PROJECT_ID = "qlRnqYv7";
    private static final int BSTATS_PLUGIN_ID = 27346;

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
        updateChecker = new UpdateChecker(this, MODRINTH_PROJECT_ID);

        // Register events
        Bukkit.getPluginManager().registerEvents(this, this);

        // Register command
        getCommand("ultimateleaderboards").setExecutor(this::handleCommand);
        getCommand("ultimateleaderboards").setTabCompleter(new UltimateLeaderboardsTabCompleter());

        // Setup PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new UltimateLeaderboardsExpansion(this, leaderboardManager).register();
            getLogger().info("Registered PlaceholderAPI placeholders.");
        } else {
            getLogger().warning("PlaceholderAPI not found — placeholders won't work.");
        }

        // Start update checker
        if (getConfig().getBoolean("settings.check-updates", true)) {
            updateChecker.checkForUpdates();
        }

        // Start leaderboard refresh task
        long refreshSeconds = getConfig().getLong("settings.refresh-interval", 60L);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                () -> leaderboardManager.updateEconomyData(),
                20L, refreshSeconds * 20L
        );

        // Setup bStats
        new Metrics(this, BSTATS_PLUGIN_ID);

        getLogger().info("UltimateLeaderboards enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("UltimateLeaderboards disabled.");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!event.getPlayer().isOp()) return;
        if (!getConfig().getBoolean("settings.check-updates", true)) return;
        if (!getConfig().getBoolean("settings.notify-op-updates", true)) return;

        updateChecker.notifyIfUpdateAvailable(event.getPlayer());
    }

    private boolean handleCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "<gold>UltimateLeaderboards by _GodlyCow");
            MessageUtil.sendMessage(sender, "<gray>Use /ultimateleaderboards help");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                if (!hasPermission(sender, "ultimateleaderboards.help")) {
                    MessageUtil.sendMessage(sender, getConfig().getString("messages.no-permission", "<red>You lack permission for this command.</red>"));
                    return true;
                }
                sendHelpMessage(sender);
                return true;

            case "reload":
                if (!hasPermission(sender, "ultimateleaderboards.reload")) {
                    MessageUtil.sendMessage(sender, getConfig().getString("messages.no-permission", "<red>You lack permission for this command.</red>"));
                    return true;
                }
                reloadConfig();
                leaderboardManager.updateEconomyData();
                MessageUtil.sendMessage(sender, getConfig().getString("messages.reload", "<green>Configuration reloaded!</green>"));
                return true;

            case "version":
                if (!hasPermission(sender, "ultimateleaderboards.version")) {
                    MessageUtil.sendMessage(sender, getConfig().getString("messages.no-permission", "<red>You lack permission for this command.</red>"));
                    return true;
                }
                sendVersionInfo(sender);
                return true;

            default:
                MessageUtil.sendMessage(sender, "<red>Unknown command. Use /ultimateleaderboards help");
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        getConfig().getStringList("messages.help").forEach(line ->
                MessageUtil.sendMessage(sender, line)
        );
    }

    private void sendVersionInfo(CommandSender sender) {
        String currentVersion = getDescription().getVersion();
        MessageUtil.sendMessage(sender, "<gold>UltimateLeaderboards <gray>v" + currentVersion + "</gray></gold>");

        if (updateChecker.isUpdateCheckComplete()) {
            updateChecker.sendVersionUpdate(sender);
        } else {
            MessageUtil.sendMessage(sender, getConfig().getString("messages.version.checking", "<gray>Checking for updates...</gray>"));
            updateChecker.checkForUpdates(() -> updateChecker.sendVersionUpdate(sender));
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }

        economy = rsp.getProvider();
        return economy != null;
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return sender.hasPermission(permission) || sender.isOp();
    }

    public static UltimateLeaderboards getInstance() {
        return instance;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    private class UltimateLeaderboardsTabCompleter implements org.bukkit.command.TabCompleter {
        private static final java.util.List<String> COMMANDS = java.util.Arrays.asList("help", "reload", "version");

        @Override
        public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                return COMMANDS.stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .collect(java.util.stream.Collectors.toList());
            }
            return java.util.Collections.emptyList();
        }
    }
}