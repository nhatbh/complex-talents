package com.complextalents.elemental.handlers;

import com.complextalents.TalentsMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles delayed execution of code, primarily used for syncing damage and
 * effects
 * with AAA particle animations.
 */
@Mod.EventBusSubscriber(modid = TalentsMod.MODID)
public class DelayedActionHandler {

    private static final Map<ServerLevel, List<DelayedAction>> delayedActions = new ConcurrentHashMap<>();

    /**
     * Queues a runnable action to be executed after a set amount of server ticks.
     */
    public static void queueAction(ServerLevel level, int delayTicks, Runnable action) {
        if (level == null || delayTicks < 0 || action == null)
            return;

        if (delayTicks == 0) {
            action.run();
            return;
        }

        delayedActions.computeIfAbsent(level, k -> new ArrayList<>())
                .add(new DelayedAction(delayTicks, action));
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.level.isClientSide)
            return;

        ServerLevel level = (ServerLevel) event.level;
        List<DelayedAction> actions = delayedActions.get(level);

        if (actions == null || actions.isEmpty())
            return;

        Iterator<DelayedAction> iterator = actions.iterator();
        while (iterator.hasNext()) {
            DelayedAction action = iterator.next();
            action.remainingTicks--;

            if (action.remainingTicks <= 0) {
                try {
                    action.runnable.run();
                } catch (Exception e) {
                    TalentsMod.LOGGER.error("Error executing delayed action", e);
                }
                iterator.remove();
            }
        }
    }

    private static class DelayedAction {
        int remainingTicks;
        final Runnable runnable;

        DelayedAction(int remainingTicks, Runnable runnable) {
            this.remainingTicks = remainingTicks;
            this.runnable = runnable;
        }
    }
}
