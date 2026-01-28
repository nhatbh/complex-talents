package com.complextalents.elemental.strategies.reactions;

import com.complextalents.elemental.ElementalReaction;
import com.complextalents.elemental.api.IReactionStrategy;
import com.complextalents.elemental.api.ReactionContext;
import com.complextalents.elemental.entity.BlackHoleEntity;
import com.complextalents.elemental.entity.ModEntities;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.elemental.SpawnFluxReactionPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Flux Reaction (Lightning + Ender)
 * Spawns a Black Hole entity at the target's location
 * The Black Hole pulls entities toward it and implodes after 5 seconds for damage
 */
public class FluxReaction implements IReactionStrategy {

    @Override
    public void execute(ReactionContext context) {
        LivingEntity target = context.getTarget();
        Vec3 pos = target.position();

        // Spawn a Black Hole at the target's position
        BlackHoleEntity blackHole = ModEntities.BLACK_HOLE.get().create(context.getLevel());
        if (blackHole != null) {
            blackHole.setPos(pos.x, pos.y + 0.5, pos.z);
            blackHole.setPersistenceRequired(); // Prevent despawn
            // Set the owner so they get credit for the implosion damage
            if (context.getAttacker() != null) {
                blackHole.setOwner(context.getAttacker().getName().getString());
            }
            context.getLevel().addFreshEntity(blackHole);
        }

        // Send visual effect packet for the initial flux burst
        PacketHandler.sendToNearby(
            new SpawnFluxReactionPacket(pos),
            context.getLevel(),
            pos
        );
    }

    @Override
    public float calculateDamage(ReactionContext context) {
        // No direct damage - damage comes from Black Hole implosion
        return 0.0f;
    }

    @Override
    public boolean canTrigger(ReactionContext context) {
        return context.getTarget() != null && context.getTarget().isAlive();
    }

    @Override
    public ElementalReaction getReactionType() {
        return ElementalReaction.FLUX;
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
