package com.complextalents.elemental.talents.mage.ward;

import com.complextalents.TalentsMod;
import com.complextalents.capability.TalentsCapabilities;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalStackManager;
import com.complextalents.elemental.attributes.MasteryAttributes;
import com.complextalents.elemental.entity.ElementalOrbEntity;
import com.complextalents.talent.BranchingActiveTalent;
import com.complextalents.talent.TalentBranches;
import com.complextalents.talent.TalentSlotType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

/**
 * Elemental Ward - Defensive and Utility skill tree
 * Resonance Slot talent for damage mitigation
 */
public class ElementalWardTalent extends BranchingActiveTalent {

    // Ward state tracking
    private static final Map<UUID, WardState> wardStates = new HashMap<>();

    // Base values per rank
    private static final float[] BLOCK_DURATION = {1.5f, 1.75f, 2.0f, 2.5f};
    private static final int[] COOLDOWN_SECONDS = {18, 16, 14, 12};

    // Rank 2 branch values
    private static final float[] PRISMATIC_THIRD_CHANCE = {0.15f, 0.25f, 0.35f, 0.50f};
    private static final float[] VOLATILE_FOCUS_GAIN = {40f, 55f, 70f, 90f};

    // Rank 3 branch values
    private static final float[] HARMONY_DAMAGE_BOOST = {0.20f, 0.25f, 0.35f, 0.50f};
    private static final int[] REPRISAL_ORB_COUNT = {2, 3, 4, 5};
    private static final float[] REPRISAL_ORB_DAMAGE = {5f, 10f, 15f, 25f};

    // Random for element selection
    private static final Random random = new Random();
    private static final ElementType[] ALL_ELEMENTS = ElementType.values();

    public ElementalWardTalent() {
        super(
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_ward"),
            Component.translatable("talent.complextalents.elemental_ward"),
            Component.translatable("talent.complextalents.elemental_ward.desc"),
            4,
            TalentSlotType.RESONANCE,
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_mage_definition"),
            COOLDOWN_SECONDS[0] * 20 // Base cooldown in ticks
        );

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        // Initialize ward state
        wardStates.putIfAbsent(player.getUUID(), new WardState());

        TalentsMod.LOGGER.info("Player {} unlocked Elemental Ward at level {}",
            player.getName().getString(), level);
    }

    @Override
    public void onRemove(ServerPlayer player) {
        wardStates.remove(player.getUUID());
    }

