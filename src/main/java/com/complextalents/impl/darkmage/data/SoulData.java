package com.complextalents.impl.darkmage.data;

import com.complextalents.TalentsMod;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.darkmage.SoulSyncPacket;
import com.complextalents.origin.OriginManager;
import com.complextalents.origin.integration.SpellCritAttributes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Server-side tracking for Dark Mage Soul stacks.
 * Souls are UNCAPPED - no maximum limit.
 * Gained from kills: 3 × √(HP/10) - 5 with ±10% variance
 * Lost: 30% on actual death (Phylactery trigger is free)
 */
public class SoulData {

    // Uncapped soul storage (can grow indefinitely) - stored as decimal for precision
    private static final ConcurrentHashMap<UUID, Double> SOUL_STACKS = new ConcurrentHashMap<>();

    // Phylactery cooldown tracking (game time when cooldown expires)
    private static final ConcurrentHashMap<UUID, Long> PHYLACTERY_COOLDOWN = new ConcurrentHashMap<>();

    // Blood Pact active state tracking
    private static final ConcurrentHashMap<UUID, Boolean> BLOOD_PACT_ACTIVE = new ConcurrentHashMap<>();

    // Multipliers for Blood Pact (Drain and Soul Effect)
    private static final ConcurrentHashMap<UUID, Float> BLOOD_PACT_DRAIN_MULTIPLIER = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Float> BLOOD_PACT_SOUL_MULTIPLIER = new ConcurrentHashMap<>();

    /**
     * Get the current soul count for a player (as decimal).
     */
    public static double getSouls(UUID playerUuid) {
        return SOUL_STACKS.getOrDefault(playerUuid, 0.0);
    }

    /**
     * Get the current soul count for a player (as decimal).
     */
    public static double getSouls(ServerPlayer player) {
        return getSouls(player.getUUID());
    }

    /**
     * Get the current soul count floored to integer (for display/packet purposes).
     */
    public static int getSoulsInt(UUID playerUuid) {
        return (int) getSouls(playerUuid);
    }

    /**
     * Get the current soul count floored to integer (for display/packet purposes).
     */
    public static int getSoulsInt(ServerPlayer player) {
        return (int) getSouls(player.getUUID());
    }

    /**
     * Set soul count for a player (no cap - can be any positive value).
     */
    public static void setSouls(UUID playerUuid, double souls) {
        double clamped = Math.max(0.0, souls);
        SOUL_STACKS.put(playerUuid, clamped);
        TalentsMod.LOGGER.debug("Dark Mage souls set to {} for player {}", clamped, playerUuid);
    }

    /**
     * Set soul count for a player and sync to client.
     */
    public static void setSouls(ServerPlayer player, double souls) {
        setSouls(player.getUUID(), souls);
        syncToClient(player);
    }

    /**
     * Add souls to a player's count (no cap).
     */
    public static void addSouls(UUID playerUuid, double amount) {
        double current = getSouls(playerUuid);
        setSouls(playerUuid, current + amount);
    }

    /**
     * Add souls to a player's count and sync to client.
     */
    public static void addSouls(ServerPlayer player, double amount) {
        addSouls(player.getUUID(), amount);
        syncToClient(player);
    }

    /**
     * Calculate souls gained from killing a mob based on its max health.
     * Two-phase formula with ±10% variance:
     * - Phase 1 (HP < 40): Linear scaling HP/40 to ensure weak mobs give souls
     * - Phase 2 (HP >= 40): Root formula 3 × √(HP/10) - 5
     *   - Anchored at 40 HP ≈ 1 soul (0.9-1.1)
     *   - Crossover with HP/40 at 1000 HP ≈ 25 souls (22.5-27.5)
     *   - Generous curve between 40-1000 HP (more than HP/40)
     *   - Harsh brake for high HP mobs (10000 HP ≈ 80-98 souls instead of 250)
     *
     * @param mobMaxHealth The mob's maximum health
     * @return Souls gained (decimal value with ±10% variance, minimum 0)
     */
    public static double calculateSoulsFromKill(double mobMaxHealth) {
        double baseSouls;
        if (mobMaxHealth < 40) {
            // Phase 1: Linear scaling prevents negative numbers for weak mobs
            baseSouls = mobMaxHealth / 40.0;
        } else {
            // Phase 2: The diminishing curve handles the mid-to-endgame economy
            baseSouls = (3.0 * Math.sqrt(mobMaxHealth / 10.0)) - 5.0;
        }
        // Apply ±10% variance (multiplier between 0.9 and 1.1)
        double variance = 0.9 + (ThreadLocalRandom.current().nextDouble() * 0.2);
        return Math.max(0.0, baseSouls * variance);
    }

