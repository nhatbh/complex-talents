package com.complextalents.origin.example;

import com.complextalents.TalentsMod;
import com.complextalents.origin.OriginBuilder;
import com.complextalents.origin.OriginManager;
import com.complextalents.origin.ResourceType;
import com.complextalents.origin.events.HolySpellDamageEvent;
import com.complextalents.passive.PassiveStackDef;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Cleric Origin - Piety-based healing/combat origin.
 *
 * Resource: Piety (0-100)
 * Generation: Gain Piety on hit (3-8 based on level)
 * Punishment: Lose Piety when hurt (15-25 based on level)
 * Economy: Forces careful aim and dodging
 *
 * Passive: Grace of the Seraphim
 * - Gain 1 stack every 5 seconds (Max 10)
 * - Lose ALL stacks when taking damage
 * - Low stacks: Increases Healing Potency
 * - Max stacks (10): Converts Healing Power into Spell Damage
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class ClericOrigin {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "cleric");

    // Define Piety resource type
    public static final ResourceType PIETY = ResourceType.register(
            ResourceLocation.fromNamespaceAndPath("complextalents", "piety"),
            "Piety",
            0,
            100,
            0xFFFFD700  // Gold color
    );

    /**
     * Register the Cleric origin.
     * Call this during mod initialization.
     */
    public static void register() {
        OriginBuilder.create("complextalents", "cleric")
                .displayName("Cleric")
                .description(Component.literal("Holy warrior gaining Piety through combat and Grace over time"))
                .resourceType(PIETY)
                .maxLevel(5)
                // Grace stacks - gain over time, lose on damage
                .passiveStack("grace", PassiveStackDef.create("grace")
                        .maxStacks(10)
                        .displayName("Grace")
                        .color(0xFFE6F0FF).build())  // Light blue
                // Custom HUD renderer for Piety bar + Grace stacks
                .renderer(new ClericRenderer())
                // Piety gained on hit: [3, 4, 5, 6, 8]
                .scaledStat("pietyOnHit", new double[]{3.0, 4.0, 5.0, 6.0, 8.0})
                // Piety gained on holy spell damage: [8, 10, 12, 15, 20]
                .scaledStat("pietyOnHolyHit", new double[]{8.0, 10.0, 12.0, 15.0, 20.0})
                // Piety lost when hurt: [15, 18, 20, 22, 25]
                .scaledStat("pietyLostOnDamage", new double[]{15.0, 18.0, 20.0, 22.0, 25.0})
                .register();
    }

    /**
     * Event handler for holy spell damage from Iron's Spellbooks.
     * Clerics gain bonus Piety when dealing holy spell damage.
     */
    @SubscribeEvent
    public static void onHolySpellDamage(HolySpellDamageEvent event) {
        if (event.getCaster() instanceof ServerPlayer player) {
            if (!ID.equals(OriginManager.getOriginId(player))) {
                return;
            }

            // Get scaled stat based on origin level - bonus Piety for holy spells
            double pietyGain = OriginManager.getOriginStat(player, "pietyOnHolyHit");
            if (pietyGain > 0) {
                OriginManager.modifyResource(player, pietyGain);
            }
        }
    }

    /**
     * Event handler for server-side ticking.
     * Handles Grace stack generation and other tick-based effects.
     */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.side.isClient() || event.phase != TickEvent.Phase.END) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (!ID.equals(OriginManager.getOriginId(player))) {
            return;
        }

        // Grace generation: gain 1 stack every 5 seconds (100 ticks)
        long gameTime = player.level().getGameTime();
        if (gameTime % 100 == 0) {
            int currentGrace = OriginManager.getPassiveStacks(player, "grace");
            if (currentGrace < 10) {
                OriginManager.modifyPassiveStacks(player, "grace", 1);
            }
        }
    }

    /**
     * Event handler for when player takes damage.
     * Lose Piety when hurt - punishment mechanic.
     * Also lose ALL Grace stacks when taking damage.
     */
    @SubscribeEvent
    public static void onPlayerHurt(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (!ID.equals(OriginManager.getOriginId(player))) {
                return;
            }

            // Lose Piety when hurt
            double pietyLoss = OriginManager.getOriginStat(player, "pietyLostOnDamage");
            if (pietyLoss > 0) {
                OriginManager.modifyResource(player, -pietyLoss);
            }

            // Lose ALL Grace stacks when taking damage
            OriginManager.setPassiveStacks(player, "grace", 0);
        }
    }
}
