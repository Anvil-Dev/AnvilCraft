package dev.dubhe.anvilcraft.block.entity.celestial;

import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipesRecord;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates a {@link PlanetaryResourceSet} for a celestial body by applying
 * matching {@link PlanetResourceRecipe} instances and random generation logic.
 */
public final class PlanetResourceGenerator {

    private PlanetResourceGenerator() {}

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    public static PlanetaryResourceSet generate(
        CelestialBodyData body,
        int ageAnvilCount,
        Level level,
        long seed
    ) {
        PlanetaryResourceSet set = new PlanetaryResourceSet();
        RandomSource random = RandomSource.create(seed);
        if (!(level instanceof ServerLevel serverLevel)) return set;

        List<PlanetResourceRecipe> recipes = new ArrayList<>();
        for (RecipeHolder<PlanetResourceRecipe> holder : RecipesRecord.getRecipes(serverLevel).byType(ModRecipeTypes.PLANET_RESOURCE.get())) {
            recipes.add(holder.value());
        }

        PlanetResourceInput input = new PlanetResourceInput(body, ageAnvilCount);

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
                case MINERAL -> { if (mineralRecipe == null) mineralRecipe = recipe; }
                case FLUID -> fluidRecipes.add(recipe);
                case GIANT_ITEM -> giantItemRecipes.add(recipe);
                case GIANT_FLUID -> giantFluidRecipes.add(recipe);
                case BIOLOGICAL -> { if (biologicalRecipe == null) biologicalRecipe = recipe; }
                case OFFERING -> { if (offeringRecipe == null) offeringRecipe = recipe; }
                case WASTELAND -> { if (wastelandRecipe == null) wastelandRecipe = recipe; }
            }
        }

        if (body instanceof RockyPlanetData rocky) {
            generateMinerals(set, mineralRecipe, level.registryAccess(), random);
            generateFluids(set, fluidRecipes, rocky);

            if (isLifeEligible(rocky)) {
                int lifeChance = getLifeChance(rocky, biologicalRecipe);
                boolean lifeExists = lifeChance > 0 && random.nextInt(100) < lifeChance;

                if (lifeExists) {
                    boolean hasCivilization = tryCivilization(set, offeringRecipe, rocky, ageAnvilCount, random);
                    if (hasCivilization) {
                        set.setHasCivilization();
                    } else {
                        tryBiologicalLifeConfirmed(set, biologicalRecipe, rocky, level, random);
                    }
                } else {
                    tryWasteland(set, wastelandRecipe, rocky, ageAnvilCount, random);
                }
            }
        } else if (body instanceof GiantPlanetData) {
            generateGiantItems(set, giantItemRecipes, random);
            generateGiantFluids(set, giantFluidRecipes, random);
        }

        return set;
    }

    private static void generateMinerals(
        PlanetaryResourceSet set,
        @Nullable PlanetResourceRecipe recipe,
        HolderLookup.Provider registries,
        RandomSource random
    ) {
        if (recipe == null) return;
        PlanetResourceRecipe.MineralData md = recipe.mineralData();
        if (md == null) return;

        TagKey<Item> sourceTag = TagKey.create(Registries.ITEM, Identifier.parse(md.sourceTag()));
        TagKey<Item> blacklistTag = TagKey.create(Registries.ITEM, Identifier.parse(md.blacklistTag()));

        Set<Identifier> blacklist = new HashSet<>();
        registries.lookupOrThrow(Registries.ITEM)
            .get(blacklistTag)
            .ifPresent(entries -> entries.forEach(
                holder -> blacklist.add(holder.unwrapKey().orElseThrow().identifier())
            ));

        List<Identifier> candidates = new ArrayList<>();
        registries.lookupOrThrow(Registries.ITEM)
            .get(sourceTag)
            .ifPresent(entries -> entries.forEach(holder -> {
                Identifier id = holder.unwrapKey().orElseThrow().identifier();
                if (!blacklist.contains(id)) {
                    candidates.add(id);
                }
            }));

        if (candidates.isEmpty()) return;
        Collections.shuffle(candidates, new java.util.Random(random.nextLong()));

        int step = md.step();
        int sum = 0;
        for (Identifier candidate : candidates) {
            if (sum >= 100) break;
            int remaining = 100 - sum;
            int maxSteps = remaining / step;
            if (maxSteps <= 0) break;
            int steps = 1 + random.nextInt(maxSteps);
            int weight = steps * step;
            if (sum + weight > 100) weight = remaining;
            if (weight <= 0) continue;
            set.addMineral(new PlanetaryResourceSet.WeightedItemStack(candidate, weight));
            sum += weight;
        }
    }

    private static void generateFluids(PlanetaryResourceSet set, List<PlanetResourceRecipe> recipes, RockyPlanetData rocky) {
        boolean isScorched = rocky.temperature() == Temperature.SCORCHED;
        for (PlanetResourceRecipe recipe : recipes) {
            PlanetResourceRecipe.FluidData fd = recipe.fluidData();
            if (fd != null && !fd.outputFluid().isEmpty()) {
                boolean isLava = fd.outputFluid().contains("lava");
                if (isScorched != isLava) continue;
                set.addFluid(new PlanetaryResourceSet.WeightedFluidStack(
                    Identifier.parse(fd.outputFluid()), 100
                ));
            }
        }
    }

    private static void generateGiantItems(PlanetaryResourceSet set, List<PlanetResourceRecipe> recipes, RandomSource random) {
        for (PlanetResourceRecipe recipe : recipes) {
            PlanetResourceRecipe.GiantData gd = recipe.giantData();
            if (gd != null) {
                for (PlanetResourceRecipe.WeightedEntry entry : gd.entries()) {
                    set.addGiantItem(new PlanetaryResourceSet.WeightedItemStack(entry.resourceId(), entry.weight()));
                }
            }
        }
    }

    private static void generateGiantFluids(PlanetaryResourceSet set, List<PlanetResourceRecipe> recipes, RandomSource random) {
        for (PlanetResourceRecipe recipe : recipes) {
            PlanetResourceRecipe.GiantData gd = recipe.giantData();
            if (gd != null) {
                for (PlanetResourceRecipe.WeightedEntry entry : gd.entries()) {
                    set.addGiantFluid(new PlanetaryResourceSet.WeightedFluidStack(entry.resourceId(), entry.weight()));
                }
            }
        }
    }

    private static boolean isLifeEligible(RockyPlanetData rocky) {
        if (rocky.liquidCoverage() == LiquidCoverage.NONE) return false;
        if (!rocky.hasAtmosphere()) return false;
        if (rocky.temperature() == Temperature.FREEZING) return false;
        return rocky.temperature() != Temperature.SCORCHED;
    }

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
            Identifier id = entry.resourceId();
            if ("anvilcraft:gem_amulet_random".equals(id.toString())) {
                Identifier randomAmulet = pickRandomGemAmulet(random);
                if (randomAmulet != null) {
                    set.addOffering(new PlanetaryResourceSet.WeightedItemStack(randomAmulet, entry.weight()));
                }
            } else if ("anvilcraft:gem_block_random".equals(id.toString())) {
                Identifier randomBlock = pickRandomGemBlock(random);
                if (randomBlock != null) {
                    set.addOffering(new PlanetaryResourceSet.WeightedItemStack(randomBlock, entry.weight()));
                }
            } else {
                set.addOffering(new PlanetaryResourceSet.WeightedItemStack(id, entry.weight()));
            }
        }
        return true;
    }

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

        TagKey<Item> blacklistTag = TagKey.create(Registries.ITEM, Identifier.parse(bd.dropBlacklistTag()));
        Set<Identifier> blacklist = buildItemBlacklist(level.registryAccess(), blacklistTag);

        Map<Identifier, Integer> dropFrequencies = new HashMap<>();
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
            List<Map.Entry<Identifier, Integer>> candidates = new ArrayList<>(dropFrequencies.entrySet());
            candidates.removeIf(e -> e.getValue() <= 0);
            Collections.shuffle(candidates, new java.util.Random(random.nextLong()));

            final int step = 10;
            int sum = 0;
            for (Map.Entry<Identifier, Integer> candidate : candidates) {
                if (sum >= 100) break;
                int remaining = 100 - sum;
                int maxSteps = remaining / step;
                if (maxSteps <= 0) break;
                int steps = 1 + random.nextInt(maxSteps);
                int weight = steps * step;
                if (sum + weight > 100) weight = remaining;
                if (weight <= 0) continue;
                set.addBiologicalItem(new PlanetaryResourceSet.WeightedItemStack(candidate.getKey(), weight));
                sum += weight;
            }
        }

        if (rocky.temperature() == Temperature.MILD && !isHighCoverage) {
            for (PlanetResourceRecipe.WeightedEntry entry : bd.mildExtraFluids()) {
                if (random.nextInt(100) < entry.weight()) {
                    set.addBiologicalFluid(new PlanetaryResourceSet.WeightedFluidStack(entry.resourceId(), 100));
                }
            }
        }
    }

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
            set.addWastelandItem(new PlanetaryResourceSet.WeightedItemStack(entry.resourceId(), entry.weight()));
        }
    }

    private static void collectEntityDropFrequencies(
        EntityType<?> entityType,
        Level level,
        RandomSource random,
        Map<Identifier, Integer> dropFrequencies,
        Set<Identifier> blacklist
    ) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        Entity entity = entityType.create(serverLevel, EntitySpawnReason.COMMAND);
        if (!(entity instanceof LivingEntity living)) {
            if (entity != null) entity.discard();
            return;
        }

        ResourceKey<LootTable> lootTableKey = living.getLootTable().orElse(null);
        entity.discard();

        if (lootTableKey == null) return;

        LootTable lootTable = serverLevel.getServer()
            .reloadableRegistries()
            .getLootTable(lootTableKey);

        Entity rollEntity = entityType.create(serverLevel, EntitySpawnReason.COMMAND);
        if (!(rollEntity instanceof LivingEntity rollLiving)) {
            if (rollEntity != null) rollEntity.discard();
            return;
        }

        int simulationRolls = 200;
        Map<Identifier, Integer> counts = new HashMap<>();
        AtomicInteger totalDrops = new AtomicInteger(0);

        for (int i = 0; i < simulationRolls; i++) {
            LootParams params = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.THIS_ENTITY, rollLiving)
                .withParameter(LootContextParams.ORIGIN, rollLiving.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE,
                    rollLiving.damageSources().generic())
                .create(LootContextParamSets.ENTITY);

            lootTable.getRandomItems(params, random.nextLong(), drop -> {
                if (drop.isEmpty()) return;
                Identifier id = drop.getItem().builtInRegistryHolder().key().identifier();
                if ("minecraft:air".equals(id.toString())) return;
                if (blacklist.contains(id)) return;
                counts.merge(id, drop.getCount(), Integer::sum);
                totalDrops.addAndGet(drop.getCount());
            });
        }

        rollEntity.discard();

        int total = totalDrops.get();
        if (total > 0) {
            for (Map.Entry<Identifier, Integer> entry : counts.entrySet()) {
                int weight = Math.max(10, (entry.getValue() * 100) / total);
                weight = ((weight + 5) / 10) * 10;
                dropFrequencies.merge(entry.getKey(), weight, Integer::sum);
            }
        }
    }

    @Nullable
    private static Identifier pickRandomGemAmulet(RandomSource random) {
        List<Identifier> knownAmulets = List.of(
            Identifier.parse("anvilcraft:emerald_amulet"),
            Identifier.parse("anvilcraft:topaz_amulet"),
            Identifier.parse("anvilcraft:ruby_amulet"),
            Identifier.parse("anvilcraft:sapphire_amulet")
        );
        return knownAmulets.get(random.nextInt(knownAmulets.size()));
    }

    @Nullable
    private static Identifier pickRandomGemBlock(RandomSource random) {
        List<Identifier> knownBlocks = List.of(
            Identifier.parse("minecraft:emerald_block"),
            Identifier.parse("anvilcraft:topaz_block"),
            Identifier.parse("anvilcraft:ruby_block"),
            Identifier.parse("anvilcraft:sapphire_block")
        );
        return knownBlocks.get(random.nextInt(knownBlocks.size()));
    }

    private static Set<Identifier> buildItemBlacklist(
        HolderLookup.Provider registries,
        TagKey<Item> blacklistTag
    ) {
        Set<Identifier> blacklist = new HashSet<>();
        registries.lookupOrThrow(Registries.ITEM)
            .get(blacklistTag)
            .ifPresent(entries -> entries.forEach(
                holder -> blacklist.add(holder.unwrapKey().orElseThrow().identifier())
            ));
        return blacklist;
    }
}
