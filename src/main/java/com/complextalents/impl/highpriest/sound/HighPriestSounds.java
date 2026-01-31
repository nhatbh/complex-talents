package com.complextalents.impl.highpriest.sound;

import com.complextalents.TalentsMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom sound events used by the High Priest origin.
 */
public class HighPriestSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TalentsMod.MODID);

    public static final RegistryObject<SoundEvent> SWORD_CLANG =
            SOUND_EVENTS.register("highpriest.sword_clang",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "highpriest.sword_clang")));

    public static final RegistryObject<SoundEvent> SWORD_HIT =
            SOUND_EVENTS.register("highpriest.sword_hit",
                    () -> SoundEvent.createVariableRangeEvent(
                            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "highpriest.sword_hit")));

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
        TalentsMod.LOGGER.info("Registered High Priest sound events");
    }
}
