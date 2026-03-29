package dev.lexon.metintasi.model;

import org.bukkit.inventory.ItemStack;

public class Reward {

    public enum RewardType {
        ITEM,
        MONEY
    }

    private final String id;
    private final RewardType type;
    private ItemStack item;
    private double moneyAmount;
    private double chance;

    public Reward(String id, ItemStack item, double chance) {
        this.id = id;
        this.type = RewardType.ITEM;
        this.item = item;
        this.moneyAmount = 0;
        this.chance = chance;
    }

    public Reward(String id, double moneyAmount, double chance) {
        this.id = id;
        this.type = RewardType.MONEY;
        this.item = null;
        this.moneyAmount = moneyAmount;
        this.chance = chance;
    }

    public String getId() {
        return id;
    }

    public RewardType getType() {
        return type;
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack item) {
        this.item = item;
    }

    public double getMoneyAmount() {
        return moneyAmount;
    }

    public void setMoneyAmount(double moneyAmount) {
        this.moneyAmount = moneyAmount;
    }

    public double getChance() {
        return chance;
    }

    public void setChance(double chance) {
        this.chance = Math.max(0, Math.min(100, chance));
    }
}
