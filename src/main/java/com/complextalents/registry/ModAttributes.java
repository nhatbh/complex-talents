package com.complextalents.registry;

import com.complextalents.TalentsMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Common registry for mod-specific attributes used by various modules.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, TalentsMod.MODID);

    public static final RegistryObject<Attribute> SPELL_CRIT_CHANCE = ATTRIBUTES.register(
            "spell_crit_chance",
            () -> new RangedAttribute(
                    "attribute.complextalents.spell_crit_chance",
                    0.0,
                    0.0,
                    1.0
            ).setSyncable(true)
    );

    public static final RegistryObject<Attribute> SPELL_CRIT_DAMAGE = ATTRIBUTES.register(
            "spell_crit_damage",
            () -> new RangedAttribute(
                    "attribute.complextalents.spell_crit_damage",
                    1.5,
                    1.0,
                    100.0
            ).setSyncable(true)
    );

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
    }

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, SPELL_CRIT_CHANCE.get());
        event.add(EntityType.PLAYER, SPELL_CRIT_DAMAGE.get());
    }
}
