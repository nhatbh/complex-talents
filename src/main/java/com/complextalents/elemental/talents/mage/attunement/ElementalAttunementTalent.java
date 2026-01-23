package com.complextalents.elemental.talents.mage.attunement;

import com.complextalents.TalentsMod;
import com.complextalents.capability.TalentsCapabilities;
import com.complextalents.talent.BranchingPassiveTalent;
import com.complextalents.talent.TalentBranches;
import com.complextalents.talent.TalentSlotType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Elemental Attunement - Focus Management skill tree
 * Harmony Slot talent with 4 ranks and branching paths
 */
public class ElementalAttunementTalent extends BranchingPassiveTalent {

    // Track resonance stacks for Path 2A players
    private static final Map<UUID, ResonanceData> resonanceStacks = new HashMap<>();

    // Bonus values per rank for each path
    private static final float[] CATALYST_BONUS = {0.20f, 0.25f, 0.35f, 0.50f};
    private static final float[] RESONANCE_BONUS_PER_STACK = {0.15f, 0.20f, 0.25f, 0.35f};
    private static final float[] SUPER_REACTION_FLAT_FOCUS = {50f, 80f, 120f, 150f};
    private static final float[] DIFFERENT_ELEMENT_FOCUS = {10f, 12f, 15f, 20f};
    private static final float[] RESERVOIR_BONUS = {50f, 75f, 115f, 150f};
    private static final float[] OVERFLOW_CHANCE = {0.20f, 0.25f, 0.30f, 0.40f};

    // Cooldowns for capstone abilities
    private static final int LOOP_CASTING_DURATION = 100; // 5 seconds in ticks
    private static final int LOOP_CASTING_COOLDOWN = 600; // 30 seconds in ticks

    public ElementalAttunementTalent() {
        super(
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_attunement"),
            Component.translatable("talent.complextalents.elemental_attunement"),
            Component.translatable("talent.complextalents.elemental_attunement.desc"),
            4, // 4 ranks with branching
            TalentSlotType.HARMONY,
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_mage_definition")
        );

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // Initialize resonance tracking if needed
        if (!resonanceStacks.containsKey(player.getUUID())) {
            resonanceStacks.put(player.getUUID(), new ResonanceData());
        }

        TalentsMod.LOGGER.info("Player {} unlocked Elemental Attunement at level {}",
            player.getName().getString(), level);
    }

    @Override
    public void onRemove(ServerPlayer player) {
        super.onRemove(player);
        resonanceStacks.remove(player.getUUID());
    }

    /**
     * Calculate Focus generation bonus from Elemental Attunement
     */
    public static float calculateFocusBonus(ServerPlayer player, boolean isSuperReaction) {
        return player.getCapability(TalentsCapabilities.PLAYER_TALENTS).map(talents -> {
            ResourceLocation talentId = ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_attunement");
            int level = talents.getTalentLevel(talentId);

            if (level == 0) return 1.0f; // No bonus

            // Rank 1: Catalyst bonus (always active)
            float multiplier = 1.0f + CATALYST_BONUS[Math.min(level - 1, 3)];

            // Check for Rank 2B: Efficient Conversion
            if (level >= 2 && TalentBranches.hasBranch(player, talentId, 2, TalentBranches.BranchChoice.PATH_B)) {
                if (isSuperReaction) {
                    // Flat Focus bonus for super reactions
                    // This is added after the multiplier is applied
                    // We'll handle this in the calling code
                }
            }

            // Check for Rank 2A: Rapid Generation (Resonance stacks bonus)
            if (level >= 2 && TalentBranches.hasBranch(player, talentId, 2, TalentBranches.BranchChoice.PATH_A)) {
                ResonanceData data = resonanceStacks.get(player.getUUID());
                if (data != null && data.stacks > 0) {
                    float resonanceBonus = RESONANCE_BONUS_PER_STACK[Math.min(level - 1, 3)] * data.stacks;
                    multiplier += resonanceBonus;
                }
            }

            return multiplier;
        }).orElse(1.0f);
    }

    /**
     * Handle resonance stack generation (Path 2A)
     */
    public static void onReactionTriggered(ServerPlayer player, String elementType) {
        ResourceLocation talentId = ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_attunement");

        // Only process if player has Rank 2A: Rapid Generation
        if (TalentBranches.hasBranch(player, talentId, 2, TalentBranches.BranchChoice.PATH_A)) {
            ResonanceData data = resonanceStacks.get(player.getUUID());
            if (data != null) {
                if (!elementType.equals(data.lastElement)) {
                    data.addStack();
                    data.lastElement = elementType;
                    data.lastStackTime = System.currentTimeMillis();
                }
            }
        }
    }

    /**
     * Get current resonance stacks for a player
     */
    public static int getResonanceStacks(ServerPlayer player) {
        ResonanceData data = resonanceStacks.get(player.getUUID());
        return data != null ? data.stacks : 0;
    }