    /**
     * Lose a percentage of souls (for Phylactery trigger).
     *
     * @param percentage The percentage to lose (0.5 = 50%)
     * @return The number of souls lost
     */
    public static double loseSouls(UUID playerUuid, double percentage) {
        double current = getSouls(playerUuid);
        double toLose = current * percentage;
        setSouls(playerUuid, current - toLose);
        return toLose;
    }

    /**
     * Lose a percentage of souls and sync to client.
     */
    public static double loseSouls(ServerPlayer player, double percentage) {
        double lost = loseSouls(player.getUUID(), percentage);
        syncToClient(player);
        return lost;
    }

    /**
     * Check if Phylactery is on cooldown.
     */
    public static boolean isPhylacteryOnCooldown(UUID playerUuid, long currentGameTime) {
        Long cooldownEnd = PHYLACTERY_COOLDOWN.get(playerUuid);
        if (cooldownEnd == null) {
            return false;
        }
        return currentGameTime < cooldownEnd;
    }

    /**
     * Set Phylactery cooldown end time.
     */
    public static void setPhylacteryCooldown(UUID playerUuid, long cooldownEndGameTime) {
        PHYLACTERY_COOLDOWN.put(playerUuid, cooldownEndGameTime);
        TalentsMod.LOGGER.debug("Dark Mage Phylactery cooldown set until game time {} for player {}",
                cooldownEndGameTime, playerUuid);
    }

    /**
     * Get remaining Phylactery cooldown in ticks.
     */
    public static long getPhylacteryCooldownRemaining(UUID playerUuid, long currentGameTime) {
        Long cooldownEnd = PHYLACTERY_COOLDOWN.get(playerUuid);
        if (cooldownEnd == null) {
            return 0;
        }
        return Math.max(0, cooldownEnd - currentGameTime);
    }

    /**
     * Track Blood Pact active state (for damage bonus application).
     */
    public static void setBloodPactActive(UUID playerUuid, boolean active) {
        if (active) {
            BLOOD_PACT_ACTIVE.put(playerUuid, true);
        } else {
            BLOOD_PACT_ACTIVE.remove(playerUuid);
        }
    }

    /**
     * Check if Blood Pact is active for a player.
     */
    public static boolean isBloodPactActive(UUID playerUuid) {
        return BLOOD_PACT_ACTIVE.getOrDefault(playerUuid, false);
    }

    /**
     * Update Blood Pact multipliers for a player.
     */
    public static void setBloodPactMultipliers(UUID playerUuid, float drainMult, float soulMult) {
        BLOOD_PACT_DRAIN_MULTIPLIER.put(playerUuid, drainMult);
        BLOOD_PACT_SOUL_MULTIPLIER.put(playerUuid, soulMult);
    }

    public static float getDrainMultiplier(UUID playerUuid) {
        return BLOOD_PACT_DRAIN_MULTIPLIER.getOrDefault(playerUuid, 1.0f);
    }

    public static float getSoulMultiplier(UUID playerUuid) {
        return BLOOD_PACT_SOUL_MULTIPLIER.getOrDefault(playerUuid, 1.0f);
    }

