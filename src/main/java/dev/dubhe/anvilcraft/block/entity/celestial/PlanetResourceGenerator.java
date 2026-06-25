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

/// 通过应用匹配的 {@link PlanetResourceRecipe} 实例和随机生成逻辑，
/// 为天体生成 {@link PlanetaryResourceSet}。
public final class PlanetResourceGenerator {

    private PlanetResourceGenerator() {}

    /// 为给定天体生成资源。
    ///
    /// body - 匹配到的天体
    /// ageAnvilCount - "时间"砧子计数（表示年龄，单位为十亿年）
    /// level - 服务端世界（用于配方管理器和战利品表访问）
    /// seed - 确定性随机种子
    /// 返回生成的资源集，永不为null
    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    public static PlanetaryResourceSet generate(
        CelestialBodyData body,
        int ageAnvilCount,
        Level level,
        long seed,
        @Nullable ResourceLocation seedItemId
    ) {
        PlanetaryResourceSet set = new PlanetaryResourceSet();
        RandomSource random = RandomSource.create(seed);
        RecipeManager recipeManager = level.getRecipeManager();

        List<PlanetResourceRecipe> recipes = recipeManager.getAllRecipesFor(
            ModRecipeTypes.PLANET_RESOURCE_TYPE.get()
        ).stream().map(RecipeHolder::value).toList();

        PlanetResourceInput input = new PlanetResourceInput(body, ageAnvilCount);

        /// 按类别收集配方
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

        /// 根据天体类型生成资源
        if (body instanceof RockyPlanetData rocky) {
            generateMinerals(set, mineralRecipe, level.registryAccess(), random, seedItemId);
            generateFluids(set, fluidRecipes, rocky);

            boolean lifeEligible = isLifeEligible(rocky);
            boolean hasCivilization;

            if (lifeEligible) {
                /// 首先检定生命：COLD/HOT=5%，MILD=10%（或由配方指定）
                int lifeChance = getLifeChance(rocky, biologicalRecipe);
                boolean lifeExists = lifeChance > 0 && random.nextInt(100) < lifeChance;

                if (lifeExists) {
                    /// 有生命的行星：50%概率有文明，否则有生物资源
                    hasCivilization = tryCivilization(set, offeringRecipe, rocky, ageAnvilCount, random);
                    if (hasCivilization) {
                        set.setHasCivilization();
                    } else {
                        tryBiologicalLifeConfirmed(set, biologicalRecipe, rocky, level, random);
                    }
                } else {
                    /// 无生命 → 尝试废土
                    tryWasteland(set, wastelandRecipe, rocky, ageAnvilCount, random);
                }
            }
        } else if (body instanceof GiantPlanetData) {
            generateGiantItems(set, giantItemRecipes, random);
            generateGiantFluids(set, giantFluidRecipes, random);
        }

        return set;
    }

    /// === 矿物 ===

    private static void generateMinerals(
        PlanetaryResourceSet set,
        @Nullable PlanetResourceRecipe recipe,
        HolderLookup.Provider registries,
        RandomSource random,
        @Nullable ResourceLocation seedItemId
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
        int candidateIndex = 0;
        for (ResourceLocation candidate : candidates) {
            if (sum >= 100) break;
            int remaining = 100 - sum;
            int maxSteps = remaining / step;
            if (maxSteps <= 0) break;
            /// 加权随机偏向低百分比（对早期候选更友好）。
            /// 指数 = 1 + 1/(n+1)：从2.0开始（强偏斜），渐近趋向1.0（均匀分布）。
            float exponent = 1.0f + 1.0f / (candidateIndex + 1);
            float skewed = (float) Math.pow(random.nextFloat(), exponent);
            int steps = 1 + (int) (skewed * maxSteps);
            /// 加成：如果种子物品匹配该矿物，额外增加1步
            if (seedItemId != null && candidate.equals(seedItemId)) {
                steps += 1;
            }
            int weight = steps * step;
            if (sum + weight > 100) {
                weight = remaining;
            }
            if (weight <= 0) continue;
            set.addMineral(new PlanetaryResourceSet.WeightedItemStack(candidate, weight));
            sum += weight;
            candidateIndex++;
        }
    }

    /// === 流体（岩石行星） ===