    @Override
    public void onActivate(ServerPlayer player, int level) {
        WardState state = wardStates.computeIfAbsent(player.getUUID(), k -> new WardState());

        // Activate ward
        state.isActive = true;
        state.level = level;
        state.activationTime = System.currentTimeMillis();
        state.duration = (long)(BLOCK_DURATION[Math.min(level - 1, 3)] * 1000);

        player.sendSystemMessage(Component.translatable("message.complextalents.ward_activated"));

        // Set cooldown
        player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            int cooldownTicks = COOLDOWN_SECONDS[Math.min(level - 1, 3)] * 20;
            talents.setTalentCooldown(this.getId(), cooldownTicks);
        });

        TalentsMod.LOGGER.debug("Elemental Ward activated for {} at level {}",
            player.getName().getString(), level);
    }

    @SubscribeEvent
    public void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        WardState state = wardStates.get(player.getUUID());
        if (state != null && state.isActive && !state.hasExpired()) {
            // Block the damage
            event.setCanceled(true);

            // Apply elemental stacks to attacker
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                applyWardEffects(player, attacker, state.level);
            }

            // Deactivate ward after successful block
            state.isActive = false;
            state.successfulBlock = true;

            player.sendSystemMessage(Component.translatable("message.complextalents.ward_blocked"));
        }
    }

    private void applyWardEffects(ServerPlayer player, LivingEntity attacker, int level) {
        // Get unapplied elements on the attacker
        Map<ElementType, Integer> existingStacks = ElementalStackManager.getEntityStacks(attacker.getUUID());
        List<ElementType> unappliedElements = new ArrayList<>();

        for (ElementType element : ALL_ELEMENTS) {
            if (!existingStacks.containsKey(element)) {
                unappliedElements.add(element);
            }
        }

        // Apply at least one random unapplied element
        if (!unappliedElements.isEmpty()) {
            ElementType firstElement = unappliedElements.get(random.nextInt(unappliedElements.size()));
            ElementalStackManager.applyElementStack(attacker, firstElement, player, 0);

            // Check for Rank 2A: Prismatic Aegis (chance for 2nd and 3rd stacks)
            if (level >= 2) {
                // Second stack guaranteed for Prismatic Aegis
                unappliedElements.remove(firstElement);
                if (!unappliedElements.isEmpty()) {
                    ElementType secondElement = unappliedElements.get(random.nextInt(unappliedElements.size()));
                    ElementalStackManager.applyElementStack(attacker, secondElement, player, 0);

                    // Chance for third stack
                    float thirdChance = PRISMATIC_THIRD_CHANCE[Math.min(level - 1, 3)];
                    if (random.nextFloat() < thirdChance) {
                        unappliedElements.remove(secondElement);
                        if (!unappliedElements.isEmpty()) {
                            ElementType thirdElement = unappliedElements.get(random.nextInt(unappliedElements.size()));
                            ElementalStackManager.applyElementStack(attacker, thirdElement, player, 0);
                        }
                    }
                }
            }

            // Check for Rank 2B: Volatile Conduit (Focus generation)
            if (level >= 2) {
                player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
                    float focusGain = VOLATILE_FOCUS_GAIN[Math.min(level - 1, 3)];
                    talents.addResource(focusGain);
                });
            }

            // Check for Rank 3A: Elemental Harmony (damage boost)
            if (level >= 3) {
                WardState state = wardStates.get(player.getUUID());
                state.harmonyBoostEndTime = System.currentTimeMillis() + 5000; // 5 seconds
                state.harmonyBoostMultiplier = 1f + HARMONY_DAMAGE_BOOST[Math.min(level - 1, 3)];
            }

            // Check for Rank 3B: Reprisal (spawn elemental orbs)
            if (level >= 3) {
                spawnElementalOrbs(player, level);
            }
        }
    }

    private void spawnElementalOrbs(ServerPlayer player, int level) {
        WardState state = wardStates.get(player.getUUID());

        // Check if player has Rank 3B: Reprisal
        ResourceLocation talentId = ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_ward");
        if (!TalentBranches.hasBranch(player, talentId, 3, TalentBranches.BranchChoice.PATH_B)) {
            return; // Only spawn orbs if player has Path B
        }

        int orbCount = REPRISAL_ORB_COUNT[Math.min(level - 1, 3)];
        float baseDamage = REPRISAL_ORB_DAMAGE[Math.min(level - 1, 3)];

        // Create orbs with random elements
        state.elementalOrbs.clear();
        Set<ElementType> usedElements = new HashSet<>();

        // Calculate starting angle offset for even distribution
        float angleStep = (float)(Math.PI * 2.0 / orbCount);

        for (int i = 0; i < orbCount; i++) {
            // Pick a random element that hasn't been used yet
            ElementType element;
            do {
                element = ALL_ELEMENTS[random.nextInt(ALL_ELEMENTS.length)];
            } while (usedElements.contains(element) && usedElements.size() < ALL_ELEMENTS.length);

            usedElements.add(element);
            state.elementalOrbs.add(new ElementalOrb(element, 600)); // 30 seconds duration

            // Spawn actual orb entity
            float startAngle = i * angleStep;
            ElementalOrbEntity orbEntity = new ElementalOrbEntity(
                player.level(),
                player,
                element,
                baseDamage,
                startAngle
            );

            player.level().addFreshEntity(orbEntity);
        }

        TalentsMod.LOGGER.info("Spawned {} elemental orbs for {} with base damage {}",
            orbCount, player.getName().getString(), baseDamage);
    }

    /**
     * Get the current Harmony damage boost multiplier
     */
    public static float getHarmonyBoost(ServerPlayer player) {
        WardState state = wardStates.get(player.getUUID());
        if (state != null && state.harmonyBoostEndTime > System.currentTimeMillis()) {
            return state.harmonyBoostMultiplier;
        }
        return 1.0f;
    }

    /**
     * Check if Perfect Counter window is active (Rank 4B)
     */
    public static boolean isPerfectCounterActive(ServerPlayer player) {
        WardState state = wardStates.get(player.getUUID());
        if (state != null && state.isActive && state.level >= 4) {
            // Perfect Counter reduces block window to 0.5 seconds
            long elapsedTime = System.currentTimeMillis() - state.activationTime;
            return elapsedTime <= 500; // 0.5 seconds
        }
        return false;
    }

    /**
     * Internal state tracking for Elemental Ward
     */
    private static class WardState {
        boolean isActive = false;
        int level = 0;
        long activationTime = 0;
        long duration = 0;
        boolean successfulBlock = false;

        // Rank 3A: Elemental Harmony
        long harmonyBoostEndTime = 0;
        float harmonyBoostMultiplier = 1.0f;

        // Rank 3B: Reprisal orbs
        List<ElementalOrb> elementalOrbs = new ArrayList<>();

        // Rank 4A: The Force
        boolean forceActive = false;

        boolean hasExpired() {
            return System.currentTimeMillis() > (activationTime + duration);
        }
    }

    /**
     * Represents an elemental orb circling the player
     */
    private static class ElementalOrb {
        final ElementType element;
        int remainingTicks;

        ElementalOrb(ElementType element, int durationTicks) {
            this.element = element;
            this.remainingTicks = durationTicks;
        }
    }

    @Override
    public boolean hasBranchingAtRank(int rank) {
        return rank == 2 || rank == 3 || rank == 4;
    }

    @Override
    public Component getBranchDescription(int rank, TalentBranches.BranchChoice choice) {
        String key = String.format("talent.complextalents.elemental_ward.rank%d.%s.desc",
            rank, choice == TalentBranches.BranchChoice.PATH_A ? "a" : "b");
        return Component.translatable(key);
    }

    @Override
    public Component getBranchName(int rank, TalentBranches.BranchChoice choice) {
        String key = String.format("talent.complextalents.elemental_ward.rank%d.%s",
            rank, choice == TalentBranches.BranchChoice.PATH_A ? "a" : "b");
        return Component.translatable(key);
    }
}