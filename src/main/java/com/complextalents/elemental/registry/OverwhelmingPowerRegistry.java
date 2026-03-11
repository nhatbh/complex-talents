package com.complextalents.elemental.registry;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.OPCooldownTracker;
import com.complextalents.elemental.OPElementType;
import com.complextalents.elemental.api.IOPStrategy;
import com.complextalents.elemental.api.OPContext;
import com.complextalents.elemental.strategies.op.AquaOPStrategy;
import com.complextalents.elemental.strategies.op.FireOPStrategy;
import com.complextalents.elemental.strategies.op.IceOPStrategy;
import com.complextalents.elemental.strategies.op.LightningOPStrategy;
import com.complextalents.elemental.strategies.op.NatureOPStrategy;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for Overwhelming Power strategies.
 */
public class OverwhelmingPowerRegistry {
    private static final OverwhelmingPowerRegistry INSTANCE = new OverwhelmingPowerRegistry();
    private final Map<OPElementType, IOPStrategy> strategies = new ConcurrentHashMap<>();

    private OverwhelmingPowerRegistry() {
    }

    public static OverwhelmingPowerRegistry getInstance() {
        return INSTANCE;
    }

    public void initialize() {
        register(OPElementType.FIRE, new FireOPStrategy());
        register(OPElementType.AQUA, new AquaOPStrategy());
        register(OPElementType.NATURE, new NatureOPStrategy());
        register(OPElementType.LIGHTNING, new LightningOPStrategy());
        register(OPElementType.ICE, new IceOPStrategy());
        TalentsMod.LOGGER.info("Registered 5 Overwhelming Power strategies");
    }

    public void register(OPElementType element, IOPStrategy strategy) {
        strategies.put(element, strategy);
    }

    public IOPStrategy getStrategy(OPElementType element) {
        return strategies.get(element);
    }

    public void trigger(OPContext context) {
        IOPStrategy strategy = getStrategy(context.getElement());
        if (strategy == null)
            return;

        Player player = context.getAttacker();

        if (!OPCooldownTracker.canTrigger(player, context.getElement())) {
            player.sendSystemMessage(Component.literal(
                    "\u00A78[OP Debug] Trigger blocked by NBT Cooldown."));
            return;
        }

        float damage = context.getRawDamage();
        int tier = calculateTier(damage);

        if (tier > 0) {
            OPCooldownTracker.startCooldown(player, context.getElement());
            sendTriggerMessage(player, context.getElement(), tier, damage);
            strategy.execute(context, tier);
        }
    }

    public static int getThreshold(int tier) {
        return switch (tier) {
            case 3 -> 50;
            case 2 -> 30;
            case 1 -> 10;
            default -> 0;
        };
    }

    private int calculateTier(float damage) {
        if (damage >= getThreshold(3))
            return 3;
        if (damage >= getThreshold(2))
            return 2;
        if (damage >= getThreshold(1))
            return 1;
        return 0;
    }

    private void sendTriggerMessage(Player player, OPElementType element, int tier, float damage) {
        String color = switch (element) {
            case FIRE -> "\u00A7c"; // Red
            case AQUA -> "\u00A7b"; // Aqua
            case NATURE -> "\u00A72"; // Dark Green
            case LIGHTNING -> "\u00A7e"; // Yellow
            case ICE -> "\u00A7f"; // White
        };

        IOPStrategy strategy = getStrategy(element);
        String tierName = getTierName(element, tier);
        String elementName = switch (element) {
            case FIRE -> "Fire";
            case AQUA -> "Aqua";
            case NATURE -> "Nature";
            case LIGHTNING -> "Lightning";
            case ICE -> "Ice";
        };

        int currentReq = getThreshold(tier);
        int nextReq = (tier < 3) ? getThreshold(tier + 1) : 0;

        String nextTierInfo = (tier < 3) ? String.format(" (Next Tier: %d)", nextReq) : " (MAX)";

        player.sendSystemMessage(Component.literal(
                String.format("%s\u00A7l[\u2741] Overwhelming Power: %s - %s (Tier %d)", color, elementName, tierName,
                        tier)));
        player.sendSystemMessage(Component.literal(
                String.format("\u00A77 \u00BB \u00A7f%.1f \u00A77Spell Damage [Min: \u00A7a%d\u00A77]%s", damage,
                        currentReq, nextTierInfo)));

        if (strategy != null) {
            java.util.List<String> breakdown = strategy.getEffectBreakdown(tier, damage);
            for (String line : breakdown) {
                player.sendSystemMessage(Component.literal("\u00A7d   + \u00A77" + line));
            }
        }
    }

    private String getTierName(OPElementType element, int tier) {
        return switch (element) {
            case FIRE -> switch (tier) {
                case 1 -> "Ignite";
                case 2 -> "Scorch";
                case 3 -> "The Supernova";
                default -> "Unknown";
            };
            case AQUA -> switch (tier) {
                case 1 -> "Splash";
                case 2 -> "Violent Splash";
                case 3 -> "The Deluge";
                default -> "Unknown";
            };
            case NATURE -> switch (tier) {
                case 1 -> "Parasitic Seed";
                case 2 -> "Spore Burst";
                case 3 -> "The Verdant Decay";
                default -> "Unknown";
            };
            case LIGHTNING -> switch (tier) {
                case 1 -> "Arcing Bolt";
                case 2 -> "Chain Surge";
                case 3 -> "The Thundergod";
                default -> "Unknown";
            };
            case ICE -> switch (tier) {
                case 1 -> "Chilled Burst";
                case 2 -> "Glacial Aura";
                case 3 -> "The Absolute Zero";
                default -> "Unknown";
            };
        };
    }
}
