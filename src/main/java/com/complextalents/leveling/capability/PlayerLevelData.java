package com.complextalents.leveling.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of IPlayerLevelData.
 * Tracks level, XP, skill points, and assist data for players.
 */
public class PlayerLevelData implements IPlayerLevelData {

    private final ServerPlayer player;
    
    private int level = 1;
    private double currentLevelXP = 0;
    private double totalXP = 0;
    private int skillPoints = 0;

    // Map of Entity UUID to last interaction timestamp
    private final Map<UUID, Long> assistData = new HashMap<>();

    public PlayerLevelData(ServerPlayer player) {
        this.player = player;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    @Override
    public double getCurrentLevelXP() {
        return currentLevelXP;
    }

    @Override
    public void setCurrentLevelXP(double xp) {
        this.currentLevelXP = Math.max(0, xp);
    }

    @Override
    public double getTotalXP() {
        return totalXP;
    }

    @Override
    public void setTotalXP(double xp) {
        this.totalXP = Math.max(0, xp);
    }

    @Override
    public int getSkillPoints() {
        return skillPoints;
    }

    @Override
    public void setSkillPoints(int points) {
        this.skillPoints = Math.max(0, points);
    }

    @Override
    public void addXP(double amount, boolean silent) {
        if (amount <= 0) return;

        this.currentLevelXP += amount;
        this.totalXP += amount;

        checkLevelUp(silent);
        sync();
    }

    private void checkLevelUp(boolean silent) {
        double xpForNext = getXPForNextLevel(this.level);
        while (this.currentLevelXP >= xpForNext) {
            this.currentLevelXP -= xpForNext;
            this.level++;
            this.skillPoints += 2; // Exactly 2 SP per level

            if (!silent) {
                // TODO: Send level-up notification/sound to player
                // player.displayClientMessage(Component.translatable("leveling.levelup", this.level).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
            }
            
            xpForNext = getXPForNextLevel(this.level);
        }
    }

    /**
     * XP progression formula: Base 100, increases with level.
     * For now using a standard quadratic curve.
     */
    private double getXPForNextLevel(int currentLevel) {
        return 100 + (Math.pow(currentLevel, 1.5) * 50);
    }

    @Override
    public void recordAssist(UUID entityId, long timestamp) {
        assistData.put(entityId, timestamp);
    }

    @Override
    public boolean hasAssist(UUID entityId, long currentTimestamp, long windowMillis) {
        Long lastTime = assistData.get(entityId);
        if (lastTime == null) return false;
        
        return (currentTimestamp - lastTime) <= windowMillis;
    }

    @Override
    public void clearAssist(UUID entityId) {
        assistData.remove(entityId);
    }

    @Override
    public void sync() {
        // TODO: Implement sync packet
    }

    @Override
    public void copyFrom(IPlayerLevelData other) {
        this.level = other.getLevel();
        this.totalXP = other.getTotalXP();
        this.skillPoints = other.getSkillPoints();
        // currentLevelXP is wiped on death by death penalty, but copyFrom is used for dimension changes too.
        // The PlayerLevelDataHandler will handle the death-specific reset.
        this.currentLevelXP = other.getCurrentLevelXP();
        sync();
    }

    @Override
    public void resetCurrentLevelXP() {
        this.currentLevelXP = 0;
        sync();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("level", level);
        tag.putDouble("currentLevelXP", currentLevelXP);
        tag.putDouble("totalXP", totalXP);
        tag.putInt("skillPoints", skillPoints);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        this.level = tag.getInt("level");
        this.currentLevelXP = tag.getDouble("currentLevelXP");
        this.totalXP = tag.getDouble("totalXP");
        this.skillPoints = tag.getInt("skillPoints");
    }
}
