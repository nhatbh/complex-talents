package com.complextalents.impl.highpriest.entity;

import com.complextalents.TalentsMod;
import com.complextalents.elemental.handlers.DelayedActionHandler;
import com.complextalents.impl.highpriest.data.SeraphSwordData;
import com.complextalents.network.PacketHandler;
import com.complextalents.network.S2CSpawnAAAParticlePacket;
import com.complextalents.network.SpawnSeraphSwordFXPacket;
import com.complextalents.util.AllyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.DamageTypeTags;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Seraph's Beacon Entity - A divine orb of light that hovers and moves through
 * space.
 * <p>
 * Three casting modes:
 * 1. Spawn & Move: Creates beacon at player position, flies to target, hovers
 * 2. Move from Hovering: Moves existing beacon with enhanced effects (1.5x
 * damage/shield)
 * 3. Pull: Target the beacon to pull enemies toward it
 */
public class SeraphsEdgeEntity extends LivingEntity {

    // Configuration constants
    private static final double MOVE_SPEED = 1.2;
    private static final double DESPAWN_RANGE = 48.0;
    private static final double PULL_RADIUS = 8.0;

    // State
    private Vec3 targetPos = null;
    private boolean wasHovering = false;
    private @Nullable UUID ownerUUID;
    private @Nullable Entity cachedOwner;

    // Configuration
    private float baseDamage = 10.0f;
    private float shieldAmount = 5.0f;

    // Tracking
    private final Set<UUID> hitEntitiesThisMove = new HashSet<>();

    public SeraphsEdgeEntity(EntityType<? extends SeraphsEdgeEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setInvulnerable(true);
    }

    public SeraphsEdgeEntity(Level level, LivingEntity owner) {
        this(HighPriestEntities.SERAPHS_EDGE.get(), level);
        this.setOwner(owner);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
    }

    // === OWNER HANDLING ===

    public void setOwner(@Nullable Entity owner) {
        this.ownerUUID = owner == null ? null : owner.getUUID();
        this.cachedOwner = owner;
    }

