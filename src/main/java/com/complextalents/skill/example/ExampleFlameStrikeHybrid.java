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
 * Example HYBRID skill: Flame Strike
 * - Passive: Your fire damage applies a burning effect (10s internal cooldown)
 * - Active: Strike with fire (5s cooldown, 30 mana)
 * - Independent cooldowns: 10s passive, 5s active
 *
 * Usage: /skill assign 1 complextalents:flame_strike
 */
@Mod.EventBusSubscriber(modid = "complextalents")
public class ExampleFlameStrikeHybrid {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "flame_strike");

    /**
     * Register this example skill.
     */
    public static void register() {
        SkillBuilder.create("complextalents", "flame_strike")
                .nature(com.complextalents.skill.SkillNature.BOTH)
                .targeting(TargetType.ENTITY)
                .maxRange(5.0)
                .activeCooldown(5.0)
                .passiveCooldown(10.0)
                .resourceCost(30.0, "mana")
                .onActive((context, rawPlayer) -> {
                    var player = context.player().getAs(net.minecraft.server.level.ServerPlayer.class);
                    var targetData = context.target().getAs(com.complextalents.skill.event.ResolvedTargetData.class);

                    // Active: Deal fire damage in a cone
                    if (!targetData.hasEntity()) {
                        return;
                    }

                    Entity targetEntity = targetData.getTargetEntity();
                    if (targetEntity instanceof LivingEntity living) {
                        living.hurt(player.level().damageSources().playerAttack(player), 8.0f);
                        living.setSecondsOnFire(5);
                    }

                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

                    player.sendSystemMessage(Component.literal("\u00A7c[Flame Strike] Active cast!"), true);
                })
                .register();
    }

    /**
     * Passive handler: Apply burn effect on fire damage.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDamage(LivingDamageEvent event) {
        // Check if attacker is a player
        Entity sourceEntity = event.getSource().getEntity();
        if (!(sourceEntity instanceof ServerPlayer player)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        LivingEntity target = (LivingEntity) event.getEntity();

        // Check if player has this skill
        player.getCapability(SkillDataProvider.SKILL_DATA).ifPresent(data -> {
            boolean hasSkill = false;
            for (int i = 0; i < 4; i++) {
                if (ID.equals(data.getSkillInSlot(i))) {
                    hasSkill = true;
                    break;
                }
            }

            if (!hasSkill) {
                return;
            }

            // Check passive cooldown
            if (data.isPassiveOnCooldown(ID)) {
                return;
            }

            // Check if damage is fire damage
            // This is simplified - real implementation would check damage type
            // For now, we'll just trigger on any damage and add a visual indicator

            // Apply bonus burning effect
            target.setSecondsOnFire(3);
            data.setPassiveCooldown(ID, 10.0);

            player.sendSystemMessage(Component.literal("\u00A7e[Flame Strike] Passive triggered!"), true);
        });
    }
}
