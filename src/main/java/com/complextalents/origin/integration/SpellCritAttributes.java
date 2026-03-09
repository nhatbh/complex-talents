package com.complextalents.origin.integration;

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

@Mod.EventBusSubscriber(modid = TalentsMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class SpellCritAttributes {

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, TalentsMod.MODID);

    /**
     * Spell crit chance: 0.0 = 0%, 1.0 = 100%.
     * All players start at 0% crit chance.
     */
    public static final RegistryObject<Attribute> SPELL_CRIT_CHANCE = ATTRIBUTES.register(
            "spell_crit_chance",
            () -> new RangedAttribute(
                    "attribute.complextalents.spell_crit_chance",
                    0.0,  // default: 0% crit chance
                    0.0,  // min
                    1.0   // max: 100%
            ).setSyncable(true)
    );

    /**
     * Spell crit damage multiplier: 1.5 = 150% damage.
     * All players start at 150% crit damage.
     */
    public static final RegistryObject<Attribute> SPELL_CRIT_DAMAGE = ATTRIBUTES.register(
            "spell_crit_damage",
            () -> new RangedAttribute(
                    "attribute.complextalents.spell_crit_damage",
                    1.5,   // default: 150% damage multiplier
                    1.0,   // min
                    100.0  // max
            ).setSyncable(true)
    );

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        TalentsMod.LOGGER.info("Registered spell crit attributes");
    }

    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, SPELL_CRIT_CHANCE.get());
        event.add(EntityType.PLAYER, SPELL_CRIT_DAMAGE.get());
    }
}
