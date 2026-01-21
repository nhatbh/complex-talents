package com.complextalents.capability;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.SyncTalentsPacket;
import com.complextalents.talent.Talent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerTalentsImpl implements PlayerTalents {
    private final Map<ResourceLocation, Integer> talents = new HashMap<>();
    private final Map<ResourceLocation, Boolean> activeTalents = new HashMap<>();
    private final Map<ResourceLocation, Integer> cooldowns = new HashMap<>();
    private boolean dirty = false;
    private ServerPlayer owningPlayer = null;

    @Override
    public boolean hasTalent(ResourceLocation talentId) {
        return talents.containsKey(talentId);
    }

    @Override
    public int getTalentLevel(ResourceLocation talentId) {
        return talents.getOrDefault(talentId, 0);
    }

    @Override
    public void unlockTalent(ResourceLocation talentId, int level) {
        TalentsMod.LOGGER.debug("Unlocking talent {} at level {} for player {}",
            talentId, level, owningPlayer != null ? owningPlayer.getName().getString() : "unknown");
        talents.put(talentId, level);
        TalentsMod.LOGGER.debug("Talent map now contains {} talents: {}", talents.size(), talents.keySet());
        markDirty();
    }

    @Override
    public void upgradeTalent(ResourceLocation talentId) {
        int currentLevel = getTalentLevel(talentId);
        if (currentLevel > 0) {
            setTalentLevel(talentId, currentLevel + 1);
        }
    }

    @Override
    public void setTalentLevel(ResourceLocation talentId, int level) {
        if (level <= 0) {
            talents.remove(talentId);
        } else {
            talents.put(talentId, level);
        }
        markDirty();
    }

    @Override
    public void removeTalent(ResourceLocation talentId) {
        talents.remove(talentId);
        activeTalents.remove(talentId);
        cooldowns.remove(talentId);
        markDirty();
    }

    @Override
    public List<ResourceLocation> getUnlockedTalents() {
        return new ArrayList<>(talents.keySet());
    }

    @Override
    public void setActiveTalent(ResourceLocation talentId, boolean active) {
        if (hasTalent(talentId)) {
            activeTalents.put(talentId, active);
            markDirty();
        }
    }

    @Override
    public boolean isTalentActive(ResourceLocation talentId) {
        return activeTalents.getOrDefault(talentId, false);
    }

    @Override
    public int getTalentCooldown(ResourceLocation talentId) {
        return cooldowns.getOrDefault(talentId, 0);
    }

    @Override
    public void setTalentCooldown(ResourceLocation talentId, int cooldownTicks) {
        cooldowns.put(talentId, cooldownTicks);
        markDirty();
    }

    public void tickCooldowns(ServerPlayer player) {
        // Early return if no cooldowns to process
        if (cooldowns.isEmpty()) {
            return;
        }

        List<ResourceLocation> toRemove = new ArrayList<>();
        // Decrement by 10 since we only tick every 10 game ticks
        int decrementAmount = 10;

        for (Map.Entry<ResourceLocation, Integer> entry : cooldowns.entrySet()) {
            int remaining = entry.getValue() - decrementAmount;
            if (remaining <= 0) {
                toRemove.add(entry.getKey());
            } else {
                cooldowns.put(entry.getKey(), remaining);
            }
        }

        for (ResourceLocation talentId : toRemove) {
            cooldowns.remove(talentId);
        }
    }

    @Override
    public void sync() {
        // Force immediate sync (used for login, respawn, etc.)
        if (owningPlayer != null) {
            PacketHandler.sendTo(new SyncTalentsPacket(serializeNBT()), owningPlayer);
        }
    }

    public void setOwningPlayer(ServerPlayer player) {
        this.owningPlayer = player;
    }

    public ServerPlayer getOwningPlayer() {
        return this.owningPlayer;
    }

    private void markDirty() {
        this.dirty = true;
    }

    public void tickSync() {
        // Batched sync - only send if dirty
        if (dirty && owningPlayer != null) {
            PacketHandler.sendTo(new SyncTalentsPacket(serializeNBT()), owningPlayer);
            dirty = false;
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag nbt = new CompoundTag();

        // Serialize talents
        CompoundTag talentsNBT = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : talents.entrySet()) {
            talentsNBT.putInt(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("Talents", talentsNBT);

        // Serialize active talents
        ListTag activeTalentsNBT = new ListTag();
        for (Map.Entry<ResourceLocation, Boolean> entry : activeTalents.entrySet()) {
            if (entry.getValue()) {
                activeTalentsNBT.add(StringTag.valueOf(entry.getKey().toString()));
            }
        }
        nbt.put("ActiveTalents", activeTalentsNBT);

        // Serialize cooldowns
        CompoundTag cooldownsNBT = new CompoundTag();
        for (Map.Entry<ResourceLocation, Integer> entry : cooldowns.entrySet()) {
            cooldownsNBT.putInt(entry.getKey().toString(), entry.getValue());
        }
        nbt.put("Cooldowns", cooldownsNBT);

        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        talents.clear();
        activeTalents.clear();
        cooldowns.clear();

        // Deserialize talents
        if (nbt.contains("Talents")) {
            CompoundTag talentsNBT = nbt.getCompound("Talents");
            for (String key : talentsNBT.getAllKeys()) {
                ResourceLocation talentId = ResourceLocation.tryParse(key);
                if (talentId != null) {
                    talents.put(talentId, talentsNBT.getInt(key));
                }
            }
        }

        // Deserialize active talents
        if (nbt.contains("ActiveTalents")) {
            ListTag activeTalentsNBT = nbt.getList("ActiveTalents", Tag.TAG_STRING);
            for (Tag tag : activeTalentsNBT) {
                ResourceLocation talentId = ResourceLocation.tryParse(tag.getAsString());
                if (talentId != null) {
                    activeTalents.put(talentId, true);
                }
            }
        }

        // Deserialize cooldowns
        if (nbt.contains("Cooldowns")) {
            CompoundTag cooldownsNBT = nbt.getCompound("Cooldowns");
            for (String key : cooldownsNBT.getAllKeys()) {
                ResourceLocation talentId = ResourceLocation.tryParse(key);
                if (talentId != null) {
                    cooldowns.put(talentId, cooldownsNBT.getInt(key));
                }
            }
        }
    }
}
