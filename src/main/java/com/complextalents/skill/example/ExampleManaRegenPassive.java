package com.complextalents.skill.example;

import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.capability.SkillDataProvider;
import com.complextalents.targeting.TargetType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Example PASSIVE skill: Mana Regeneration Boost
 * - Always active, increases mana regeneration by 50%
 * - No targeting, no cooldown
 *
 * Usage: /skill assign 1 complextalents:mana_regen_passive
 */
@Mod.EventBusSubscriber(modid = "complextalents")
public class ExampleManaRegenPassive {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "mana_regen_passive");

    /**
     * Register this example skill.
     */
    public static void register() {
        SkillBuilder.create("complextalents", "mana_regen_passive")
                .nature(com.complextalents.skill.SkillNature.PASSIVE)
                .targeting(TargetType.NONE)
                // No active handler - passive effects are event-driven
                .register();

        // Register event listener for this passive
        // The passive handler checks if the player has this skill in any slot
    }

    /**
     * Passive tick handler for mana regeneration.
     * This runs every tick and checks if the player has the passive skill.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // Only run on server side, at end of tick, every 20 ticks (1 second)
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        // Check every second (20 ticks)
        if (player.level().getGameTime() % 20 != 0) {
            return;
        }

        // Check if player has this passive skill assigned
        player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
            boolean hasPassive = false;
            for (int i = 0; i < 4; i++) {
                if (ID.equals(data.getSkillInSlot(i))) {
                    hasPassive = true;
                    break;
                }
            }

            if (hasPassive) {
                // Apply mana regen boost effect
                // This is a placeholder - actual implementation would integrate
                // with Iron's Spellbooks or another mana system
                player.sendSystemMessage(Component.literal("\u00A79[Passive] Mana regeneration boost active!"),
                    true);
            }
        });
    }
}