    /**
     * Sync soul data to a specific client.
     */
    public static void syncToClient(ServerPlayer player) {
        double souls = getSouls(player.getUUID());
        boolean bloodPactActive = isBloodPactActive(player.getUUID());
        long currentGameTime = player.level().getGameTime();
        long cooldownRemaining = getPhylacteryCooldownRemaining(player.getUUID(), currentGameTime);

        // Get total cooldown in ticks from origin stat (seconds * 20)
        double cooldownSeconds = OriginManager.getOriginStat(player, "phylacteryCooldown");
        long totalCooldownTicks = (long) (cooldownSeconds * 20);

        // Read Blood Pact combat stats from player attributes
        float spellPower = readAttribute(player, ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spell_power"));
        float critChance = readAttribute(player, SpellCritAttributes.SPELL_CRIT_CHANCE.get());
        float critDamage = readAttribute(player, SpellCritAttributes.SPELL_CRIT_DAMAGE.get());

        // Get multipliers
        float drainMult = getDrainMultiplier(player.getUUID());
        float soulMult = getSoulMultiplier(player.getUUID());

        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new SoulSyncPacket(souls, bloodPactActive, cooldownRemaining, totalCooldownTicks,
                        spellPower, critChance, critDamage, drainMult, soulMult));
    }

    private static float readAttribute(ServerPlayer player, ResourceLocation id) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(id);
        if (attr == null) return 0f;
        var inst = player.getAttributes().getInstance(attr);
        return inst != null ? (float) inst.getValue() : 0f;
    }

    private static float readAttribute(ServerPlayer player, Attribute attr) {
        if (attr == null) return 0f;
        var inst = player.getAttributes().getInstance(attr);
        return inst != null ? (float) inst.getValue() : 0f;
    }

    /**
     * Clean up all data for a player (on logout/origin change).
     */
    public static void cleanup(UUID playerUuid) {
        SOUL_STACKS.remove(playerUuid);
        PHYLACTERY_COOLDOWN.remove(playerUuid);
        BLOOD_PACT_ACTIVE.remove(playerUuid);
        BLOOD_PACT_DRAIN_MULTIPLIER.remove(playerUuid);
        BLOOD_PACT_SOUL_MULTIPLIER.remove(playerUuid);
        TalentsMod.LOGGER.debug("Cleaned up Dark Mage data for player {}", playerUuid);
    }

    /**
     * Clean up all data for a player.
     */
    public static void cleanup(ServerPlayer player) {
        cleanup(player.getUUID());
    }

    // --- NBT Serialization for Persistence ---

    /**
     * Serialize soul data for a player to NBT.
     * Used for saving to PlayerPersistentData.
     */
    public static CompoundTag serializeNBT(UUID playerUuid) {
        CompoundTag tag = new CompoundTag();
        tag.putDouble("souls", getSouls(playerUuid));
        // Note: Blood Pact active state and Phylactery cooldown are NOT persisted
        // Blood Pact should be re-activated manually after login/respawn
        // Phylactery cooldown resets on logout
        // Death handler tasks:
        // - [x] Modify Phylactery soul loss mechanic <!-- id: 33 -->
        // - [x] Draft implementation plan for soul loss shift <!-- id: 34 -->
        // - [x] Remove soul loss from `PhylacteryHandler` <!-- id: 35 -->
        // - [x] Implement 30% soul loss on actual death <!-- id: 36 -->
        // - [x] Update documentation and in-game descriptions <!-- id: 37 -->
        return tag;
    }

    /**
     * Serialize soul data for a player to NBT.
     */
    public static CompoundTag serializeNBT(ServerPlayer player) {
        return serializeNBT(player.getUUID());
    }

    /**
     * Deserialize soul data from NBT.
     * Used for restoring from PlayerPersistentData.
     */
    public static void deserializeNBT(UUID playerUuid, CompoundTag tag) {
        if (tag.contains("souls")) {
            double souls = tag.getDouble("souls");
            setSouls(playerUuid, souls);
            TalentsMod.LOGGER.info("Restored {} souls for player {}", souls, playerUuid);
        }
    }

    /**
     * Deserialize soul data from NBT and sync to client.
     */
    public static void deserializeNBT(ServerPlayer player, CompoundTag tag) {
        deserializeNBT(player.getUUID(), tag);
        syncToClient(player);
    }
}