    @Nullable
    public Entity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        } else if (this.ownerUUID != null && this.level() instanceof ServerLevel serverLevel) {
            this.cachedOwner = serverLevel.getEntity(this.ownerUUID);
            return this.cachedOwner;
        } else {
            return null;
        }
    }

    // === LIVING ENTITY BOILERPLATE ===

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isEffectiveAi() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Only allow discard from owner or high-level damage (creative/void)
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    // === PUBLIC API ===

    public void configure(float damage, float shield) {
        this.baseDamage = damage;
        this.shieldAmount = shield;
    }

    public void moveTo(Vec3 target) {
        this.wasHovering = (this.targetPos == null);
        this.targetPos = target.add(0, 1, 0);
        this.hitEntitiesThisMove.clear();

        Vec3 direction = targetPos.subtract(position()).normalize();
        Vec3 velocity = direction.scale(MOVE_SPEED);
        setDeltaMovement(velocity);
    }

    public int pullEnemies() {
        if (level().isClientSide)
            return 0;

        Entity owner = getOwner();
        List<LivingEntity> enemies = level().getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBox().inflate(PULL_RADIUS),
                e -> e.isAlive() && owner instanceof Player p && AllyHelper.isEnemy(p, e));

        for (LivingEntity enemy : enemies) {
            // Play hit particle effect immediately (it covers both pull and hit

            // Scale pull with distance: further away = stronger pull
            Vec3 pullVec = position().subtract(enemy.position());
            enemy.setDeltaMovement(enemy.getDeltaMovement().add(pullVec.scale(0.3)));
            enemy.hurtMarked = true;

            // Delayed damage and slowdown after 10 ticks
            DelayedActionHandler.queueAction((ServerLevel) level(), 10, () -> {
                if (enemy.isAlive()) {
                    Entity currentOwner = getOwner();
                    enemy.hurt(
                            level().damageSources().mobProjectile(this,
                                    currentOwner instanceof LivingEntity l ? l : null),
                            baseDamage * 0.8f);
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                }
            });
        }
        
        // Play orb pull particle effect
        PacketHandler.sendToNearby(
                new S2CSpawnAAAParticlePacket(
                        ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "orbpull"),
                        position().add(0, 0.6, 0)),
                (ServerLevel) level(),
                position());

        PacketHandler.sendToNearby(
                new SpawnSeraphSwordFXPacket(position().add(0, 0, 0), null, 4),
                (ServerLevel) level(),
                position());

        return enemies.size();
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 prevPos = position();

        if (!level().isClientSide) {
            checkOwnerDistance();

            if (targetPos != null) {
                Vec3 currentPos = position();
                Vec3 toTarget = targetPos.subtract(currentPos);
                double distance = toTarget.length();

                if (distance < MOVE_SPEED) {
                    setDeltaMovement(Vec3.ZERO);
                    setPos(targetPos);
                    targetPos = null;

                    PacketHandler.sendToNearby(
                            new SpawnSeraphSwordFXPacket(position().add(0, 0.5, 0), null, 5),
                            (ServerLevel) level(),
                            position());
                } else {
                    Vec3 direction = toTarget.normalize();
                    Vec3 velocity = direction.scale(MOVE_SPEED);
                    setDeltaMovement(velocity);
                }
            }
        }

        Vec3 vel = getDeltaMovement();
        if (vel.lengthSqr() > 1.0E-7) {
            move(MoverType.SELF, vel);

            if (!level().isClientSide) {
                applyPathEffects(prevPos, position());
            }
        }

        if (!level().isClientSide) {
            sendParticleFX();
        }
    }

    // === PATH EFFECTS ===

    private void applyPathEffects(Vec3 start, Vec3 end) {
        AABB sweepBox = new AABB(start, end).inflate(1.5);
        List<LivingEntity> entities = level().getEntitiesOfClass(
                LivingEntity.class,
                sweepBox,
                e -> e.isAlive() && e != this && !hitEntitiesThisMove.contains(e.getUUID()));

        Entity owner = getOwner();

        for (LivingEntity entity : entities) {
            if (entity == owner)
                continue;

            hitEntitiesThisMove.add(entity.getUUID());
            boolean isAlly = owner instanceof Player p && AllyHelper.isAlly(p, entity);

            if (isAlly) {
                applyAllyEffects(entity);
            } else {
                applyEnemyEffects(entity);
            }
        }
    }

    private void applyAllyEffects(LivingEntity ally) {
        float shieldAmplifier = wasHovering ? shieldAmount * 1.5f : shieldAmount;
        ally.addEffect(new MobEffectInstance(
                MobEffects.ABSORPTION,
                200,
                (int) (shieldAmplifier / 2)));

        int speedAmplifier = wasHovering ? 2 : 1;
        ally.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                100,
                speedAmplifier));

        // Play hit particle effect for allies
        PacketHandler.sendToNearby(
                new S2CSpawnAAAParticlePacket(
                        ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "hiteffect"),
                        ally.position().add(0, ally.getBbHeight() * 0.5, 0)),
                (ServerLevel) level(),
                ally.position());
    }

    private void applyEnemyEffects(LivingEntity enemy) {
        float damage = wasHovering ? baseDamage * 1.5f : baseDamage;
        Entity owner = getOwner();
        enemy.hurt(
                level().damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null),
                damage);

        int slowAmplifier = wasHovering ? 2 : 1;
        int duration = wasHovering ? 60 : 40;
        enemy.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                duration,
                slowAmplifier));

        // Play hit particle effect
        PacketHandler.sendToNearby(
                new S2CSpawnAAAParticlePacket(
                        ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "hiteffect"),
                        enemy.position().add(0, enemy.getBbHeight() * 0.5, 0)),
                (ServerLevel) level(),
                enemy.position());
    }

    private void checkOwnerDistance() {
        Entity owner = getOwner();
        if (!(owner instanceof Player player)) {
            discardAndClear();
            return;
        }

        if (player.distanceToSqr(this) > DESPAWN_RANGE * DESPAWN_RANGE) {
            discardAndClear();
        }
    }

    private void discardAndClear() {
        Entity owner = getOwner();
        if (owner instanceof Player p) {
            SeraphSwordData.clearActiveSword(p.getUUID());
        }
        discard();
    }

    private void sendParticleFX() {
        if (tickCount % 2 != 0)
            return;
        int fxType = (targetPos != null) ? 0 : 3;

        PacketHandler.sendToNearby(
                new SpawnSeraphSwordFXPacket(position().add(0, 0.5, 0), getDeltaMovement(), fxType),
                (ServerLevel) level(),
                position());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("WasHovering", wasHovering);
        if (targetPos != null) {
            compound.putBoolean("HasTarget", true);
            compound.putDouble("TargetX", targetPos.x);
            compound.putDouble("TargetY", targetPos.y);
            compound.putDouble("TargetZ", targetPos.z);
        } else {
            compound.putBoolean("HasTarget", false);
        }
        compound.putFloat("BaseDamage", baseDamage);
        compound.putFloat("ShieldAmount", shieldAmount);
        if (ownerUUID != null) {
            compound.putUUID("OwnerUUID", ownerUUID);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        wasHovering = compound.getBoolean("WasHovering");
        if (compound.getBoolean("HasTarget")) {
            targetPos = new Vec3(
                    compound.getDouble("TargetX"),
                    compound.getDouble("TargetY"),
                    compound.getDouble("TargetZ"));
        } else {
            targetPos = null;
        }
        baseDamage = compound.getFloat("BaseDamage");
        shieldAmount = compound.getFloat("ShieldAmount");
        if (compound.hasUUID("OwnerUUID")) {
            ownerUUID = compound.getUUID("OwnerUUID");
        }
    }
}
