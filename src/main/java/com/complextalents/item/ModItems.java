package com.complextalents.item;

import com.complextalents.TalentsMod;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TalentsMod.MODID);

    public static final RegistryObject<Item> INFERNO_TESTER = ITEMS.register("inferno_tester",
            () -> new AAAParticleTesterItem(new Item.Properties().stacksTo(1), "inferno"));

    public static final RegistryObject<Item> GREATFLOOD_TESTER = ITEMS.register("greatflood_tester",
            () -> new AAAParticleTesterItem(new Item.Properties().stacksTo(1), "greatflood"));

    public static final RegistryObject<Item> SANDSTORM_TESTER = ITEMS.register("sandstorm_tester",
            () -> new AAAParticleTesterItem(new Item.Properties().stacksTo(1), "sandstorm"));

    public static final RegistryObject<Item> SUPERCELL_TESTER = ITEMS.register("supercell_tester",
            () -> new AAAParticleTesterItem(new Item.Properties().stacksTo(1), "supercell"));

    public static final RegistryObject<Item> NIFTHELM_TESTER = ITEMS.register("nifthelm_tester",
            () -> new AAAParticleTesterItem(new Item.Properties().stacksTo(1), "nifthelm"));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
