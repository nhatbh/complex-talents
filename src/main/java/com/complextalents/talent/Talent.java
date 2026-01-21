package com.complextalents.talent;

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
    private final ChatFormatting rarityColor;

    public Talent(ResourceLocation id, Component name, Component description, int maxLevel, TalentType type) {
        this(id, name, description, maxLevel, type, ChatFormatting.WHITE);
    }

    public Talent(ResourceLocation id, Component name, Component description, int maxLevel, TalentType type, ChatFormatting rarityColor) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.maxLevel = maxLevel;
        this.type = type;
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
