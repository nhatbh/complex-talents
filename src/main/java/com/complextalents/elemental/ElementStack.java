package com.complextalents.elemental;

import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

public class ElementStack {
    private final ElementType element;
    private int stackCount;
    private long lastAppliedTime;
    private final LivingEntity entity;
    private final UUID sourceId;

    public ElementStack(ElementType element, LivingEntity entity, LivingEntity source) {
        this.element = element;
        this.entity = entity;
        this.sourceId = source.getUUID();
        this.stackCount = 1;
        this.lastAppliedTime = System.currentTimeMillis();
    }

    public ElementType getElement() {
        return element;
    }

    public int getStackCount() {
        return stackCount;
    }

    public long getLastAppliedTime() {
        return lastAppliedTime;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public void addStack() {
        this.stackCount++;
        this.lastAppliedTime = System.currentTimeMillis();
    }

    public void removeStack() {
        this.stackCount = Math.max(0, this.stackCount - 1);
    }

    public void setStackCount(int count) {
        this.stackCount = Math.max(0, count);
    }

    public void refreshTimer() {
        this.lastAppliedTime = System.currentTimeMillis();
    }

    public boolean isExpired(long decayMillis) {
        return (System.currentTimeMillis() - lastAppliedTime) > decayMillis;
    }

    public long getTimeUntilExpiry(long decayMillis) {
        long elapsed = System.currentTimeMillis() - lastAppliedTime;
        return Math.max(0, decayMillis - elapsed);
    }
}
