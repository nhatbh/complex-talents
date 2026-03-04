package com.complextalents.skill.capability;

import com.complextalents.network.PacketHandler;
import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.network.SkillCooldownSyncPacket;
import com.complextalents.skill.network.SkillDataSyncPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Implementation of player skill data capability.
 * Stores slot assignments, cooldowns, and toggle states.
 */
public class PlayerSkillData implements IPlayerSkillData, INBTSerializable<CompoundTag> {

    private final ServerPlayer player;

    // Slot assignments: index 0 maps to skill ID
    private final ResourceLocation[] skillSlots = new ResourceLocation[SLOT_COUNT];

    // Active cooldowns: skillId -> expiration game time
    private final Map<ResourceLocation, Long> activeCooldowns = new HashMap<>();

    // Passive cooldowns: skillId -> expiration game time (for hybrid skills)
    private final Map<ResourceLocation, Long> passiveCooldowns = new HashMap<>();

    // Toggle states: skillId -> isActive
    private final Set<ResourceLocation> activeToggles = new HashSet<>();

    // Toggle activation times: skillId -> game time when activated
    private final Map<ResourceLocation, Long> toggleActivationTimes = new HashMap<>();

    // Skill levels: skillId -> level (default 1)
    private final Map<ResourceLocation, Integer> skillLevels = new HashMap<>();

    // Form tracking
    private ResourceLocation activeForm = null;
    private long formExpiration = 0;  // Game time when form expires

    public PlayerSkillData(ServerPlayer player) {
        this.player = player;
    }

