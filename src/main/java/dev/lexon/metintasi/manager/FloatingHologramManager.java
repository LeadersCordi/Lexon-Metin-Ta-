package dev.lexon.metintasi.manager;

import dev.lexon.metintasi.MetinTasiPlugin;
import dev.lexon.metintasi.model.MetinStone;
import dev.lexon.metintasi.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Metin tasinin blogundan bagimsiz, DecentHolograms benzeri cok satirli TextDisplay.
 * Siralama (&lt;lb_n&gt;) burada cozulur; tas holograminda yoktur.
 */
public class FloatingHologramManager {

    /** Ayak hizasindaki siralama hologrami: tas basina tek kayit (here / reset). */
    public static String rankingHologramId(UUID stoneId) {
        return "siralama-" + stoneId;
    }

    private final MetinTasiPlugin plugin;
    private final Map<String, FloatingEntry> byId = new ConcurrentHashMap<>();
    private File dataFile;
    private int taskId = -1;

    private static final class FloatingEntry {
        String id;
        Location location;
        UUID bindStoneId;
        final List<TextDisplay> displays = new ArrayList<>();
        /** Son çözülen metin; aynıysa TextDisplay güncellenmez (TPS). */
        final List<String> lastResolved = new ArrayList<>();
    }

    public FloatingHologramManager(MetinTasiPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() {
        this.dataFile = new File(plugin.getDataFolder(), "floating-holograms.yml");
        load();
        startTicker();
    }

    public void shutdown() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        for (FloatingEntry e : byId.values()) {
            clearDisplays(e);
        }
        byId.clear();
    }

    public void reload() {
        shutdown();
        init();
    }

