package com.complextalents.skill.capability;

import com.complextalents.network.PacketHandler;
import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
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

    // Slot assignments: index 0-3 maps to skill IDs
    private final ResourceLocation[] skillSlots = new ResourceLocation[SLOT_COUNT];

    // Active cooldowns: skillId -> expiration game time
    private final Map<ResourceLocation, Long> activeCooldowns = new HashMap<>();

    // Passive cooldowns: skillId -> expiration game time (for hybrid skills)
    private final Map<ResourceLocation, Long> passiveCooldowns = new HashMap<>();

    // Toggle states: skillId -> isActive
    private final Set<ResourceLocation> activeToggles = new HashSet<>();

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
    }

    @Override
    public void clearCooldown(ResourceLocation skillId) {
        activeCooldowns.remove(skillId);
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
        } else {
            activeToggles.remove(skillId);
        }
        sync();
    }

    @Override
    public void tick() {
        // Handle toggle skill resource consumption
        Iterator<ResourceLocation> toggleIterator = activeToggles.iterator();
        while (toggleIterator.hasNext()) {
            ResourceLocation toggleSkill = toggleIterator.next();
            Skill skill = SkillRegistry.getInstance().getSkill(toggleSkill);

            if (skill != null && skill.getToggleCostPerTick() > 0) {
                // Check if player has enough resources
                if (!hasEnoughResource(skill)) {
                    // Turn off toggle if not enough resources
                    toggleIterator.remove();
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "\u00A7cNot enough resources to sustain " + skill.getDisplayName().getString()
                    ));
                    sync();
                } else {
                    // Consume resources (per tick cost is per second, so divide by 20)
                    consumeResource(skill, skill.getToggleCostPerTick() / 20.0);
                }
            }
        }

        // Check form expiration
        if (activeForm != null) {
            long currentTime = player.level().getGameTime();
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

    @Override
    public void clear() {
        Arrays.fill(skillSlots, null);
        activeCooldowns.clear();
        passiveCooldowns.clear();
        activeToggles.clear();
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

        // Note: Cooldowns are not persisted as they reset on respawn/death
        // This is intentional - players shouldn't keep cooldowns after death

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        // Clear existing data
        Arrays.fill(skillSlots, null);
        activeToggles.clear();

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
    }

    /**
     * Check if player has enough of a resource.
     * This is a placeholder for integration with resource systems like Iron's Spellbooks.
     */
    private boolean hasEnoughResource(Skill skill) {
        ResourceLocation resourceType = skill.getResourceType();
        if (resourceType == null) {
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
