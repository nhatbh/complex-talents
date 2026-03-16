package com.complextalents.impl.darkmage.skill;

import com.complextalents.impl.darkmage.data.SoulData;
import com.complextalents.origin.OriginManager;
import com.complextalents.origin.integration.SpellCritAttributes;
import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.SkillNature;
import com.complextalents.targeting.TargetType;
import com.complextalents.util.UUIDHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Blood Pact - Dark Mage's core toggle skill.
 * <p>
 * Transforms the Dark Mage into a glass cannon, trading HP for immense power.
 * </p>
 * <p>
 * <strong>Effects while active:</strong>
 * <ul>
 *   <li>HP drain per second: 8%/7%/6%/5%/4% (by level)</li>
 *   <li>Cast Speed bonus: +10%/20%/30%/40%/50% (by level)</li>
 *   <li>Soul-scaled Mana Regeneration: 1.0 + (souls / 200.0)</li>
 *   <li>Soul damage bonus: +0.05%/0.1%/0.15%/0.2%/0.25% per soul (by level)</li>
 * </ul>
 * <p>
 * <strong>Costs:</strong>
 * <ul>
 *   <li>30 second cooldown after toggling off</li>
 *   <li>Requires 20% HP to activate</li>
 *   <li>Auto-deactivates at 1 HP</li>
 * </ul>
 */
