package com.complextalents.leveling.handlers;

import com.complextalents.TalentsMod;
import com.complextalents.leveling.events.xp.XPContext;
import com.complextalents.leveling.events.xp.XPSource;
import com.complextalents.leveling.service.LevelingService;
import com.complextalents.leveling.util.XPFormula;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Handles primary XP awards from combat kills.
 * Listens to LivingDeathEvent and awards XP to the killer and assists.
 *
 * <p>Primary XP is calculated from the victim's max health using XPFormula.
 * Fatigue multiplier is applied via XPPreAwardEvent (not here).</p>
 *
 * <p>Assists are also handled here via AssistHandler.</p>
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class PrimaryXPHandler {

    /**
     * Handles mob death and awards primary XP to killer and assists.
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;

        ServerLevel level = (ServerLevel) victim.level();
        ChunkPos chunkPos = new ChunkPos(victim.blockPosition());

        // Calculate base XP from victim's max health
        double baseXP = XPFormula.calculatePrimaryXP(victim.getMaxHealth());

        // Award to killer if they're a player
        if (event.getSource().getEntity() instanceof ServerPlayer killer) {
            // Build context with metadata
            XPContext context = XPContext.builder()
                    .source(XPSource.PRIMARY_COMBAT)
                    .chunkPos(chunkPos)
                    .rawAmount(baseXP)
                    .metadata("victimMaxHealth", victim.getMaxHealth())
                    .build();

            // Award XP via service (fires events)
            LevelingService.getInstance().awardXP(killer, baseXP, XPSource.PRIMARY_COMBAT, context);
        }

        // AssistHandler will handle assists via LivingDeathEvent at different priority
    }
}
