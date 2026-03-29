package dev.lexon.metintasi.integration;

import dev.lexon.metintasi.MetinTasiPlugin;
import dev.lexon.metintasi.model.MetinStone;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * %metintasi_total%, %metintasi_broken%, %metintasi_active%
 */
public class MetinTasiPlaceholders extends PlaceholderExpansion {

    private final MetinTasiPlugin plugin;

    public MetinTasiPlaceholders(MetinTasiPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "metintasi";
    }

    @Override
    public @NotNull String getAuthor() {
        return "CordiDev";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        var stones = plugin.getMetinStoneManager().getAllStones();
        int total = stones.size();
        long broken = stones.stream().filter(MetinStone::isBroken).count();
        int active = total - (int) broken;

        return switch (params.toLowerCase()) {
            case "total" -> String.valueOf(total);
            case "broken" -> String.valueOf(broken);
            case "active" -> String.valueOf(active);
            default -> null;
        };
    }
}
