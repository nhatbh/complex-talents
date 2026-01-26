package com.complextalents.util;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class SoundHelper {
        /**
     * Plays a stacked explosion sound for powerful effects
     */
    public static void playStackedExplosionSound(Level level, Vec3 pos, int stacks, float pitchMultiplier) {
        float baseVolume = 0.5f;
        float totalVolume = Math.min(baseVolume * stacks * 0.7f, 2.0f); // Cap at 2.0
        float pitch = 0.9f + (stacks * 0.1f) * pitchMultiplier;

        level.playLocalSound(pos.x, pos.y, pos.z,
            SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
            totalVolume, pitch, false);
    }

    /**
     * Plays a stacked sound effect
     */
    public static void playStackedSound(Level level, Vec3 pos, SoundEvent sound, int stacks, float baseVolume, float pitch) {
        float totalVolume = Math.min(baseVolume * stacks * 0.6f, 1.5f);
        float finalPitch = pitch + (stacks * 0.05f);

        level.playLocalSound(pos.x, pos.y, pos.z,
            sound, SoundSource.PLAYERS,
            totalVolume, finalPitch, false);
    }
}
