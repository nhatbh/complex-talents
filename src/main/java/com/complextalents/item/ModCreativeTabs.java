package com.complextalents.item;

import com.complextalents.TalentsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister
            .create(Registries.CREATIVE_MODE_TAB, TalentsMod.MODID);

    public static final RegistryObject<CreativeModeTab> COMPLEX_TALENTS_TAB = CREATIVE_MODE_TABS.register(
            "complex_talents_tab",
            () -> CreativeModeTab.builder()
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> new ItemStack(ModItems.INFERNO_TESTER.get()))
                    .title(Component.translatable("creativetab.complextalents.tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.INFERNO_TESTER.get());
                        output.accept(ModItems.GREATFLOOD_TESTER.get());
                        output.accept(ModItems.SANDSTORM_TESTER.get());
                        output.accept(ModItems.SUPERCELL_TESTER.get());
                        output.accept(ModItems.NIFTHELM_TESTER.get());
                    }).build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
