package dev.lexon.metintasi;

import dev.lexon.metintasi.command.MetinTasiCommand;
import dev.lexon.metintasi.gui.GUIListener;
import dev.lexon.metintasi.gui.InputManager;
import dev.lexon.metintasi.integration.MetinTasiPlaceholders;
import dev.lexon.metintasi.integration.WebhookManager;
import dev.lexon.metintasi.listener.ChatListener;
import dev.lexon.metintasi.listener.PlayerInteractListener;
import dev.lexon.metintasi.listener.StoneBreakListener;
import dev.lexon.metintasi.listener.StoneProtectionListener;
import dev.lexon.metintasi.manager.ConfigManager;
import dev.lexon.metintasi.manager.FloatingHologramManager;
import dev.lexon.metintasi.manager.HologramManager;
import dev.lexon.metintasi.manager.MetinStoneManager;
import dev.lexon.metintasi.manager.RewardManager;
import dev.lexon.metintasi.manager.SpawnScheduler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class MetinTasiPlugin extends JavaPlugin {

    private static MetinTasiPlugin instance;

    private ConfigManager configManager;
    private HologramManager hologramManager;
    private FloatingHologramManager floatingHologramManager;
    private MetinStoneManager metinStoneManager;
    private RewardManager rewardManager;
    private SpawnScheduler spawnScheduler;
    private WebhookManager webhookManager;
    private Economy economy;

    @Override
    public void onEnable() {
        instance = this;

        configManager = new ConfigManager(this);
        configManager.loadAll();

        hologramManager = new HologramManager(this);
        metinStoneManager = new MetinStoneManager(this);
        rewardManager = new RewardManager(this);
        spawnScheduler = new SpawnScheduler(this);
        webhookManager = new WebhookManager(this);

        setupEconomy();

        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new StoneBreakListener(this), this);
        getServer().getPluginManager().registerEvents(new StoneProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new InputManager(), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MetinTasiPlaceholders(this).register();
            getLogger().info("PlaceholderAPI baglantisi kuruldu.");
        }

        MetinTasiCommand commandHandler = new MetinTasiCommand(this);
        getCommand("metintasi").setExecutor(commandHandler);
        getCommand("metintasi").setTabCompleter(commandHandler);

        metinStoneManager.loadStones();
        floatingHologramManager = new FloatingHologramManager(this);
        floatingHologramManager.init();
        metinStoneManager.startTasks();
        spawnScheduler.startScheduler();

        getLogger().info("MetinTasi plugin aktif edildi!");
    }

    @Override
    public void onDisable() {
        if (metinStoneManager != null) {
            metinStoneManager.stopTasks();
            metinStoneManager.saveAllStones();
        }
        if (hologramManager != null) {
            hologramManager.removeAll();
        }
        if (floatingHologramManager != null) {
            floatingHologramManager.shutdown();
        }
        if (spawnScheduler != null) {
            spawnScheduler.stopScheduler();
        }
        getLogger().info("MetinTasi plugin devre disi birakildi!");
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault bulunamadi! Para odulleri devre disi.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
            getLogger().info("Vault ekonomi sistemi baglandi.");
        }
    }

    public void reloadPlugin() {
        if (metinStoneManager != null) {
            metinStoneManager.stopTasks();
        }
        if (hologramManager != null) {
            hologramManager.removeAll();
        }
        if (floatingHologramManager != null) {
            floatingHologramManager.shutdown();
        }
        if (spawnScheduler != null) {
            spawnScheduler.stopScheduler();
        }

        configManager.loadAll();
        webhookManager = new WebhookManager(this);
        metinStoneManager.reloadStones();
        if (floatingHologramManager == null) {
            floatingHologramManager = new FloatingHologramManager(this);
        }
        floatingHologramManager.init();
        metinStoneManager.startTasks();
        rewardManager.reload();
        spawnScheduler.startScheduler();
    }

    public static MetinTasiPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public FloatingHologramManager getFloatingHologramManager() {
        return floatingHologramManager;
    }

    public MetinStoneManager getMetinStoneManager() {
        return metinStoneManager;
    }

    public RewardManager getRewardManager() {
        return rewardManager;
    }

    public SpawnScheduler getSpawnScheduler() {
        return spawnScheduler;
    }

    public WebhookManager getWebhookManager() {
        return webhookManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean hasEconomy() {
        return economy != null;
    }

    public void debug(String message) {
        if (configManager != null && configManager.isDebugMode()) {
            getLogger().info("[DEBUG] " + message);
        }
    }
}
