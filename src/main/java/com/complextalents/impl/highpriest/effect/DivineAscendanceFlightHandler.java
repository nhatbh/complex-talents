package com.complextalents.impl.highpriest.effect;

import com.complextalents.TalentsMod;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Server-side handler for Divine Ascendance flight mechanics.
 * <p>
 * Handles:
 * - Enabling flight for survival players with the effect
 * - Limiting maximum height to 5-8 blocks above ground
 * - Disabling flight when effect ends
 * </p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class DivineAscendanceFlightHandler {

    // Maximum height above ground (in blocks)
    private static final double MAX_HEIGHT_ABOVE_GROUND = 6.5D;

    // Distance to raytrace downward for ground detection
    private static final double GROUND_RAYTRACE_DISTANCE = 20.0D;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (event.side.isClient()) {
            return;
        }

        Player player = event.player;
        Level level = player.level();

        boolean hasEffect = player.hasEffect(HighPriestEffects.DIVINE_ASCENDANCE_FLIGHT.get());

        if (hasEffect) {
            enableFlight(player);
            limitHeight(player, level);
        } else {
            // Remove flight only if they are NOT creative/spectator
            if (!player.isCreative() && !player.isSpectator()) {
                disableFlight(player);
            }
        }
    }

    /**
     * Enables flight for the player if not already enabled.
     */
    private static void enableFlight(Player player) {
        if (!player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            player.onUpdateAbilities();
        }
    }

    /**
     * Disables flight for the player.
     */
    private static void disableFlight(Player player) {
        if (player.getAbilities().mayfly) {
            player.getAbilities().flying = false;
            player.getAbilities().mayfly = false;
            player.onUpdateAbilities();
        }
    }

    /**
     * Limits the player's height to MAX_HEIGHT_ABOVE_GROUND above the nearest ground block.
     * Uses raycasting for efficient ground detection.
     */
    private static void limitHeight(Player player, Level level) {
        if (!player.getAbilities().flying) {
            return;
        }

        Vec3 start = player.position();
        Vec3 end = start.subtract(0, GROUND_RAYTRACE_DISTANCE, 0);

        ClipContext context = new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player
        );

        HitResult hit = level.clip(context);

        if (hit.getType() != HitResult.Type.BLOCK) {
            return; // No ground detected below
        }

        double groundY = hit.getLocation().y;
        double maxY = groundY + MAX_HEIGHT_ABOVE_GROUND;

        if (player.getY() >= maxY) {
            // Kill upward velocity smoothly and push down
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x, -0.1, motion.z);

            // Clamp position to just below max
            player.setPos(player.getX(), maxY - 0.01, player.getZ());
        }
    }
}
