package com.complextalents.skill.form;

import com.complextalents.TalentsMod;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for skill form definitions.
 * <p>
 * Thread-safe singleton following the SkillRegistry pattern.
 * Skills that enhance other skills when activated (stances, forms, ultimates)
 * register their SkillFormDefinition here.
 */
public class SkillFormRegistry {

    private static SkillFormRegistry INSTANCE;

    private final Map<ResourceLocation, SkillFormDefinition> formDefinitions;
    private final Object registryLock = new Object();

    private SkillFormRegistry() {
        this.formDefinitions = new ConcurrentHashMap<>();
    }

    /**
     * Get the singleton instance of the registry.
     */
    public static SkillFormRegistry getInstance() {
        if (INSTANCE == null) {
            synchronized (SkillFormRegistry.class) {
                if (INSTANCE == null) {
                    INSTANCE = new SkillFormRegistry();
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Register a skill form definition.
     *
     * @param definition The form definition to register
     * @throws IllegalArgumentException if a form with this ID is already registered
     */
    public void register(SkillFormDefinition definition) {
        synchronized (registryLock) {
            if (formDefinitions.containsKey(definition.getFormSkillId())) {
                throw new IllegalArgumentException("Form skill " + definition.getFormSkillId() + " already registered");
            }
            formDefinitions.put(definition.getFormSkillId(), definition);
            TalentsMod.LOGGER.debug("Registered skill form: {}", definition.getFormSkillId());
        }
    }

    /**
     * Get a skill form definition by ID.
     *
     * @param formSkillId The form skill's ResourceLocation
     * @return The form definition, or null if not found
     */
    @Nullable
    public SkillFormDefinition getForm(ResourceLocation formSkillId) {
        return formDefinitions.get(formSkillId);
    }

    /**
     * Check if a skill ID is a form skill.
     *
     * @param skillId The skill ID to check
     * @return true if this skill has a registered form definition
     */
    public boolean isFormSkill(ResourceLocation skillId) {
        return formDefinitions.containsKey(skillId);
    }

    /**
     * Get all registered form skill IDs.
     *
     * @return A collection of all form skill IDs
     */
    public Collection<ResourceLocation> getAllFormIds() {
        return formDefinitions.keySet();
    }
}
