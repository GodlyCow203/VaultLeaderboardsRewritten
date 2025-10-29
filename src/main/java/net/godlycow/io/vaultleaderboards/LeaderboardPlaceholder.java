package net.godlycow.io.vaultleaderboards;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class LeaderboardPlaceholder extends PlaceholderExpansion {

    private final VaultLeaderboards plugin;
    private final LeaderboardManager manager;

    public LeaderboardPlaceholder(VaultLeaderboards plugin, LeaderboardManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "vaultleaderboards";
    }

    @Override
    public @NotNull String getAuthor() {
        return "_GodlyCow";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (identifier.startsWith("top_")) {
            try {
                int rank = Integer.parseInt(identifier.split("_")[1]);
                return LegacyComponentSerializer.legacySection().serialize(manager.getTopPlayerComponent(rank));
            } catch (Exception e) {
                return "§cInvalid rank";
            }
        }

        if (identifier.matches("^[A-Za-z0-9_]{3,16}$")) {
            return LegacyComponentSerializer.legacySection().serialize(manager.getPlayerRankComponent(identifier));
        }

        return null;
    }
}
