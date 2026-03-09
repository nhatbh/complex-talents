package com.complextalents.elemental.api;

import com.complextalents.elemental.OPElementType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Context for Overwhelming Power execution.
 */
public class OPContext {
    private final LivingEntity target;
    private final ServerPlayer attacker;
    private final OPElementType element;
    private final float rawDamage;
    private final ServerLevel level;

    public OPContext(LivingEntity target, ServerPlayer attacker, OPElementType element, float rawDamage) {
        this.target = target;
        this.attacker = attacker;
        this.element = element;
        this.rawDamage = rawDamage;
        this.level = (ServerLevel) target.level();
    }

    public LivingEntity getTarget() {
        return target;
    }

    public ServerPlayer getAttacker() {
        return attacker;
    }

    public OPElementType getElement() {
        return element;
    }

    public float getRawDamage() {
        return rawDamage;
    }

    public ServerLevel getLevel() {
        return level;
    }
}
