package com.complextalents.network;

import com.complextalents.TalentsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Utility class for sending network packets.
 */
public class Messages {

    /**
     * Sends a packet to all players tracking a specific position in a level.
     */
    public static void sendToAllTracking(Object packet, Level level, Vec3 pos) {
        if (level instanceof ServerLevel serverLevel) {
            PacketHandler.sendToNearby(packet, serverLevel, pos);
        }
    }

    /**
     * Spawns a AAA particle effect with default scale and no rotation.
     */
    public static void spawnAAAParticle(Level level, Vec3 pos, String effectName) {
        spawnAAAParticle(level, pos, effectName, new Vector3f(0), 1.0f);
    }

    /**
     * Spawns a AAA particle effect with custom scale and rotation.
     */
    public static void spawnAAAParticle(Level level, Vec3 pos, String effectName, Vector3f rotation, float scale) {
        if (level.isClientSide)
            return;

        sendToAllTracking(
                new S2CSpawnAAAParticlePacket(
                        ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, effectName),
                        pos, rotation, scale),
                level, pos);
    }
}
