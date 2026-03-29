package dev.lexon.metintasi.listener;

import dev.lexon.metintasi.MetinTasiPlugin;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final MetinTasiPlugin plugin;

    public PlayerInteractListener(MetinTasiPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand.getType() != Material.BEDROCK) return;

        if (!player.isSneaking()) return;

        if (!player.hasPermission("metintasi.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) return;

        Block targetBlock = clickedBlock.getRelative(BlockFace.UP);
        if (targetBlock.getType() != Material.AIR) {
            player.sendMessage(plugin.getConfigManager().getMessage("block-not-empty"));
            return;
        }

        event.setCancelled(true);

        plugin.getMetinStoneManager().addPlayerToChatMode(player.getUniqueId(), targetBlock.getLocation());
        player.sendMessage(plugin.getConfigManager().getMessage("enter-block-type"));

        plugin.debug(player.getName() + " metin tasi yerlesim moduna girdi.");
    }
}
