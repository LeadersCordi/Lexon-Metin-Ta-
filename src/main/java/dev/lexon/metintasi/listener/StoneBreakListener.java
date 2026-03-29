package dev.lexon.metintasi.listener;

import dev.lexon.metintasi.MetinTasiPlugin;
import dev.lexon.metintasi.model.MetinStone;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;

public class StoneBreakListener implements Listener {

    private final MetinTasiPlugin plugin;

    public StoneBreakListener(MetinTasiPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event) {
        MetinStone stone = plugin.getMetinStoneManager().getStoneAt(event.getBlock().getLocation());
        if (stone == null) return;

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDamage(BlockDamageEvent event) {
        Player player = event.getPlayer();
        MetinStone stone = plugin.getMetinStoneManager().getStoneAt(event.getBlock().getLocation());
        if (stone == null) return;

        event.setCancelled(true);

        if (stone.isBroken()) {
            player.sendMessage(plugin.getConfigManager().getMessage("stone-already-broken"));
            return;
        }

        if (!player.hasPermission("metintasi.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        if (stone.isBeingBroken() && !stone.getCurrentBreaker().equals(player.getUniqueId())) {
            if (!stone.hasBreakerTimedOut(plugin.getConfigManager().getBreakerTimeoutSeconds() * 1000L)) {
                Player breaker = Bukkit.getPlayer(stone.getCurrentBreaker());
                String breakerName = breaker != null ? breaker.getName() : "Bilinmeyen";
                String msg = plugin.getConfigManager().getMessage("stone-being-broken");
                msg = msg.replace("<oyuncu>", breakerName);
                player.sendMessage(msg);
                return;
            }
            stone.releaseBreaker();
        }

        if (!stone.isBeingBroken()) {
            stone.setCurrentBreaker(player.getUniqueId());
            plugin.debug(player.getName() + " metin tasini kirmaya basladi.");
        }

        stone.setLastHitTime(System.currentTimeMillis());

        double damage = plugin.getConfigManager().getDamagePerHit();
        stone.damage(damage);

        plugin.debug(player.getName() + " hasar verdi: " + damage + " | Kalan can: " + stone.getCurrentHealth());

        if (stone.isBroken()) {
            onStoneBroken(player, stone);
        } else {
            plugin.getHologramManager().updateHologram(stone);
            if (plugin.getFloatingHologramManager() != null) {
                plugin.getFloatingHologramManager().refreshForStone(stone.getId());
            }
        }
    }

    private void onStoneBroken(Player player, MetinStone stone) {
        plugin.getMetinStoneManager().markBroken(stone, player.getUniqueId());

        plugin.getRewardManager().giveRewards(player, stone);

        String msg = plugin.getConfigManager().getMessage("stone-broken");
        msg = msg.replace("<oyuncu>", player.getName());
        player.sendMessage(msg);

        String announcement = plugin.getConfigManager().getRawMessage("stone-broken-broadcast");
        announcement = announcement.replace("<oyuncu>", player.getName());
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.equals(player)) {
                online.sendMessage(announcement);
            }
        }

        plugin.debug("Metin Tasi kirildi! Kiran: " + player.getName());
    }
}
