package com.complextalents.stats;

import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores the cost matrix for stat ranks across different classes.
 */
public class ClassCostMatrix {

    private static final Map<ResourceLocation, Map<StatType, Integer>> MATRIX = new HashMap<>();


    static {
        // Costs are now registered dynamically by each origin module
    }

    public static CostBuilder defineCosts(String originPath) {
        return new CostBuilder(ResourceLocation.fromNamespaceAndPath("complextalents", originPath));
    }

    public static CostBuilder defineCosts(ResourceLocation originId) {
        return new CostBuilder(originId);
    }

    public static int getCost(ResourceLocation originId, StatType stat) {
        Map<StatType, Integer> classCosts = MATRIX.get(originId);
        if (classCosts == null) return 4; // Default to most expensive if unknown
        return classCosts.getOrDefault(stat, 4);
    }

    public static class CostBuilder {
        private final Map<StatType, Integer> costs = new HashMap<>();

        public CostBuilder(ResourceLocation id) {
            MATRIX.put(id, costs);
        }

        public CostBuilder cost(StatType stat, int cost) {
            costs.put(stat, cost);
            return this;
        }
    }
}
