package com.complextalents.impl.highpriest.effect;

import com.complextalents.TalentsMod;
import com.complextalents.origin.OriginManager;
import com.complextalents.skill.capability.IPlayerSkillData;
import com.complextalents.skill.capability.SkillDataProvider;
import com.complextalents.skill.event.SkillToggleTerminationEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.MinecraftForge;

import java.util.UUID;

/**
 * Event handler for Divine Ascendance aura mechanics.
 * <p>
 * Handles:
 * <ul>
 *   <li>AoE buff application to nearby allies (50 block radius)</li>
 *   <li>Piety regeneration when allies kill enemies</li>
 *   <li>Auto-toggle off when Piety reaches 0</li>
 *   <li>Cleanup on deactivation/disconnect</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class DivineAscendanceAuraHandler {

    // Aura configuration
    private static final double AURA_RADIUS = 50.0;
    private static final double PIETY_REGEN_ON_KILL = 5.0;
    private static final double PIETY_DRAIN_PER_SECOND = 10.0;
    private static final int TICK_CHECK_INTERVAL = 2;  // Check every 2 ticks

    // Skill ID constant
    public static final ResourceLocation SKILL_ID = ResourceLocation.fromNamespaceAndPath("complextalents", "divine_ascendance");


    /**
     * Server tick handler - maintains aura buffs and checks Piety.
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.getServer();
        long gameTime = server.getTickCount();

        // Only check every N ticks for performance
        if (gameTime % TICK_CHECK_INTERVAL != 0) {
            return;
        }

        // Check all levels for priests with active Divine Ascendance
        for (ServerLevel level : server.getAllLevels()) {
            for (Player player : level.players()) {
                if (!(player instanceof ServerPlayer priest)) {
                    continue;
                }
                if (!priest.isAlive()) {
                    continue;
                }

                // Check if this priest has Divine Ascendance active
                if (!isDivineAscendanceActive(priest)) {
                    continue;
                }

                // Get skill level for effect amplifier
                int skillLevel = getSkillLevel(priest);
                int amplifier = Math.max(0, skillLevel - 1);

                // Drain Piety per second (10 Piety/sec = 0.5 Piety/tick)
                // We check every 2 ticks, so deduct (10 / 20 * 2) = 1.0 Piety per check
                double pietyToDrain = (PIETY_DRAIN_PER_SECOND / 20.0) * TICK_CHECK_INTERVAL;
                OriginManager.modifyResource(priest, -pietyToDrain);

                // Check Piety - auto-toggle off if empty (after drain)
                double currentPiety = OriginManager.getResource(priest);
                if (currentPiety <= 0) {
                    // Fire termination event
                    MinecraftForge.EVENT_BUS.post(new SkillToggleTerminationEvent(
                            priest,
                            SKILL_ID,
                            SkillToggleTerminationEvent.TerminationReason.INSUFFICIENT_RESOURCE
                    ));
                    continue;
                }

                // Apply/refresh buff to all nearby players
                applyAuraToNearbyPlayers(priest, level, amplifier);
            }
        }
    }

    /**
     * Handle death events for Piety regeneration.
     * Grants 5 Piety when ANY ally (or the priest) kills an enemy within 50 blocks.
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        LivingEntity victim = event.getEntity();

        // Don't reward for player deaths (PVP protection)
        if (victim instanceof Player) {
            return;
        }

        // Find the killer
        LivingEntity killer = victim.getKillCredit();
        if (killer == null) {
            // Try to get from damage source if kill credit is null
            var sourceEntity = event.getSource().getEntity();
            if (sourceEntity instanceof LivingEntity living) {
                killer = living;
            } else {
                return;
            }
        }

        // Find all High Priests with active Divine Ascendance
        for (ServerLevel serverLevel : level.getServer().getAllLevels()) {
            for (Player player : serverLevel.players()) {
                if (!(player instanceof ServerPlayer priest)) {
                    continue;
                }
                if (!priest.isAlive()) {
                    continue;
                }

                if (!isDivineAscendanceActive(priest)) {
                    continue;
                }

                // Check distance - killer must be within 50 blocks of the priest
                double distance = killer.position().distanceTo(priest.position());
                if (distance <= AURA_RADIUS) {
                    // Grant Piety
                    OriginManager.modifyResource(priest, PIETY_REGEN_ON_KILL);

                    // Feedback message (optional, can be spammy in combat)
                    // priest.sendSystemMessage(Component.literal(
                    //     "\u00A7e+5 Piety from ally kill within Divine Ascendance"
                    // ));

                    TalentsMod.LOGGER.debug("Divine Ascendance: Priest {} gained {} Piety from kill by {} at distance {}",
                            priest.getName().getString(), PIETY_REGEN_ON_KILL, killer.getName().getString(), distance);
                }
            }
        }
    }

    /**
     * Handle player logout - clean up any effects this player is maintaining.
     */
    @SubscribeEvent
    public static void onPlayerLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer priest)) {
            return;
        }

        // If this priest has Divine Ascendance active, remove it from all buffed players
        if (isDivineAscendanceActive(priest)) {
            removeBuffFromAllPlayers(priest);
        }
    }

    /**
     * Apply the aura buff to all nearby players.
     */
    private static void applyAuraToNearbyPlayers(ServerPlayer priest, ServerLevel level, int amplifier) {
        // Find all players within radius
        for (var ally : level.getEntitiesOfClass(ServerPlayer.class,
                priest.getBoundingBox().inflate(AURA_RADIUS))) {

            // Apply or refresh the buff effect
            MobEffectInstance effectInstance = new MobEffectInstance(
                    HighPriestEffects.DIVINE_ASCENDANCE.get(),
                    100,  // 5 seconds, refreshes every check
                    amplifier,
                    false,  // No particles (too spammy for AoE)
                    true    // Show in HUD
            );

            // Store the caster UUID on the ally
            DivineAscendanceEffect.setCasterUUID(ally, priest.getUUID());

            ally.addEffect(effectInstance);
        }
    }

    /**
     * Remove the buff from all players who were buffed by this priest.
     */
    public static void removeBuffFromAllPlayers(ServerPlayer priest) {
        // Check all players in all levels
        for (ServerLevel level : priest.getServer().getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                // Check if this player has the buff from this priest
                if (player.hasEffect(HighPriestEffects.DIVINE_ASCENDANCE.get())) {
                    UUID caster = DivineAscendanceEffect.getCasterUUID(player);
                    if (caster != null && caster.equals(priest.getUUID())) {
                        player.removeEffect(HighPriestEffects.DIVINE_ASCENDANCE.get());
                    }
                }
            }
        }
    }

    /**
     * Check if a priest has Divine Ascendance active.
     */
    public static boolean isDivineAscendanceActive(ServerPlayer priest) {
        IPlayerSkillData data = priest.getCapability(SkillDataProvider.SKILL_DATA).orElse(null);
        if (data == null) {
            return false;
        }
        return data.isToggleActive(SKILL_ID);
    }

    /**
     * Get the skill level for Divine Ascendance.
     */
    private static int getSkillLevel(ServerPlayer priest) {
        IPlayerSkillData data = priest.getCapability(SkillDataProvider.SKILL_DATA).orElse(null);
        if (data == null) {
            return 1;
        }
        return data.getSkillLevel(SKILL_ID);
    }
}
