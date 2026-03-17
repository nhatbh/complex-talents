package com.complextalents.stats.capability;

import com.complextalents.stats.StatType;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Map;

/**
 * Interface for general stats data capability.
 */
public interface IGeneralStatsData extends INBTSerializable<CompoundTag> {
    
    int getStatRank(StatType type);
    
    void setStatRank(StatType type, int rank);
    
    int getSkillPoints();
    
    void setSkillPoints(int points);
    
    void addSkillPoints(int points);
    
    void sync();
    
    Map<StatType, Integer> getAllRanks();
}
