package com.complextalents.skill;

import com.complextalents.targeting.TargetType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Concrete skill implementation built from SkillBuilder.
 * Stores all skill data and execution handlers.
 */
public class BuiltSkill implements Skill {

    private final ResourceLocation id;
    private final SkillNature nature;
    private final TargetType targetingType;
    private final double maxRange;
    private final double activeCooldown;
    private final double passiveCooldown;
    private final double resourceCost;
    private final ResourceLocation resourceType;
    private final boolean toggleable;
    private final double toggleCostPerTick;
    private final boolean allowSelfTarget;
    private final boolean targetAllyOnly;
    private final boolean targetPlayerOnly;
    private final int maxLevel;
    private final java.util.Map<String, double[]> scaledStats;

    private final BiConsumer<ExecutionContext, Object> activeHandler;
    private final Consumer<Object> passiveHandler;
    private final SkillBuilder.ChanneledHandler channeledHandler;

    // Channeling properties
    private final double minChannelTime;
    private final double maxChannelTime;

    /**
     * Create a BuiltSkill from a SkillBuilder.
     * Package-private constructor used by SkillBuilder.
     */
    BuiltSkill(SkillBuilder builder) {
        this.id = builder.getId();
        this.nature = builder.getNature();
        this.targetingType = builder.getTargetingType();
        this.maxRange = builder.getMaxRange();
        this.activeCooldown = builder.getActiveCooldown();
        this.passiveCooldown = builder.getPassiveCooldown();
        this.resourceCost = builder.getResourceCost();
        this.resourceType = builder.getResourceType();
        this.toggleable = builder.isToggleable();
        this.toggleCostPerTick = builder.getToggleCostPerTick();
        this.allowSelfTarget = builder.isAllowSelfTarget();
        this.targetAllyOnly = builder.isTargetAllyOnly();
        this.targetPlayerOnly = builder.isTargetPlayerOnly();
        this.maxLevel = builder.getMaxLevel();
        this.scaledStats = builder.getScaledStats();
        this.minChannelTime = builder.getMinChannelTime();
        this.maxChannelTime = builder.getMaxChannelTime();
        this.activeHandler = builder.getActiveHandler();
        this.passiveHandler = builder.getPassiveHandler();
        this.channeledHandler = builder.getChanneledHandler();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public SkillNature getNature() {
        return nature;
    }

    @Override
    public TargetType getTargetingType() {
        return targetingType;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("skill." + id.getNamespace() + "." + id.getPath());
    }

    @Override
    public Component getDescription() {
        return Component.translatable("skill." + id.getNamespace() + "." + id.getPath() + ".desc");
    }

    @Override
    public double getMaxRange() {
        return maxRange;
    }

    @Override
    public double getActiveCooldown() {
        return activeCooldown;
    }

    @Override
    public double getPassiveCooldown() {
        return passiveCooldown;
    }

    @Override
    public double getResourceCost() {
        return resourceCost;
    }

    @Override
    @Nullable
    public ResourceLocation getResourceType() {
        return resourceType;
    }

    @Override
    public boolean isToggleable() {
        return toggleable;
    }

    @Override
    public boolean allowsSelfTarget() {
        return allowSelfTarget;
    }

    @Override
    public boolean targetsAllyOnly() {
        return targetAllyOnly;
    }

    @Override
    public boolean targetsPlayerOnly() {
        return targetPlayerOnly;
    }

    @Override
    public double getToggleCostPerTick() {
        return toggleCostPerTick;
    }

    @Override
    public void executeActive(ExecutionContext context) {
        if (activeHandler != null) {
            activeHandler.accept(context, context.player().get());
        }
    }

    @Override
    public double getMinChannelTime() {
        return minChannelTime;
    }

    @Override
    public double getMaxChannelTime() {
        return maxChannelTime;
    }

    @Override
    public boolean isChanneling() {
        return maxChannelTime > 0;
    }

    @Override
    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Get a scaled stat value for a given skill level.
     * Clamps to the last value if level exceeds array length.
     *
     * @param statName The name of the stat
     * @param level The skill level (1-based)
     * @return The resolved stat value, or 0 if stat not found
     */
    public double getScaledStat(String statName, int level) {
        double[] values = scaledStats.get(statName);
        if (values == null || values.length == 0) {
            return 0.0;
        }

        // Clamp to valid range: level 1 uses index 0, level N uses index N-1
        // If level exceeds array length, use the last value
        int index = Math.min(Math.max(level - 1, 0), values.length - 1);
        return values[index];
    }

    @Override
    public void executeChanneled(ExecutionContext context, double channelTime) {
        if (channeledHandler != null) {
            channeledHandler.handle(context, context.player().get(), channelTime);
        }
    }

    @Override
    public boolean hasActiveHandler() {
        return activeHandler != null;
    }

    /**
     * Get the passive event handler for this skill.
     * Used when registering passive event listeners.
     */
    @Nullable
    public Consumer<Object> getPassiveHandler() {
        return passiveHandler;
    }

    /**
     * Check if this skill has a passive handler.
     */
    public boolean hasPassiveHandler() {
        return passiveHandler != null;
    }

    @Override
    public String toString() {
        return "BuiltSkill{" +
                "id=" + id +
                ", nature=" + nature +
                ", targetingType=" + targetingType +
                ", activeCooldown=" + activeCooldown +
                ", passiveCooldown=" + passiveCooldown +
                ", resourceCost=" + resourceCost +
                ", toggleable=" + toggleable +
                ", maxLevel=" + maxLevel +
                ", scaledStats=" + scaledStats.keySet() +
                '}';
    }
}