    private static void generateFluids(PlanetaryResourceSet set, List<PlanetResourceRecipe> recipes, RockyPlanetData rocky) {
        boolean isScorched = rocky.temperature() == Temperature.SCORCHED;
        for (PlanetResourceRecipe recipe : recipes) {
            PlanetResourceRecipe.FluidData fd = recipe.fluidData();
            if (fd != null && !fd.outputFluid().isEmpty()) {
                /// 焦灼行星只产熔岩；其他温度的行星只产非熔岩流体
                boolean isLava = fd.outputFluid().contains("lava");
                if (isScorched != isLava) continue;
                set.addFluid(new PlanetaryResourceSet.WeightedFluidStack(
                    ResourceLocation.parse(fd.outputFluid()), 100
                ));
            }
        }
    }

    /// === 气态行星物品 ===

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

    /// === 气态行星流体 ===

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

    /// === 生命前提条件 ===

    private static boolean isLifeEligible(RockyPlanetData rocky) {
        if (rocky.liquidCoverage() == LiquidCoverage.NONE) return false;
        if (!rocky.hasAtmosphere()) return false;
        if (rocky.temperature() == Temperature.FREEZING) return false;
        return rocky.temperature() != Temperature.SCORCHED;
    }

    /// 获取岩石行星生命出现的百分比概率。
    /// 默认值（配方未覆盖时）：COLD/HOT=5%，MILD=10%。
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

    /// === 文明（祭品） ===

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
            } else if ("anvilcraft:gem_block_random".equals(id.toString())) {
                ResourceLocation randomBlock = pickRandomGemBlock(random);
                if (randomBlock != null) {
                    set.addOffering(new PlanetaryResourceSet.WeightedItemStack(randomBlock, entry.weight()));
                }
            } else {
                set.addOffering(new PlanetaryResourceSet.WeightedItemStack(id, entry.weight()));
            }
        }
        return true;
    }

    /// === 生物资源（调用方已确认存在生命） ===

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

        /// 从所有匹配实体中收集物品掉落频率
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
            /// 构建(item, baseWeight)对列表，baseWeight已是10的倍数
            List<Map.Entry<ResourceLocation, Integer>> candidates = new ArrayList<>(dropFrequencies.entrySet());
            /// 过滤掉零权重条目（战利品表为空的实体）
            candidates.removeIf(e -> e.getValue() <= 0);
            Collections.shuffle(candidates, new java.util.Random(random.nextLong()));

            /// 加权随机偏向低百分比 —— 与矿物相同的多样性逻辑。
            /// 早期候选（洗牌列表中靠前的物品）更容易获得中等百分比，
            /// 这样能出现更多不同的生物掉落物。
            final int step = 10;
            int sum = 0;
            int candidateIndex = 0;
            for (Map.Entry<ResourceLocation, Integer> candidate : candidates) {
                if (sum >= 100) break;
                int remaining = 100 - sum;
                int maxSteps = remaining / step;
                if (maxSteps <= 0) break;
                float exponent = 1.0f + 1.0f / (candidateIndex + 1);
                float skewed = (float) Math.pow(random.nextFloat(), exponent);
                int steps = 1 + (int) (skewed * maxSteps);
                int weight = steps * step;
                if (sum + weight > 100) {
                    weight = remaining;
                }
                if (weight <= 0) continue;
                set.addBiologicalItem(new PlanetaryResourceSet.WeightedItemStack(candidate.getKey(), weight));
                sum += weight;
                candidateIndex++;
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

    /// === 废土 ===

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

    /// === 实体掉落收集 ===

    /// 模拟实体的战利品表并将掉落频率收集到给定映射中。
    /// 战利品表为空或只有空气掉落的实体会被跳过（不添加条目）。
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

        /// 重新创建实体以构建战利品上下文
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
                /// 跳过空气物品（战利品表为空或无效的实体）
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

    /// === 辅助方法 ===

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

    @Nullable
    private static ResourceLocation pickRandomGemBlock(RandomSource random) {
        List<ResourceLocation> knownBlocks = List.of(
            ResourceLocation.parse("minecraft:emerald_block"),
            ResourceLocation.parse("anvilcraft:topaz_block"),
            ResourceLocation.parse("anvilcraft:ruby_block"),
            ResourceLocation.parse("anvilcraft:sapphire_block")
        );
        return knownBlocks.get(random.nextInt(knownBlocks.size()));
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