    private void startTicker() {
        int ticks = Math.max(20, plugin.getConfigManager().getFloatingHologramUpdateTicks());
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::refreshAllBound, ticks, ticks);
    }

    public void createOrMoveAtFeet(String holoId, Location playerFeet, MetinStone bindStone) {
        createOrMoveAtFeet(holoId, playerFeet, bindStone, true);
    }

    public void createOrMoveAtFeet(String holoId, Location playerFeet, MetinStone bindStone, boolean saveToDisk) {
        if (holoId == null || holoId.isBlank() || playerFeet.getWorld() == null || bindStone == null) return;

        World world = playerFeet.getWorld();
        FloatingEntry entry = byId.computeIfAbsent(holoId.trim(), k -> {
            FloatingEntry e = new FloatingEntry();
            e.id = k;
            return e;
        });

        clearDisplays(entry);
        entry.lastResolved.clear();
        entry.location = playerFeet.clone();
        entry.bindStoneId = bindStone.getId();

        List<String> lines = new ArrayList<>(plugin.getConfigManager().getFloatingHologramLines());
        if (lines.isEmpty()) {
            lines = List.of("&7<tas_ismi>", "&8Sıralama", "<lb_1>", "<lb_2>", "<lb_3>");
        }

        double spacing = plugin.getConfigManager().getFloatingHologramSpacing();
        double off = plugin.getConfigManager().getFloatingHologramOffsetY();
        int n = lines.size();
        double feetY = playerFeet.getY();
        double baseY = feetY - off - (n > 0 ? (n - 1) * spacing : 0);
        Location base = new Location(world, playerFeet.getX(), baseY, playerFeet.getZ());

        MetinStone live = plugin.getMetinStoneManager().getStoneById(bindStone.getId());
        if (live == null) live = bindStone;

        for (int i = 0; i < lines.size(); i++) {
            final String resolvedLine = plugin.getHologramManager().resolveFloatingLine(lines.get(i), live);

            Location lineLoc = base.clone().add(0, (n - 1 - i) * spacing, 0);

            TextDisplay display = world.spawn(lineLoc, TextDisplay.class, textDisplay -> {
                textDisplay.text(ColorUtil.toComponentNoStyle(resolvedLine));
                textDisplay.setBillboard(Display.Billboard.CENTER);
                textDisplay.setAlignment(TextDisplay.TextAlignment.CENTER);
                textDisplay.setShadowed(true);
                textDisplay.setSeeThrough(false);
                textDisplay.setBackgroundColor(org.bukkit.Color.fromARGB(200, 22, 22, 28));
                textDisplay.setInterpolationDuration(0);
                textDisplay.setViewRange(40.0f);
                textDisplay.setPersistent(false);
                textDisplay.setInvulnerable(true);
                textDisplay.setGravity(false);
            });
            entry.displays.add(display);
            entry.lastResolved.add(resolvedLine);
        }

        if (saveToDisk) {
            save();
        }
    }

    public boolean delete(String holoId) {
        FloatingEntry e = byId.remove(holoId);
        if (e == null) return false;
        clearDisplays(e);
        save();
        return true;
    }

    /** Tas silinince bu tasa bagli yuzen holograflari kaldir. */
    public void removeBoundToStone(UUID stoneId) {
        if (stoneId == null) return;
        Iterator<Map.Entry<String, FloatingEntry>> it = byId.entrySet().iterator();
        boolean changed = false;
        while (it.hasNext()) {
            Map.Entry<String, FloatingEntry> en = it.next();
            if (stoneId.equals(en.getValue().bindStoneId)) {
                clearDisplays(en.getValue());
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            save();
        }
    }

    public List<String> listIds() {
        return new ArrayList<>(byId.keySet());
    }

    public void refreshForStone(UUID stoneId) {
        for (FloatingEntry e : byId.values()) {
            if (stoneId.equals(e.bindStoneId)) {
                refreshEntry(e);
            }
        }
    }

    private void refreshAllBound() {
        for (FloatingEntry e : byId.values()) {
            refreshEntry(e);
        }
    }

    private void refreshEntry(FloatingEntry e) {
        if (e.location == null || e.bindStoneId == null) return;
        MetinStone stone = plugin.getMetinStoneManager().getStoneById(e.bindStoneId);
        if (stone == null) return;

        List<String> lines = new ArrayList<>(plugin.getConfigManager().getFloatingHologramLines());
        if (lines.isEmpty()) {
            lines = List.of("&7<tas_ismi>", "&8Sıralama", "<lb_1>", "<lb_2>", "<lb_3>");
        }

        World world = e.location.getWorld();
        if (world == null) return;

        double spacing = plugin.getConfigManager().getFloatingHologramSpacing();
        double off = plugin.getConfigManager().getFloatingHologramOffsetY();
        int n = lines.size();
        double baseY = e.location.getY() - off - (n > 0 ? (n - 1) * spacing : 0);
        Location base = new Location(world, e.location.getX(), baseY, e.location.getZ());

        while (e.lastResolved.size() < lines.size()) {
            e.lastResolved.add("");
        }
        while (e.lastResolved.size() > lines.size()) {
            e.lastResolved.remove(e.lastResolved.size() - 1);
        }

        for (int i = 0; i < lines.size(); i++) {
            if (i >= e.displays.size()) break;
            final String resolved = plugin.getHologramManager().resolveFloatingLine(lines.get(i), stone);

            if (resolved.equals(e.lastResolved.get(i))) {
                continue;
            }
            e.lastResolved.set(i, resolved);

            TextDisplay td = e.displays.get(i);
            if (!td.isDead()) {
                td.text(ColorUtil.toComponentNoStyle(resolved));
                Location lineLoc = base.clone().add(0, (n - 1 - i) * spacing, 0);
                if (td.getLocation().distanceSquared(lineLoc) > 1e-10) {
                    td.teleport(lineLoc);
                }
            }
        }
    }

    private static void clearDisplays(FloatingEntry e) {
        Iterator<TextDisplay> it = e.displays.iterator();
        while (it.hasNext()) {
            TextDisplay td = it.next();
            if (td != null && !td.isDead()) {
                td.remove();
            }
            it.remove();
        }
        e.lastResolved.clear();
    }

    private void load() {
        byId.clear();
        if (!dataFile.exists()) {
            return;
        }
        FileConfiguration c = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection root = c.getConfigurationSection("holograms");
        if (root == null) return;

        for (String id : root.getKeys(false)) {
            ConfigurationSection sec = root.getConfigurationSection(id);
            if (sec == null) continue;
            String wn = sec.getString("world");
            World w = wn != null ? Bukkit.getWorld(wn) : null;
            if (w == null) continue;

            double x = sec.getDouble("x");
            double y = sec.getDouble("y");
            double z = sec.getDouble("z");
            String bind = sec.getString("bind-stone");
            if (bind == null) continue;
            UUID stoneId;
            try {
                stoneId = UUID.fromString(bind);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            MetinStone stone = plugin.getMetinStoneManager().getStoneById(stoneId);
            if (stone == null) continue;

            Location loc = new Location(w, x, y, z);
            createOrMoveAtFeet(id, loc, stone, false);
        }
    }

    public void save() {
        YamlConfiguration c = new YamlConfiguration();
        ConfigurationSection root = c.createSection("holograms");

        for (Map.Entry<String, FloatingEntry> en : byId.entrySet()) {
            FloatingEntry e = en.getValue();
            if (e.location == null || e.bindStoneId == null) continue;
            String path = en.getKey();
            root.set(path + ".world", e.location.getWorld().getName());
            root.set(path + ".x", e.location.getX());
            root.set(path + ".y", e.location.getY());
            root.set(path + ".z", e.location.getZ());
            root.set(path + ".bind-stone", e.bindStoneId.toString());
        }

        try {
            c.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("floating-holograms.yml kaydedilemedi: " + ex.getMessage());
        }
    }
}
