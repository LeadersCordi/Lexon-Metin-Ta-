package dev.lexon.metintasi.listener;

import dev.lexon.metintasi.MetinTasiPlugin;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.StructureGrowEvent;

/**
 * vxMetin tarzinda metin taslarinin patlama, piston, fizik vb. ile bozulmasini engeller.
 */
public class StoneProtectionListener implements Listener {

    private final MetinTasiPlugin plugin;

    public StoneProtectionListener(MetinTasiPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean isStone(Block b) {
        if (b == null || plugin.getConfigManager() == null) return false;
        if (!plugin.getConfigManager().isProtectionEnabled()) return false;
        return plugin.getMetinStoneManager().isMetinStoneBlock(b.getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        e.blockList().removeIf(this::isStone);
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(this::isStone);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent e) {
        if (isStone(e.getToBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) {
            if (isStone(b)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent e) {
        for (Block b : e.getBlocks()) {
            if (isStone(b)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent e) {
        if (isStone(e.getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (isStone(e.getBlockReplacedState().getBlock())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent e) {
        e.getBlocks().removeIf(b -> isStone(b.getBlock()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onLightningStrike(LightningStrikeEvent e) {
        Location loc = e.getLightning().getLocation();
        if (loc.getWorld() == null) return;
        if (isStone(loc.getBlock())) e.setCancelled(true);
    }
}
