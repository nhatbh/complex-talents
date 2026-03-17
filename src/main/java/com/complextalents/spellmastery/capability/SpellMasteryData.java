package com.complextalents.spellmastery.capability;

import com.complextalents.network.PacketHandler;
import com.complextalents.spellmastery.network.SpellMasterySyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of the Spell Mastery capability.
 */
public class SpellMasteryData implements ISpellMasteryData {

    private final Player player;
    private final Map<ResourceLocation, Integer> masteryLevels = new HashMap<>();
    private final Map<ResourceLocation, Integer> learnedSpells = new HashMap<>(); // SpellID -> Max Level Learned

    public SpellMasteryData(Player player) {
        this.player = player;
    }

    @Override
    public int getMasteryLevel(ResourceLocation schoolId) {
        return masteryLevels.getOrDefault(schoolId, 0);
    }

    @Override
    public void setMasteryLevel(ResourceLocation schoolId, int level) {
        masteryLevels.put(schoolId, level);
        if (!player.level().isClientSide) {
            sync();
        }
    }

    @Override
    public boolean isSpellLearned(ResourceLocation spellId, int level) {
        return learnedSpells.getOrDefault(spellId, 0) >= level;
    }

    @Override
    public void learnSpell(ResourceLocation spellId, int level) {
        int currentMax = learnedSpells.getOrDefault(spellId, 0);
        if (level > currentMax) {
            learnedSpells.put(spellId, level);
            if (!player.level().isClientSide) {
                sync();
            }
        }
    }

    @Override
    public void forgetSpell(ResourceLocation spellId) {
        learnedSpells.remove(spellId);
        if (!player.level().isClientSide) {
            sync();
        }
    }

    @Override
    public Set<ResourceLocation> getLearnedSpells() {
        return new HashSet<>(learnedSpells.keySet());
    }

    @Override
    public Map<ResourceLocation, Integer> getAllMasteryLevels() {
        return new HashMap<>(masteryLevels);
    }

    @Override
    public void sync() {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendTo(new SpellMasterySyncPacket(serializeNBT()), serverPlayer);
        }
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();
        
        CompoundTag masteryNbt = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : masteryLevels.entrySet()) {
            masteryNbt.putInt(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("MasteryLevels", masteryNbt);
        
        CompoundTag spellsNbt = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : learnedSpells.entrySet()) {
            spellsNbt.putInt(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("LearnedSpellsMap", spellsNbt);
        
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        masteryLevels.clear();
        CompoundTag masteryNbt = nbt.getCompound("MasteryLevels");
        for (String key : masteryNbt.getAllKeys()) {
            masteryLevels.put(ResourceLocation.parse(key), masteryNbt.getInt(key));
        }
        
        learnedSpells.clear();
        if (nbt.contains("LearnedSpellsMap")) {
            CompoundTag spellsNbt = nbt.getCompound("LearnedSpellsMap");
            for (String key : spellsNbt.getAllKeys()) {
                learnedSpells.put(ResourceLocation.parse(key), spellsNbt.getInt(key));
            }
        } else if (nbt.contains("LearnedSpells")) {
            // Migration from simple Set
            ListTag legacySpells = nbt.getList("LearnedSpells", Tag.TAG_STRING);
            for (int i = 0; i < legacySpells.size(); i++) {
                learnedSpells.put(ResourceLocation.parse(legacySpells.getString(i)), 1); // Assume min level 1
            }
        }
    }
}
