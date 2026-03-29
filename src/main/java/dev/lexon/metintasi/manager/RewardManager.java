package dev.lexon.metintasi.manager;

import dev.lexon.metintasi.MetinTasiPlugin;
import dev.lexon.metintasi.model.MetinStone;
import dev.lexon.metintasi.model.Reward;
import dev.lexon.metintasi.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

@SuppressWarnings("unchecked")
public class RewardManager {

    private final MetinTasiPlugin plugin;
    private final List<Reward> rewards;
    private final Random random;

    public RewardManager(MetinTasiPlugin plugin) {
        this.plugin = plugin;
        this.rewards = new ArrayList<>();
        this.random = new Random();
        loadRewards();
    }

    public void loadRewards() {
        rewards.clear();
        FileConfiguration config = plugin.getConfigManager().getRewardsConfig();
        ConfigurationSection section = config.getConfigurationSection("rewards");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection rewardSec = section.getConfigurationSection(key);
            if (rewardSec == null) continue;

            String type = rewardSec.getString("type", "item");
            double chance = rewardSec.getDouble("chance", 50.0);

            if (type.equalsIgnoreCase("money")) {
                double amount = rewardSec.getDouble("amount", 100.0);
                rewards.add(new Reward(key, amount, chance));
            } else {
                ItemStack item = readItemStack(rewardSec);
                if (item == null) {
                    String materialStr = rewardSec.getString("material", "DIAMOND");
                    int amount = rewardSec.getInt("amount", 1);
                    Material mat;
                    try {
                        mat = Material.valueOf(materialStr.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Gecersiz odul materyali: " + materialStr);
                        continue;
                    }
                    item = new ItemStack(mat, amount);
                }
                rewards.add(new Reward(key, item, chance));
            }
        }

        plugin.debug(rewards.size() + " odul yuklendi.");
    }

    private ItemStack readItemStack(ConfigurationSection rewardSec) {
        Object raw = rewardSec.get("item-data");
        if (raw instanceof Map) {
            try {
                return ItemStack.deserialize(new LinkedHashMap<>((Map<String, Object>) raw));
            } catch (Exception e) {
                plugin.getLogger().warning("item-data okunamadi: " + e.getMessage());
            }
        }
        ItemStack legacy = rewardSec.getItemStack("itemstack");
        if (legacy != null) {
            return legacy;
        }
        return null;
    }

    public void reload() {
        loadRewards();
    }

    public void giveRewards(Player player) {
        giveRewards(player, null);
    }

