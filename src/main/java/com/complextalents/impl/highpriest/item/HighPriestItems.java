package com.complextalents.impl.highpriest.item;

import com.complextalents.TalentsMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registry for High Priest items.
 * <p>
 * These items are primarily used for model baking by entity renderers.
 */
public class HighPriestItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TalentsMod.MODID);

    public static final RegistryObject<Item> DIVINE_PUNISHER =
            ITEMS.register("divinepunisher", DivinePunisherItem::new);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        TalentsMod.LOGGER.info("Registered High Priest items");
    }
}
