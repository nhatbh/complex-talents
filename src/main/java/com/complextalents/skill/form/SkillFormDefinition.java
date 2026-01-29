package com.complextalents.skill.form;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Defines enhanced skills for a form skill.
 * <p>
 * When a form skill is activated, it can transform the skills in slots 0-2
 * into completely different registered skills.
 * <p>
 * Enhanced skills are full BuiltSkills with all capabilities (cooldowns, resource costs, stats, etc.).
 * <p>
 * This is designed for ultimate skills but is generic enough for any skill
 * to use this "stance/form switching" mechanic.
 */
public class SkillFormDefinition {

    private final ResourceLocation formSkillId;
    private final double duration;
    private final double cooldown;

    // Enhanced skill IDs for slots 0, 1, 2 (null = no enhancement for that slot)
    private final ResourceLocation enhancedSkillId0;
    private final ResourceLocation enhancedSkillId1;
    private final ResourceLocation enhancedSkillId2;

    // Optional: Icon overrides for future UI work
    private final ResourceLocation iconOverride0;
    private final ResourceLocation iconOverride1;
    private final ResourceLocation iconOverride2;

    private SkillFormDefinition(Builder builder) {
        this.formSkillId = builder.formSkillId;
        this.duration = builder.duration;
        this.cooldown = builder.cooldown;
        this.enhancedSkillId0 = builder.enhancedSkillId0;
        this.enhancedSkillId1 = builder.enhancedSkillId1;
        this.enhancedSkillId2 = builder.enhancedSkillId2;
        this.iconOverride0 = builder.iconOverride0;
        this.iconOverride1 = builder.iconOverride1;
        this.iconOverride2 = builder.iconOverride2;
    }

    /**
     * Get the form skill's ResourceLocation.
     */
    public ResourceLocation getFormSkillId() {
        return formSkillId;
    }

    /**
     * Get the duration this form lasts (in seconds).
     */
    public double getDuration() {
        return duration;
    }

    /**
     * Get the cooldown after the form ends (in seconds).
     */
    public double getCooldown() {
        return cooldown;
    }

    /**
     * Get the enhanced skill ID for a specific slot.
     *
     * @param slot The slot index (0-2)
     * @return The enhanced skill ID, or null if no enhancement for this slot
     */
    @Nullable
    public ResourceLocation getEnhancedSkillId(int slot) {
        return switch (slot) {
            case 0 -> enhancedSkillId0;
            case 1 -> enhancedSkillId1;
            case 2 -> enhancedSkillId2;
            default -> null;
        };
    }

    /**
     * Check if this form has an enhancement for a specific slot.
     *
     * @param slot The slot index (0-2)
     * @return true if an enhanced skill exists for this slot
     */
    public boolean hasEnhancementForSlot(int slot) {
        return getEnhancedSkillId(slot) != null;
    }

    /**
     * Get the icon override for a specific slot (for future HUD implementation).
     *
     * @param slot The slot index (0-2)
     * @return The icon override, or null if no override
     */
    @Nullable
    public ResourceLocation getIconOverride(int slot) {
        return switch (slot) {
            case 0 -> iconOverride0;
            case 1 -> iconOverride1;
            case 2 -> iconOverride2;
            default -> null;
        };
    }

    /**
     * Create a new builder for a skill form definition.
     *
     * @param formSkillId The form skill's ResourceLocation
     * @return A new builder
     */
    public static Builder builder(ResourceLocation formSkillId) {
        return new Builder(formSkillId);
    }

    /**
     * Builder pattern for skill form definitions.
     */
    public static class Builder {
        private final ResourceLocation formSkillId;
        private double duration = 15.0;  // Default 15 seconds
        private double cooldown = 60.0;  // Default 60 seconds

        private ResourceLocation enhancedSkillId0;
        private ResourceLocation enhancedSkillId1;
        private ResourceLocation enhancedSkillId2;

        private ResourceLocation iconOverride0;
        private ResourceLocation iconOverride1;
        private ResourceLocation iconOverride2;

        private Builder(ResourceLocation formSkillId) {
            this.formSkillId = formSkillId;
        }

        /**
         * Set the duration of the form (in seconds).
         */
        public Builder duration(double seconds) {
            this.duration = seconds;
            return this;
        }

        /**
         * Set the cooldown after the form ends (in seconds).
         */
        public Builder cooldown(double seconds) {
            this.cooldown = seconds;
            return this;
        }

        /**
         * Set the enhanced skill for slot 0.
         *
         * @param skillId The skill ID to replace slot 0 with when form is active
         */
        public Builder enhanceSlot0(ResourceLocation skillId) {
            this.enhancedSkillId0 = skillId;
            return this;
        }

        /**
         * Set the enhanced skill for slot 1.
         *
         * @param skillId The skill ID to replace slot 1 with when form is active
         */
        public Builder enhanceSlot1(ResourceLocation skillId) {
            this.enhancedSkillId1 = skillId;
            return this;
        }

        /**
         * Set the enhanced skill for slot 2.
         *
         * @param skillId The skill ID to replace slot 2 with when form is active
         */
        public Builder enhanceSlot2(ResourceLocation skillId) {
            this.enhancedSkillId2 = skillId;
            return this;
        }

        /**
         * Set the icon override for slot 0 (for future HUD implementation).
         */
        public Builder iconOverride0(ResourceLocation iconId) {
            this.iconOverride0 = iconId;
            return this;
        }

        /**
         * Set the icon override for slot 1 (for future HUD implementation).
         */
        public Builder iconOverride1(ResourceLocation iconId) {
            this.iconOverride1 = iconId;
            return this;
        }

        /**
         * Set the icon override for slot 2 (for future HUD implementation).
         */
        public Builder iconOverride2(ResourceLocation iconId) {
            this.iconOverride2 = iconId;
            return this;
        }

        /**
         * Build the skill form definition.
         */
        public SkillFormDefinition build() {
            return new SkillFormDefinition(this);
        }
    }
}
