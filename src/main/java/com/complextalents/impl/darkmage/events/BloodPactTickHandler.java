package com.complextalents.impl.darkmage.events;

import com.complextalents.TalentsMod;
import com.complextalents.impl.darkmage.data.SoulData;
import com.complextalents.impl.darkmage.origin.DarkMageOrigin;
import com.complextalents.impl.darkmage.skill.BloodPactSkill;
import com.complextalents.origin.OriginManager;
import com.complextalents.skill.capability.IPlayerSkillData;
import com.complextalents.skill.capability.SkillDataProvider;
import com.complextalents.skill.event.SkillToggleTerminationEvent;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event handler for Blood Pact ongoing effects.
 * <p>
 * Handles:
 * <ul>
 *   <li>HP drain per second</li>
 *   <li>Soul-scaled mana regeneration</li>
 *   <li>Auto-deactivate at critical HP</li>
 *   <li>Soul damage bonus during Blood Pact</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class BloodPactTickHandler {

    // Check every 2 ticks for performance
    private static final int TICK_INTERVAL = 2;

    // Tracks the server tick when each player activated Blood Pact
    private static final ConcurrentHashMap<UUID, Long> activationStartTick = new ConcurrentHashMap<>();

    /**
     * Server tick handler for Blood Pact effects.
     * - HP drain per second
     * - Auto-deactivate if HP drops to critical
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.getServer();
        long gameTime = server.getTickCount();

        // Only check every N ticks for performance
        if (gameTime % TICK_INTERVAL != 0) {
            return;
        }

        // Check all levels for Dark Mages with active Blood Pact
        for (ServerLevel level : server.getAllLevels()) {
            for (Player player : level.players()) {
                if (!(player instanceof ServerPlayer serverPlayer)) {
                    continue;
                }
                if (!serverPlayer.isAlive()) {
                    continue;
                }
                if (!DarkMageOrigin.isDarkMage(serverPlayer)) {
                    continue;
                }
                if (!isBloodPactActive(serverPlayer)) {
                    // Clean up activation time if Blood Pact is no longer active
                    activationStartTick.remove(serverPlayer.getUUID());
                    continue;
                }

                // Record activation start tick the first time we see Blood Pact active
                UUID playerId = serverPlayer.getUUID();
                activationStartTick.putIfAbsent(playerId, gameTime);
                long startTick = activationStartTick.get(playerId);

                // Exponential drain: doubles every 20 seconds (2^(t/20))
                double elapsedSeconds = (gameTime - startTick) / 20.0;
                double exponentialMultiplier = Math.pow(2.0, elapsedSeconds / 20.0);
                
                // Soul scaling: increases by 1.5x every 20 seconds (1.5^(t/20))
                double soulEffectMultiplier = Math.pow(1.5, elapsedSeconds / 20.0);

                // Update multipliers in SoulData for HUD sync
                SoulData.setBloodPactMultipliers(serverPlayer.getUUID(), (float) exponentialMultiplier, (float) soulEffectMultiplier);

                // Update attribute bonuses periodically (every 10 ticks = 0.5s) to reflect soul scaling
                if (gameTime % 10 == 0) {
                    BloodPactSkill.updateScaledBonuses(serverPlayer, soulEffectMultiplier);
                    // Also sync to client when multipliers change significantly or periodically
                    SoulData.syncToClient(serverPlayer);
                }

                // Get base drain rate from scaled stats and apply exponential multiplier
                double drainPerSecond = OriginManager.getOriginStat(serverPlayer, "bloodPactHpDrainPercent");
                // Convert to per-tick (20 ticks/sec) and multiply by interval and exponential factor
                float hpToDrain = (float) (serverPlayer.getMaxHealth() * drainPerSecond * exponentialMultiplier / 20.0 * TICK_INTERVAL);

                // Check if this would kill the player (leave at 1 HP minimum)
                if (serverPlayer.getHealth() - hpToDrain <= 1.0f) {
                    // Auto-deactivate at 1 HP
                    activationStartTick.remove(serverPlayer.getUUID());
                    MinecraftForge.EVENT_BUS.post(new SkillToggleTerminationEvent(
                            serverPlayer,
                            BloodPactSkill.ID,
                            SkillToggleTerminationEvent.TerminationReason.INSUFFICIENT_RESOURCE
                    ));
                    serverPlayer.sendSystemMessage(Component.literal(
                            "\u00A7cBlood Pact deactivated - HP critical!"
                    ));
                    continue;
                }

                // Drain HP
                serverPlayer.setHealth(serverPlayer.getHealth() - hpToDrain);



                // Bleeding particle effect every 10 ticks
                if (gameTime % 10 == 0) {
                    spawnBleedingParticles(serverPlayer.serverLevel(), serverPlayer);
                }
            }
        }
    }



    /**
     * Spawn two-layer blood particle effect on the player while Blood Pact is active.
     * Matches the assassin backstab blood fx style.
     */
    private static void spawnBleedingParticles(ServerLevel level, ServerPlayer player) {
        double x = player.getX();
        double y = player.getY() + player.getBbHeight() / 2.0;
        double z = player.getZ();

        // Chunky blood splatter (redstone block texture)
        BlockParticleOption bloodSplatter = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.REDSTONE_BLOCK.defaultBlockState());
        level.sendParticles(bloodSplatter, x, y, z, 8, 0.3, 0.4, 0.3, 0.1);

        // Fine blood mist (dark red dust)
        DustParticleOptions bloodMist = new DustParticleOptions(new Vector3f(0.6f, 0.0f, 0.0f), 1.2f);
        level.sendParticles(bloodMist, x, y, z, 5, 0.25, 0.35, 0.25, 0.03);
    }

    /**
     * Check if a player has Blood Pact active.
     */
    public static boolean isBloodPactActive(ServerPlayer player) {
        IPlayerSkillData data = player.getCapability(SkillDataProvider.SKILL_DATA).orElse(null);
        if (data == null) {
            return false;
        }
        return data.isToggleActive(BloodPactSkill.ID);
    }
}
