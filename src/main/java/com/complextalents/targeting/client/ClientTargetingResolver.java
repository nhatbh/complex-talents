package com.complextalents.targeting.client;

import com.complextalents.targeting.*;
import com.complextalents.util.AllyHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.function.Predicate;

/**
 * Single source of targeting truth on the client.
 */
public class ClientTargetingResolver {

    private static final ClientTargetingResolver INSTANCE = new ClientTargetingResolver();
    private final Minecraft minecraft = Minecraft.getInstance();

    private ClientTargetingResolver() {
    }

    public static ClientTargetingResolver getInstance() {
        return INSTANCE;
    }

    public TargetingSnapshot resolve(TargetingRequest request) {
        Player player = request.getPlayer();
        Level level = player.level();

        Vec3 origin = getEyePosition(player);
        Vec3 look = player.getLookAngle();
        Vec3 maxEnd = origin.add(look.scale(request.getMaxRange()));

        EnumSet<TargetType> resolvedTypes = EnumSet.of(TargetType.DIRECTION);

        Vec3 targetPosition = maxEnd;
        int targetEntityId = -1;
        boolean hasEntity = false;
        boolean isAlly = false;
        double distance = request.getMaxRange();
        boolean hitBlock = false;

        /* -------------------- BLOCK RAYCAST -------------------- */
        if (request.getAllowedTypes().contains(TargetType.POSITION)) {
            BlockHitResult blockHit = raycastBlocks(level, origin, maxEnd);
            if (blockHit.getType() != HitResult.Type.MISS) {
                targetPosition = blockHit.getLocation();
                distance = origin.distanceTo(targetPosition);
                hitBlock = true;
            }
            resolvedTypes.add(TargetType.POSITION);
        }

        /* -------------------- ENTITY RAYCAST -------------------- */
        if (request.getAllowedTypes().contains(TargetType.ENTITY)
                || request.getAllowedTypes().contains(TargetType.POSITION)) {
            EntityHitResult entityHit = raycastEntities(
                    level,
                    origin,
                    targetPosition,
                    createEntityPredicate(request));

            if (entityHit != null && entityHit.getType() == HitResult.Type.ENTITY) {
                Entity hit = entityHit.getEntity();
                Vec3 hitPos = entityHit.getLocation();
                double hitDistance = origin.distanceTo(hitPos);

                if (hitDistance <= request.getMaxRange()) {
                    if (!request.isRequireLineOfSight()
                            || hasLineOfSight(level, origin, hitPos, hit)) {

                        targetEntityId = hit.getId();
                        hasEntity = true;
                        targetPosition = hitPos;
                        distance = hitDistance;
                        isAlly = AllyHelper.isAlly(player, hit);

                        resolvedTypes.add(TargetType.ENTITY);
                        resolvedTypes.add(TargetType.POSITION);
                    }
                }
            }

        }

        if (!hasEntity && (!resolvedTypes.contains(TargetType.POSITION) || !hitBlock) &&
                request.isTargetSelfAllowed()) {
            targetEntityId = player.getId();
            hasEntity = true;
            targetPosition = player.position();
            distance = 0.0;
            isAlly = true;

            resolvedTypes.add(TargetType.ENTITY);
            resolvedTypes.add(TargetType.POSITION);
        }

        return new TargetingSnapshot(
                origin,
                look,
                targetPosition,
                targetEntityId,
                hasEntity,
                isAlly,
                distance,
                resolvedTypes);
    }

    public TargetingSnapshot resolveForLocalPlayer(
            double maxRange,
            EnumSet<TargetType> allowedTypes,
            TargetRelation relationFilter) {
        Player localPlayer = minecraft.player;
        if (localPlayer == null) {
            return TargetingSnapshot.createEmpty();
        }

        return resolve(TargetingRequest.builder(localPlayer)
                .maxRange(maxRange)
                .allowedTypes(allowedTypes)
                .relationFilter(relationFilter)
                .build());
    }

    /* ========================================================= */
    /* ===================== RAYCASTING ======================== */
    /* ========================================================= */

    private Vec3 getEyePosition(Player player) {
        return new Vec3(
                player.getX(),
                player.getEyeY(),
                player.getZ());
    }

    private BlockHitResult raycastBlocks(Level level, Vec3 start, Vec3 end) {
        return level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                null));
    }

    /**
     * Entity raycast using vanilla ProjectileUtil.
     */
    private EntityHitResult raycastEntities(
            Level level,
            Vec3 start,
            Vec3 end,
            Predicate<Entity> predicate) {
        return ProjectileUtil.getEntityHitResult(
                level,
                null, // no projectile owner
                start,
                end,
                new AABB(start, end).inflate(1.0D),
                entity -> predicate.test(entity)
                        && entity.isPickable()
                        && !entity.isSpectator());
    }

    /* ========================================================= */
    /* ===================== FILTERING ========================= */
    /* ========================================================= */

    private Predicate<Entity> createEntityPredicate(TargetingRequest request) {
        Player player = request.getPlayer();

        return entity -> {
            if (entity == player && !request.isTargetSelfAllowed()) {
                return false;
            }

            if (!(entity instanceof LivingEntity living)) {
                return false;
            }

            if (!living.isAlive()) {
                return false;
            }

            // Filter by player-only if enabled
            if (request.isTargetPlayerOnly() && !(entity instanceof Player)) {
                return false;
            }

            boolean ally = AllyHelper.isAlly(player, entity);

            // Filter by ally-only if enabled
            if (request.isTargetAllyOnly() && !ally) {
                return false;
            }

            return request.getRelationFilter().matches(ally);
        };
    }

    private boolean hasLineOfSight(
            Level level,
            Vec3 start,
            Vec3 end,
            Entity target) {
        BlockHitResult result = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                target));

        return result.getType() == HitResult.Type.MISS
                || result.getLocation().distanceTo(start) >= end.distanceTo(start) - 0.1;
    }
}
