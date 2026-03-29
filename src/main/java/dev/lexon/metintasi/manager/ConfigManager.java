package dev.lexon.metintasi.manager;

import dev.lexon.metintasi.MetinTasiPlugin;
import dev.lexon.metintasi.model.MetinStone;
import dev.lexon.metintasi.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class ConfigManager {

    private final MetinTasiPlugin plugin;
    /** Bukkit config.yml (plugins/MetinTasi/config.yml) */
    private FileConfiguration settingsConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration rewardsConfig;
    private File messagesFile;
    private File rewardsFile;

    public ConfigManager(MetinTasiPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadAll() {
        loadSettings();
        loadMessages();
        loadRewards();
    }

    private void loadSettings() {
        plugin.getDataFolder().mkdirs();
        File configYaml = new File(plugin.getDataFolder(), "config.yml");
        File legacySettings = new File(plugin.getDataFolder(), "settings.yml");
        try {
            if (!configYaml.exists() && legacySettings.exists()) {
                Files.copy(legacySettings.toPath(), configYaml.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("settings.yml icerigi config.yml olarak kopyalandi. Ayarlari artik config.yml uzerinden duzenleyin.");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("config.yml olusturulamadi: " + e.getMessage());
        }
        if (!configYaml.exists()) {
            plugin.saveDefaultConfig();
        }
        plugin.reloadConfig();
        settingsConfig = plugin.getConfig();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    private void loadRewards() {
        rewardsFile = new File(plugin.getDataFolder(), "rewards.yml");
        if (!rewardsFile.exists()) {
            plugin.saveResource("rewards.yml", false);
        }
        rewardsConfig = YamlConfiguration.loadConfiguration(rewardsFile);
    }

    public void saveRewards() {
        try {
            rewardsConfig.save(rewardsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("rewards.yml kaydedilemedi: " + e.getMessage());
        }
    }

    public void saveSettings() {
        plugin.saveConfig();
    }

    // --- Settings getters ---

    public boolean isDebugMode() {
        return settingsConfig.getBoolean("debug", false);
    }

    /**
     * Patlama, piston, fizik vb. ile metin blogunun bozulmasini engelle (vxMetin ProtectionListener fikri).
     */
    public boolean isProtectionEnabled() {
        return settingsConfig.getBoolean("protection.enabled", true);
    }

    public Material getDefaultBlockType() {
        String type = settingsConfig.getString("metin-stone.default-block", "STONE");
        try {
            return Material.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Gecersiz blok tipi: " + type + ", STONE kullaniliyor.");
            return Material.STONE;
        }
    }

    public double getDefaultHealth() {
        return settingsConfig.getDouble("metin-stone.default-health", 100.0);
    }

    public double getDamagePerHit() {
        return settingsConfig.getDouble("metin-stone.damage-per-hit", 1.0);
    }

    public int getBreakerTimeoutSeconds() {
        return settingsConfig.getInt("metin-stone.breaker-timeout", 10);
    }

    public List<String> getHologramLines() {
        return settingsConfig.getStringList("hologram.lines");
    }

    public List<String> getBrokenHologramLines() {
        List<String> lines = settingsConfig.getStringList("hologram.broken-lines");
        if (lines.isEmpty()) {
            return List.of("&c&lKirildi!", "&7Aktif: &f<aktif_zaman>");
        }
        return lines;
    }

    public double getHologramLineSpacing() {
        return settingsConfig.getDouble("hologram.line-spacing", 0.3);
    }

    public double getHologramOffsetY() {
        return settingsConfig.getDouble("hologram.offset-y", 2.0);
    }

    /** Tas kirmada hem genel hem tas ozel oduller cekilsin mi */
    public boolean isMergeGlobalRewardsWithStone() {
        return settingsConfig.getBoolean("rewards.merge-global-with-stone", true);
    }

    public List<String> getFloatingHologramLines() {
        return settingsConfig.getStringList("floating-hologram.lines");
    }

    public double getFloatingHologramSpacing() {
        if (settingsConfig.contains("floating-hologram.line-spacing")) {
            return settingsConfig.getDouble("floating-hologram.line-spacing");
        }
        return getHologramLineSpacing();
    }

    public double getFloatingHologramOffsetY() {
        return settingsConfig.getDouble("floating-hologram.offset-y", 0);
    }

    public int getFloatingHologramUpdateTicks() {
        return settingsConfig.getInt("floating-hologram.update-ticks", 40);
    }

    public String getDefaultStoneDisplayName() {
        return settingsConfig.getString("metin-stone.default-name", "Metin Taşı");
    }

    public int getLeaderboardMaxEntries() {
        return settingsConfig.getInt("leaderboard.max-entries", 5);
    }

    public String getLeaderboardLineFormat() {
        return settingsConfig.getString("leaderboard.line-format", "&e#<sira> &f<isim> &7(&a<kirilis>&7)");
    }

    public String getLeaderboardEmptySlot() {
        return settingsConfig.getString("leaderboard.empty-slot", "&7-");
    }

    // --- Spawn settings ---

    public List<String> getSpawnTimes() {
        return settingsConfig.getStringList("spawn.times");
    }

    public boolean isSpawnEnabled() {
        return settingsConfig.getBoolean("spawn.enabled", false);
    }

    public String getSpawnWorld() {
        return settingsConfig.getString("spawn.world", "world");
    }

    public int getSpawnX() {
        return settingsConfig.getInt("spawn.location.x", 0);
    }

    public int getSpawnY() {
        return settingsConfig.getInt("spawn.location.y", 64);
    }

    public int getSpawnZ() {
        return settingsConfig.getInt("spawn.location.z", 0);
    }

    // --- Reactivation settings ---

    public String getReactivationMode() {
        return settingsConfig.getString("reactivation.mode", "interval");
    }

    public int getReactivationIntervalMinutes() {
        double hours = settingsConfig.getDouble("reactivation.interval-hours", 0);
        if (hours > 0) {
            return (int) Math.round(hours * 60.0);
        }
        return settingsConfig.getInt("reactivation.interval-minutes", 120);
    }

    /** Yeni oluşturulan taşlara yazılacak yenilenme süresi (saat); 0 = taş dosyasına süre yazılmaz. */
    public double getDefaultReactivationHoursForNewStones() {
        return settingsConfig.getDouble("metin-stone.default-reactivation-hours", 0);
    }

    public boolean isReactivationBroadcastEnabled() {
        return settingsConfig.getBoolean("reactivation-broadcast.enabled", true);
    }

    public int getReactivationBroadcastTitleSpaces() {
        return Math.max(0, settingsConfig.getInt("reactivation-broadcast.title-leading-spaces", 28));
    }

    public String getReactivationBroadcastTitle() {
        return settingsConfig.getString("reactivation-broadcast.title", "DUYURU");
    }

    public String getReactivationBroadcastMessage() {
        return settingsConfig.getString("reactivation-broadcast.message",
                "&f<isim> &7adlı metin taşı &e<dunya> &7dünyasında yenilendi.");
    }

    /** Taş dosyasındaki reactivation-interval-minutes varsa o, yoksa genel ayar. */
    public int getReactivationIntervalMinutesForStone(MetinStone stone) {
        if (stone == null) return getReactivationIntervalMinutes();
        Integer o = stone.getReactivationIntervalMinutes();
        if (o != null && o > 0) return o;
        return getReactivationIntervalMinutes();
    }

    /** Kırık taş hologramında kalan süre yazısının yenilenme sıklığı (tick). */
    public int getBrokenHologramRefreshTicks() {
        return Math.max(1, settingsConfig.getInt("hologram.broken-refresh-ticks", 20));
    }

    public List<String> getReactivationTimes() {
        return settingsConfig.getStringList("reactivation.times");
    }

    // --- Messages getters ---

    public String getMessage(String key) {
        String prefix = messagesConfig.getString("prefix", "&8(&6Metin Taşı&8) &r");
        String message = messagesConfig.getString(key, "&cMesaj bulunamadi: " + key);
        return ColorUtil.colorize(prefix + message);
    }

    public String getRawMessage(String key) {
        return ColorUtil.colorize(messagesConfig.getString(key, "&cMesaj bulunamadi: " + key));
    }

    // --- Config accessors ---

    public FileConfiguration getSettingsConfig() {
        return settingsConfig;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public FileConfiguration getRewardsConfig() {
        return rewardsConfig;
    }
}
