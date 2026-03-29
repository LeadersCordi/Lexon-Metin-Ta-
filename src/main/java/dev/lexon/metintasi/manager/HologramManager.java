package dev.lexon.metintasi.manager;

import dev.lexon.metintasi.MetinTasiPlugin;
import dev.lexon.metintasi.model.MetinStone;
import dev.lexon.metintasi.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HologramManager {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final MetinTasiPlugin plugin;

    public HologramManager(MetinTasiPlugin plugin) {
        this.plugin = plugin;
    }

    public void createHologram(MetinStone stone) {
        removeHologram(stone);

        Location baseLoc = stone.getLocation().clone().add(0.5, 0, 0.5);
        double offsetY = stone.getHologramOffsetYOverride() != null
                ? stone.getHologramOffsetYOverride()
                : plugin.getConfigManager().getHologramOffsetY();
        double spacing = plugin.getConfigManager().getHologramLineSpacing();
        List<String> lines = stone.getHologramLinesOverride();
        if (lines == null || lines.isEmpty()) {
            lines = plugin.getConfigManager().getHologramLines();
        }
        if (lines.isEmpty()) {
            lines = List.of(
                    "&6&l<isim>",
                    "&7Bu metinin &c<can_current>&7/&c<can_max>&7 canı var",
                    "&7Şu an kıran: &f<oyuncu>",
                    "&7Son kıran: &f<son_kiran>",
                    "&7Durum: &f<durum>",
                    "&7Metin no: &6#<tas_no>"
            );
        }

        List<String> resolved = new ArrayList<>();
        for (String line : lines) {
            resolved.add(replaceAllPlaceholders(line, stone, false));
        }

        spawnLines(baseLoc, offsetY, spacing, resolved, stone);
    }

    public void updateHologram(MetinStone stone) {
        removeHologram(stone);
        if (!stone.isBroken()) {
            createHologram(stone);
        } else {
            createBrokenHologram(stone);
        }
    }

    public void createBrokenHologram(MetinStone stone) {
        removeHologram(stone);

        Location baseLoc = stone.getLocation().clone().add(0.5, 0, 0.5);
        double offsetY = stone.getHologramOffsetYOverride() != null
                ? stone.getHologramOffsetYOverride()
                : plugin.getConfigManager().getHologramOffsetY();
        double spacing = plugin.getConfigManager().getHologramLineSpacing();

        List<String> resolved = buildResolvedBrokenLines(stone);
        spawnLines(baseLoc, offsetY, spacing, resolved, stone);
    }

    /** Kırık taş hologramı metnini yeniler (süre sayacı için periyodik). */
    public void tickBrokenHolograms() {
        for (MetinStone stone : plugin.getMetinStoneManager().getAllStones()) {
            if (!stone.isBroken()) continue;
            refreshBrokenHologramTexts(stone);
        }
    }

    private void refreshBrokenHologramTexts(MetinStone stone) {
        List<TextDisplay> displays = stone.getHologramDisplays();
        if (displays.isEmpty()) return;

        List<String> resolved = buildResolvedBrokenLines(stone);
        for (int i = 0; i < resolved.size() && i < displays.size(); i++) {
            TextDisplay td = displays.get(i);
            if (td != null && !td.isDead()) {
                td.text(ColorUtil.toComponentNoStyle(resolved.get(i)));
            }
        }
    }

    private List<String> buildResolvedBrokenLines(MetinStone stone) {
        List<String> brokenLines = stone.getBrokenHologramLinesOverride();
        if (brokenLines == null || brokenLines.isEmpty()) {
            brokenLines = plugin.getConfigManager().getBrokenHologramLines();
        }
        if (brokenLines.isEmpty()) {
            brokenLines = List.of(
                    "&c&lKırık",
                    "&7<isim>",
                    "&7Kıran: &f<son_kiran>",
                    "&7Kalan: &f<aktif_zaman>"
            );
        }
        List<String> resolved = new ArrayList<>();
        for (String line : brokenLines) {
            resolved.add(replaceAllPlaceholders(line, stone, true));
        }
        return resolved;
    }

    private void spawnLines(Location baseLoc, double offsetY, double spacing, List<String> lines, MetinStone stone) {
        World world = baseLoc.getWorld();
        if (world == null) return;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Location lineLoc = baseLoc.clone().add(0, offsetY + ((lines.size() - 1 - i) * spacing), 0);

            TextDisplay display = world.spawn(lineLoc, TextDisplay.class, textDisplay -> {
                textDisplay.text(ColorUtil.toComponentNoStyle(line));
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
            stone.getHologramDisplays().add(display);
        }
    }

    public void removeHologram(MetinStone stone) {
        stone.clearHologramEntities();
    }

    public void removeAll() {
        for (MetinStone stone : plugin.getMetinStoneManager().getAllStones()) {
            removeHologram(stone);
        }
    }

    private String replaceAllPlaceholders(String text, MetinStone stone, boolean brokenState) {
        text = text.replace("<isim>", stone.getDisplayName());
        text = text.replace("<can>", String.format("%.0f", stone.getCurrentHealth()) + "/" + String.format("%.0f", stone.getMaxHealth()));
        text = text.replace("<can_current>", String.format("%.0f", stone.getCurrentHealth()));
        text = text.replace("<can_max>", String.format("%.0f", stone.getMaxHealth()));
        text = text.replace("<durum>", brokenState ? "Kırık" : "Aktif");
        text = text.replace("<tas_no>", String.valueOf(plugin.getMetinStoneManager().getStableStoneNumber(stone)));

        text = text.replace("<oyuncu>", uuidToName(stone.getCurrentBreaker()));
        text = text.replace("<son_kiran>", uuidToName(stone.getLastBreakerId()));
        text = text.replace("<blok>", stone.getBlockType().name().toLowerCase());

        if (brokenState) {
            text = text.replace("<aktif_zaman>", getNextReactivationText(stone));
        }

        int max = plugin.getConfigManager().getLeaderboardMaxEntries();
        String empty = plugin.getConfigManager().getLeaderboardEmptySlot();
        for (int r = 1; r <= max; r++) {
            text = text.replace("<lb_" + r + ">", empty);
        }

        return text;
    }

    public String resolveFloatingLine(String text, MetinStone stone) {
        text = text.replace("<tas_ismi>", stone.getDisplayName());
        text = text.replace("<isim>", stone.getDisplayName());
        text = text.replace("<can>", String.format("%.0f", stone.getCurrentHealth()) + "/" + String.format("%.0f", stone.getMaxHealth()));
        text = text.replace("<can_current>", String.format("%.0f", stone.getCurrentHealth()));
        text = text.replace("<can_max>", String.format("%.0f", stone.getMaxHealth()));
        text = text.replace("<durum>", stone.isBroken() ? "Kırık" : "Aktif");
        text = text.replace("<tas_no>", String.valueOf(plugin.getMetinStoneManager().getStableStoneNumber(stone)));
        text = text.replace("<oyuncu>", uuidToName(stone.getCurrentBreaker()));
        text = text.replace("<son_kiran>", uuidToName(stone.getLastBreakerId()));

        if (stone.isLeaderboardEnabled()) {
            int max = plugin.getConfigManager().getLeaderboardMaxEntries();
            List<Map.Entry<UUID, Integer>> top = stone.getTopBreakers(max);
            String fmt = plugin.getConfigManager().getLeaderboardLineFormat();
            String empty = plugin.getConfigManager().getLeaderboardEmptySlot();

            for (int r = 1; r <= max; r++) {
                String token = "<lb_" + r + ">";
                if (r <= top.size()) {
                    Map.Entry<UUID, Integer> e = top.get(r - 1);
                    String name = uuidToName(e.getKey());
                    String line = fmt.replace("<sira>", String.valueOf(r))
                            .replace("<isim>", name)
                            .replace("<kirilis>", String.valueOf(e.getValue()));
                    text = text.replace(token, line);
                } else {
                    text = text.replace(token, empty);
                }
            }
        } else {
            int max = plugin.getConfigManager().getLeaderboardMaxEntries();
            String empty = plugin.getConfigManager().getLeaderboardEmptySlot();
            for (int r = 1; r <= max; r++) {
                text = text.replace("<lb_" + r + ">", empty);
            }
        }
        return text;
    }

    private static String uuidToName(UUID id) {
        if (id == null) return "—";
        OfflinePlayer off = Bukkit.getOfflinePlayer(id);
        String n = off.getName();
        return n != null ? n : "Bilinmeyen";
    }

    private String getNextReactivationText(MetinStone stone) {
        String mode = plugin.getConfigManager().getReactivationMode();

        if (mode.equalsIgnoreCase("interval")) {
            int minutesTotal = plugin.getConfigManager().getReactivationIntervalMinutesForStone(stone);
            long intervalMs = minutesTotal * 60L * 1000L;
            long elapsed = System.currentTimeMillis() - stone.getBrokenTime();
            long remaining = intervalMs - elapsed;

            if (remaining <= 0) return "Hazır";

            long totalSec = Math.max(0, remaining / 1000);
            long h = totalSec / 3600;
            long m = (totalSec % 3600) / 60;
            long s = totalSec % 60;

            if (h > 0) {
                return h + "s " + m + "dk " + s + "sn";
            }
            if (m > 0) {
                return m + "dk " + s + "sn";
            }
            return s + "sn";
        } else {
            List<String> times = plugin.getConfigManager().getReactivationTimes();
            if (times.isEmpty()) return "Belirsiz";

            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Istanbul"));
            String currentTime = now.format(TIME_FORMAT);

            for (String time : times) {
                String trimmed = time.trim();
                if (trimmed.compareTo(currentTime) > 0) {
                    return trimmed;
                }
            }
            return times.get(0).trim() + " (yarın)";
        }
    }
}
