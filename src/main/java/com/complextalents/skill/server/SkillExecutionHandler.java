package com.complextalents.skill.server;

import com.complextalents.skill.BuiltSkill;
import com.complextalents.skill.Skill;
import com.complextalents.skill.SkillRegistry;
import com.complextalents.skill.event.ResolvedTargetData;
import com.complextalents.skill.event.SkillExecuteEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Central handler for executing skills through the event pipeline.
 * Stage 4: Executes skill active handlers.
 */
@Mod.EventBusSubscriber(modid = "complextalents")
public class SkillExecutionHandler {

    /**
     * Stage 4: Execute skill effects.
     * This is the PRIMARY integration point for all skills.
     */
    @SubscribeEvent
    public static void onSkillExecute(SkillExecuteEvent event) {
        ResourceLocation skillId = event.getSkillId();
        Skill skill = SkillRegistry.getInstance().getSkill(skillId);

        if (skill == null) {
            // Unknown skill - log and cancel
            event.setCanceled(true);
            return;
        }

        // Only execute if this skill has an active component
        if (!skill.getNature().hasActive()) {
            // Passive-only skills shouldn't be cast
            event.setCanceled(true);
            return;
        }

        // Execute the skill's active handler
        if (skill instanceof BuiltSkill builtSkill) {
            if (builtSkill.hasActiveHandler()) {
                // Create execution context
                Skill.ExecutionContext context = createExecutionContext(event);
                builtSkill.executeActive(context);
            } else {
                // Skill has no handler registered
                event.setCanceled(true);
            }
        }
        // For custom skill implementations, they should have their own event listeners
        // This allows for more complex skill behaviors
    }

    /**
     * Create an execution context from a SkillExecuteEvent.
     */
    private static Skill.ExecutionContext createExecutionContext(SkillExecuteEvent event) {
        ServerPlayer player = event.getPlayer();
        ResolvedTargetData targetData = event.getTargetData();

        Skill.ServerPlayerWrapper playerWrapper = new Skill.ServerPlayerWrapper(player);
        Skill.ResolvedTargetWrapper targetWrapper = new Skill.ResolvedTargetWrapper(targetData);

        return new Skill.ExecutionContext(playerWrapper, targetWrapper);
    }
}
