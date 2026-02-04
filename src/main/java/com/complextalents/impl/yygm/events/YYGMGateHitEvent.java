package com.complextalents.impl.yygm.events;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/**
 * Fired when a YYGM player hits their harmonized target.
 * The damage handler calculates the angle/gate and includes all info.
 * <p>
 * HitResult indicates what type of hit occurred:
 * <ul>
 *   <li>TRUE_GATE: Correct gate hit (Yang when Yang required, or Yin when Yin required)</li>
 *   <li>FALSE_GATE: Wrong gate hit (Yang when Yin required, or Yin when Yang required)</li>
 *   <li>EMPTY_GATE: Hit neither gate (body hit)</li>
 * </ul>
 */
public class YYGMGateHitEvent extends Event {

    private final ServerPlayer player;
    private final LivingEntity target;
    private final HitResult hitResult;
    private final int gateType;
    private final int compassDirection;
    private final double attackAngle;
    private final int nextRequiredGate;
    private final boolean isFromSwordDance;

    public YYGMGateHitEvent(ServerPlayer player, LivingEntity target,
                           HitResult hitResult, int gateType,
                           int compassDirection, double attackAngle,
                           int nextRequiredGate) {
        this(player, target, hitResult, gateType, compassDirection, attackAngle, nextRequiredGate, false);
    }

    public YYGMGateHitEvent(ServerPlayer player, LivingEntity target,
                           HitResult hitResult, int gateType,
                           int compassDirection, double attackAngle,
                           int nextRequiredGate, boolean isFromSwordDance) {
        this.player = player;
        this.target = target;
        this.hitResult = hitResult;
        this.gateType = gateType;
        this.compassDirection = compassDirection;
        this.attackAngle = attackAngle;
        this.nextRequiredGate = nextRequiredGate;
        this.isFromSwordDance = isFromSwordDance;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public HitResult getHitResult() {
        return hitResult;
    }

    public int getGateType() {
        return gateType;
    }

    public int getCompassDirection() {
        return compassDirection;
    }

    public double getAttackAngle() {
        return attackAngle;
    }

    public int getNextRequiredGate() {
        return nextRequiredGate;
    }

    public boolean isFromSwordDance() {
        return isFromSwordDance;
    }

    /**
     * The result of the gate hit calculation.
     */
    public enum HitResult {
        /** Correct gate hit - should deal true damage and track for pair completion */
        TRUE_GATE,
        /** Wrong gate hit - should zero damage, lose all equilibrium, apply discord */
        FALSE_GATE,
        /** Hit neither gate - should lose 1 equilibrium, apply body penalty */
        EMPTY_GATE
    }

    // Gate type constants
    public static final int GATE_YANG = 0;
    public static final int GATE_YIN = 1;
    public static final int GATE_NONE = -1;
}
