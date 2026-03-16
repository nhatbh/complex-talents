package com.complextalents.skill;

import com.complextalents.TalentsMod;
import com.complextalents.targeting.TargetType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for all skills.
 * Thread-safe singleton following the ReactionRegistry pattern.
 */
public class SkillRegistry {

    private static SkillRegistry INSTANCE;

    private final Map<ResourceLocation, Skill> skillsById;
    private final Map<SkillNature, List<Skill>> skillsByNature;
    private final Object registryLock = new Object();
    private volatile boolean initialized = false;

    private SkillRegistry() {
        this.skillsById = new ConcurrentHashMap<>();
        this.skillsByNature = new ConcurrentHashMap<>();
        for (SkillNature nature : SkillNature.values()) {
            skillsByNature.put(nature, new ArrayList<>());
        }
    }

    /**
     * Get the singleton instance of the SkillRegistry.
     */
    public static SkillRegistry getInstance() {
        if (INSTANCE == null) {
            synchronized (SkillRegistry.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SkillRegistry();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Initialize the registry with default skills.
     * Called during mod common setup.
     */
    public void initialize() {
        synchronized (registryLock) {
            if (initialized) {
                TalentsMod.LOGGER.warn("SkillRegistry already initialized");
                return;
            }

            TalentsMod.LOGGER.info("Initializing Skill Registry");

            // Register default example skills
            registerDefaultSkills();

            initialized = true;
            TalentsMod.LOGGER.info("Registered {} skills", skillsById.size());
        }
    }

    /**
     * Register a skill.
     *
     * @param skill The skill to register
     * @throws IllegalArgumentException if a skill with the same ID is already
     *                                  registered
     */
    public void register(Skill skill) {
        Objects.requireNonNull(skill, "Skill cannot be null");

        synchronized (registryLock) {
            if (skillsById.containsKey(skill.getId())) {
                throw new IllegalArgumentException("Skill " + skill.getId() + " already registered");
            }

            skillsById.put(skill.getId(), skill);
            skillsByNature.get(skill.getNature()).add(skill);

            TalentsMod.LOGGER.debug("Registered skill: {}", skill.getId());
        }
    }

    /**
     * Register a skill, replacing any existing registration.
     *
     * @param skill The skill to register
     */
    public void registerOrReplace(Skill skill) {
        Objects.requireNonNull(skill, "Skill cannot be null");

        synchronized (registryLock) {
            // Remove from nature list if replacing
            if (skillsById.containsKey(skill.getId())) {
                Skill old = skillsById.get(skill.getId());
                skillsByNature.get(old.getNature()).remove(old);
            }

            skillsById.put(skill.getId(), skill);
            skillsByNature.get(skill.getNature()).add(skill);

            TalentsMod.LOGGER.debug("Registered/replaced skill: {}", skill.getId());
        }
    }

    /**
     * Get a skill by ID.
     *
     * @param skillId The skill ID
     * @return The skill, or null if not found
     */
    @Nullable
    public Skill getSkill(ResourceLocation skillId) {
        return skillsById.get(skillId);
    }

    /**
     * Get all skills of a specific nature.
     *
     * @param nature The skill nature
     * @return Unmodifiable list of skills with the given nature
     */
    public List<Skill> getSkillsByNature(SkillNature nature) {
        return Collections.unmodifiableList(skillsByNature.getOrDefault(nature, Collections.emptyList()));
    }

    /**
     * Get all registered skills.
     *
     * @return Unmodifiable collection of all skills
     */
    public Collection<Skill> getAllSkills() {
        return Collections.unmodifiableCollection(skillsById.values());
    }

    /**
     * Check if a skill exists.
     *
     * @param skillId The skill ID
     * @return true if the skill is registered
     */
    public boolean hasSkill(ResourceLocation skillId) {
        return skillsById.containsKey(skillId);
    }

    /**
     * Get the skill nature by ID.
     * Convenience method for the event pipeline.
     *
     * @param skillId The skill ID
     * @return The skill nature, or ACTIVE if not found
     */
    public SkillNature getSkillNature(ResourceLocation skillId) {
        Skill skill = getSkill(skillId);
        return skill != null ? skill.getNature() : SkillNature.ACTIVE;
    }

    /**
     * Get the targeting type by ID.
     * Convenience method for the event pipeline.
     *
     * @param skillId The skill ID
     * @return The targeting type, or NONE if not found
     */
    public TargetType getTargetingType(ResourceLocation skillId) {
        Skill skill = getSkill(skillId);
        return skill != null ? skill.getTargetingType() : TargetType.NONE;
    }

    /**
     * Unregister a skill.
     *
     * @param skillId The skill ID to unregister
     * @return The unregistered skill, or null if not found
     */
    @Nullable
    public Skill unregister(ResourceLocation skillId) {
        synchronized (registryLock) {
            Skill removed = skillsById.remove(skillId);
            if (removed != null) {
                skillsByNature.get(removed.getNature()).remove(removed);
                TalentsMod.LOGGER.debug("Unregistered skill: {}", skillId);
            }
            return removed;
        }
    }

    /**
     * Clear all registered skills.
     * Useful for testing or reload scenarios.
     */
    public void clear() {
        synchronized (registryLock) {
            skillsById.clear();
            for (List<Skill> list : skillsByNature.values()) {
                list.clear();
            }
            initialized = false;
            TalentsMod.LOGGER.info("Cleared skill registry");
        }
    }

    /**
     * Reload the registry, re-registering all default skills.
     */
    public void reload() {
        synchronized (registryLock) {
            clear();
            initialize();
            TalentsMod.LOGGER.info("Reloaded skill registry");
        }
    }

    /**
     * Gets statistics about the registry.
     *
     * @return Map of statistic names to values
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_skills", skillsById.size());
        stats.put("initialized", initialized);

        // Count by nature
        Map<String, Long> natureCounts = new HashMap<>();
        for (Skill skill : skillsById.values()) {
            natureCounts.merge(skill.getNature().name(), 1L, Long::sum);
        }
        stats.put("nature_distribution", natureCounts);

        return stats;
    }

    /**
     * Register default example skills.
     * This is called during initialization.
     * Mods can add their own skills by calling register().
     */
    private void registerDefaultSkills() {
        TalentsMod.LOGGER.info("Registering example skills...");

        // Register High Priest skills
        com.complextalents.impl.highpriest.skills.seraphsedge.SeraphicEchoSkill.register();

        TalentsMod.LOGGER.info("Example skills registered successfully");
    }
}