public class BloodPactSkill {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "blood_pact");

    // UUIDs for attribute modifiers
    private static final UUID CAST_SPEED_UUID       = UUIDHelper.generateAttributeModifierUUID("dark_mage", "blood_pact_cast_speed");
    private static final UUID MANA_REGEN_UUID        = UUIDHelper.generateAttributeModifierUUID("dark_mage", "blood_pact_mana_regen");
    private static final UUID SPELL_CRIT_CHANCE_UUID = UUIDHelper.generateAttributeModifierUUID("dark_mage", "blood_pact_spell_crit_chance");
    private static final UUID SPELL_CRIT_DAMAGE_UUID = UUIDHelper.generateAttributeModifierUUID("dark_mage", "blood_pact_spell_crit_damage");
    private static final UUID SPELL_POWER_UUID       = UUIDHelper.generateAttributeModifierUUID("dark_mage", "blood_pact_spell_power");



    /**
     * Register this skill.
     */
    public static void register() {
        SkillBuilder.create("complextalents", "blood_pact")
                .nature(SkillNature.ACTIVE)
                .description("Toggle to fuel magic with life force. Grants soul-scaled mana regeneration (1.0 + souls/200), increased cast speed (up to +50%), and massive scaling damage based on harvested souls (up to +0.25%/soul). Drains 4-8% HP/sec.")
                .targeting(TargetType.NONE)
                .icon(ResourceLocation.fromNamespaceAndPath("complextalents", "textures/skill/darkmage/bloodpact.png"))
                .toggleable(true)
                // Cooldown after toggle off: 30 seconds at all levels
                .scaledCooldown(new double[]{30.0, 30.0, 30.0, 30.0, 30.0})
                .setMaxLevel(5)
                .validate((context, player) -> {
                    ServerPlayer serverPlayer = (ServerPlayer) player;

                    // Cannot activate if HP too low (below 20%)
                    if (serverPlayer.getHealth() < serverPlayer.getMaxHealth() * 0.2f) {
                        serverPlayer.sendSystemMessage(Component.literal(
                                "\u00A7cBlood Pact requires at least 20% HP to activate!"
                        ));
                        return false;
                    }

                    return true;
                })
                .onActive((context, player) -> {
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    ServerLevel level = serverPlayer.serverLevel();
                    int skillLevel = context.skillLevel();

                    // Apply cast speed (fixed), mana regen, spell crit, and spell power modifiers (scaled)
                    applyCastSpeedBonus(serverPlayer);
                    updateScaledBonuses(serverPlayer, 1.0);

                    // Track Blood Pact active state
                    SoulData.setBloodPactActive(serverPlayer.getUUID(), true);

                    // Visual activation effects
                    level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.5f);
                    level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 1.0f, 0.8f);

                    // Feedback message
                    double souls = SoulData.getSouls(serverPlayer);
                    double bonusPerSoul = OriginManager.getOriginStat(serverPlayer, "soulDamageBonusPercent");
                    double totalBonus = souls * bonusPerSoul * 100;

                    serverPlayer.sendSystemMessage(Component.literal(
                            "\u00A75\u00A7lBLOOD PACT ACTIVATED!\u00A7r \u00A7dYour life force fuels your magic! " +
                                    "\u00A78(" + String.format("%.1f", souls) + " souls = +" + String.format("%.1f", totalBonus) + "% damage)"
                    ));

                    // Sync state to client
                    SoulData.syncToClient(serverPlayer);
                })
                .onToggleOff(player -> {
                    ServerPlayer serverPlayer = (ServerPlayer) player;
                    ServerLevel level = serverPlayer.serverLevel();

                    // Remove cast speed, mana regen, spell crit, and spell power modifiers
                    removeCastSpeedBonus(serverPlayer);
                    removeManaRegenBonus(serverPlayer);
                    removeCritBonus(serverPlayer);
                    removeSpellPowerBonus(serverPlayer);

                    // Track Blood Pact inactive state
                    SoulData.setBloodPactActive(serverPlayer.getUUID(), false);

                    // Play deactivation sound
                    level.playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                            SoundEvents.SOUL_ESCAPE, SoundSource.PLAYERS, 0.8f, 1.2f);

                    serverPlayer.sendSystemMessage(Component.literal(
                            "\u00A7cBlood Pact deactivated."
                    ));

                    // Sync state to client
                    SoulData.syncToClient(serverPlayer);
                })
                .register();
    }

    /**
     * Apply the cast speed bonus attribute modifier.
     */
    public static void applyCastSpeedBonus(ServerPlayer player) {
        double bonus = OriginManager.getOriginStat(player, "bloodPactCastSpeedBonus");

        ResourceLocation castTimeAttrId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "cast_time_reduction");
        Attribute castTimeAttr = ForgeRegistries.ATTRIBUTES.getValue(castTimeAttrId);

        if (castTimeAttr != null) {
            var attributeInstance = player.getAttributes().getInstance(castTimeAttr);
            if (attributeInstance != null) {
                // Remove existing modifier if present
                attributeInstance.removeModifier(CAST_SPEED_UUID);

                // Add new modifier
                AttributeModifier modifier = new AttributeModifier(
                        CAST_SPEED_UUID,
                        "Blood Pact Cast Speed",
                        bonus,
                        AttributeModifier.Operation.ADDITION
                );
                attributeInstance.addTransientModifier(modifier);
            }
        }
    }

    /**
     * Remove the cast speed bonus attribute modifier.
     */
    public static void removeCastSpeedBonus(ServerPlayer player) {
        ResourceLocation castTimeAttrId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "cast_time_reduction");
        Attribute castTimeAttr = ForgeRegistries.ATTRIBUTES.getValue(castTimeAttrId);

        if (castTimeAttr != null) {
            var attributeInstance = player.getAttributes().getInstance(castTimeAttr);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(CAST_SPEED_UUID);
            }
        }
    }

    /**
     * Update all soul-scaled bonuses (mana regen, crit, spell power) with a multiplier.
     */
    public static void updateScaledBonuses(ServerPlayer player, double soulMultiplier) {
        applyManaRegenBonus(player, soulMultiplier);
        applyCritBonus(player, soulMultiplier);
        applySpellPowerBonus(player, soulMultiplier);
    }

    /**
     * Apply massive mana regeneration bonus (simulates infinite mana).
     */
    public static void applyManaRegenBonus(ServerPlayer player, double multiplier) {
        ResourceLocation manaRegenAttrId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mana_regen");
        Attribute manaRegenAttr = ForgeRegistries.ATTRIBUTES.getValue(manaRegenAttrId);

        if (manaRegenAttr != null) {
            var attributeInstance = player.getAttributes().getInstance(manaRegenAttr);
            if (attributeInstance != null) {
                // Remove existing modifier if present
                attributeInstance.removeModifier(MANA_REGEN_UUID);

                // Add mana regen modifier: (1.0 + (souls / 200.0)) * multiplier
                double souls = SoulData.getSouls(player.getUUID());
                double bonus = (1.0 + (souls / 200.0)) * multiplier;
                
                AttributeModifier modifier = new AttributeModifier(
                        MANA_REGEN_UUID,
                        "Blood Pact Mana Regen",
                        bonus,
                        AttributeModifier.Operation.ADDITION
                );
                attributeInstance.addTransientModifier(modifier);
            }
        }
    }

    public static void applyManaRegenBonus(ServerPlayer player) {
        applyManaRegenBonus(player, 1.0);
    }

    /**
     * Remove the mana regeneration bonus attribute modifier.
     */
    public static void removeManaRegenBonus(ServerPlayer player) {
        ResourceLocation manaRegenAttrId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mana_regen");
        Attribute manaRegenAttr = ForgeRegistries.ATTRIBUTES.getValue(manaRegenAttrId);

        if (manaRegenAttr != null) {
            var attributeInstance = player.getAttributes().getInstance(manaRegenAttr);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(MANA_REGEN_UUID);
            }
        }
    }

    /**
     * Apply spell crit chance (and overflow crit damage) based on current soul count.
     * Crit chance = souls × rate × multiplier, capped at 1.0 (100%).
     * Any excess beyond 1.0 is applied as additive crit damage bonus.
     */
    public static void applyCritBonus(ServerPlayer player, double multiplier) {
        double souls = SoulData.getSouls(player.getUUID());
        double rate  = OriginManager.getOriginStat(player, "soulSpellCritPercent");
        double totalBonus = souls * rate * multiplier;

        double critChanceBonus = Math.min(totalBonus, 1.0);
        double critDamageBonus = Math.max(0.0, totalBonus - 1.0);

        var critChanceInst = player.getAttribute(SpellCritAttributes.SPELL_CRIT_CHANCE.get());
        if (critChanceInst != null) {
            critChanceInst.removeModifier(SPELL_CRIT_CHANCE_UUID);
            critChanceInst.addTransientModifier(new AttributeModifier(
                    SPELL_CRIT_CHANCE_UUID, "Blood Pact Spell Crit Chance",
                    critChanceBonus, AttributeModifier.Operation.ADDITION));
        }

        var critDamageInst = player.getAttribute(SpellCritAttributes.SPELL_CRIT_DAMAGE.get());
        if (critDamageInst != null) {
            critDamageInst.removeModifier(SPELL_CRIT_DAMAGE_UUID);
            if (critDamageBonus > 0.0) {
                critDamageInst.addTransientModifier(new AttributeModifier(
                        SPELL_CRIT_DAMAGE_UUID, "Blood Pact Spell Crit Damage",
                        critDamageBonus, AttributeModifier.Operation.ADDITION));
            }
        }
    }

    public static void applyCritBonus(ServerPlayer player) {
        applyCritBonus(player, 1.0);
    }

    /**
     * Remove spell crit chance and damage modifiers applied by Blood Pact.
     */
    public static void removeCritBonus(ServerPlayer player) {
        var critChanceInst = player.getAttribute(SpellCritAttributes.SPELL_CRIT_CHANCE.get());
        if (critChanceInst != null) critChanceInst.removeModifier(SPELL_CRIT_CHANCE_UUID);

        var critDamageInst = player.getAttribute(SpellCritAttributes.SPELL_CRIT_DAMAGE.get());
        if (critDamageInst != null) critDamageInst.removeModifier(SPELL_CRIT_DAMAGE_UUID);
    }

    /**
     * Apply spell power bonus based on current soul count, locked in at activation.
     * Bonus = souls × soulDamageBonusPercent × multiplier, applied as additive spell_power.
     */
    public static void applySpellPowerBonus(ServerPlayer player, double multiplier) {
        double souls = SoulData.getSouls(player.getUUID());
        double bonusPerSoul = OriginManager.getOriginStat(player, "soulDamageBonusPercent");
        double bonus = souls * bonusPerSoul * multiplier;

        ResourceLocation spellPowerAttrId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spell_power");
        Attribute spellPowerAttr = ForgeRegistries.ATTRIBUTES.getValue(spellPowerAttrId);

        if (spellPowerAttr != null) {
            var attributeInstance = player.getAttributes().getInstance(spellPowerAttr);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(SPELL_POWER_UUID);
                if (bonus > 0.0) {
                    attributeInstance.addTransientModifier(new AttributeModifier(
                            SPELL_POWER_UUID, "Blood Pact Spell Power",
                            bonus, AttributeModifier.Operation.ADDITION));
                }
            }
        }
    }

    public static void applySpellPowerBonus(ServerPlayer player) {
        applySpellPowerBonus(player, 1.0);
    }

    /**
     * Remove spell power modifier applied by Blood Pact.
     */
    public static void removeSpellPowerBonus(ServerPlayer player) {
        ResourceLocation spellPowerAttrId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "spell_power");
        Attribute spellPowerAttr = ForgeRegistries.ATTRIBUTES.getValue(spellPowerAttrId);

        if (spellPowerAttr != null) {
            var attributeInstance = player.getAttributes().getInstance(spellPowerAttr);
            if (attributeInstance != null) {
                attributeInstance.removeModifier(SPELL_POWER_UUID);
            }
        }
    }
}
