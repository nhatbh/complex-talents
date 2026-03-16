package com.complextalents.leveling.fatigue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;

/**
 * Persists and manages XP Fatigue multipliers for chunks.
 */
public class ChunkFatigueData extends SavedData {

    private static final String DATA_NAME = "complex_talents_chunk_fatigue";
    
    // Recovery rate: 1% every 1200 ticks (1 minute)
    private static final double RECOVERY_RATE = 0.01;
    private static final int RECOVERY_INTERVAL = 1200;

    // Degradation: 2.5% per mob kill (0% after 40 mobs)
    private static final double DEGRADATION_PER_KILL = 0.025;

    // Map of ChunkPos (as Long) to current multiplier (0.0 to 1.0)
    private final Map<Long, Double> fatigueMap = new HashMap<>();
    private int lastTickTime = 0;

    public static ChunkFatigueData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            ChunkFatigueData::load,
            ChunkFatigueData::new,
            DATA_NAME
        );
    }

    private ChunkFatigueData() {}

    public ChunkFatigueData(CompoundTag nbt) {
        load(nbt);
    }

    public double getMultiplier(ChunkPos pos) {
        return fatigueMap.getOrDefault(pos.toLong(), 1.0);
    }

    public void applyDegradation(ChunkPos pos) {
        applyDegradation(pos, 30.0); // Assume standard kill (30 XP) by default
    }

    public void applyDegradation(ChunkPos pos, double xpAmount) {
        long key = pos.toLong();
        double current = fatigueMap.getOrDefault(key, 1.0);
        
        // Scale degradation: 30 XP (standard kill) = 2.5% degradation
        double weight = xpAmount / 30.0;
        double next = Math.max(0, current - (DEGRADATION_PER_KILL * weight));
        
        fatigueMap.put(key, next);
        setDirty();
    }

    public void tick(ServerLevel level) {
        int currentTime = (int) level.getGameTime();
        if (currentTime - lastTickTime < RECOVERY_INTERVAL) return;
        
        lastTickTime = currentTime;
        
        boolean changed = false;
        // Regenerate all chunks that aren't at 100%
        var iterator = fatigueMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            double val = entry.getValue();
            if (val < 1.0) {
                double next = Math.min(1.0, val + RECOVERY_RATE);
                if (next >= 0.999) { // Practically 100%
                    iterator.remove();
                } else {
                    entry.setValue(next);
                }
                changed = true;
            }
        }

        if (changed) setDirty();
    }

    public static ChunkFatigueData load(CompoundTag nbt) {
        ChunkFatigueData data = new ChunkFatigueData();
        CompoundTag fatigueTag = nbt.getCompound("fatigue");
        for (String key : fatigueTag.getAllKeys()) {
            data.fatigueMap.put(Long.parseLong(key), fatigueTag.getDouble(key));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {
        CompoundTag fatigueTag = new CompoundTag();
        for (Map.Entry<Long, Double> entry : fatigueMap.entrySet()) {
            fatigueTag.putDouble(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("fatigue", fatigueTag);
        return nbt;
    }
}
