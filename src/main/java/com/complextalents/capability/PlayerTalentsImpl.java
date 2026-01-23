package com.complextalents.capability;

import com.complextalents.TalentsMod;
import com.complextalents.client.DefaultResourceBarRenderer;
import com.complextalents.talent.TalentBranches;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.SyncTalentsPacket;
import com.complextalents.talent.ResourceBarConfig;
import com.complextalents.talent.ResourceBarRenderer;
import com.complextalents.talent.Talent;
import com.complextalents.talent.TalentRegistry;
import com.complextalents.talent.TalentSlotType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerTalentsImpl implements PlayerTalents {
    private final Map<ResourceLocation, Integer> talents = new HashMap<>();
    private final Map<ResourceLocation, Boolean> activeTalents = new HashMap<>();
    private final Map<ResourceLocation, Integer> cooldowns = new HashMap<>();
    private final Map<TalentSlotType, ResourceLocation> equippedTalents = new EnumMap<>(TalentSlotType.class);
    private float currentResource = 0.0f;
    private boolean combatModeEnabled = false;
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

        // Auto-unequip from slot if equipped
        equippedTalents.entrySet().removeIf(entry -> entry.getValue().equals(talentId));

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

    @Override
    public ResourceLocation getTalentInSlot(TalentSlotType slotType) {
        return equippedTalents.get(slotType);
    }

    @Override
    public boolean equipTalentToSlot(ResourceLocation talentId, TalentSlotType slotType) {
        // Check if talent is unlocked
        if (!hasTalent(talentId)) {
            TalentsMod.LOGGER.warn("Cannot equip talent {} - not unlocked", talentId);
            return false;
        }

        // Check if talent matches the slot type
        Talent talent = TalentRegistry.getTalent(talentId);
        if (talent == null) {
            TalentsMod.LOGGER.warn("Cannot equip talent {} - not found in registry", talentId);
            return false;
        }

        if (talent.getSlotType() != slotType) {
            TalentsMod.LOGGER.warn("Cannot equip talent {} to slot {} - talent is for slot {}",
                    talentId, slotType, talent.getSlotType());
            return false;
        }

        // Check if required Definition talent is equipped (if this talent has a requirement)
        if (talent.hasDefinitionRequirement()) {
            ResourceLocation requiredDef = talent.getRequiredDefinition();
            ResourceLocation equippedDef = equippedTalents.get(TalentSlotType.DEFINITION);

            if (equippedDef == null || !equippedDef.equals(requiredDef)) {
                TalentsMod.LOGGER.warn("Cannot equip talent {} - requires Definition talent {} to be equipped first",
                        talentId, requiredDef);
                return false;
            }
        }

        // Equip the talent
        equippedTalents.put(slotType, talentId);

        // If equipping a Definition talent with a resource bar, initialize resource
        if (slotType == TalentSlotType.DEFINITION && talent.hasResourceBar()) {
            ResourceBarConfig config = talent.getResourceBarConfig();
            currentResource = config.getStartingValue();
            TalentsMod.LOGGER.debug("Initialized resource bar for {} with starting value {}",
                    talentId, config.getStartingValue());
        }

        markDirty();
        TalentsMod.LOGGER.debug("Equipped talent {} to slot {}", talentId, slotType);
        return true;
    }

    @Override
    public void unequipTalentFromSlot(TalentSlotType slotType) {
        // If unequipping a Definition talent, check if any equipped talents depend on it
        if (slotType == TalentSlotType.DEFINITION) {
            ResourceLocation definitionId = equippedTalents.get(TalentSlotType.DEFINITION);
            if (definitionId != null) {
                // Check all other equipped talents for dependencies
                for (Map.Entry<TalentSlotType, ResourceLocation> entry : equippedTalents.entrySet()) {
                    if (entry.getKey() == TalentSlotType.DEFINITION) continue;

                    Talent equippedTalent = TalentRegistry.getTalent(entry.getValue());
                    if (equippedTalent != null && equippedTalent.hasDefinitionRequirement()) {
                        if (definitionId.equals(equippedTalent.getRequiredDefinition())) {
                            TalentsMod.LOGGER.warn("Cannot unequip Definition talent {} - talent {} depends on it",
                                    definitionId, entry.getValue());
                            return;
                        }
                    }
                }
            }
        }

        if (equippedTalents.remove(slotType) != null) {
            markDirty();
            TalentsMod.LOGGER.debug("Unequipped talent from slot {}", slotType);
        }
    }

    @Override
    public Map<TalentSlotType, ResourceLocation> getEquippedTalents() {
        return new EnumMap<>(equippedTalents);
    }

    @Override
    public boolean isSlotFilled(TalentSlotType slotType) {
        return equippedTalents.containsKey(slotType);
    }

    @Override
    public boolean canEquipTalent(ResourceLocation talentId, TalentSlotType slotType) {
        // Check if talent is unlocked
        if (!hasTalent(talentId)) {
            return false;
        }

        // Check if talent exists in registry
        Talent talent = TalentRegistry.getTalent(talentId);
        if (talent == null) {
            return false;
        }

        // Check if talent matches the slot type
        if (talent.getSlotType() != slotType) {
            return false;
        }

        // Check if required Definition talent is equipped (if this talent has a requirement)
        if (talent.hasDefinitionRequirement()) {
            ResourceLocation requiredDef = talent.getRequiredDefinition();
            ResourceLocation equippedDef = equippedTalents.get(TalentSlotType.DEFINITION);

            if (equippedDef == null || !equippedDef.equals(requiredDef)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public List<ResourceLocation> getDependentTalents(ResourceLocation definitionId) {
        List<ResourceLocation> dependents = new ArrayList<>();

        // Check all equipped talents for dependencies on this definition
        for (Map.Entry<TalentSlotType, ResourceLocation> entry : equippedTalents.entrySet()) {
            if (entry.getKey() == TalentSlotType.DEFINITION) continue;

            Talent equippedTalent = TalentRegistry.getTalent(entry.getValue());
            if (equippedTalent != null && equippedTalent.hasDefinitionRequirement()) {
                if (definitionId.equals(equippedTalent.getRequiredDefinition())) {
                    dependents.add(entry.getValue());
                }
            }
        }

        return dependents;
    }

    // ===== Resource Bar Implementation =====

    @Override
    public ResourceBarConfig getResourceBarConfig() {
        ResourceLocation definitionId = equippedTalents.get(TalentSlotType.DEFINITION);
        if (definitionId == null) {
            return null;
        }

        Talent definitionTalent = TalentRegistry.getTalent(definitionId);
        if (definitionTalent == null) {
            return null;
        }

        return definitionTalent.getResourceBarConfig();
    }

    @Override
    public float getResource() {
        return currentResource;
    }

    @Override
    public void setResource(float value) {
        ResourceBarConfig config = getResourceBarConfig();
        if (config == null) {
            currentResource = 0.0f;
            return;
        }

        // Clamp between 0 and max
        currentResource = Math.max(0.0f, Math.min(value, config.getMaxValue()));
        markDirty();
    }

    @Override
    public float addResource(float amount) {
        ResourceBarConfig config = getResourceBarConfig();
        if (config == null) {
            return 0.0f;
        }

        float oldValue = currentResource;
        setResource(currentResource + amount);
        return currentResource - oldValue;
    }

    @Override
    public float getMaxResource() {
        ResourceBarConfig config = getResourceBarConfig();
        return config != null ? config.getMaxValue() : 0.0f;
    }

    @Override
    public boolean hasResource(float amount) {
        return currentResource >= amount;
    }

    @Override
    public boolean consumeResource(float amount) {
        if (!hasResource(amount)) {
            return false;
        }

        setResource(currentResource - amount);
        return true;
    }

    @Override
    public ResourceBarRenderer getResourceBarRenderer() {
        ResourceBarConfig config = getResourceBarConfig();
        if (config == null) {
            return null;
        }

        // Use custom renderer if provided, otherwise use default
        if (config.hasCustomRenderer()) {
            return config.getRendererSupplier().get();
        }

        return new DefaultResourceBarRenderer();
    }

    public void tickResource() {
        ResourceBarConfig config = getResourceBarConfig();
        if (config == null) {
            currentResource = 0.0f;
            return;
        }

        // Apply regeneration/decay (rate is per second, tick every 10 ticks = 0.5 seconds)
        float regenPerTick = config.getRegenRate() * 0.5f;
        if (regenPerTick != 0) {
            addResource(regenPerTick);
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

        // Serialize equipped talents (slot system)
        CompoundTag equippedNBT = new CompoundTag();
        for (Map.Entry<TalentSlotType, ResourceLocation> entry : equippedTalents.entrySet()) {
            equippedNBT.putString(entry.getKey().name(), entry.getValue().toString());
        }
        nbt.put("EquippedTalents", equippedNBT);

        // Serialize resource value
        nbt.putFloat("Resource", currentResource);

        // Serialize combat mode
        nbt.putBoolean("CombatMode", combatModeEnabled);

        // Serialize branch selections
        if (owningPlayer != null) {
            CompoundTag branchData = TalentBranches.saveToNBT(owningPlayer.getUUID());
            if (!branchData.isEmpty()) {
                nbt.put("TalentBranches", branchData);
            }
        }

        return nbt;
    }

    public void deserializeNBT(CompoundTag nbt) {
        talents.clear();
        activeTalents.clear();
        cooldowns.clear();
        equippedTalents.clear();

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

        // Deserialize equipped talents (slot system)
        if (nbt.contains("EquippedTalents")) {
            CompoundTag equippedNBT = nbt.getCompound("EquippedTalents");
            for (String key : equippedNBT.getAllKeys()) {
                try {
                    TalentSlotType slotType = TalentSlotType.valueOf(key);
                    ResourceLocation talentId = ResourceLocation.tryParse(equippedNBT.getString(key));
                    if (talentId != null) {
                        equippedTalents.put(slotType, talentId);
                    }
                } catch (IllegalArgumentException e) {
                    TalentsMod.LOGGER.warn("Unknown slot type in save data: {}", key);
                }
            }
        }

        // Deserialize resource value
        if (nbt.contains("Resource")) {
            currentResource = nbt.getFloat("Resource");
        } else {
            currentResource = 0.0f;
        }

        // Deserialize combat mode
        if (nbt.contains("CombatMode")) {
            combatModeEnabled = nbt.getBoolean("CombatMode");
        } else {
            combatModeEnabled = false;
        }

        // Deserialize branch selections
        if (nbt.contains("TalentBranches") && owningPlayer != null) {
            CompoundTag branchData = nbt.getCompound("TalentBranches");
            TalentBranches.loadFromNBT(owningPlayer.getUUID(), branchData);
        }
    }

    // ===== Combat Mode Implementation =====

    @Override
    public boolean isCombatModeEnabled() {
        return combatModeEnabled;
    }

    @Override
    public void setCombatMode(boolean enabled) {
        if (combatModeEnabled != enabled) {
            combatModeEnabled = enabled;
            markDirty();
            TalentsMod.LOGGER.debug("Combat Mode {} for player {}",
                enabled ? "enabled" : "disabled",
                owningPlayer != null ? owningPlayer.getName().getString() : "unknown");
        }
    }

    @Override
    public boolean toggleCombatMode() {
        setCombatMode(!combatModeEnabled);
        return combatModeEnabled;
    }
}
