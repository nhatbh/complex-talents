package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.entity.ModEntities;
import com.complextalents.elemental.entity.SpringPotionEntity;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnSpringReactionPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Spring Reaction (Aqua + Ender)
 * Spawns a potion item on the ground at the target's location
 * When picked up by a player, grants a random buff for 10 seconds
 * Buff amplifier scales with elemental mastery
 */
public class SpringReaction implements IReactionStrategy {

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        Vec3 pos = target.position();

        // Spawn the Spring Potion at the target's feet
        SpringPotionEntity potion = ModEntities.SPRING_POTION.get().create(context.getLevel());
        if (potion != null) {
            potion.setPos(pos.x, pos.y, pos.z);
            potion.setPersistenceRequired(); // Prevent despawn
            // Set the owner and mastery for buff calculation
            if (context.getAttacker() != null) {
                potion.setOwner(context.getAttacker().getName().getString(), context.getElementalMastery());
            }
            context.getLevel().addFreshEntity(potion);
        }

        // Send visual effect packet
        PacketHandler.sendToNearby(
            new SpawnSpringReactionPacket(pos),
            context.getLevel(),
            pos
        );
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        // No direct damage - effect is the buff from picking up the potion
        return 0.0f;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.SPRING;
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public boolean consumesStacks() {
        return true;
    }
}
