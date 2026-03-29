package dev.lexon.metintasi.manager;

import dev.lexon.metintasi.MetinTasiPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpawnScheduler {

    private static final ZoneId TURKEY_ZONE = ZoneId.of("Europe/Istanbul");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final MetinTasiPlugin plugin;
    private int taskId = -1;
    private final Set<String> triggeredToday;

    public SpawnScheduler(MetinTasiPlugin plugin) {
        this.plugin = plugin;
        this.triggeredToday = new HashSet<>();
    }

    public void startScheduler() {
        stopScheduler();
        triggeredToday.clear();

        if (!plugin.getConfigManager().isSpawnEnabled()) {
            plugin.debug("Otomatik spawn devre disi.");
            return;
        }

        List<String> times = plugin.getConfigManager().getSpawnTimes();
        if (times.isEmpty()) {
            plugin.debug("Spawn saati tanimlanmamis.");
            return;
        }

        taskId = new BukkitRunnable() {
            private String lastDate = "";

            @Override
            public void run() {
                ZonedDateTime now = ZonedDateTime.now(TURKEY_ZONE);
                String currentDate = now.toLocalDate().toString();
                String currentTime = now.format(TIME_FORMAT);

                if (!currentDate.equals(lastDate)) {
                    triggeredToday.clear();
                    lastDate = currentDate;
                }

                for (String time : times) {
                    String trimmed = time.trim();
                    if (currentTime.equals(trimmed) && !triggeredToday.contains(trimmed)) {
                        triggeredToday.add(trimmed);
                        plugin.debug("Zamanlanmis spawn tetiklendi: " + trimmed);
                        plugin.getMetinStoneManager().spawnAtConfigLocation();
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L * 30).getTaskId();

        plugin.debug("Spawn zamanlayici baslatildi. Saatler: " + times);
    }

    public void stopScheduler() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }
}
