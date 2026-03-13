package com.complextalents.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKeys;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.eventlistener.TakeDamageEvent;

@Mixin(value = GuardSkill.class, remap = false)
public class GuardSkillMixin {
    @Inject(method = "guard", at = @At("HEAD"))
    private void complextalents$onGuard(SkillContainer container, CapabilityItem itemCapability,
            TakeDamageEvent.Attack event, float knockback, float impact, boolean advanced, CallbackInfo ci) {
        float penalty = container.getDataManager().getDataValue(SkillDataKeys.PENALTY.get());
        boolean isParry = penalty == 0.0F;
        if (container.getExecutor().getOriginal() instanceof ServerPlayer serverPlayer) {
            LivingEntity attacker = event.getDamageSource().getEntity() instanceof LivingEntity le ? le : null;

            // Post the custom event to the Forge event bus
            MinecraftForge.EVENT_BUS.post(new com.complextalents.epicfight.event.EpicFightGuardEvent(
                    serverPlayer,
                    attacker,
                    event,
                    impact,
                    penalty,
                    isParry));
        }
    }
}
