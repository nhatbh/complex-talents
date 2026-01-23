package com.complextalents.elemental.talents.mage.conflux;

import com.complextalents.TalentsMod;
import com.complextalents.capability.TalentsCapabilities;
import com.complextalents.elemental.ElementType;
import com.complextalents.elemental.ElementalStackManager;
import com.complextalents.elemental.attributes.MasteryAttributes;
import com.complextalents.talent.BranchingActiveTalent;
import com.complextalents.talent.TalentBranches;
import com.complextalents.talent.TalentSlotType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.*;

/**
 * Elemental Conflux - Ultimate setup tool
 * Finale Slot talent that creates a zone applying random elements
 */
public class ElementalConfluxTalent extends BranchingActiveTalent {

    // Active conflux zones
    private static final Map<UUID, ConfluxZone> activeZones = new HashMap<>();

    // Base values per rank
    private static final int[] DURATION_SECONDS = {12, 14, 16, 20};
    private static final float[] PULSE_INTERVAL = {2.5f, 2.2f, 1.8f, 1.5f};
    private static final float[] ZONE_DAMAGE = {2f, 3f, 5f, 8f};
    private static final int[] COOLDOWN_SECONDS = {140, 130, 120, 100};
    private static final float ZONE_RADIUS = 10f;

    // Rank 2 branch values
    private static final float[] MAELSTROM_THIRD_CHANCE = {0.25f, 0.35f, 0.45f, 0.60f};
    private static final float[] FOCAL_LENS_INTERVAL = {2f, 1.5f, 1f, 0.5f};

    // Rank 3 branch values
    private static final float[] CATACLYSM_RANGE = {3f, 4f, 5f, 10f};
    private static final float[] SHATTERPOINT_MAX_HP_DAMAGE = {0.02f, 0.03f, 0.04f, 0.05f};

    // Rank 4 Blackhole execute threshold now uses mastery scaling instead of static array
    // Formula: 0.05f * (1 + 0.2f * (masteryValue - 1))
    // Base threshold is 5% at mastery 1.0, scales up to 10% at mastery 26.0

    private static final Random random = new Random();
    private static final ElementType[] ALL_ELEMENTS = ElementType.values();

    public ElementalConfluxTalent() {
        super(
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_conflux"),
            Component.translatable("talent.complextalents.elemental_conflux"),
            Component.translatable("talent.complextalents.elemental_conflux.desc"),
            4,
            TalentSlotType.FINALE,
            ResourceLocation.fromNamespaceAndPath(TalentsMod.MODID, "elemental_mage_definition"),
            COOLDOWN_SECONDS[0] * 20 // Base cooldown in ticks
        );

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onUnlock(ServerPlayer player, int level) {
        TalentsMod.LOGGER.info("Player {} unlocked Elemental Conflux at level {}",
            player.getName().getString(), level);
    }

    @Override
    public void onRemove(ServerPlayer player) {
        // Remove any active zones
        activeZones.remove(player.getUUID());
    }

    @Override
    public void onActivate(ServerPlayer player, int level) {
        // Get target location (where player is looking, max 30 blocks)
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 targetPos = eyePos.add(lookVec.scale(30));

        BlockHitResult hitResult = player.level().clip(new ClipContext(
            eyePos,
            targetPos,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            player
        ));

        Vec3 zoneCenter = hitResult.getLocation();

        // Create the conflux zone
        ConfluxZone zone = new ConfluxZone();
        zone.center = zoneCenter;
        zone.level = level;
        zone.serverLevel = player.serverLevel();
        zone.ownerUUID = player.getUUID();
        zone.durationTicks = DURATION_SECONDS[Math.min(level - 1, 3)] * 20;
        zone.pulseInterval = (int)(PULSE_INTERVAL[Math.min(level - 1, 3)] * 20);
        zone.nextPulseTick = zone.pulseInterval;
        zone.radius = ZONE_RADIUS;
        zone.damage = ZONE_DAMAGE[Math.min(level - 1, 3)];

        // Store the zone
        activeZones.put(player.getUUID(), zone);

        // Set cooldown
        player.getCapability(TalentsCapabilities.PLAYER_TALENTS).ifPresent(talents -> {
            int cooldownTicks = COOLDOWN_SECONDS[Math.min(level - 1, 3)] * 20;
            talents.setTalentCooldown(this.getId(), cooldownTicks);
        });

        player.sendSystemMessage(Component.translatable("message.complextalents.conflux_created",
            zoneCenter.x, zoneCenter.y, zoneCenter.z));

        TalentsMod.LOGGER.debug("Elemental Conflux created at {} by {}",
            zoneCenter, player.getName().getString());
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Iterator<Map.Entry<UUID, ConfluxZone>> iterator = activeZones.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ConfluxZone> entry = iterator.next();
            ConfluxZone zone = entry.getValue();

            // Check if zone has expired
            if (zone.durationTicks <= 0) {
                iterator.remove();
                continue;
            }

            // Process zone pulse
            zone.nextPulseTick--;
            if (zone.nextPulseTick <= 0) {
                pulseZone(zone);
                zone.nextPulseTick = zone.pulseInterval;
            }

            // Process Focal Lens beam if active
            if (zone.focalLensActive) {
                processFocalLensBeam(zone);
            }

            zone.durationTicks--;
        }
    }

