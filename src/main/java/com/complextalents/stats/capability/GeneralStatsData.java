package com.complextalents.stats.capability;

import com.complextalents.stats.StatModifierApplier;
import com.complextalents.stats.StatType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Implementation of the General Stats capability.
 */
public class GeneralStatsData implements IGeneralStatsData {

    private final Player player;
    private final Map<StatType, Integer> ranks = new EnumMap<>(StatType.class);
    private int skillPoints = 0;

    public GeneralStatsData(Player player) {
        this.player = player;
    }

    @Override
    public int getStatRank(StatType type) {
        return ranks.getOrDefault(type, 0);
    }

    @Override
    public void setStatRank(StatType type, int rank) {
        ranks.put(type, rank);
        if (!player.level().isClientSide) {
            applyModifiers();
            sync();
        }
    }

    @Override
    public int getSkillPoints() {
        return skillPoints;
    }

    @Override
    public void setSkillPoints(int points) {
        this.skillPoints = points;
        if (!player.level().isClientSide) {
            sync();
        }
    }

    @Override
    public void addSkillPoints(int points) {
        setSkillPoints(this.skillPoints + points);
    }

    @Override
    public Map<StatType, Integer> getAllRanks() {
        return new EnumMap<>(ranks);
    }

    @Override
    public void sync() {
        if (player instanceof ServerPlayer serverPlayer) {
            // TODO: Implement network packet for syncing stats
            // For now, attributes are primary sync mechanism
        }
    }

    private void applyModifiers() {
        StatModifierApplier.applyAll(player, ranks);
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("SkillPoints", skillPoints);
        
        CompoundTag ranksNbt = new CompoundTag();
        for (Map.Entry<StatType, Integer> entry : ranks.entrySet()) {
            ranksNbt.putInt(entry.getKey().name(), entry.getValue());
        }
        nbt.put("Ranks", ranksNbt);
        
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        this.skillPoints = nbt.getInt("SkillPoints");
        
        this.ranks.clear();
        CompoundTag ranksNbt = nbt.getCompound("Ranks");
        for (StatType type : StatType.values()) {
            if (ranksNbt.contains(type.name())) {
                this.ranks.put(type, ranksNbt.getInt(type.name()));
            }
        }

        if (!player.level().isClientSide) {
            applyModifiers();
        }
    }
}
