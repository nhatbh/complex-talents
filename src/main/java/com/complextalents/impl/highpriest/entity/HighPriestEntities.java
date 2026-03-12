package com.complextalents.impl.highpriest.entity;

import com.complextalents.TalentsMod;
import com.complextalents.impl.highpriest.sound.HighPriestSounds;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
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

    public static final RegistryObject<EntityType<SeraphsEdgeEntity>> SERAPHS_EDGE =
            ENTITY_TYPES.register("seraphs_edge",
                    () -> EntityType.Builder.<SeraphsEdgeEntity>of(SeraphsEdgeEntity::new, MobCategory.MISC)
                            .sized(0.8f, 0.8f)
                            .clientTrackingRange(8)
                            .updateInterval(1)
                            .fireImmune()
                            .build("seraphs_edge"));



    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        HighPriestSounds.register(modEventBus);
        modEventBus.addListener(HighPriestEntities::registerAttributes);
        TalentsMod.LOGGER.info("Registered High Priest entity types");
    }

    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put((EntityType<? extends LivingEntity>) (EntityType<?>) DIVINE_PUNISHER.get(), LivingEntity.createLivingAttributes().build());
        event.put((EntityType<? extends LivingEntity>) (EntityType<?>) SERAPHS_EDGE.get(), LivingEntity.createLivingAttributes().build());
    }
}
