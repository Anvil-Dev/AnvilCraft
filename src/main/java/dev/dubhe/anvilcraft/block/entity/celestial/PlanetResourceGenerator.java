package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates a {@link PlanetaryResourceSet} for a celestial body by applying
 * matching {@link PlanetResourceRecipe} instances and random generation logic.
 */
public final class PlanetResourceGenerator {

    private PlanetResourceGenerator() {}

    /**
     * Generate resources for the given celestial body.
     *
     * @param body           the matched celestial body
     * @param ageAnvilCount  the "time" anvil count (represents age in billion years)
     * @param level          the server level (for recipe manager and loot table access)
     * @param seed           deterministic seed for random generation
     * @return the generated resource set, never null
     */
    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    public static PlanetaryResourceSet generate(
        CelestialBodyData body,
        int ageAnvilCount,
        Level level,
        long seed
    ) {
        PlanetaryResourceSet set = new PlanetaryResourceSet();
        RandomSource random = RandomSource.create(seed);
        RecipeManager recipeManager = level.getRecipeManager();

        List<PlanetResourceRecipe> recipes = recipeManager.getAllRecipesFor(
            ModRecipeTypes.PLANET_RESOURCE_TYPE.get()
        ).stream().map(RecipeHolder::value).toList();

        PlanetResourceInput input = new PlanetResourceInput(body, ageAnvilCount);

        // Collect recipes by category
        PlanetResourceRecipe mineralRecipe = null;
        List<PlanetResourceRecipe> fluidRecipes = new ArrayList<>();
        List<PlanetResourceRecipe> giantItemRecipes = new ArrayList<>();
        List<PlanetResourceRecipe> giantFluidRecipes = new ArrayList<>();
        PlanetResourceRecipe biologicalRecipe = null;
        PlanetResourceRecipe offeringRecipe = null;
        PlanetResourceRecipe wastelandRecipe = null;

        for (PlanetResourceRecipe recipe : recipes) {
            if (!recipe.matches(input, level)) continue;
            switch (recipe.category()) {
                case MINERAL -> { if (mineralRecipe == null) mineralRecipe = recipe;
                }
                case FLUID -> fluidRecipes.add(recipe);
                case GIANT_ITEM -> giantItemRecipes.add(recipe);
                case GIANT_FLUID -> giantFluidRecipes.add(recipe);
                case BIOLOGICAL -> { if (biologicalRecipe == null) biologicalRecipe = recipe;
                }
                case OFFERING -> { if (offeringRecipe == null) offeringRecipe = recipe;
                }
                case WASTELAND -> { if (wastelandRecipe == null) wastelandRecipe = recipe;
                }
            }
        }

        // Generate based on body type
        if (body instanceof RockyPlanetData rocky) {
            generateMinerals(set, mineralRecipe, level.registryAccess(), random);
            generateFluids(set, fluidRecipes, rocky);

            boolean lifeEligible = isLifeEligible(rocky);
            boolean hasCivilization;

            if (lifeEligible) {
                // Roll for life first: COLD/HOT=5%, MILD=10% (or from recipe)
                int lifeChance = getLifeChance(rocky, biologicalRecipe);
                boolean lifeExists = lifeChance > 0 && random.nextInt(100) < lifeChance;

                if (lifeExists) {
                    // Life-bearing planet: 50% chance of civilization, else biological resources
                    hasCivilization = tryCivilization(set, offeringRecipe, rocky, ageAnvilCount, random);
                    if (hasCivilization) {
                        set.setHasCivilization();
                    } else {
                        tryBiologicalLifeConfirmed(set, biologicalRecipe, rocky, level, random);
                    }
                } else {
                    // No life → try wasteland
                    tryWasteland(set, wastelandRecipe, rocky, ageAnvilCount, random);
                }
            }
        } else if (body instanceof GiantPlanetData) {
            generateGiantItems(set, giantItemRecipes, random);
            generateGiantFluids(set, giantFluidRecipes, random);
        }

        return set;
    }

    // === Minerals ===

