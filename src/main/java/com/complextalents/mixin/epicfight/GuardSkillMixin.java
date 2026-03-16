package com.complextalents.mixin.epicfight;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.WeakHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataKeys;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.entity.eventlistener.TakeDamageEvent;

@Mixin(value = GuardSkill.class, remap = false)
public abstract class GuardSkillMixin {
    @org.spongepowered.asm.mixin.Shadow
    protected abstract float getPenalizer(CapabilityItem itemCapability);

    private static final Map<SkillContainer, Integer> START_TICKS = new WeakHashMap<>();

    @Inject(method = "startHolding", at = @At(value = "HEAD"))
    private void complextalents$onStartHolding(SkillContainer container, CallbackInfo ci) {
        if (container.getExecutor().getOriginal() instanceof ServerPlayer player) {
            START_TICKS.put(container, player.tickCount);
        }
    }

    @Inject(method = "guard", at = @At(value = "HEAD"))
    private void complextalents$onGuard(SkillContainer container, CapabilityItem itemCapability,
            TakeDamageEvent.Attack event, float knockback, float impact, boolean advanced, CallbackInfo ci) {
        float penalty = container.getDataManager().getDataValue(SkillDataKeys.PENALTY.get()) + this.getPenalizer(itemCapability);
        float consumeAmount = penalty * impact;
        
        boolean isParry = false;
        if (container.getExecutor().getOriginal() instanceof ServerPlayer serverPlayer) {
            Integer startTick = START_TICKS.get(container);
            if (startTick != null) {
                int currentTick = serverPlayer.tickCount;
                // 10 ticks = 0.5 seconds
                isParry = (currentTick - startTick) <= 10;
            }
            
            LivingEntity attacker = event.getDamageSource().getEntity() instanceof LivingEntity le ? le : null;

            // Post the custom event to the Forge event bus
            MinecraftForge.EVENT_BUS.post(new com.complextalents.epicfight.event.EpicFightGuardEvent(
                    serverPlayer,
                    attacker,
                    event,
                    impact,
                    penalty,
                    isParry,
                    consumeAmount,
                    container));
        }
    }
}