    /**
     * Tas kirilinca: genel + tas ozel odulleri (ayar: rewards.merge-global-with-stone).
     */
    public void giveRewards(Player player, MetinStone stone) {
        boolean merge = plugin.getConfigManager().isMergeGlobalRewardsWithStone();
        boolean received = false;

        if (stone != null && (!merge || !stone.getStoneRewards().isEmpty())) {
            received |= rollRewardList(player, stone.getStoneRewards());
        }
        if (merge || stone == null || stone.getStoneRewards().isEmpty()) {
            received |= rollRewardList(player, rewards);
        }

        if (!received) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-reward"));
        }
    }

    private boolean rollRewardList(Player player, List<Reward> list) {
        if (list == null || list.isEmpty()) return false;
        boolean any = false;
        for (Reward reward : list) {
            double roll = random.nextDouble() * 100;
            if (roll <= reward.getChance()) {
                giveReward(player, reward);
                any = true;
            }
        }
        return any;
    }

    private void giveReward(Player player, Reward reward) {
        switch (reward.getType()) {
            case ITEM -> {
                if (reward.getItem() == null) return;
                ItemStack item = reward.getItem().clone();
                HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                if (!leftover.isEmpty()) {
                    for (ItemStack drop : leftover.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                }

                String itemName = getItemDisplayName(item);
                String msg = plugin.getConfigManager().getMessage("reward-item");
                msg = msg.replace("<esya>", itemName)
                        .replace("<miktar>", String.valueOf(item.getAmount()));
                player.sendMessage(msg);
            }
            case MONEY -> {
                if (plugin.hasEconomy()) {
                    plugin.getEconomy().depositPlayer(player, reward.getMoneyAmount());
                    String msg = plugin.getConfigManager().getMessage("reward-money");
                    msg = msg.replace("<miktar>", String.format("%.2f", reward.getMoneyAmount()));
                    player.sendMessage(msg);
                } else {
                    plugin.getLogger().warning("Vault yok, para odulu verilemedi.");
                }
            }
        }
    }

    private String getItemDisplayName(ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return ColorUtil.componentToPlain(meta.displayName());
            }
        }
        return item.getType().name().toLowerCase().replace("_", " ");
    }

    public List<Reward> getRewards() {
        return rewards;
    }

    public void addReward(Reward reward) {
        rewards.add(reward);
        saveRewards();
    }

    public void removeReward(String id) {
        rewards.removeIf(r -> r.getId().equals(id));
        saveRewards();
    }

    public void moveReward(String id, boolean up) {
        int i = -1;
        for (int j = 0; j < rewards.size(); j++) {
            if (rewards.get(j).getId().equals(id)) {
                i = j;
                break;
            }
        }
        if (i < 0) return;
        int swap = up ? i - 1 : i + 1;
        if (swap < 0 || swap >= rewards.size()) return;
        Reward a = rewards.get(i);
        rewards.set(i, rewards.get(swap));
        rewards.set(swap, a);
        saveRewards();
    }

    public void addItemRewardFromHand(Player player, double chance) {
        addItemRewardFromHand(player, chance, null);
    }

    public void addItemRewardFromHand(Player player, double chance, MetinStone stone) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            player.sendMessage(plugin.getConfigManager().getMessage("reward-additem-empty-hand"));
            return;
        }
        ItemStack copy = hand.clone();
        String id = "item_" + System.currentTimeMillis();
        Reward rw = new Reward(id, copy, chance);
        if (stone != null) {
            stone.getStoneRewards().add(rw);
            plugin.getMetinStoneManager().saveStone(stone);
        } else {
            rewards.add(rw);
            saveRewards();
        }
        player.getInventory().setItemInMainHand(null);
        if (stone != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("reward-added-item-stone")
                    .replace("<tas>", stone.getDisplayName()));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("reward-added"));
        }
    }

    public void addStoneMoneyReward(MetinStone stone, double amount, double chance) {
        String id = "money_" + System.currentTimeMillis();
        stone.getStoneRewards().add(new Reward(id, amount, chance));
        plugin.getMetinStoneManager().saveStone(stone);
    }

    public void addGlobalMoneyReward(double amount, double chance) {
        String id = "money_" + System.currentTimeMillis();
        rewards.add(new Reward(id, amount, chance));
        saveRewards();
    }

    public void addStoneItemReward(MetinStone stone, ItemStack item, double chance) {
        String id = "item_" + System.currentTimeMillis();
        stone.getStoneRewards().add(new Reward(id, item, chance));
        plugin.getMetinStoneManager().saveStone(stone);
    }

    public void moveStoneReward(MetinStone stone, String rewardId, boolean up) {
        List<Reward> list = stone.getStoneRewards();
        int i = -1;
        for (int j = 0; j < list.size(); j++) {
            if (list.get(j).getId().equals(rewardId)) {
                i = j;
                break;
            }
        }
        if (i < 0) return;
        int swap = up ? i - 1 : i + 1;
        if (swap < 0 || swap >= list.size()) return;
        Reward a = list.get(i);
        list.set(i, list.get(swap));
        list.set(swap, a);
        plugin.getMetinStoneManager().saveStone(stone);
    }

    public void loadStoneRewardsFromSection(ConfigurationSection section, List<Reward> into) {
        into.clear();
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection rewardSec = section.getConfigurationSection(key);
            if (rewardSec == null) continue;
            Reward r = readRewardFromSection(key, rewardSec);
            if (r != null) into.add(r);
        }
    }

    private Reward readRewardFromSection(String key, ConfigurationSection rewardSec) {
        String type = rewardSec.getString("type", "item");
        double chance = rewardSec.getDouble("chance", 50.0);
        if (type.equalsIgnoreCase("money")) {
            double amount = rewardSec.getDouble("amount", 100.0);
            return new Reward(key, amount, chance);
        }
        ItemStack item = readItemStack(rewardSec);
        if (item == null) {
            String materialStr = rewardSec.getString("material", "DIAMOND");
            int amount = rewardSec.getInt("amount", 1);
            Material mat;
            try {
                mat = Material.valueOf(materialStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
            item = new ItemStack(mat, amount);
        }
        return new Reward(key, item, chance);
    }

    public void writeStoneRewardsToConfig(org.bukkit.configuration.file.YamlConfiguration c, MetinStone stone) {
        c.set("stone-rewards", null);
        if (stone.getStoneRewards().isEmpty()) return;
        for (Reward reward : stone.getStoneRewards()) {
            writeRewardToPath(c, "stone-rewards." + reward.getId(), reward);
        }
    }

    private void writeRewardToPath(FileConfiguration config, String path, Reward reward) {
        config.set(path + ".chance", reward.getChance());
        if (reward.getType() == Reward.RewardType.MONEY) {
            config.set(path + ".type", "money");
            config.set(path + ".amount", reward.getMoneyAmount());
        } else {
            config.set(path + ".type", "item");
            config.set(path + ".itemstack", null);
            if (reward.getItem() != null) {
                try {
                    Map<String, Object> serialized = reward.getItem().serialize();
                    if (serialized != null) {
                        config.set(path + ".item-data", serialized);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Tas odulu kaydedilemedi " + reward.getId() + ": " + e.getMessage());
                }
            }
        }
    }

    public Reward getRewardById(String id) {
        return rewards.stream().filter(r -> r.getId().equals(id)).findFirst().orElse(null);
    }

    public void saveRewards() {
        FileConfiguration config = plugin.getConfigManager().getRewardsConfig();

        config.set("rewards", null);

        for (Reward reward : rewards) {
            writeRewardToPath(config, "rewards." + reward.getId(), reward);
        }

        plugin.getConfigManager().saveRewards();
    }
}