    private static void generateMinerals(
        PlanetaryResourceSet set,
        @Nullable PlanetResourceRecipe recipe,
        HolderLookup.Provider registries,
        RandomSource random
    ) {
        if (recipe == null) return;
        PlanetResourceRecipe.MineralData md = recipe.mineralData();
        if (md == null) return;

        TagKey<Item> sourceTag = TagKey.create(Registries.ITEM, ResourceLocation.parse(md.sourceTag()));
        TagKey<Item> blacklistTag = TagKey.create(Registries.ITEM, ResourceLocation.parse(md.blacklistTag()));

        Set<ResourceLocation> blacklist = new HashSet<>();
        registries.lookupOrThrow(Registries.ITEM)
            .get(blacklistTag)
            .ifPresent(entries -> entries.forEach(
                holder -> blacklist.add(holder.unwrapKey().orElseThrow().location())
            ));

        List<ResourceLocation> candidates = new ArrayList<>();
        registries.lookupOrThrow(Registries.ITEM)
            .get(sourceTag)
            .ifPresent(entries -> entries.forEach(holder -> {
                ResourceLocation id = holder.unwrapKey().orElseThrow().location();
                if (!blacklist.contains(id)) {
                    candidates.add(id);
                }
            }));

        if (candidates.isEmpty()) return;
        Collections.shuffle(candidates, new java.util.Random(random.nextLong()));

        int step = md.step();
        int sum = 0;
        for (ResourceLocation candidate : candidates) {
            if (sum >= 100) break;
            int remaining = 100 - sum;
            int maxSteps = remaining / step;
            if (maxSteps <= 0) break;
            int steps = 1 + random.nextInt(maxSteps);
            int weight = steps * step;
            if (sum + weight > 100) {
                weight = remaining;
            }
            if (weight <= 0) continue;
            set.addMineral(new PlanetaryResourceSet.WeightedItemStack(candidate, weight));
            sum += weight;
        }
    }

    // === Fluids (rocky planets) ===

    private static void generateFluids(PlanetaryResourceSet set, List<PlanetResourceRecipe> recipes, RockyPlanetData rocky) {
        boolean isScorched = rocky.temperature() == Temperature.SCORCHED;
        for (PlanetResourceRecipe recipe : recipes) {
            PlanetResourceRecipe.FluidData fd = recipe.fluidData();
            if (fd != null && !fd.outputFluid().isEmpty()) {
                // Scorched planets only get lava; other temperatures only get non-lava fluids
                boolean isLava = fd.outputFluid().contains("lava");
                if (isScorched != isLava) continue;
                set.addFluid(new PlanetaryResourceSet.WeightedFluidStack(
                    ResourceLocation.parse(fd.outputFluid()), 100
                ));
            }
        }
    }

    // === Giant planet items ===

    private static void generateGiantItems(
        PlanetaryResourceSet set,
        List<PlanetResourceRecipe> recipes,
        RandomSource random
    ) {
        for (PlanetResourceRecipe recipe : recipes) {
            PlanetResourceRecipe.GiantData gd = recipe.giantData();
            if (gd != null) {
                for (PlanetResourceRecipe.WeightedEntry entry : gd.entries()) {
                    set.addGiantItem(new PlanetaryResourceSet.WeightedItemStack(
                        entry.resourceId(), entry.weight()
                    ));
                }
            }
        }
    }

    // === Giant planet fluids ===

    private static void generateGiantFluids(
        PlanetaryResourceSet set,
        List<PlanetResourceRecipe> recipes,
        RandomSource random
    ) {
        for (PlanetResourceRecipe recipe : recipes) {
            PlanetResourceRecipe.GiantData gd = recipe.giantData();
            if (gd != null) {
                for (PlanetResourceRecipe.WeightedEntry entry : gd.entries()) {
                    set.addGiantFluid(new PlanetaryResourceSet.WeightedFluidStack(
                        entry.resourceId(), entry.weight()
                    ));
                }
            }
        }
    }

    // === Life prerequisites ===

    private static boolean isLifeEligible(RockyPlanetData rocky) {
        if (rocky.liquidCoverage() == LiquidCoverage.NONE) return false;
        if (!rocky.hasAtmosphere()) return false;
        if (rocky.temperature() == Temperature.FREEZING) return false;
        return rocky.temperature() != Temperature.SCORCHED;
    }

    /**
     * Get the life chance percentage for a rocky planet.
     * Defaults (when no recipe overrides): COLD/HOT = 5%, MILD = 10%.
     */
    private static int getLifeChance(RockyPlanetData rocky, @Nullable PlanetResourceRecipe biologicalRecipe) {
        if (biologicalRecipe != null) {
            PlanetResourceRecipe.BiologicalData bd = biologicalRecipe.biologicalData();
            if (bd != null) {
                int chance = bd.lifeChances().forTemperature(rocky.temperature());
                if (chance > 0) return chance;
            }
        }
        return switch (rocky.temperature()) {
            case COLD, HOT -> 5;
            case MILD -> 10;
            default -> 0;
        };
    }