    @Override
    @Nullable
    public ResourceLocation getSkillInSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) {
            return null;
        }
        return skillSlots[slotIndex];
    }

    @Override
    public void setSkillInSlot(int slotIndex, @Nullable ResourceLocation skillId) {
        if (slotIndex < 0 || slotIndex >= SLOT_COUNT) {
            return;
        }

        // If removing a toggle skill, turn it off first
        ResourceLocation oldSkill = skillSlots[slotIndex];
        if (oldSkill != null && activeToggles.contains(oldSkill)) {
            setToggleActive(oldSkill, false);
        }

        skillSlots[slotIndex] = skillId;
        sync();
    }

    @Override
    public ResourceLocation[] getAssignedSlots() {
        return Arrays.copyOf(skillSlots, SLOT_COUNT);
    }

    @Override
    public boolean isOnCooldown(ResourceLocation skillId) {
        if (!activeCooldowns.containsKey(skillId)) {
            return false;
        }

        long gameTime = player.level().getGameTime();
        if (gameTime >= activeCooldowns.get(skillId)) {
            activeCooldowns.remove(skillId);
            return false;
        }
        return true;
    }

    @Override
    public double getCooldown(ResourceLocation skillId) {
        if (!activeCooldowns.containsKey(skillId)) {
            return 0;
        }

        long gameTime = player.level().getGameTime();
        long expiration = activeCooldowns.get(skillId);

        if (gameTime >= expiration) {
            activeCooldowns.remove(skillId);
            return 0;
        }

        return (expiration - gameTime) / 20.0; // Convert ticks to seconds
    }

    @Override
    public void setCooldown(ResourceLocation skillId, double seconds) {
        long ticks = (long) (seconds * 20);
        long expiration = player.level().getGameTime() + ticks;
        activeCooldowns.put(skillId, expiration);
        syncCooldowns();
    }

    @Override
    public void clearCooldown(ResourceLocation skillId) {
        activeCooldowns.remove(skillId);
        syncCooldowns();
    }

    /**
     * Get the cooldown expiration time for a skill.
     *
     * @param skillId The skill ID
     * @return The expiration game time, or null if not on cooldown
     */
    public Long getCooldownExpiration(ResourceLocation skillId) {
        if (!activeCooldowns.containsKey(skillId)) {
            return null;
        }
        long gameTime = player.level().getGameTime();
        long expiration = activeCooldowns.get(skillId);
        if (gameTime >= expiration) {
            activeCooldowns.remove(skillId);
            return null;
        }
        return expiration;
    }

    @Override
    public boolean isPassiveOnCooldown(ResourceLocation skillId) {
        if (!passiveCooldowns.containsKey(skillId)) {
            return false;
        }

        long gameTime = player.level().getGameTime();
        if (gameTime >= passiveCooldowns.get(skillId)) {
            passiveCooldowns.remove(skillId);
            return false;
        }
        return true;
    }

    @Override
    public double getPassiveCooldown(ResourceLocation skillId) {
        if (!passiveCooldowns.containsKey(skillId)) {
            return 0;
        }

        long gameTime = player.level().getGameTime();
        long expiration = passiveCooldowns.get(skillId);

        if (gameTime >= expiration) {
            passiveCooldowns.remove(skillId);
            return 0;
        }

        return (expiration - gameTime) / 20.0; // Convert ticks to seconds
    }

    @Override
    public void setPassiveCooldown(ResourceLocation skillId, double seconds) {
        long ticks = (long) (seconds * 20);
        long expiration = player.level().getGameTime() + ticks;
        passiveCooldowns.put(skillId, expiration);
    }

    @Override
    public void clearPassiveCooldown(ResourceLocation skillId) {
        passiveCooldowns.remove(skillId);
    }

    @Override
    public boolean isToggleActive(ResourceLocation skillId) {
        return activeToggles.contains(skillId);
    }

    @Override
    public void setToggleActive(ResourceLocation skillId, boolean active) {
        if (active) {
            activeToggles.add(skillId);
            // Track activation time for max duration checking
            toggleActivationTimes.put(skillId, player.level().getGameTime());
        } else {
            activeToggles.remove(skillId);
            // Clear activation time when deactivated
            toggleActivationTimes.remove(skillId);
        }
        sync();
    }

    @Override
    public long getToggleActivationTime(ResourceLocation skillId) {
        return toggleActivationTimes.getOrDefault(skillId, 0L);
    }

    @Override
    public void setToggleActivationTime(ResourceLocation skillId, long gameTime) {
        if (activeToggles.contains(skillId)) {
            toggleActivationTimes.put(skillId, gameTime);
        }
    }

    @Override
    public void tick() {
        long currentTime = player.level().getGameTime();

        // Handle toggle skill resource consumption and max duration
        Iterator<ResourceLocation> toggleIterator = activeToggles.iterator();
        while (toggleIterator.hasNext()) {
            ResourceLocation toggleSkill = toggleIterator.next();
            Skill skill = SkillRegistry.getInstance().getSkill(toggleSkill);

            if (skill == null) {
                continue;
            }

            // Get skill level for scaling
            int skillLevel = getSkillLevel(toggleSkill);

            // Check if max duration has been reached
            if (skill.getToggleMaxDuration() > 0) {
                Long activationTime = toggleActivationTimes.get(toggleSkill);
                if (activationTime != null) {
                    long durationTicks = (long) (skill.getToggleMaxDuration() * 20);
                    if (currentTime >= activationTime + durationTicks) {
                        // Max duration reached - turn off toggle and start cooldown
                        toggleIterator.remove();
                        toggleActivationTimes.remove(toggleSkill);
                        double cooldown = skill.getActiveCooldown(skillLevel);
                        if (cooldown > 0) {
                            setCooldown(toggleSkill, cooldown);
                        }
                        // Call toggle-off handler if present
                        if (skill.hasToggleOffHandler()) {
                            skill.executeToggleOff(player);
                        }
                        sync();
                        continue;
                    }
                }
            }

            double toggleCost = skill.getToggleCostPerTick(skillLevel);
            if (toggleCost > 0) {
                // Check if player has enough resources
                if (!hasEnoughResource(skill, toggleCost / 20.0)) {
                    // Turn off toggle if not enough resources
                    toggleIterator.remove();
                    toggleActivationTimes.remove(toggleSkill);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "\u00A7cNot enough resources to sustain " + skill.getDisplayName().getString()
                    ));
                    sync();
                } else {
                    // Consume resources (per tick cost is per second, so divide by 20)
                    consumeResource(skill, toggleCost / 20.0);
                }
            }
        }

        // Check form expiration
        if (activeForm != null) {
            if (currentTime >= formExpiration) {
                // Form expired - deactivate via SkillFormManager
                com.complextalents.skill.form.SkillFormManager.deactivateForm(player);
            }
        }
    }

    @Override
    public void sync() {
        // Send sync packet to client
        PacketHandler.sendTo(new SkillDataSyncPacket(player.getUUID(), getAssignedSlots(), new HashMap<>(skillLevels)), player);
    }

    /**
     * Sync cooldown data to client.
     */
    public void syncCooldowns() {
        PacketHandler.sendTo(new SkillCooldownSyncPacket(new HashMap<>(activeCooldowns), player.level().getGameTime()), player);
    }

    @Override
    public void clear() {
        Arrays.fill(skillSlots, null);
        activeCooldowns.clear();
        passiveCooldowns.clear();
        activeToggles.clear();
        toggleActivationTimes.clear();
        skillLevels.clear();
        activeForm = null;
        formExpiration = 0;
        sync();
    }

    @Override
    public int getSkillLevel(ResourceLocation skillId) {
        // Check if skill is assigned to any slot
        boolean isAssigned = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (skillId.equals(skillSlots[i])) {
                isAssigned = true;
                break;
            }
        }

        if (!isAssigned) {
            return 1; // Skill not assigned, return default
        }

        // Return stored level or default to 1
        return skillLevels.getOrDefault(skillId, 1);
    }

    @Override
    public void setSkillLevel(ResourceLocation skillId, int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Skill level must be at least 1, got: " + level);
        }

        // Validate skill is assigned
        boolean isAssigned = false;
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (skillId.equals(skillSlots[i])) {
                isAssigned = true;
                break;
            }
        }

        if (!isAssigned) {
            throw new IllegalArgumentException("Cannot set level for unassigned skill: " + skillId);
        }

        // Validate against skill's max level
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);
        if (skill != null && level > skill.getMaxLevel()) {
            throw new IllegalArgumentException("Level " + level + " exceeds max level " +
                skill.getMaxLevel() + " for skill: " + skillId);
        }

        skillLevels.put(skillId, level);
        sync();
    }

    @Override
    public ResourceLocation getActiveForm() {
        return activeForm;
    }

    @Override
    public void setActiveForm(ResourceLocation formSkillId) {
        this.activeForm = formSkillId;
        sync();
    }

    @Override
    public long getFormExpiration() {
        return formExpiration;
    }

    @Override
    public void setFormExpiration(long expirationTime) {
        this.formExpiration = expirationTime;
    }

    @Override
    public void copyFrom(IPlayerSkillData other) {
        // Copy skill slot assignments
        ResourceLocation[] otherSlots = other.getAssignedSlots();
        for (int i = 0; i < SLOT_COUNT; i++) {
            skillSlots[i] = otherSlots[i];
        }

        // Copy skill levels by iterating through assigned slots
        for (int i = 0; i < SLOT_COUNT; i++) {
            ResourceLocation skillId = skillSlots[i];
            if (skillId != null) {
                int level = other.getSkillLevel(skillId);
                if (level > 1) {
                    skillLevels.put(skillId, level);
                }
            }
        }

        // Copy active form and expiration
        ResourceLocation otherForm = other.getActiveForm();
        if (otherForm != null) {
            activeForm = otherForm;
            formExpiration = other.getFormExpiration();
        }

        // Note: We do NOT copy active toggles, toggle activation times, or cooldowns
        // Toggles must be reactivated after death/respawn for safety
        // Cooldowns reset on death as a gameplay mechanic

        sync();
    }

    // NBT serialization for persistence
    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // Serialize slots
        ListTag slotsList = new ListTag();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (skillSlots[i] != null) {
                CompoundTag slotTag = new CompoundTag();
                slotTag.putInt("slot", i);
                slotTag.putString("skill", skillSlots[i].toString());
                slotsList.add(slotTag);
            }
        }
        tag.put("slots", slotsList);

        // Serialize active toggles
        ListTag togglesList = new ListTag();
        for (ResourceLocation toggle : activeToggles) {
            togglesList.add(StringTag.valueOf(toggle.toString()));
        }
        tag.put("activeToggles", togglesList);

        // Serialize toggle activation times
        CompoundTag activationTimesTag = new CompoundTag();
        for (var entry : toggleActivationTimes.entrySet()) {
            activationTimesTag.putLong(entry.getKey().toString(), entry.getValue());
        }
        tag.put("toggleActivationTimes", activationTimesTag);

        // Serialize skill levels
        ListTag levelsList = new ListTag();
        for (var entry : skillLevels.entrySet()) {
            CompoundTag levelTag = new CompoundTag();
            levelTag.putString("skill", entry.getKey().toString());
            levelTag.putInt("level", entry.getValue());
            levelsList.add(levelTag);
        }
        tag.put("skillLevels", levelsList);

        // Serialize active form (for logout/login persistence)
        if (activeForm != null) {
            tag.putString("activeForm", activeForm.toString());
            tag.putLong("formExpiration", formExpiration);
        }

        // Serialize active cooldowns (store remaining duration, not absolute time)
        long currentGameTime = player.level().getGameTime();
        CompoundTag activeCooldownsTag = new CompoundTag();
        for (var entry : activeCooldowns.entrySet()) {
            long remainingTicks = entry.getValue() - currentGameTime;
            if (remainingTicks > 0) {
                activeCooldownsTag.putLong(entry.getKey().toString(), remainingTicks);
            }
        }
        tag.put("activeCooldowns", activeCooldownsTag);

        // Serialize passive cooldowns (store remaining duration)
        CompoundTag passiveCooldownsTag = new CompoundTag();
        for (var entry : passiveCooldowns.entrySet()) {
            long remainingTicks = entry.getValue() - currentGameTime;
            if (remainingTicks > 0) {
                passiveCooldownsTag.putLong(entry.getKey().toString(), remainingTicks);
            }
        }
        tag.put("passiveCooldowns", passiveCooldownsTag);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // Clear existing data
        Arrays.fill(skillSlots, null);
        activeToggles.clear();
        toggleActivationTimes.clear();

        // Deserialize slots
        if (tag.contains("slots")) {
            ListTag slotsList = tag.getList("slots", 10); // 10 = COMPOUND
            for (int i = 0; i < slotsList.size(); i++) {
                CompoundTag slotTag = slotsList.getCompound(i);
                int slot = slotTag.getInt("slot");
                String skillStr = slotTag.getString("skill");
                if (slot >= 0 && slot < SLOT_COUNT) {
                    skillSlots[slot] = ResourceLocation.tryParse(skillStr);
                }
            }
        }

        // Deserialize active toggles (but don't restore them - safer to reset on respawn)
        // Players need to manually reactivate toggles after death/respawn

        // Deserialize skill levels
        if (tag.contains("skillLevels")) {
            ListTag levelsList = tag.getList("skillLevels", 10); // 10 = COMPOUND
            for (int i = 0; i < levelsList.size(); i++) {
                CompoundTag levelTag = levelsList.getCompound(i);
                String skillStr = levelTag.getString("skill");
                int level = levelTag.getInt("level");
                ResourceLocation skillId = ResourceLocation.tryParse(skillStr);
                if (skillId != null && level >= 1) {
                    skillLevels.put(skillId, level);
                }
            }
        }

        // Deserialize active form (but don't restore on respawn - players must reactivate)
        // This is intentional: forms should not persist through death
        // If we wanted to restore after logout/login but not death, we'd need a flag

        // Deserialize active cooldowns (restore from remaining duration)
        if (tag.contains("activeCooldowns")) {
            CompoundTag cooldownsTag = tag.getCompound("activeCooldowns");
            long currentGameTime = player.level().getGameTime();
            for (String key : cooldownsTag.getAllKeys()) {
                ResourceLocation skillId = ResourceLocation.tryParse(key);
                if (skillId != null) {
                    long remainingTicks = cooldownsTag.getLong(key);
                    // Calculate new expiration based on current game time + remaining duration
                    activeCooldowns.put(skillId, currentGameTime + remainingTicks);
                }
            }
        }

        // Deserialize passive cooldowns (restore from remaining duration)
        if (tag.contains("passiveCooldowns")) {
            CompoundTag cooldownsTag = tag.getCompound("passiveCooldowns");
            long currentGameTime = player.level().getGameTime();
            for (String key : cooldownsTag.getAllKeys()) {
                ResourceLocation skillId = ResourceLocation.tryParse(key);
                if (skillId != null) {
                    long remainingTicks = cooldownsTag.getLong(key);
                    passiveCooldowns.put(skillId, currentGameTime + remainingTicks);
                }
            }
        }
    }

    /**
     * Check if player has enough of a resource.
     * This is a placeholder for integration with resource systems like Iron's Spellbooks.
     *
     * @param skill The skill with resource cost
     * @param amount The amount to check
     * @return true if player has enough resources
     */
    private boolean hasEnoughResource(Skill skill, double amount) {
        ResourceLocation resourceType = skill.getResourceType();
        if (resourceType == null || amount <= 0) {
            return true;
        }

        // Placeholder: always return true
        // Actual implementation should check the player's resource (mana, energy, etc.)
        return true;
    }

    /**
     * Consume a resource from the player.
     * This is a placeholder for integration with resource systems.
     */
    private void consumeResource(Skill skill, double amount) {
        ResourceLocation resourceType = skill.getResourceType();
        if (resourceType == null) {
            return;
        }

        // Placeholder: no actual consumption
        // Actual implementation should deduct from the player's resource
    }
}
