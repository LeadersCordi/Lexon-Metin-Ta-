package dev.lexon.metintasi.model;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MetinStone {

    private final UUID id;
    private final Location location;
    private Material blockType;
    private double maxHealth;
    private double currentHealth;
    private UUID currentBreaker;
    private long lastHitTime;
    private boolean broken;
    private long brokenTime;
    private String displayName;
    private List<String> hologramLinesOverride;
    private List<String> brokenHologramLinesOverride;
    private boolean leaderboardEnabled;
    /** Oyuncu /metintasi hologram here ile ayarlar; null ise settings hologram.offset-y kullanilir */
    private Double hologramOffsetYOverride;
    /** Taşı en son tamamen kıran oyuncu (hologram &lt;son_kiran&gt;) */
    private UUID lastBreakerId;
    /** null: config.yml reactivation süresi kullanılır */
    private Integer reactivationIntervalMinutes;
    private final List<Reward> stoneRewards;
    private final Map<UUID, Integer> breakCounts;
    private final List<TextDisplay> hologramDisplays;

    public MetinStone(Location location, Material blockType, double maxHealth, String displayName) {
        this.id = UUID.randomUUID();
        this.location = location.getBlock().getLocation();
        this.blockType = blockType;
        this.maxHealth = maxHealth;
        this.currentHealth = maxHealth;
        this.currentBreaker = null;
        this.lastHitTime = 0;
        this.broken = false;
        this.brokenTime = 0;
        this.displayName = displayName != null ? displayName : "Metin Taşı";
        this.hologramLinesOverride = null;
        this.brokenHologramLinesOverride = null;
        this.leaderboardEnabled = true;
        this.hologramOffsetYOverride = null;
        this.stoneRewards = new ArrayList<>();
        this.breakCounts = new HashMap<>();
        this.lastBreakerId = null;
        this.reactivationIntervalMinutes = null;
        this.hologramDisplays = new ArrayList<>();
    }

    public MetinStone(UUID id, Location location, Material blockType, double maxHealth,
                      double currentHealth, boolean broken, long brokenTime, String displayName,
                      List<String> hologramLinesOverride, List<String> brokenHologramLinesOverride,
                      boolean leaderboardEnabled, Map<UUID, Integer> breakCounts) {
        this.id = id;
        this.location = location;
        this.blockType = blockType;
        this.maxHealth = maxHealth;
        this.currentHealth = currentHealth;
        this.currentBreaker = null;
        this.lastHitTime = 0;
        this.broken = broken;
        this.brokenTime = brokenTime;
        this.displayName = displayName != null ? displayName : "Metin Taşı";
        this.hologramLinesOverride = (hologramLinesOverride != null && !hologramLinesOverride.isEmpty())
                ? new ArrayList<>(hologramLinesOverride) : null;
        this.brokenHologramLinesOverride = (brokenHologramLinesOverride != null && !brokenHologramLinesOverride.isEmpty())
                ? new ArrayList<>(brokenHologramLinesOverride) : null;
        this.leaderboardEnabled = leaderboardEnabled;
        this.hologramOffsetYOverride = null;
        this.stoneRewards = new ArrayList<>();
        this.breakCounts = breakCounts != null ? new HashMap<>(breakCounts) : new HashMap<>();
        this.lastBreakerId = null;
        this.reactivationIntervalMinutes = null;
        this.hologramDisplays = new ArrayList<>();
    }

    public UUID getLastBreakerId() {
        return lastBreakerId;
    }

    public void setLastBreakerId(UUID lastBreakerId) {
        this.lastBreakerId = lastBreakerId;
    }

    public Integer getReactivationIntervalMinutes() {
        return reactivationIntervalMinutes;
    }

    public void setReactivationIntervalMinutes(Integer reactivationIntervalMinutes) {
        this.reactivationIntervalMinutes = reactivationIntervalMinutes;
    }

    public List<Reward> getStoneRewards() {
        return stoneRewards;
    }

    public void clearStoneRewards() {
        stoneRewards.clear();
    }

    public Double getHologramOffsetYOverride() {
        return hologramOffsetYOverride;
    }

    public void setHologramOffsetYOverride(Double hologramOffsetYOverride) {
        this.hologramOffsetYOverride = hologramOffsetYOverride;
    }

    public UUID getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public Material getBlockType() {
        return blockType;
    }

    public void setBlockType(Material blockType) {
        this.blockType = blockType;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public double getCurrentHealth() {
        return currentHealth;
    }

    public void setCurrentHealth(double currentHealth) {
        this.currentHealth = Math.max(0, currentHealth);
    }

    public UUID getCurrentBreaker() {
        return currentBreaker;
    }

    public void setCurrentBreaker(UUID currentBreaker) {
        this.currentBreaker = currentBreaker;
        this.lastHitTime = System.currentTimeMillis();
    }

    public long getLastHitTime() {
        return lastHitTime;
    }

    public void setLastHitTime(long lastHitTime) {
        this.lastHitTime = lastHitTime;
    }

    public boolean isBroken() {
        return broken;
    }

    public void setBroken(boolean broken) {
        this.broken = broken;
    }

    public long getBrokenTime() {
        return brokenTime;
    }

    public void setBrokenTime(long brokenTime) {
        this.brokenTime = brokenTime;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName.trim();
        }
    }

    public List<String> getHologramLinesOverride() {
        return hologramLinesOverride;
    }

    public void setHologramLinesOverride(List<String> hologramLinesOverride) {
        this.hologramLinesOverride = (hologramLinesOverride != null && !hologramLinesOverride.isEmpty())
                ? new ArrayList<>(hologramLinesOverride) : null;
    }

    public List<String> getBrokenHologramLinesOverride() {
        return brokenHologramLinesOverride;
    }

    public void setBrokenHologramLinesOverride(List<String> brokenHologramLinesOverride) {
        this.brokenHologramLinesOverride = (brokenHologramLinesOverride != null && !brokenHologramLinesOverride.isEmpty())
                ? new ArrayList<>(brokenHologramLinesOverride) : null;
    }

    public boolean isLeaderboardEnabled() {
        return leaderboardEnabled;
    }

    public void setLeaderboardEnabled(boolean leaderboardEnabled) {
        this.leaderboardEnabled = leaderboardEnabled;
    }

    public Map<UUID, Integer> getBreakCounts() {
        return breakCounts;
    }

    public void recordBreak(UUID playerId) {
        if (playerId == null) return;
        breakCounts.merge(playerId, 1, Integer::sum);
    }

    public List<Map.Entry<UUID, Integer>> getTopBreakers(int limit) {
        return breakCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(Math.max(0, limit))
                .toList();
    }

    public List<TextDisplay> getHologramDisplays() {
        return hologramDisplays;
    }

    public void clearHologramEntities() {
        for (Entity e : new ArrayList<>(hologramDisplays)) {
            if (e != null && !e.isDead()) {
                e.remove();
            }
        }
        hologramDisplays.clear();
    }

    public void damage(double amount) {
        this.currentHealth = Math.max(0, this.currentHealth - amount);
        if (this.currentHealth <= 0) {
            this.broken = true;
            this.brokenTime = System.currentTimeMillis();
        }
    }

    public void reactivate() {
        this.broken = false;
        this.brokenTime = 0;
        this.currentHealth = this.maxHealth;
        this.currentBreaker = null;
        this.lastHitTime = 0;
        this.lastBreakerId = null;
    }

    public boolean isBeingBroken() {
        return currentBreaker != null;
    }

    public void releaseBreaker() {
        this.currentBreaker = null;
        this.lastHitTime = 0;
    }

    public boolean hasBreakerTimedOut(long timeoutMs) {
        if (currentBreaker == null) return false;
        return System.currentTimeMillis() - lastHitTime > timeoutMs;
    }

    public String getLocationKey() {
        if (location.getWorld() == null) return "unknown:0:0:0";
        return location.getWorld().getName() + ":" +
                location.getBlockX() + ":" +
                location.getBlockY() + ":" +
                location.getBlockZ();
    }
}
