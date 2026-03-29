package dev.lexon.metintasi.listener;

import dev.lexon.metintasi.MetinTasiPlugin;
import dev.lexon.metintasi.model.MetinStone;
import dev.lexon.metintasi.model.PendingStoneChat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {

    private final MetinTasiPlugin plugin;

    public ChatListener(MetinTasiPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getMetinStoneManager().isPlayerInChatMode(player.getUniqueId())) {
            return;
        }

        PendingStoneChat pending = plugin.getMetinStoneManager().getPendingChat(player.getUniqueId());
        if (pending == null) return;

        event.setCancelled(true);
        String raw = event.getMessage().trim();

        if (raw.equalsIgnoreCase("iptal") || raw.equalsIgnoreCase("cancel")) {
            plugin.getMetinStoneManager().removePlayerFromChatMode(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().getMessage("placement-cancelled"));
            return;
        }

        if (pending.getStep() == PendingStoneChat.Step.BLOCK_TYPE) {
            String input = raw.toUpperCase();
            Material material;
            try {
                material = Material.valueOf(input);
            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.getConfigManager().getMessage("invalid-block-type"));
                return;
            }

            if (!material.isBlock()) {
                player.sendMessage(plugin.getConfigManager().getMessage("not-a-block"));
                return;
            }

            pending.setMaterial(material);
            pending.setStep(PendingStoneChat.Step.DISPLAY_NAME);
            player.sendMessage(plugin.getConfigManager().getMessage("enter-stone-name"));
            return;
        }

        if (pending.getStep() == PendingStoneChat.Step.DISPLAY_NAME) {
            Location placement = pending.getPlacement();
            Material material = pending.getMaterial();
            plugin.getMetinStoneManager().removePlayerFromChatMode(player.getUniqueId());

            if (placement == null || material == null || placement.getWorld() == null) {
                player.sendMessage(plugin.getConfigManager().getMessage("placement-error"));
                return;
            }

            String displayName = raw.equals("-") ? plugin.getConfigManager().getDefaultStoneDisplayName() : raw;

            Bukkit.getScheduler().runTask(plugin, () -> {
                MetinStone stone = plugin.getMetinStoneManager().createStone(placement, material, displayName);

                player.sendMessage(plugin.getConfigManager().getMessage("stone-created"));

                String announcement = plugin.getConfigManager().getRawMessage("stone-spawned");
                announcement = announcement.replace("<x>", String.valueOf(placement.getBlockX()))
                        .replace("<y>", String.valueOf(placement.getBlockY()))
                        .replace("<z>", String.valueOf(placement.getBlockZ()))
                        .replace("<dunya>", placement.getWorld().getName());

                for (Player online : Bukkit.getOnlinePlayers()) {
                    online.sendMessage(announcement);
                }

                plugin.debug("Metin Tasi: " + stone.getLocationKey() + " | " + material.name() + " | " + displayName);
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getMetinStoneManager().removePlayerFromChatMode(event.getPlayer().getUniqueId());
    }
}
