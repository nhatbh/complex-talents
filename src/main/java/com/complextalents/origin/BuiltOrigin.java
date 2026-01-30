package com.complextalents.origin;

import com.complextalents.origin.client.OriginRenderer;
import com.complextalents.passive.PassiveStackDef;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Built-in implementation of the Origin interface.
 * Created by OriginBuilder and registered in OriginRegistry.
 */
public class BuiltOrigin implements Origin {

    private final ResourceLocation id;
    private final Component displayName;
    private final Component description;
    private final ResourceType resourceType;
    private final int maxLevel;
    private final Map<String, double[]> scaledStats;
    private final Map<String, PassiveStackDef> passiveStacks;
    private final OriginRenderer renderer;

    /**
     * Create a BuiltOrigin from an OriginBuilder.
     */
    protected BuiltOrigin(OriginBuilder builder) {
        this.id = builder.getId();
        this.displayName = builder.getDisplayName();
        this.description = builder.getDescription();
        this.resourceType = builder.getResourceType();
        this.maxLevel = builder.getMaxLevel();
        this.scaledStats = builder.getScaledStats();
        this.passiveStacks = builder.getPassiveStacks();
        this.renderer = builder.getRenderer();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public Component getDisplayName() {
        return displayName;
    }

    @Override
    public Component getDescription() {
        return description;
    }

    @Override
    public ResourceType getResourceType() {
        return resourceType;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    @Override
    public double getScaledStat(String statName, int level) {
        double[] values = scaledStats.get(statName);
        if (values == null || values.length == 0) {
            return 0.0;
        }
        // Clamp level to valid range
        int index = Math.min(Math.max(level - 1, 0), values.length - 1);
        return values[index];
    }

    @Override
    public Map<String, PassiveStackDef> getPassiveStacks() {
        return passiveStacks;
    }

    @Override
    @Nullable
    public PassiveStackDef getPassiveStackDef(String stackName) {
        return passiveStacks.get(stackName);
    }

    @Override
    @Nullable
    public OriginRenderer getRenderer() {
        return renderer;
    }
}