    /**
     * Check and handle Loop Casting activation (Rank 4A capstone)
     */
    public static boolean tryActivateLoopCasting(ServerPlayer player) {
        ResourceLocation talentId = ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_attunement");

        // Check if player has Rank 4A: Loop Casting
        if (!TalentBranches.hasBranch(player, talentId, 4, TalentBranches.BranchChoice.PATH_A)) {
            return false;
        }

        ResonanceData data = resonanceStacks.get(player.getUUID());
        if (data != null && data.stacks >= 3 && !data.loopCastingActive) {
            data.stacks = 0; // Consume all stacks
            data.loopCastingActive = true;
            data.loopCastingEndTime = System.currentTimeMillis() + (LOOP_CASTING_DURATION * 50); // Convert to ms

            // Apply Loop Casting effects
            // Store the buff in the player's persistent data for other systems to check
            player.getPersistentData().putBoolean("loop_casting_active", true);
            player.getPersistentData().putLong("loop_casting_end", data.loopCastingEndTime);

            // TODO: [LOOP CASTING - INSTANT CAST INTEGRATION]
            // Hook point: IronSpellbooks spell casting system
            // Expected behavior: All spells cast during Loop Casting should be instant (0 cast time)
            // Implementation location: Hook into spell pre-cast event or spell casting attribute modification
            // Check: player.getPersistentData().getBoolean("loop_casting_active")
            // Effect: Override spell cast time to 0 for all spells while active

            // TODO: [LOOP CASTING - COOLDOWN REDUCTION]
            // Hook point: Talent cooldown system (TalentsCapabilities)
            // Expected behavior: 200% cooldown reduction = 3x faster cooldowns (divide cooldown by 3)
            // Implementation location: Hook into talent cooldown application/tick
            // Check: player.getPersistentData().getBoolean("loop_casting_active")
            // Effect: When Loop Casting is active, multiply cooldown reduction by 3x
            // Example: Normal cooldown 60s -> with Loop Casting: 60s / 3 = 20s

            // TODO: [LOOP CASTING - MANA REGENERATION BOOST]
            // Hook point: Mana regeneration system (if using IronSpellbooks mana system)
            // Expected behavior: 500% mana regeneration = 6x faster mana regen (multiply by 6)
            // Implementation location: Hook into mana regeneration tick event
            // Check: player.getPersistentData().getBoolean("loop_casting_active")
            // Effect: Multiply base mana regeneration rate by 6 while Loop Casting is active
            // Example: Normal regen 10 mana/sec -> with Loop Casting: 60 mana/sec

            // Send feedback to player
            player.displayClientMessage(
                Component.translatable("talent.complextalents.elemental_attunement.loop_casting_activated")
                    .withStyle(ChatFormatting.GOLD), true);

            return true;
        }
        return false;
    }

    /**
     * Check if Overflow should trigger (Rank 3B) and activate Resonant Cascade (Rank 4B)
     */
    public static boolean checkOverflowTrigger(ServerPlayer player, int level) {
        ResourceLocation talentId = ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_attunement");

        // Check if player has Rank 3B: Overflow
        if (level >= 3 && TalentBranches.hasBranch(player, talentId, 3, TalentBranches.BranchChoice.PATH_B)) {
            float chance = OVERFLOW_CHANCE[Math.min(level - 1, 3)];
            boolean overflowTriggered = Math.random() < chance;

            if (overflowTriggered) {
                // Check if player also has Rank 4B: Resonant Cascade
                if (level >= 4 && TalentBranches.hasBranch(player, talentId, 4, TalentBranches.BranchChoice.PATH_B)) {
                    // Grant Resonant Cascade buff for 10 seconds
                    long expirationTime = player.level().getGameTime() + 200; // 10 seconds in ticks
                    player.getPersistentData().putBoolean("resonant_cascade_active", true);
                    player.getPersistentData().putLong("resonant_cascade_expiration", expirationTime);

                    // Send feedback to player
                    player.displayClientMessage(
                        Component.translatable("talent.complextalents.elemental_attunement.resonant_cascade_activated")
                            .withStyle(ChatFormatting.LIGHT_PURPLE), true);

                    TalentsMod.LOGGER.info("Resonant Cascade activated for {} (expires at {})",
                        player.getName().getString(), expirationTime);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Get maximum Focus bonus from Reservoir (Rank 3A)
     */
    public static float getReservoirBonus(ServerPlayer player) {
        return player.getCapability(TalentsCapabilities.PLAYER_TALENTS).map(talents -> {
            ResourceLocation talentId = ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_attunement");
            int level = talents.getTalentLevel(talentId);

            // Check if player has Rank 3A: Reservoir
            if (level >= 3 && TalentBranches.hasBranch(player, talentId, 3, TalentBranches.BranchChoice.PATH_A)) {
                return RESERVOIR_BONUS[Math.min(level - 1, 3)];
            }

            return 0f;
        }).orElse(0f);
    }

    @Override
    public boolean hasBranchingAtRank(int rank) {
        return rank == 2 || rank == 3 || rank == 4;
    }

    @Override
    public Component getBranchDescription(int rank, TalentBranches.BranchChoice choice) {
        String key = String.format("talent.complextalents.elemental_attunement.rank%d.%s.desc",
            rank, choice == TalentBranches.BranchChoice.PATH_A ? "a" : "b");
        return Component.translatable(key);
    }

    @Override
    public Component getBranchName(int rank, TalentBranches.BranchChoice choice) {
        String key = String.format("talent.complextalents.elemental_attunement.rank%d.%s",
            rank, choice == TalentBranches.BranchChoice.PATH_A ? "a" : "b");
        return Component.translatable(key);
    }

    /**
     * Internal class to track resonance data
     */
    private static class ResonanceData {
        int stacks = 0;
        String lastElement = "";
        long lastStackTime = 0;
        boolean loopCastingActive = false;
        long loopCastingEndTime = 0;

        void addStack() {
            stacks = Math.min(stacks + 1, 3);
        }

        void decay() {
            // Decay after 5 seconds of no new stacks
            if (System.currentTimeMillis() - lastStackTime > 5000 && stacks > 0) {
                stacks--;
                lastStackTime = System.currentTimeMillis();
            }
        }

        void updateLoopCasting() {
            if (loopCastingActive && System.currentTimeMillis() > loopCastingEndTime) {
                loopCastingActive = false;
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResonanceData data = resonanceStacks.get(player.getUUID());
            if (data != null) {
                data.decay();
                data.updateLoopCasting();
            }
        }
    }
}