    // === Civilization (offerings) ===

    private static boolean tryCivilization(
        PlanetaryResourceSet set,
        @Nullable PlanetResourceRecipe recipe,
        RockyPlanetData rocky,
        int ageAnvilCount,
        RandomSource random
    ) {
        if (recipe == null) return false;
        PlanetResourceRecipe.OfferingData od = recipe.offeringData();
        if (od == null) return false;
        if (rocky.liquidCoverage() != LiquidCoverage.MEDIUM) return false;
        if (ageAnvilCount < od.ageMin() || ageAnvilCount > od.ageMax()) return false;
        if (random.nextInt(100) >= od.civilizationChance()) return false;

        for (PlanetResourceRecipe.WeightedEntry entry : od.entries()) {
            ResourceLocation id = entry.resourceId();
            if ("anvilcraft:gem_amulet_random".equals(id.toString())) {
                ResourceLocation randomAmulet = pickRandomGemAmulet(random);
                if (randomAmulet != null) {
                    set.addOffering(new PlanetaryResourceSet.WeightedItemStack(randomAmulet, entry.weight()));
                }
            } else {
                set.addOffering(new PlanetaryResourceSet.WeightedItemStack(id, entry.weight()));
            }
        }
        return true;
    }

    // === Biological resources (life already confirmed by caller) ===

    private static void tryBiologicalLifeConfirmed(
        PlanetaryResourceSet set,
        @Nullable PlanetResourceRecipe recipe,
        RockyPlanetData rocky,
        Level level,
        RandomSource random
    ) {
        if (recipe == null) return;
        PlanetResourceRecipe.BiologicalData bd = recipe.biologicalData();
        if (bd == null) return;

        boolean isHighCoverage = rocky.liquidCoverage() == LiquidCoverage.HIGH;

        TagKey<Item> blacklistTag = TagKey.create(Registries.ITEM, ResourceLocation.parse(bd.dropBlacklistTag()));
        Set<ResourceLocation> blacklist = buildItemBlacklist(level.registryAccess(), blacklistTag);

        // Collect item drop frequencies from all matching entities
        Map<ResourceLocation, Integer> dropFrequencies = new HashMap<>();
        level.registryAccess().lookupOrThrow(Registries.ENTITY_TYPE)
            .listElements()
            .forEach(holder -> {
                EntityType<?> entityType = holder.value();
                var cat = entityType.getCategory();
                boolean matches = isHighCoverage
                    ? cat == MobCategory.WATER_CREATURE
                       || cat == MobCategory.WATER_AMBIENT
                       || cat == MobCategory.UNDERGROUND_WATER_CREATURE
                    : cat == MobCategory.CREATURE;
                if (matches) {
                    collectEntityDropFrequencies(entityType, level, random, dropFrequencies, blacklist);
                }
            });

        if (!dropFrequencies.isEmpty()) {
            // Build flat list of (item, baseWeight) pairs, baseWeight already multiples of 10
            List<Map.Entry<ResourceLocation, Integer>> candidates = new ArrayList<>(dropFrequencies.entrySet());
            // Filter out zero-weight entries (entities with empty loot tables)
            candidates.removeIf(e -> e.getValue() <= 0);
            Collections.shuffle(candidates, new java.util.Random(random.nextLong()));

            // Same step-based algorithm as minerals: randomly pick weight (10/20/30/…), truncate at 100%
            final int step = 10;
            int sum = 0;
            for (Map.Entry<ResourceLocation, Integer> candidate : candidates) {
                if (sum >= 100) break;
                int remaining = 100 - sum;
                int maxSteps = remaining / step;
                if (maxSteps <= 0) break;
                int steps = 1 + random.nextInt(maxSteps);
                int weight = steps * step;
                if (sum + weight > 100) {
                    weight = remaining;
                }
                if (weight <= 0) continue;
                set.addBiologicalItem(new PlanetaryResourceSet.WeightedItemStack(candidate.getKey(), weight));
                sum += weight;
            }
        }

        if (rocky.temperature() == Temperature.MILD && !isHighCoverage) {
            for (PlanetResourceRecipe.WeightedEntry entry : bd.mildExtraFluids()) {
                if (random.nextInt(100) < entry.weight()) {
                    set.addBiologicalFluid(new PlanetaryResourceSet.WeightedFluidStack(
                        entry.resourceId(), 100
                    ));
                }
            }
        }
    }

