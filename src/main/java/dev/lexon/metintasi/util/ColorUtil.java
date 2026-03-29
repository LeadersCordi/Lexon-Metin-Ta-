package dev.lexon.metintasi.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public final class ColorUtil {

    private ColorUtil() {
    }

    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static Component toComponent(String text) {
        if (text == null) return Component.empty();
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    public static Component toComponentNoStyle(String text) {
        if (text == null) return Component.empty();
        Component component = toComponent(text);
        return component.decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, false);
    }

    public static String componentToPlain(Component component) {
        if (component == null) return "";
        return LegacyComponentSerializer.legacySection().serialize(component)
                .replaceAll("\u00a7[0-9a-fk-or]", "");
    }
}
