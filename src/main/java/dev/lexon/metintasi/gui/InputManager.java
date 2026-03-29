package dev.lexon.metintasi.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class InputManager implements Listener {

    private static final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();

    public static void addPendingInput(UUID playerId, Consumer<String> callback) {
        pendingInputs.put(playerId, callback);
    }

    public static boolean hasPendingInput(UUID playerId) {
        return pendingInputs.containsKey(playerId);
    }

    public static void removePendingInput(UUID playerId) {
        pendingInputs.remove(playerId);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        Consumer<String> callback = pendingInputs.remove(playerId);
        if (callback != null) {
            event.setCancelled(true);
            org.bukkit.Bukkit.getScheduler().runTask(
                    dev.lexon.metintasi.MetinTasiPlugin.getInstance(),
                    () -> callback.accept(event.getMessage().trim())
            );
        }
    }
}