    private void pulseZone(ConfluxZone zone) {
        // Zone should store server level reference
        ServerLevel level = zone.serverLevel;
        if (level == null) return;

        // Find the owner player
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(zone.ownerUUID);
        if (owner == null) return;

        // Find all entities in the zone
        AABB bounds = new AABB(
            zone.center.subtract(zone.radius, zone.radius, zone.radius),
            zone.center.add(zone.radius, zone.radius, zone.radius)
        );

        List<LivingEntity> entities = level.getEntitiesOfClass(
            LivingEntity.class,
            bounds,
            entity -> entity.position().distanceTo(zone.center) <= zone.radius &&
                     entity != owner
        );

        for (LivingEntity entity : entities) {
            // Apply random element stacks
            applyRandomElements(entity, zone, owner);

            // Deal zone damage
            entity.hurt(level.damageSources().magic(), zone.damage);

            // Check for Rank 4A: Supermassive Blackhole execution
            if (zone.level >= 4 && zone.isBlackhole) {
                checkBlackholeExecution(entity, zone, owner);
            }
        }
    }

    private void applyRandomElements(LivingEntity entity, ConfluxZone zone, ServerPlayer owner) {
        int stacksToApply = 1;

        // Rank 2A: Maelstrom - apply 2 stacks, chance for 3rd
        if (zone.level >= 2 && zone.maelstromActive) {
            stacksToApply = 2;
            float thirdChance = MAELSTROM_THIRD_CHANCE[Math.min(zone.level - 1, 3)];
            if (random.nextFloat() < thirdChance) {
                stacksToApply = 3;
            }
        }

        Set<ElementType> appliedElements = new HashSet<>();
        for (int i = 0; i < stacksToApply; i++) {
            ElementType element;
            do {
                element = ALL_ELEMENTS[random.nextInt(ALL_ELEMENTS.length)];
            } while (appliedElements.contains(element));

            appliedElements.add(element);
            ElementalStackManager.applyElementStack(entity, element, owner, 0);
        }

        // Track for Focal Lens
        if (zone.focalLensActive && !appliedElements.isEmpty()) {
            zone.lastAppliedElement = appliedElements.iterator().next();
        }
    }

    private void processFocalLensBeam(ConfluxZone zone) {
        // Zone should store server level reference
        ServerLevel level = zone.serverLevel;
        if (level == null) return;

        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(zone.ownerUUID);
        if (owner == null || zone.lastAppliedElement == null) return;

        // Check if beam should apply stacks
        zone.focalLensTicks--;
        float interval = FOCAL_LENS_INTERVAL[Math.min(zone.level - 1, 3)] * 20;
        if (zone.focalLensTicks <= 0) {
            // Apply element to entities between zone center and player
            Vec3 beamStart = zone.center;
            Vec3 beamEnd = owner.position().add(0, owner.getBbHeight() / 2, 0);

            applyBeamEffect(beamStart, beamEnd, zone, owner);
            zone.focalLensTicks = (int)interval;
        }
    }

    private void applyBeamEffect(Vec3 start, Vec3 end, ConfluxZone zone, ServerPlayer owner) {
        if (!(owner.level() instanceof ServerLevel level)) return;

        // Create beam bounding box
        double minX = Math.min(start.x, end.x) - 1;
        double minY = Math.min(start.y, end.y) - 1;
        double minZ = Math.min(start.z, end.z) - 1;
        double maxX = Math.max(start.x, end.x) + 1;
        double maxY = Math.max(start.y, end.y) + 1;
        double maxZ = Math.max(start.z, end.z) + 1;

        AABB beamBounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        List<LivingEntity> entities = level.getEntitiesOfClass(
            LivingEntity.class,
            beamBounds,
            entity -> isEntityInBeam(entity, start, end) && entity != owner
        );

        for (LivingEntity entity : entities) {
            // Apply the last applied element
            ElementalStackManager.applyElementStack(entity, zone.lastAppliedElement, owner, 0);

            // Rank 3B: Shatterpoint - intensified beam damage
            if (zone.level >= 3 && zone.shatterpointActive) {
                float maxHpDamage = entity.getMaxHealth() * SHATTERPOINT_MAX_HP_DAMAGE[Math.min(zone.level - 1, 3)];
                entity.hurt(level.damageSources().magic(), maxHpDamage);
            }
        }
    }