    // === Wasteland ===

    private static void tryWasteland(
        PlanetaryResourceSet set,
        @Nullable PlanetResourceRecipe recipe,
        RockyPlanetData rocky,
        int ageAnvilCount,
        RandomSource random
    ) {
        if (recipe == null) return;
        PlanetResourceRecipe.WastelandData wd = recipe.wastelandData();
        if (wd == null) return;
        if (rocky.liquidCoverage() == LiquidCoverage.HIGH) return;
        if (ageAnvilCount < wd.ageMin()) return;
        if (random.nextInt(100) >= wd.wastelandChance()) return;

        set.setWasteland();
        for (PlanetResourceRecipe.WeightedEntry entry : wd.entries()) {
            set.addWastelandItem(new PlanetaryResourceSet.WeightedItemStack(
                entry.resourceId(), entry.weight()
            ));
        }
    }

    // === Entity drop collection ===

    /**
     * Simulate an entity's loot table and collect drop frequencies into the given map.
     * Entities with empty loot tables or only air drops are skipped (no entries added).
     */
    private static void collectEntityDropFrequencies(
        EntityType<?> entityType,
        Level level,
        RandomSource random,
        Map<ResourceLocation, Integer> dropFrequencies,
        Set<ResourceLocation> blacklist
    ) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Entity entity = entityType.create(serverLevel);
        if (!(entity instanceof LivingEntity living)) {
            if (entity != null) entity.discard();
            return;
        }

        ResourceKey<LootTable> lootTableKey = living.getLootTable();
        entity.discard();

        LootTable lootTable = serverLevel.getServer()
            .reloadableRegistries()
            .getLootTable(lootTableKey);

        // Recreate entity for loot context
        Entity rollEntity = entityType.create(serverLevel);
        if (!(rollEntity instanceof LivingEntity rollLiving)) {
            if (rollEntity != null) rollEntity.discard();
            return;
        }

        int simulationRolls = 200;
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        int totalDrops = 0;

        for (int i = 0; i < simulationRolls; i++) {
            LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, rollLiving)
                .withParameter(LootContextParams.ORIGIN, rollLiving.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE,
                    rollLiving.damageSources().generic())
                .create(LootContextParamSets.ENTITY);

            List<ItemStack> drops = lootTable.getRandomItems(params, random.nextLong());
            for (ItemStack drop : drops) {
                if (drop.isEmpty()) continue;
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(drop.getItem());
                // Skip air items (entities with empty or invalid loot tables)
                if ("minecraft:air".equals(id.toString())) continue;
                if (blacklist.contains(id)) continue;
                counts.merge(id, drop.getCount(), Integer::sum);
                totalDrops += drop.getCount();
            }
        }

        rollEntity.discard();

        if (totalDrops > 0) {
            for (Map.Entry<ResourceLocation, Integer> entry : counts.entrySet()) {
                int weight = Math.max(10, (entry.getValue() * 100) / totalDrops);
                weight = ((weight + 5) / 10) * 10;
                dropFrequencies.merge(entry.getKey(), weight, Integer::sum);
            }
        }
    }

    // === Helpers ===

    @Nullable
    private static ResourceLocation pickRandomGemAmulet(RandomSource random) {
        List<ResourceLocation> knownAmulets = List.of(
            ResourceLocation.parse("anvilcraft:emerald_amulet"),
            ResourceLocation.parse("anvilcraft:topaz_amulet"),
            ResourceLocation.parse("anvilcraft:ruby_amulet"),
            ResourceLocation.parse("anvilcraft:sapphire_amulet")
        );
        return knownAmulets.get(random.nextInt(knownAmulets.size()));
    }

    private static Set<ResourceLocation> buildItemBlacklist(
        HolderLookup.Provider registries,
        TagKey<Item> blacklistTag
    ) {
        Set<ResourceLocation> blacklist = new HashSet<>();
        registries.lookupOrThrow(Registries.ITEM)
            .get(blacklistTag)
            .ifPresent(entries -> entries.forEach(
                holder -> blacklist.add(holder.unwrapKey().orElseThrow().location())
            ));
        return blacklist;
    }
}
