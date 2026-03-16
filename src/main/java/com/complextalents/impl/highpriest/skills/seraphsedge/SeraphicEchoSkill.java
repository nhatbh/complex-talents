package com.complextalents.impl.highpriest.skills.seraphsedge;

import com.complextalents.impl.highpriest.data.SeraphSwordData;
import com.complextalents.impl.highpriest.entity.SeraphsEdgeEntity;
import com.complextalents.skill.SkillBuilder;
import com.complextalents.skill.SkillNature;
import com.complextalents.skill.event.ResolvedTargetData;
import com.complextalents.targeting.TargetType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import com.complextalents.leveling.service.LevelingService;
import com.complextalents.leveling.events.xp.XPSource;
import com.complextalents.leveling.events.xp.XPContext;
import com.complextalents.leveling.util.XPFormula;
import com.complextalents.leveling.data.PlayerLevelingData;
import net.minecraft.world.level.ChunkPos;

/**
 * Seraphic Echo - A divine orb of light that hovers and moves through space.
 * <p>
 * When cast on block/entity, move the beacon to that position.
 * Damages/Debuffs enemies and Shields/Buffs allies on its path.
 */
public class SeraphicEchoSkill {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("complextalents", "seraphic_echo");

    public static void register() {
        SkillBuilder.create("complextalents", "seraphic_echo")
                .nature(SkillNature.ACTIVE)
                .description("A divine orb of light. Move the beacon to damage and slow enemies, or shield and buff allies on its path. Scales with Faith and Holy Spell Power. Pulls enemies if cast on self with high Command.")
                .targeting(TargetType.POSITION)
                .allowSelfTarget(true)
                .icon(ResourceLocation.fromNamespaceAndPath("complextalents",
                        "textures/skill/highpriest/seraphs_echo.png"))
                .maxRange(32.0)
                .minChannelTime(0.3)
                .maxChannelTime(0.3)
                .scaledCooldown(new double[] { 2, 2, 2, 2, 2 })
                .setMaxLevel(5)
                .scaledStat("damage", new double[] { 5, 7, 10, 12, 15 })
                .scaledStat("shield", new double[] { 4, 6, 8, 10, 15 })
                .onActive((context, rawPlayer) -> {
                    ServerPlayer player = (ServerPlayer) rawPlayer;
                    ResolvedTargetData targetData = context.target().getAs(ResolvedTargetData.class);
                    if (targetData == null)
                        return;

                    double faith = com.complextalents.impl.highpriest.data.FaithData.getFaith(player);
                    double baseDamage = context.getStat("damage");
                    double shieldAmount = context.getStat("shield");

                    // Faith scaling
                    baseDamage *= (1.0 + (faith * 0.0005));
                    shieldAmount *= (1.0 + (faith * 0.0005));

                    Attribute holyPowerAttr = ForgeRegistries.ATTRIBUTES.getValue(
                            ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "holy_spell_power"));
                    if (holyPowerAttr != null) {
                        double holySpellPower = player.getAttributeValue(holyPowerAttr);
                        baseDamage *= holySpellPower;
                        shieldAmount *= holySpellPower;
                    }

                    Vec3 targetPos;
                    Entity targetEntity = null;
                    if (targetData.hasEntity()) {
                        targetEntity = targetData.getTargetEntity();
                        targetPos = targetEntity.position();
                    } else {
                        targetPos = targetData.getTargetPosition();
                    }

                    SeraphsEdgeEntity sword = SeraphSwordData.getActiveSword(player);

                    // Case 1: Target is the sword itself -> Pull
                    if (sword != null && targetEntity == sword) {
                        if (!com.complextalents.passive.PassiveManager.hasPassiveStacks(player, "command", 5)) {
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cNot enough Command (5 required)"), true);
                            return;
                        }
                        com.complextalents.passive.PassiveManager.modifyPassiveStacks(player, "command", -5);

                        int pulledCount = sword.pullEnemies();
                        player.level().playSound(null, sword.getX(), sword.getY(), sword.getZ(),
                                SoundEvents.TRIDENT_RETURN, SoundSource.PLAYERS, 1.0f, 1.5f);

                        // Award Crowd Control XP
                        if (pulledCount > 0) {
                            int playerLevel = PlayerLevelingData.get(player.serverLevel()).getLevel(player.getUUID());
                            double crowdXP = XPFormula.calculateHighPriestCrowdControlXP(pulledCount, playerLevel);
                            ChunkPos chunkPos = new ChunkPos(player.blockPosition());
                            XPContext crowdContext = XPContext.builder()
                                .source(XPSource.HIGHPRIEST_CROWD_CONTROL)
                                .chunkPos(chunkPos)
                                .rawAmount(crowdXP)
                                .metadata("mobsPulled", pulledCount)
                                .metadata("playerLevel", playerLevel)
                                .metadata("swordPosition", sword != null ? sword.position().toString() : "unknown")
                                .build();
                            LevelingService.getInstance().awardXP(player, crowdXP, XPSource.HIGHPRIEST_CROWD_CONTROL, crowdContext);
                        }
                        return;
                    }

                    // Otherwise -> Move/Spawn
                    if (!com.complextalents.passive.PassiveManager.hasPassiveStacks(player, "command", 1)) {
                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cNot enough Command (1 required)"), true);
                        return;
                    }
                    com.complextalents.passive.PassiveManager.modifyPassiveStacks(player, "command", -1);

                    // Case 2: No sword or sword too far -> Spawn at player
                    if (sword == null || sword.distanceToSqr(player) > 64 * 64) {
                        sword = new SeraphsEdgeEntity(player.level(), player);
                        sword.configure((float) baseDamage, (float) shieldAmount);

                        Vec3 spawnPos = player.getEyePosition().add(player.getLookAngle().scale(1.5));
                        sword.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

                        player.level().addFreshEntity(sword);
                        SeraphSwordData.setActiveSword(player, sword);
                    }

                    // Move sword to target
                    sword.moveTo(targetPos);

                    // Sound
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);
                })
                .register();
    }
}
