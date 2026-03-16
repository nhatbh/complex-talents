package com.complextalents.impl.darkmage.events;

import com.complextalents.TalentsMod;
import com.complextalents.impl.darkmage.data.SoulData;
import com.complextalents.impl.darkmage.origin.DarkMageOrigin;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles Dark Mage soul loss on actual death.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class DarkMageDeathHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (event.getEntity() instanceof ServerPlayer player) {
            if (DarkMageOrigin.isDarkMage(player)) {
                double souls = SoulData.getSouls(player.getUUID());
                if (souls > 0) {
                    double lost = SoulData.loseSouls(player, 0.3);
                    double remaining = SoulData.getSouls(player.getUUID());

                    player.sendSystemMessage(Component.literal(
                            "\u00A74\u00A7lDEATH PENALTY:\u00A7r \u00A7cLost " + String.format("%.1f", lost) + 
                            " souls! (" + String.format("%.1f", remaining) + " remaining)"
                    ));

                    TalentsMod.LOGGER.info("Dark Mage {} died and lost 30% souls. Souls: {:.2f} -> {:.2f} (lost {:.2f})",
                            player.getName().getString(), souls, remaining, lost);
                }
            }
        }
    }
}
