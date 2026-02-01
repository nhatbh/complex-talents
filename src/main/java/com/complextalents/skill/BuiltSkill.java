package com.complextalents.skill;

import com.complextalents.passive.PassiveStackDef;
import com.complextalents.targeting.TargetType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
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
    private final double toggleMaxDuration;
    private final Consumer<Object> toggleOffHandler;
    private final boolean allowSelfTarget;
    private final boolean targetAllyOnly;
    private final boolean targetPlayerOnly;
    private final int maxLevel;
    private final ResourceLocation icon;
    private final java.util.Map<String, double[]> scaledStats;
    private final java.util.Map<String, PassiveStackDef> passiveStacks;

    // Scaling arrays for cooldown, cost, and toggle cost
    private final double[] scaledActiveCooldown;
    private final double[] scaledPassiveCooldown;
    private final double[] scaledResourceCost;
    private final double[] scaledToggleCost;

    private final BiConsumer<ExecutionContext, Object> activeHandler;
    private final Consumer<Object> passiveHandler;
    private final SkillBuilder.ChanneledHandler channeledHandler;
    private final BiFunction<ExecutionContext, Object, Boolean> validationHandler;

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
        this.toggleMaxDuration = builder.getToggleMaxDuration();
        this.toggleOffHandler = builder.getToggleOffHandler();
        this.allowSelfTarget = builder.isAllowSelfTarget();
        this.targetAllyOnly = builder.isTargetAllyOnly();
        this.targetPlayerOnly = builder.isTargetPlayerOnly();
        this.maxLevel = builder.getMaxLevel();
        this.icon = builder.getIcon();
        this.scaledStats = builder.getScaledStats();
        this.passiveStacks = builder.getPassiveStacks();
        this.scaledActiveCooldown = builder.getScaledActiveCooldown();
        this.scaledPassiveCooldown = builder.getScaledPassiveCooldown();
        this.scaledResourceCost = builder.getScaledResourceCost();
        this.scaledToggleCost = builder.getScaledToggleCost();
        this.minChannelTime = builder.getMinChannelTime();
        this.maxChannelTime = builder.getMaxChannelTime();
        this.activeHandler = builder.getActiveHandler();
        this.passiveHandler = builder.getPassiveHandler();
        this.channeledHandler = builder.getChanneledHandler();
        this.validationHandler = builder.getValidationHandler();
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
    public double getActiveCooldown(int level) {
        if (scaledActiveCooldown == null || scaledActiveCooldown.length == 0) {
            return activeCooldown;
        }
        int index = Math.min(Math.max(level - 1, 0), scaledActiveCooldown.length - 1);
        return scaledActiveCooldown[index];
    }

    @Override
    public double getPassiveCooldown() {
        return passiveCooldown;
    }

    @Override
    public double getPassiveCooldown(int level) {
        if (scaledPassiveCooldown == null || scaledPassiveCooldown.length == 0) {
            return passiveCooldown;
        }
        int index = Math.min(Math.max(level - 1, 0), scaledPassiveCooldown.length - 1);
        return scaledPassiveCooldown[index];
    }

    @Override
    public double getResourceCost() {
        return resourceCost;
    }

    @Override
    public double getResourceCost(int level) {
        if (scaledResourceCost == null || scaledResourceCost.length == 0) {
            return resourceCost;
        }
        int index = Math.min(Math.max(level - 1, 0), scaledResourceCost.length - 1);
        return scaledResourceCost[index];
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
    public double getToggleCostPerTick(int level) {
        if (scaledToggleCost == null || scaledToggleCost.length == 0) {
            return toggleCostPerTick;
        }
        int index = Math.min(Math.max(level - 1, 0), scaledToggleCost.length - 1);
        return scaledToggleCost[index];
    }

    @Override
    public double getToggleMaxDuration() {
        return toggleMaxDuration;
    }

    @Override
    public boolean hasToggleOffHandler() {
        return toggleOffHandler != null;
    }

    @Override
    public void executeToggleOff(Object player) {
        if (toggleOffHandler != null) {
            toggleOffHandler.accept(player);
        }
    }

    @Override
    @Nullable
    public ResourceLocation getIcon() {
        return icon;
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

    @Override
    public boolean canCast(ExecutionContext context) {
        if (validationHandler != null) {
            Boolean result = validationHandler.apply(context, context.player().get());
            return result != null && result;
        }
        return true;
    }

    /**
     * Check if this skill has a validation handler.
     */
    public boolean hasValidationHandler() {
        return validationHandler != null;
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

    /**
     * Get passive stack definitions for this skill.
     * Returns map of stack name -> definition.
     *
     * @return Map of passive stack definitions (empty by default)
     */
    public Map<String, PassiveStackDef> getPassiveStacks() {
        return passiveStacks;
    }

    /**
     * Get a specific passive stack definition.
     *
     * @param stackName The stack type name
     * @return The stack definition, or null if not found
     */
    @Nullable
    public PassiveStackDef getPassiveStackDef(String stackName) {
        return passiveStacks.get(stackName);
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
