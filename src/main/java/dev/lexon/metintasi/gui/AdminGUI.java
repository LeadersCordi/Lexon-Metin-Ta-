package dev.lexon.metintasi.gui;

import dev.lexon.metintasi.MetinTasiPlugin;
import dev.lexon.metintasi.model.MetinStone;
import dev.lexon.metintasi.model.Reward;
import dev.lexon.metintasi.util.ColorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AdminGUI {

    public enum StoneListMode {
        BROWSE, REACTIVATE, DELETE, REWARD_PICK
    }

    /** Para ekle / listeler için: genel mi taş mı */
    public enum ParaMoneyFlow {
        ADD,
        LIST
    }

    public static final String BRAND = "[MT] ";
    public static final String MAIN_TITLE = BRAND + "Metin Taşı";
    public static final String PARA_HEDEF_TITLE = BRAND + "Para: hedef seç";
    public static final String MONEY_REWARDS_TITLE = BRAND + "Genel para ödülleri";
    public static final String STONE_PARA_REWARDS_TITLE = BRAND + "Taş para ödülleri";
    public static final String STONE_BROWSE_TITLE = BRAND + "Taşlar";
    public static final String STONE_REWARD_PICK_TITLE = BRAND + "Taş seç";
    public static final String STONE_REACTIVATE_TITLE = BRAND + "Kırık taşlar";
    public static final String STONE_DELETE_TITLE = BRAND + "Taş sil";
    public static final String CONFIRM_DELETE_TITLE = BRAND + "Silinsin mi?";

    public static final int CONFIRM_YES_SLOT = 15;
    public static final int CONFIRM_NO_SLOT = 11;

    private static final Material FILL = Material.LIGHT_GRAY_STAINED_GLASS_PANE;

    private final MetinTasiPlugin plugin;

    public AdminGUI(MetinTasiPlugin plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        int size = 27;
        Inventory inv = Bukkit.createInventory(null, size, Component.text(MAIN_TITLE)
                .decoration(TextDecoration.ITALIC, false));

        ItemStack bg = createItem(FILL, " ", null);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, bg.clone());
        }

        inv.setItem(10, createItem(Material.RECOVERY_COMPASS, "&fTaşlara git", List.of("&7Sol tık: ışınlan")));
        inv.setItem(12, createItem(Material.EMERALD, "&aPara ödülü ekle", List.of("&7Önce hedef: genel veya taş")));
        inv.setItem(14, createItem(Material.GOLD_INGOT, "&6Para ödülleri", List.of("&7Genel veya taşa özel listeler")));
        inv.setItem(16, createItem(Material.BEACON, "&eKırık taşlar", List.of("&7Yeniden aç")));
        inv.setItem(22, createItem(Material.BARRIER, "&cTaşı sil", List.of("&7Listeden seç")));

        player.openInventory(inv);
    }

    public void openParaMoneyTargetMenu(Player player, ParaMoneyFlow flow) {
        int size = 27;
        Inventory inv = Bukkit.createInventory(null, size, Component.text(PARA_HEDEF_TITLE)
                .decoration(TextDecoration.ITALIC, false));

        ItemStack bg = createItem(FILL, " ", null);
        for (int i = 0; i < size; i++) {
            inv.setItem(i, bg.clone());
        }

        String addOrList = flow == ParaMoneyFlow.ADD ? "ekle" : "listele";
        inv.setItem(11, createItem(Material.GOLD_BLOCK, "&6Tüm taşlar", List.of(
                "&7rewards.yml (ortak havuz)",
                "&e" + (flow == ParaMoneyFlow.ADD ? "Sohbet: miktar şans" : "Ödül listesi")
        )));
        inv.setItem(15, createItem(Material.NETHER_STAR, "&dBelirli bir taş", List.of(
                "&7stones/&8<id>.yml &7ödülleri",
                "&e" + addOrList + " &7için taşı seç"
        )));
        inv.setItem(22, createItem(Material.ARROW, "&7Geri", List.of("&7Ana menü")));

        player.openInventory(inv);
    }

    public void openStoneListMenu(Player player, StoneListMode mode) {
        List<MetinStone> list = new ArrayList<>();
        for (MetinStone s : plugin.getMetinStoneManager().getAllStones()) {
            if (mode == StoneListMode.REACTIVATE && !s.isBroken()) continue;
            list.add(s);
        }
        list.sort(Comparator.comparing(s -> s.getDisplayName().toLowerCase()));

        String titleStr = switch (mode) {
            case BROWSE -> STONE_BROWSE_TITLE;
            case REACTIVATE -> STONE_REACTIVATE_TITLE;
            case DELETE -> STONE_DELETE_TITLE;
            case REWARD_PICK -> STONE_REWARD_PICK_TITLE;
        };

        if (list.isEmpty()) {
            player.sendMessage(plugin.getConfigManager().getMessage(
                    mode == StoneListMode.REACTIVATE ? "gui-no-broken" : "gui-empty-stones"));
            openMainMenu(player);
            return;
        }

        int size = Math.min(54, Math.max(36, (int) Math.ceil((list.size() + 9) / 9.0) * 9));
        Inventory inv = Bukkit.createInventory(null, size, Component.text(titleStr)
                .decoration(TextDecoration.ITALIC, false));

        int slot = 0;
        for (MetinStone stone : list) {
            if (slot >= size - 9) break;

            Material icon = stone.getBlockType();
            if (stone.isBroken()) {
                icon = Material.BEDROCK;
            }

            String w = stoWorld(stone);
            int x = stone.getLocation().getBlockX();
            int y = stone.getLocation().getBlockY();
            int z = stone.getLocation().getBlockZ();

            List<String> lore = new ArrayList<>();
            lore.add("&8*" + w + " &7/ &f" + x + " " + y + " " + z);
            lore.add(stone.isBroken() ? "&cKırık" : "&aAktif");
            int nPara = (int) stone.getStoneRewards().stream()
                    .filter(r -> r.getType() == Reward.RewardType.MONEY).count();
            lore.add("&7Para ödülü: &f" + nPara);
            lore.add("&0id:" + stone.getId());

            switch (mode) {
                case BROWSE -> lore.add("&7Sol tık: ışınlan");
                case REACTIVATE -> lore.add("&7Sol tık: yeniden aç");
                case DELETE -> lore.add("&7Sol tık: sil");
                case REWARD_PICK -> lore.add("&7Sol tık: seç");
            }

            inv.setItem(slot, createItem(icon, "&f&l" + stone.getDisplayName(), lore));
            slot++;
        }

        inv.setItem(size - 5, createItem(Material.ARROW, "&7Geri", List.of("&7Önceki adım")));
        fillBorderExceptBottomRow(inv, size);
        player.openInventory(inv);
    }

    private static String stoWorld(MetinStone stone) {
        return stone.getLocation().getWorld() != null ? stone.getLocation().getWorld().getName() : "?";
    }

    public void openConfirmDeleteMenu(Player player, MetinStone stone) {
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(CONFIRM_DELETE_TITLE)
                .decoration(TextDecoration.ITALIC, false));

        inv.setItem(13, createItem(Material.PAPER, "&c" + stone.getDisplayName(), List.of(
                "&7Bu taş kalıcı silinsin mi?",
                "&0id:" + stone.getId()
        )));

        inv.setItem(CONFIRM_NO_SLOT, createItem(Material.RED_WOOL, "&cİptal", List.of("&7Geri")));
        inv.setItem(CONFIRM_YES_SLOT, createItem(Material.LIME_WOOL, "&aSil", List.of("&cGeri alınamaz")));

        fillBorder(inv, 27);
        player.openInventory(inv);
    }

    public void openMoneyRewardsMenu(Player player) {
        List<Reward> moneyOnly = plugin.getRewardManager().getRewards().stream()
                .filter(r -> r.getType() == Reward.RewardType.MONEY)
                .collect(Collectors.toList());
        openMoneyRewardsMenu(player, moneyOnly, false, null);
    }

    public void openStoneMoneyRewardsMenu(Player player, MetinStone stone) {
        List<Reward> moneyOnly = stone.getStoneRewards().stream()
                .filter(r -> r.getType() == Reward.RewardType.MONEY)
                .collect(Collectors.toList());
        openMoneyRewardsMenu(player, moneyOnly, true, stone);
    }

    private void openMoneyRewardsMenu(Player player, List<Reward> moneyRewards, boolean stoneMode, MetinStone stone) {
        int size = Math.max(36, (int) Math.ceil((moneyRewards.size() + 18) / 9.0) * 9);
        size = Math.min(54, size);

        String title = stoneMode ? STONE_PARA_REWARDS_TITLE : MONEY_REWARDS_TITLE;
        Inventory inv = Bukkit.createInventory(null, size, Component.text(title)
                .decoration(TextDecoration.ITALIC, false));

        int slot = 0;
        int maxSlot = size - 9;
        for (Reward reward : moneyRewards) {
            if (slot >= maxSlot) break;

            ItemStack display = createItem(Material.GOLD_INGOT,
                    "&6" + String.format("%.2f", reward.getMoneyAmount()) + " para",
                    List.of(
                            "&7ID: " + reward.getId(),
                            "&7Şans: %" + String.format("%.1f", reward.getChance()),
                            "",
                            "&cSol tık: sil",
                            "&eSağ tık: şans"
                    ));
            inv.setItem(slot, display);
            slot++;
        }

        inv.setItem(size - 5, createItem(Material.ARROW, "&7Geri",
                List.of(stoneMode ? "&7Taş listesi" : "&7Hedef seç")));
        inv.setItem(size - 7, createItem(Material.EMERALD, "&aPara ekle",
                List.of(stoneMode ? "&7Bu taşa" : "&7Genel havuza")));

        if (stoneMode && stone != null) {
            inv.setItem(size - 3, createItem(Material.PAPER, "&7" + stone.getDisplayName(),
                    List.of("&8Taş ödülleri")));
        }

        fillBorderExceptBottomRow(inv, size);
        player.openInventory(inv);
    }

    public ItemStack createItem(Material material, String name, List<String> loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ColorUtil.toComponentNoStyle(name));

            if (loreLines != null && !loreLines.isEmpty()) {
                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                for (String line : loreLines) {
                    lore.add(ColorUtil.toComponentNoStyle(line));
                }
                meta.lore(lore);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBorder(Inventory inv, int size) {
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < size; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }

    private void fillBorderExceptBottomRow(Inventory inv, int size) {
        int bottomStart = size - 9;
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < bottomStart; i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, filler);
            }
        }
    }

    public static boolean isAdminGUI(String title) {
        return title != null && title.contains(BRAND.trim());
    }

    public static java.util.UUID parseHiddenStoneId(ItemStack clicked) {
        if (clicked == null || !clicked.hasItemMeta()) return null;
        List<net.kyori.adventure.text.Component> lore = clicked.getItemMeta().lore();
        if (lore == null) return null;
        for (var line : lore) {
            String plain = ColorUtil.componentToPlain(line).trim();
            if (plain.startsWith("id:")) {
                try {
                    return java.util.UUID.fromString(plain.substring(3).trim());
                } catch (IllegalArgumentException ignored) {
                    return null;
                }
            }
        }
        return null;
    }
}
