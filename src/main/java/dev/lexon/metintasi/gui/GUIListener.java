package dev.lexon.metintasi.gui;

import dev.lexon.metintasi.MetinTasiPlugin;
import dev.lexon.metintasi.model.MetinStone;
import dev.lexon.metintasi.model.Reward;
import dev.lexon.metintasi.util.ColorUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class GUIListener implements Listener {

    private static final Pattern UID_LINE = Pattern.compile(".*UID:\\s*([a-fA-F0-9\\-]{36}).*");

    private final MetinTasiPlugin plugin;
    private final AdminGUI adminGUI;
    private final Map<UUID, UUID> pendingDeleteStone = new ConcurrentHashMap<>();
    /** Para ekle / listele akışı: genel mi taş mı seçilecek */
    private final Map<UUID, AdminGUI.ParaMoneyFlow> pendingParaFlow = new ConcurrentHashMap<>();
    /** null değilse ödül bu taşa yazılır */
    private final Map<UUID, UUID> rewardContextStone = new ConcurrentHashMap<>();

    public GUIListener(MetinTasiPlugin plugin) {
        this.plugin = plugin;
        this.adminGUI = new AdminGUI(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = ColorUtil.componentToPlain(event.getView().title());

        if (!AdminGUI.isAdminGUI(title)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

        if (title.contains(AdminGUI.CONFIRM_DELETE_TITLE)) {
            handleConfirmDelete(player, event.getRawSlot());
            return;
        }

        if (title.contains(AdminGUI.PARA_HEDEF_TITLE)) {
            handleParaHedefMenu(player, event.getSlot());
            return;
        }

        if (title.contains(AdminGUI.STONE_BROWSE_TITLE)) {
            handleStoneList(player, event, AdminGUI.StoneListMode.BROWSE, clicked);
            return;
        }
        if (title.contains(AdminGUI.STONE_REACTIVATE_TITLE)) {
            handleStoneList(player, event, AdminGUI.StoneListMode.REACTIVATE, clicked);
            return;
        }
        if (title.contains(AdminGUI.STONE_DELETE_TITLE)) {
            handleStoneList(player, event, AdminGUI.StoneListMode.DELETE, clicked);
            return;
        }
        if (title.contains(AdminGUI.STONE_REWARD_PICK_TITLE)) {
            handleStoneList(player, event, AdminGUI.StoneListMode.REWARD_PICK, clicked);
            return;
        }

        if (title.contains(AdminGUI.MAIN_TITLE)) {
            handleMainMenu(player, event.getSlot());
        } else if (title.contains(AdminGUI.MONEY_REWARDS_TITLE)) {
            handleMoneyRewardsMenu(player, event, clicked, false);
        } else if (title.contains(AdminGUI.STONE_PARA_REWARDS_TITLE)) {
            handleMoneyRewardsMenu(player, event, clicked, true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = ColorUtil.componentToPlain(event.getView().title());

        if (title.contains(AdminGUI.CONFIRM_DELETE_TITLE)) {
            pendingDeleteStone.remove(player.getUniqueId());
        }
    }

    private void handleMainMenu(Player player, int slot) {
        switch (slot) {
            case 10 -> adminGUI.openStoneListMenu(player, AdminGUI.StoneListMode.BROWSE);
            case 12 -> {
                pendingParaFlow.put(player.getUniqueId(), AdminGUI.ParaMoneyFlow.ADD);
                adminGUI.openParaMoneyTargetMenu(player, AdminGUI.ParaMoneyFlow.ADD);
            }
            case 14 -> {
                pendingParaFlow.put(player.getUniqueId(), AdminGUI.ParaMoneyFlow.LIST);
                adminGUI.openParaMoneyTargetMenu(player, AdminGUI.ParaMoneyFlow.LIST);
            }
            case 16 -> adminGUI.openStoneListMenu(player, AdminGUI.StoneListMode.REACTIVATE);
            case 22 -> adminGUI.openStoneListMenu(player, AdminGUI.StoneListMode.DELETE);
        }
    }

    private void handleParaHedefMenu(Player player, int slot) {
        AdminGUI.ParaMoneyFlow flow = pendingParaFlow.get(player.getUniqueId());
        if (flow == null) {
            adminGUI.openMainMenu(player);
            return;
        }

        if (slot == 22) {
            pendingParaFlow.remove(player.getUniqueId());
            rewardContextStone.remove(player.getUniqueId());
            adminGUI.openMainMenu(player);
            return;
        }

        if (slot == 11) {
            if (flow == AdminGUI.ParaMoneyFlow.ADD) {
                rewardContextStone.remove(player.getUniqueId());
                pendingParaFlow.remove(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getMessage("enter-money-reward"));
                startMoneyRewardInput(player);
            } else {
                rewardContextStone.remove(player.getUniqueId());
                adminGUI.openMoneyRewardsMenu(player);
            }
            return;
        }

        if (slot == 15) {
            adminGUI.openStoneListMenu(player, AdminGUI.StoneListMode.REWARD_PICK);
        }
    }

    private void handleStoneList(Player player, InventoryClickEvent event, AdminGUI.StoneListMode mode, ItemStack clicked) {
        int topSize = event.getView().getTopInventory().getSize();
        int raw = event.getRawSlot();

        if (raw == topSize - 5) {
            ItemMeta meta = clicked.getItemMeta();
            if (meta != null && meta.displayName() != null) {
                String dn = ColorUtil.componentToPlain(meta.displayName());
                if (dn.contains("Geri")) {
                    if (mode == AdminGUI.StoneListMode.REWARD_PICK) {
                        AdminGUI.ParaMoneyFlow flow = pendingParaFlow.get(player.getUniqueId());
                        if (flow != null) {
                            adminGUI.openParaMoneyTargetMenu(player, flow);
                        } else {
                            adminGUI.openMainMenu(player);
                        }
                    } else {
                        adminGUI.openMainMenu(player);
                    }
                }
            }
            return;
        }

        if (raw >= topSize - 9) return;

        UUID stoneId = parseStoneUid(clicked);
        if (stoneId == null) stoneId = AdminGUI.parseHiddenStoneId(clicked);
        if (stoneId == null) return;

        MetinStone stone = plugin.getMetinStoneManager().getStoneById(stoneId);
        if (stone == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("gui-not-found"));
            adminGUI.openMainMenu(player);
            return;
        }

        switch (mode) {
            case BROWSE -> {
                Location dest = stone.getLocation().clone().add(0.5, 1.0, 0.5);
                if (dest.getWorld() != null) {
                    player.closeInventory();
                    player.teleport(dest);
                    player.sendMessage(plugin.getConfigManager().getMessage("gui-teleported"));
                }
            }
            case REACTIVATE -> {
                if (!stone.isBroken()) {
                    adminGUI.openStoneListMenu(player, AdminGUI.StoneListMode.REACTIVATE);
                    return;
                }
                plugin.getMetinStoneManager().reactivateStone(stone);
                player.sendMessage(plugin.getConfigManager().getMessage("gui-reactivated"));
                adminGUI.openStoneListMenu(player, AdminGUI.StoneListMode.REACTIVATE);
            }
            case DELETE -> {
                pendingDeleteStone.put(player.getUniqueId(), stoneId);
                adminGUI.openConfirmDeleteMenu(player, stone);
            }
            case REWARD_PICK -> {
                AdminGUI.ParaMoneyFlow flow = pendingParaFlow.get(player.getUniqueId());
                if (flow == AdminGUI.ParaMoneyFlow.ADD) {
                    rewardContextStone.put(player.getUniqueId(), stoneId);
                    pendingParaFlow.remove(player.getUniqueId());
                    player.closeInventory();
                    player.sendMessage(plugin.getConfigManager().getMessage("enter-money-reward-stone"));
                    startMoneyRewardInput(player);
                } else if (flow == AdminGUI.ParaMoneyFlow.LIST) {
                    rewardContextStone.put(player.getUniqueId(), stoneId);
                    adminGUI.openStoneMoneyRewardsMenu(player, stone);
                } else {
                    adminGUI.openMainMenu(player);
                }
            }
        }
    }

    private void handleConfirmDelete(Player player, int rawSlot) {
        UUID stoneId = pendingDeleteStone.get(player.getUniqueId());
        if (stoneId == null) {
            pendingDeleteStone.remove(player.getUniqueId());
            adminGUI.openMainMenu(player);
            return;
        }

        if (rawSlot == AdminGUI.CONFIRM_NO_SLOT) {
            pendingDeleteStone.remove(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().getMessage("gui-delete-cancelled"));
            adminGUI.openMainMenu(player);
            return;
        }

        if (rawSlot == AdminGUI.CONFIRM_YES_SLOT) {
            MetinStone stone = plugin.getMetinStoneManager().getStoneById(stoneId);
            pendingDeleteStone.remove(player.getUniqueId());
            if (stone != null) {
                plugin.getMetinStoneManager().deleteStone(stone);
                player.sendMessage(plugin.getConfigManager().getMessage("gui-deleted"));
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("gui-not-found"));
            }
            adminGUI.openMainMenu(player);
        }
    }

    private UUID parseStoneUid(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;
        List<Component> lore = meta.lore();
        if (lore == null) return null;
        for (Component line : lore) {
            String plain = ColorUtil.componentToPlain(line);
            var m = UID_LINE.matcher(plain);
            if (m.matches()) {
                try {
                    return UUID.fromString(m.group(1));
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
            if (plain.startsWith("id:")) {
                try {
                    return UUID.fromString(plain.substring(3).trim());
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private void handleMoneyRewardsMenu(Player player, InventoryClickEvent event, ItemStack clicked, boolean stoneMode) {
        int topSize = event.getView().getTopInventory().getSize();
        int raw = event.getRawSlot();

        if (raw == topSize - 5) {
            rewardContextStone.remove(player.getUniqueId());
            if (stoneMode) {
                AdminGUI.ParaMoneyFlow flow = pendingParaFlow.get(player.getUniqueId());
                if (flow == AdminGUI.ParaMoneyFlow.LIST) {
                    adminGUI.openStoneListMenu(player, AdminGUI.StoneListMode.REWARD_PICK);
                } else {
                    adminGUI.openMainMenu(player);
                }
            } else {
                AdminGUI.ParaMoneyFlow flow = pendingParaFlow.get(player.getUniqueId());
                if (flow == AdminGUI.ParaMoneyFlow.LIST) {
                    adminGUI.openParaMoneyTargetMenu(player, AdminGUI.ParaMoneyFlow.LIST);
                } else {
                    adminGUI.openMainMenu(player);
                }
            }
            return;
        }

        if (raw == topSize - 7) {
            if (stoneMode) {
                MetinStone st = getRewardContextStone(player);
                if (st == null) {
                    adminGUI.openMainMenu(player);
                    return;
                }
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getMessage("enter-money-reward-stone"));
                startMoneyRewardInput(player);
            } else {
                rewardContextStone.remove(player.getUniqueId());
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getMessage("enter-money-reward"));
                startMoneyRewardInput(player);
            }
            return;
        }

        if (raw == topSize - 3 && stoneMode) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null || meta.displayName() == null) return;

        String displayName = ColorUtil.componentToPlain(meta.displayName());
        if (displayName.contains("Geri")) {
            return;
        }

        List<Component> lore = meta.lore();
        if (lore == null || lore.isEmpty()) return;

        String rewardId = null;
        for (Component loreLine : lore) {
            String line = ColorUtil.componentToPlain(loreLine);
            if (line.startsWith("ID: ")) {
                rewardId = line.substring(4);
                break;
            }
        }

        if (rewardId == null) return;
        final String rid = rewardId;

        if (stoneMode) {
            MetinStone st = getRewardContextStone(player);
            if (st == null) return;
            Reward rw = findMoneyRewardInStone(st, rid);
            if (rw == null) return;

            if (event.isLeftClick()) {
                st.getStoneRewards().removeIf(r -> r.getId().equals(rid));
                plugin.getMetinStoneManager().saveStone(st);
                player.sendMessage(plugin.getConfigManager().getMessage("reward-removed"));
                adminGUI.openStoneMoneyRewardsMenu(player, st);
            } else if (event.isRightClick()) {
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getMessage("enter-chance"));
                startChanceInputStone(player, st, rid);
            }
        } else {
            Reward rw = plugin.getRewardManager().getRewardById(rid);
            if (rw == null || rw.getType() != Reward.RewardType.MONEY) return;

            if (event.isLeftClick()) {
                plugin.getRewardManager().removeReward(rid);
                player.sendMessage(plugin.getConfigManager().getMessage("reward-removed"));
                adminGUI.openMoneyRewardsMenu(player);
            } else if (event.isRightClick()) {
                player.closeInventory();
                player.sendMessage(plugin.getConfigManager().getMessage("enter-chance"));
                startChanceInputGlobal(player, rid);
            }
        }
    }

    private static Reward findMoneyRewardInStone(MetinStone stone, String id) {
        return stone.getStoneRewards().stream()
                .filter(r -> r.getId().equals(id) && r.getType() == Reward.RewardType.MONEY)
                .findFirst().orElse(null);
    }

    private MetinStone getRewardContextStone(Player player) {
        UUID sid = rewardContextStone.get(player.getUniqueId());
        return sid == null ? null : plugin.getMetinStoneManager().getStoneById(sid);
    }

    private void startMoneyRewardInput(Player player) {
        final UUID stoneIdSnap = rewardContextStone.get(player.getUniqueId());
        InputManager.addPendingInput(player.getUniqueId(), input -> {
            try {
                String[] parts = input.split(" ");
                if (parts.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getMessage("invalid-format-money"));
                    return;
                }
                double amount = Double.parseDouble(parts[0]);
                double chance = Double.parseDouble(parts[1]);

                String id = "money_" + System.currentTimeMillis();
                Reward reward = new Reward(id, amount, chance);

                if (stoneIdSnap != null) {
                    MetinStone live = plugin.getMetinStoneManager().getStoneById(stoneIdSnap);
                    if (live != null) {
                        live.getStoneRewards().add(reward);
                        plugin.getMetinStoneManager().saveStone(live);
                        player.sendMessage(plugin.getConfigManager().getMessage("reward-added-stone")
                                .replace("<tas>", live.getDisplayName()));
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("gui-not-found"));
                    }
                    rewardContextStone.remove(player.getUniqueId());
                } else {
                    plugin.getRewardManager().addReward(reward);
                    player.sendMessage(plugin.getConfigManager().getMessage("reward-added"));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
            }
        });
    }

    private void startChanceInputGlobal(Player player, String rewardId) {
        InputManager.addPendingInput(player.getUniqueId(), input -> {
            try {
                double chance = Double.parseDouble(input);
                Reward reward = plugin.getRewardManager().getRewardById(rewardId);
                if (reward != null && reward.getType() == Reward.RewardType.MONEY) {
                    reward.setChance(chance);
                    plugin.getRewardManager().saveRewards();
                    player.sendMessage(plugin.getConfigManager().getMessage("chance-updated"));
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("reward-not-found"));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
            }
        });
    }

    private void startChanceInputStone(Player player, MetinStone stone, String rewardId) {
        final UUID stoneId = stone.getId();
        InputManager.addPendingInput(player.getUniqueId(), input -> {
            try {
                double chance = Double.parseDouble(input);
                MetinStone live = plugin.getMetinStoneManager().getStoneById(stoneId);
                if (live == null) {
                    player.sendMessage(plugin.getConfigManager().getMessage("gui-not-found"));
                    return;
                }
                Reward reward = findMoneyRewardInStone(live, rewardId);
                if (reward != null) {
                    reward.setChance(chance);
                    plugin.getMetinStoneManager().saveStone(live);
                    player.sendMessage(plugin.getConfigManager().getMessage("chance-updated"));
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("reward-not-found"));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getConfigManager().getMessage("invalid-number"));
            }
        });
    }

    public AdminGUI getAdminGUI() {
        return adminGUI;
    }
}
