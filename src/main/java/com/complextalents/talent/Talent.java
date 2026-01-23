package com.complextalents.talent;

import com.complextalents.capability.TalentsCapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public abstract class Talent {
    private final ResourceLocation id;
    private final Component name;
    private final Component description;
    private final int maxLevel;
    private final TalentType type;
    private final TalentSlotType slotType;
    private final ChatFormatting rarityColor;
    private final ResourceLocation requiredDefinition;
    private final ResourceBarConfig resourceBarConfig;

    public Talent(ResourceLocation id, Component name, Component description, int maxLevel, TalentType type, TalentSlotType slotType) {
        this(id, name, description, maxLevel, type, slotType, null, null, ChatFormatting.WHITE);
    }

    public Talent(ResourceLocation id, Component name, Component description, int maxLevel, TalentType type, TalentSlotType slotType, ChatFormatting rarityColor) {
        this(id, name, description, maxLevel, type, slotType, null, null, rarityColor);
    }

    public Talent(ResourceLocation id, Component name, Component description, int maxLevel, TalentType type, TalentSlotType slotType, ResourceLocation requiredDefinition) {
        this(id, name, description, maxLevel, type, slotType, requiredDefinition, null, ChatFormatting.WHITE);
    }

    public Talent(ResourceLocation id, Component name, Component description, int maxLevel, TalentType type, TalentSlotType slotType, ResourceLocation requiredDefinition, ChatFormatting rarityColor) {
        this(id, name, description, maxLevel, type, slotType, requiredDefinition, null, rarityColor);
    }

    public Talent(ResourceLocation id, Component name, Component description, int maxLevel, TalentType type, TalentSlotType slotType, ResourceLocation requiredDefinition, ResourceBarConfig resourceBarConfig, ChatFormatting rarityColor) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.maxLevel = maxLevel;
        this.type = type;
        this.slotType = slotType;
        this.requiredDefinition = requiredDefinition;
        this.resourceBarConfig = resourceBarConfig;
        this.rarityColor = rarityColor;
    }

    /**
     * Legacy constructor for backwards compatibility - defaults to DEFINITION slot
     * @deprecated Use constructor with TalentSlotType parameter
     */
    @Deprecated
    public Talent(ResourceLocation id, Component name, Component description, int maxLevel, TalentType type) {
        this(id, name, description, maxLevel, type, TalentSlotType.DEFINITION, null, null, ChatFormatting.WHITE);
    }

    /**
     * Legacy constructor for backwards compatibility - defaults to DEFINITION slot
     * @deprecated Use constructor with TalentSlotType parameter
     */
    @Deprecated
    public Talent(ResourceLocation id, Component name, Component description, int maxLevel, TalentType type, ChatFormatting rarityColor) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.maxLevel = maxLevel;
        this.type = type;
        this.slotType = TalentSlotType.DEFINITION;
        this.requiredDefinition = null;
        this.resourceBarConfig = null;
        this.rarityColor = rarityColor;
    }

    public ResourceLocation getId() {
        return id;
    }

    public Component getName() {
        return name;
    }

    public Component getDescription() {
        return description;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public TalentType getType() {
        return type;
    }

    public ChatFormatting getRarityColor() {
        return rarityColor;
    }

    public TalentSlotType getSlotType() {
        return slotType;
    }

    /**
     * Get the required Definition talent ID, if any
     * @return The required Definition talent ID, or null if no requirement
     */
    public ResourceLocation getRequiredDefinition() {
        return requiredDefinition;
    }

    /**
     * Check if this talent has a Definition requirement
     * @return true if this talent requires a specific Definition talent
     */
    public boolean hasDefinitionRequirement() {
        return requiredDefinition != null;
    }

    /**
     * Get the resource bar configuration for this talent
     * Only Definition talents should have resource bars
     * @return The resource bar config, or null if this talent doesn't define a resource bar
     */
    public ResourceBarConfig getResourceBarConfig() {
        return resourceBarConfig;
    }

    /**
     * Check if this talent defines a resource bar
     * @return true if this talent has a resource bar configuration
     */
    public boolean hasResourceBar() {
        return resourceBarConfig != null;
    }

    // ===== Resource Access Helper Methods =====
    // These methods allow talents to easily access the player's resource

    /**
     * Get the player's current resource value
     * @param player The player
     * @return The current resource value
     */
    protected float getResource(ServerPlayer player) {
        return player.getCapability(TalentsCapabilities.PLAYER_TALENTS)
                .map(talents -> talents.getResource())
                .orElse(0.0f);
    }

    /**
     * Set the player's resource value
     * @param player The player
     * @param value The new resource value
     */
    protected void setResource(ServerPlayer player, float value) {
        player.getCapability(TalentsCapabilities.PLAYER_TALENTS)
                .ifPresent(talents -> talents.setResource(value));
    }

    /**
     * Add to the player's resource value
     * @param player The player
     * @param amount The amount to add (can be negative)
     * @return The actual amount added (may be less due to clamping)
     */
    protected float addResource(ServerPlayer player, float amount) {
        return player.getCapability(TalentsCapabilities.PLAYER_TALENTS)
                .map(talents -> talents.addResource(amount))
                .orElse(0.0f);
    }

    /**
     * Check if the player has enough resource
     * @param player The player
     * @param amount The amount to check
     * @return true if the player has enough resource
     */
    protected boolean hasResource(ServerPlayer player, float amount) {
        return player.getCapability(TalentsCapabilities.PLAYER_TALENTS)
                .map(talents -> talents.hasResource(amount))
                .orElse(false);
    }

    /**
     * Consume resource from the player (only if enough is available)
     * @param player The player
     * @param amount The amount to consume
     * @return true if the resource was consumed, false if not enough resource
     */
    protected boolean consumeResource(ServerPlayer player, float amount) {
        return player.getCapability(TalentsCapabilities.PLAYER_TALENTS)
                .map(talents -> talents.consumeResource(amount))
                .orElse(false);
    }

    /**
     * Called when a player unlocks this talent
     * @param player The player unlocking the talent
     * @param level The level being unlocked
     */
    public abstract void onUnlock(ServerPlayer player, int level);

    /**
     * Called when a talent is removed from a player
     * @param player The player losing the talent
     */
    public abstract void onRemove(ServerPlayer player);

    /**
     * Called every tick for the player
     * Override this for talents with passive tick effects
     * @param player The player
     * @param level The current level of the talent
     */
    public void onTick(ServerPlayer player, int level) {
    }

    /**
     * Called when an active or hybrid talent is activated
     * @param player The player activating the talent
     * @param level The current level of the talent
     */
    public void onActivate(ServerPlayer player, int level) {
    }
}
