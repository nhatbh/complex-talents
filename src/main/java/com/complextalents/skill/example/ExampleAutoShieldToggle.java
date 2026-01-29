package com.complextalents.skill.example;

import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.capability.SkillDataProvider;
import com.complextalents.targeting.TargetType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Example TOGGLE skill: Auto Shield
 * - When on: Automatically reflects 50% of damage taken (costs 2 mana/second)
 * - When off: No effect
 * - Initial cast: 10 mana
 * - Toggle: Press key again to turn off
 *
 * Usage: /skill assign 1 complextalents:auto_shield
 */
@Mod.EventBusSubscriber(modid = "complextalents")
public class ExampleAutoShieldToggle {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "auto_shield");

    /**
     * Register this example skill.
     */
    public static void register() {
        SkillBuilder.create("complextalents", "auto_shield")
                .nature(com.complextalents.skill.SkillNature.ACTIVE)
                .targeting(TargetType.NONE)
                .activeCooldown(1.0)
                .resourceCost(10.0, "mana")
                .toggleable(true)
                .toggleCost(0.1)  // 2 mana per second (0.1 * 20 ticks)
                .onActive((context, rawPlayer) -> {
                    var player = context.player().getAs(net.minecraft.server.level.ServerPlayer.class);

                    // The actual toggle logic is handled by the capability
                    // This is just for any one-time effects on activation
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.5f, 1.5f);

                    player.sendSystemMessage(Component.literal("\u00A7b[Auto Shield] Activated!"), true);
                })
                .register();
    }

    /**
     * Damage reflection event handler.
     * When toggle is active, reflects 50% of damage back to attacker.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDamage(LivingDamageEvent event) {
        // Only process server-side damage to players
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // Check if toggle is active
        player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
            if (!data.isToggleActive(ID)) {
                return;
            }

            // Reflect 50% damage
            float reflectedDamage = event.getAmount() * 0.5f;

            // Find attacker and deal damage
            Entity sourceEntity = event.getSource().getEntity();
            if (sourceEntity instanceof LivingEntity attacker) {
                attacker.hurt(player.level().damageSources().thorns(player), reflectedDamage);

                // Visual effect
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);

                player.sendSystemMessage(Component.literal("\u00A7b[Auto Shield] Reflected " +
                        String.format("%.1f", reflectedDamage) + " damage!"), true);
            }
        });
    }
}
