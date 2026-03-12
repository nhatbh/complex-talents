package com.complextalents.impl.elementalmage.events;

import com.complextalents.TalentsMod;
import com.complextalents.impl.elementalmage.ElementalMageData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class HarmonicConvergenceEventHandler {

    @SubscribeEvent
    public static void onSpellPreCast(io.redspace.ironsspellbooks.api.events.SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        ElementalMageData.ConvergenceBuff buff = ElementalMageData.getConvergenceBuff(player.getUUID());
        if (buff.waitingForNextSpell) {
            buff.waitingForNextSpell = false;
            buff.buffedSpellId = event.getSpellId();
            buff.buffWindowTicks = 200; // 10 seconds tracking window
            
            TalentsMod.LOGGER.debug("Harmonic Convergence applied to next spell: {}", buff.buffedSpellId);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        ElementalMageData.ConvergenceBuff buff = ElementalMageData.getConvergenceBuff(player.getUUID());
        if (buff.buffWindowTicks > 0) {
            buff.buffWindowTicks--;
            if (buff.buffWindowTicks == 0) {
                buff.buffedSpellId = null;
                buff.cachedCritChanceOffset = 0.0;
                buff.cachedCritDamageBonus = 0.0;
                TalentsMod.LOGGER.debug("Harmonic Convergence tracking window expired.");
            }
        }
    }
}
