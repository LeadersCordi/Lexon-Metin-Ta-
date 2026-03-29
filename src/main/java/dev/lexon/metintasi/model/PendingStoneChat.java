package dev.lexon.metintasi.model;

import org.bukkit.Location;
import org.bukkit.Material;

public class PendingStoneChat {

    public enum Step {
        BLOCK_TYPE,
        DISPLAY_NAME
    }

    private final Location placement;
    private Step step;
    private Material material;

    public PendingStoneChat(Location placement) {
        this.placement = placement;
        this.step = Step.BLOCK_TYPE;
    }

    public Location getPlacement() {
        return placement;
    }

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }
}
