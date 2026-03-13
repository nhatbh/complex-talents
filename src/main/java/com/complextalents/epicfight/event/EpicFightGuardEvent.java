package com.complextalents.epicfight.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;
import yesman.epicfight.world.entity.eventlistener.TakeDamageEvent;

/**
 * Fired when a player successfully guards an attack in Epic Fight mode.
 * Can be a standard block or an advanced parry.
 */
public class EpicFightGuardEvent extends Event {
    private final ServerPlayer player;
    private final LivingEntity attacker;
    private final TakeDamageEvent.Attack attackEvent;
    private final float impact;
    private final float penalty;
    private final boolean isParry;

    public EpicFightGuardEvent(ServerPlayer player, LivingEntity attacker, TakeDamageEvent.Attack attackEvent, float impact, float penalty, boolean isParry) {
        this.player = player;
        this.attacker = attacker;
        this.attackEvent = attackEvent;
        this.impact = impact;
        this.penalty = penalty;
        this.isParry = isParry;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public LivingEntity getAttacker() {
        return attacker;
    }

    public TakeDamageEvent.Attack getAttackEvent() {
        return attackEvent;
    }

    public float getImpact() {
        return impact;
    }

    public float getPenalty() {
        return penalty;
    }

    public boolean isParry() {
        return isParry;
    }
}
