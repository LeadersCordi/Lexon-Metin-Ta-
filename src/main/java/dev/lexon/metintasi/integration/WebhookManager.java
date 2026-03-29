package dev.lexon.metintasi.integration;

import dev.lexon.metintasi.MetinTasiPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * Discord webhook (vxMetin fikri) - harici kutuphane yok; basit JSON.
 */
public class WebhookManager {

    private final MetinTasiPlugin plugin;
    private final boolean enabled;
    private final String url;
    private final String username;
    private final boolean onSpawn;
    private final boolean onBreak;
    private final boolean onReactivate;

    public WebhookManager(MetinTasiPlugin plugin) {
        this.plugin = plugin;
        FileConfiguration c = plugin.getConfigManager().getSettingsConfig();
        this.enabled = c.getBoolean("webhook.enabled", false);
        this.url = c.getString("webhook.url", "");
        this.username = c.getString("webhook.username", "MetinTasi");
        this.onSpawn = c.getBoolean("webhook.events.spawn", true);
        this.onBreak = c.getBoolean("webhook.events.break", true);
        this.onReactivate = c.getBoolean("webhook.events.reactivate", false);
    }

    public boolean isActive() {
        if (!enabled || url == null || url.isBlank()) return false;
        String u = url.trim();
        return u.startsWith("https://discord.com/api/webhooks/")
                || u.startsWith("https://discordapp.com/api/webhooks/");
    }

    public void stoneSpawned(String stoneName, String world, int x, int y, int z) {
        if (!isActive() || !onSpawn) return;
        FileConfiguration c = plugin.getConfigManager().getSettingsConfig();
        String title = c.getString("webhook.titles.spawn", "Metin Tasi olusturuldu");
        String template = c.getString("webhook.descriptions.spawn",
                "**Tas:** {stone}\n**Dunya:** {world}\n**Konum:** {x}, {y}, {z}");
        String desc = applyPlaceholders(template, stoneName, null, world, x, y, z);
        postEmbed(title, desc, embedColor("webhook.colors.spawn", 3066993));
    }

    public void stoneBroken(String stoneName, String player, String world, int x, int y, int z) {
        if (!isActive() || !onBreak) return;
        FileConfiguration c = plugin.getConfigManager().getSettingsConfig();
        String title = c.getString("webhook.titles.break", "Metin Tasi kirildi");
        String template = c.getString("webhook.descriptions.break",
                "**Tas:** {stone}\n**Oyuncu:** {player}\n**Dunya:** {world}\n**Konum:** {x}, {y}, {z}");
        String desc = applyPlaceholders(template, stoneName, player, world, x, y, z);
        postEmbed(title, desc, embedColor("webhook.colors.break", 15158332));
    }

    public void stoneReactivated(String stoneName, String world, int x, int y, int z) {
        if (!isActive() || !onReactivate) return;
        FileConfiguration c = plugin.getConfigManager().getSettingsConfig();
        String title = c.getString("webhook.titles.reactivate", "Metin Tasi aktif");
        String template = c.getString("webhook.descriptions.reactivate",
                "**Tas:** {stone}\n**Dunya:** {world}\n**Konum:** {x}, {y}, {z}");
        String desc = applyPlaceholders(template, stoneName, null, world, x, y, z);
        postEmbed(title, desc, embedColor("webhook.colors.reactivate", 3447003));
    }

    private static String applyPlaceholders(String template, String stone, String player, String world, int x, int y, int z) {
        if (template == null) template = "";
        String p = player != null ? player : "";
        return template
                .replace("{stone}", stone != null ? stone : "")
                .replace("{player}", p)
                .replace("{world}", world != null ? world : "")
                .replace("{x}", String.valueOf(x))
                .replace("{y}", String.valueOf(y))
                .replace("{z}", String.valueOf(z));
    }

    private int embedColor(String path, int def) {
        FileConfiguration c = plugin.getConfigManager().getSettingsConfig();
        if (c.isInt(path)) return c.getInt(path);
        String s = c.getString(path, "");
        if (s == null || s.isEmpty()) return def;
        s = s.trim();
        try {
            if (s.startsWith("#")) return Integer.parseInt(s.substring(1), 16);
            if (s.startsWith("0x") || s.startsWith("0X")) return Integer.parseInt(s.substring(2), 16);
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private void postEmbed(String title, String description, int color) {
        CompletableFuture.runAsync(() -> {
            try {
                String body = "{\"username\":\"" + esc(username) + "\",\"embeds\":[{\"title\":\"" + esc(title)
                        + "\",\"description\":\"" + esc(description) + "\",\"color\":" + color + "}]}";
                HttpURLConnection con = (HttpURLConnection) URI.create(url.trim()).toURL().openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);
                con.setConnectTimeout(8000);
                con.setReadTimeout(8000);
                try (OutputStream os = con.getOutputStream()) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
                con.getResponseCode();
                con.disconnect();
            } catch (Exception e) {
                plugin.getLogger().warning("Webhook gonderilemedi: " + e.getMessage());
            }
        });
    }
}
