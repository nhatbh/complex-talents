package com.complextalents.impl.highpriest.entity;

import com.complextalents.TalentsMod;
import com.complextalents.impl.highpriest.sound.HighPriestSounds;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for custom entity types used by the High Priest origin.
 */
public class HighPriestEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, TalentsMod.MODID);

    public static final RegistryObject<EntityType<DivinePunisherEntity>> DIVINE_PUNISHER =
            ENTITY_TYPES.register("divine_punisher",
                    () -> EntityType.Builder.<DivinePunisherEntity>of(DivinePunisherEntity::new, MobCategory.MISC)
                            .sized(0.8f, 0.8f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .fireImmune()
                            .build("divine_punisher"));

    public static final RegistryObject<EntityType<SeraphsBouncingSwordEntity>> SERAPHS_BOUNCING_SWORD =
            ENTITY_TYPES.register("seraphs_bouncing_sword",
                    () -> EntityType.Builder.<SeraphsBouncingSwordEntity>of(SeraphsBouncingSwordEntity::new, MobCategory.MISC)
                            .sized(0.8f, 0.8f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .fireImmune()
                            .build("seraphs_bouncing_sword"));

    public static final RegistryObject<EntityType<SanctuaryBarrierEntity>> SANCTUARY_BARRIER =
            ENTITY_TYPES.register("sanctuary_barrier",
                    () -> EntityType.Builder.<SanctuaryBarrierEntity>of(SanctuaryBarrierEntity::new, MobCategory.MISC)
                            .sized(1.0f, 1.0f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .fireImmune()
                            .build("sanctuary_barrier"));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        HighPriestSounds.register(modEventBus);
        TalentsMod.LOGGER.info("Registered High Priest entity types");
    }
}
