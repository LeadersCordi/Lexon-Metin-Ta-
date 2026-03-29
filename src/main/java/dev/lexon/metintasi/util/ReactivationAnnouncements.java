package dev.lexon.metintasi.util;

import dev.lexon.metintasi.MetinTasiPlugin;
import dev.lexon.metintasi.manager.ConfigManager;
import dev.lexon.metintasi.model.MetinStone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Taş yenilendiğinde sohbette turuncu kalın başlık + özelleştirilebilir metin.
 */
public final class ReactivationAnnouncements {

    private ReactivationAnnouncements() {
    }

    public static void broadcast(MetinTasiPlugin plugin, MetinStone stone) {
        if (stone == null) return;
        ConfigManager cm = plugin.getConfigManager();
        if (!cm.isReactivationBroadcastEnabled()) return;

        Location l = stone.getLocation();
        String isim = stone.getDisplayName();
        String dunya = l.getWorld() != null ? l.getWorld().getName() : "?";
        int x = l.getBlockX();
        int y = l.getBlockY();
        int z = l.getBlockZ();

        String bodyRaw = cm.getReactivationBroadcastMessage()
                .replace("<isim>", isim)
                .replace("<dunya>", dunya)
                .replace("<x>", String.valueOf(x))
                .replace("<y>", String.valueOf(y))
                .replace("<z>", String.valueOf(z))
                .replace("<konum>", x + ", " + y + ", " + z);

        int spaces = Math.max(0, cm.getReactivationBroadcastTitleSpaces());
        String titleText = cm.getReactivationBroadcastTitle();
        Component titleLine = Component.text(" ".repeat(spaces))
                .append(Component.text(titleText, Style.style(NamedTextColor.GOLD, TextDecoration.BOLD)));

        String coloredBody = ColorUtil.colorize(bodyRaw);
        Component bodyLine = LegacyComponentSerializer.legacySection().deserialize(coloredBody);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(titleLine);
            p.sendMessage(bodyLine);
        }
        Bukkit.getConsoleSender().sendMessage(titleLine);
        Bukkit.getConsoleSender().sendMessage(bodyLine);
    }
}