    private boolean isEntityInBeam(Entity entity, Vec3 start, Vec3 end) {
        Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2, 0);

        // Calculate distance from entity to line segment
        Vec3 beamVec = end.subtract(start);
        double beamLength = beamVec.length();
        Vec3 beamDir = beamVec.normalize();

        Vec3 toEntity = entityPos.subtract(start);
        double projection = toEntity.dot(beamDir);

        // Check if projection is within beam segment
        if (projection < 0 || projection > beamLength) {
            return false;
        }

        // Calculate perpendicular distance
        Vec3 closestPoint = start.add(beamDir.scale(projection));
        double distance = entityPos.distanceTo(closestPoint);

        return distance <= 1.5; // Beam radius of 1.5 blocks
    }

    private void checkBlackholeExecution(LivingEntity entity, ConfluxZone zone, ServerPlayer owner) {
        // Check if entity is within 3 blocks of center
        if (entity.position().distanceTo(zone.center) <= 3) {
            // Calculate mastery-scaled execute threshold
            // Formula: 0.05f * (1 + 0.2f * (masteryValue - 1))
            // Base threshold is 5% at mastery 1.0, scales up with mastery
            double generalMasteryAttr = owner.getAttributeValue(MasteryAttributes.ELEMENTAL_MASTERY.get());
            double enderMasteryAttr = owner.getAttributeValue(MasteryAttributes.ENDER_MASTERY.get());

            // Use higher of general or ender mastery (base is 0, so add 1 for default mastery of 1.0)
            float masteryValue = (float)Math.max(generalMasteryAttr + 1.0, enderMasteryAttr + 1.0);

            // Calculate execute threshold with mastery scaling
            float executeThreshold = 0.05f * (1 + 0.2f * (masteryValue - 1));

            float healthPercent = entity.getHealth() / entity.getMaxHealth();

            if (healthPercent <= executeThreshold) {
                // Execute the entity
                entity.hurt(owner.level().damageSources().magic(), Float.MAX_VALUE);

                owner.sendSystemMessage(Component.translatable("message.complextalents.blackhole_execute",
                    entity.getName().getString()));

                TalentsMod.LOGGER.debug("Blackhole executed entity at {}% HP (threshold: {}% with mastery {})",
                    healthPercent * 100, executeThreshold * 100, masteryValue);
            }
        }
    }

    /**
     * Check if a conflux zone is active at a location
     */
    public static boolean isInConfluxZone(Vec3 position) {
        for (ConfluxZone zone : activeZones.values()) {
            if (position.distanceTo(zone.center) <= zone.radius) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the owner of a conflux zone at a position
     */
    public static ServerPlayer getConfluxOwner(Vec3 position, ServerLevel level) {
        for (ConfluxZone zone : activeZones.values()) {
            if (position.distanceTo(zone.center) <= zone.radius) {
                // Find the owner from online players
                ServerPlayer owner = level.getServer().getPlayerList().getPlayer(zone.ownerUUID);
                if (owner != null) {
                    return owner;
                }
            }
        }
        return null;
    }

    /**
     * Internal class representing an active Conflux zone
     */
    private static class ConfluxZone {
        Vec3 center;
        int level;
        UUID ownerUUID;
        ServerLevel serverLevel; // Reference to the world where this zone exists
        int durationTicks;
        int pulseInterval;
        int nextPulseTick;
        float radius;
        float damage;

        // Rank 2A: Maelstrom
        boolean maelstromActive = true; // Default path for now

        // Rank 2B: Focal Lens
        boolean focalLensActive = false;
        ElementType lastAppliedElement = null;
        int focalLensTicks = 0;

        // Rank 3A: Cataclysm
        boolean cataclysmActive = true; // Default path for now

        // Rank 3B: Shatterpoint
        boolean shatterpointActive = false;

        // Rank 4A: Supermassive Blackhole
        boolean isBlackhole = false; // Would be true with Rank 4A selected

        // Rank 4B: Reality Tear
        boolean isRealityTear = false;
        int pulverizedStacks = 0;
    }

    @Override
    public boolean hasBranchingAtRank(int rank) {
        return rank == 2 || rank == 3 || rank == 4;
    }

    @Override
    public Component getBranchDescription(int rank, TalentBranches.BranchChoice choice) {
        String key = String.format("talent.complextalents.elemental_conflux.rank%d.%s.desc",
            rank, choice == TalentBranches.BranchChoice.PATH_A ? "a" : "b");
        return Component.translatable(key);
    }

    @Override
    public Component getBranchName(int rank, TalentBranches.BranchChoice choice) {
        String key = String.format("talent.complextalents.elemental_conflux.rank%d.%s",
            rank, choice == TalentBranches.BranchChoice.PATH_A ? "a" : "b");
        return Component.translatable(key);
    }
}