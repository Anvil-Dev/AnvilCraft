package dev.dubhe.anvilcraft.data.recipe;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.mineral.AroundRequirement;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainActiveRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainHeat;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainPassiveRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainReplacementRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

public class MineralFountainRecipeLoader {
    public static void init(RegistrumRecipeProvider provider) {
        mineralFountainDeepslate(provider, Tags.Blocks.STORAGE_BLOCKS_RAW_COPPER, Blocks.DEEPSLATE_COPPER_ORE);
        mineralFountainDeepslate(provider, Tags.Blocks.STORAGE_BLOCKS_RAW_IRON, Blocks.DEEPSLATE_IRON_ORE);
        mineralFountainDeepslate(provider, Tags.Blocks.STORAGE_BLOCKS_RAW_GOLD, Blocks.DEEPSLATE_GOLD_ORE);
        mineralFountainDeepslate(provider, ModBlockTags.STORAGE_BLOCKS_RAW_ZINC, ModBlocks.DEEPSLATE_ZINC_ORE.get());
        mineralFountainDeepslate(provider, ModBlockTags.STORAGE_BLOCKS_RAW_TIN, ModBlocks.DEEPSLATE_TIN_ORE.get());
        mineralFountainDeepslate(provider, ModBlockTags.STORAGE_BLOCKS_RAW_LEAD, ModBlocks.DEEPSLATE_LEAD_ORE.get());
        mineralFountainDeepslate(provider, ModBlockTags.STORAGE_BLOCKS_RAW_SILVER, ModBlocks.DEEPSLATE_SILVER_ORE.get());
        mineralFountainDeepslate(provider, ModBlockTags.STORAGE_BLOCKS_RAW_TITANIUM, ModBlocks.DEEPSLATE_TITANIUM_ORE.get());
        mineralFountainDeepslate(provider, ModBlockTags.STORAGE_BLOCKS_RAW_TUNGSTEN, ModBlocks.DEEPSLATE_TUNGSTEN_ORE.get());
        mineralFountainDeepslate(provider, ModBlockTags.STORAGE_BLOCKS_RAW_URANIUM, ModBlocks.DEEPSLATE_URANIUM_ORE.get());

        // 熔岩：四周皆为熔岩源时向上方生成熔岩并供热；有排水口则直接灌入，否则放置熔岩方块
        MineralFountainActiveRecipe.builder()
            .fromBlock(from(Blocks.AIR))
            .require(new AroundRequirement(liquidSource(Blocks.LAVA), 4))
            .toBlock(result(Blocks.LAVA))
            .toFluid(new FluidStack(Fluids.LAVA, FluidType.BUCKET_VOLUME))
            .heat(new MineralFountainHeat(HeatTier.REDHOT, 20, 1))
            .save(provider, "lava");

        // 粗矿配方有概率被替换：主世界为虚空石或地核碎片矿，下界 / 末地概率更高。
        // 不写 replace 列表 ⇒ 可替换任意主动配方，实际只命中材料相容的粗矿配方
        replacement(provider, Level.OVERWORLD, ModBlocks.VOID_STONE.get(), 0.01);
        replacement(provider, Level.OVERWORLD, ModBlocks.EARTH_CORE_SHARD_ORE.get(), 0.01);
        replacement(provider, CelestialTravelManager.OVERWORLD_LIKE_LEVEL, ModBlocks.VOID_STONE.get(), 0.01);
        replacement(provider, CelestialTravelManager.OVERWORLD_LIKE_LEVEL, ModBlocks.EARTH_CORE_SHARD_ORE.get(), 0.01);
        replacement(provider, Level.NETHER, ModBlocks.EARTH_CORE_SHARD_ORE.get(), 0.1);
        replacement(provider, Level.END, ModBlocks.VOID_STONE.get(), 0.1);

        // 被动生产：无主动匹配且上方为空气时产出。
        // 下界 / 末地为粉尘与火山灰各半；其余维度由无维度、低优先级的火山灰兜底
        MineralFountainPassiveRecipe.builder()
            .priority(-1)
            .toBlock(result(ModBlocks.CINERITE.get()))
            .save(provider, "cinerite");
        MineralFountainPassiveRecipe.builder()
            .dimension(Level.NETHER.location())
            .weight(1)
            .toBlock(result(ModBlocks.NETHER_DUST.get()))
            .save(provider, "nether_dust");
        MineralFountainPassiveRecipe.builder()
            .dimension(Level.NETHER.location())
            .weight(1)
            .toBlock(result(ModBlocks.CINERITE.get()))
            .save(provider, "cinerite_in_nether");
        MineralFountainPassiveRecipe.builder()
            .dimension(Level.END.location())
            .weight(1)
            .toBlock(result(ModBlocks.END_DUST.get()))
            .save(provider, "end_dust");
        MineralFountainPassiveRecipe.builder()
            .dimension(Level.END.location())
            .weight(1)
            .toBlock(result(ModBlocks.CINERITE.get()))
            .save(provider, "cinerite_in_end");
    }

    private static BlockStatePredicate from(Block block) {
        return BlockStatePredicate.builder().of(block).build();
    }

    /** 流体源谓词：方块为该流体且 level=0。 */
    private static BlockStatePredicate liquidSource(Block block) {
        return BlockStatePredicate.builder()
            .of(block)
            .with(BlockStateProperties.LEVEL, 0)
            .build();
    }

    /** ChanceBlockState.of 只接受 Supplier，这里统一按方块构造。 */
    private static ChanceBlockState result(Block block) {
        return new ChanceBlockState(block.defaultBlockState(), 1.0f);
    }

    private static void replacement(
        RegistrumRecipeProvider provider,
        ResourceKey<Level> dimension,
        Block block,
        double chance
    ) {
        MineralFountainReplacementRecipe.builder()
            .dimension(dimension.location())
            .fromBlock(from(Blocks.DEEPSLATE))
            .chance(chance)
            .toBlock(result(block))
            .save(provider, replacementId(dimension, block));
    }

    private static String replacementId(ResourceKey<Level> dimension, Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getPath() + "_from_" + dimension.location().getPath();
    }

    private static void mineralFountainDeepslate(RegistrumRecipeProvider provider, TagKey<Block> require, Block block) {
        MineralFountainActiveRecipe.builder()
            .fromBlock(from(Blocks.DEEPSLATE))
            .require(new AroundRequirement(BlockStatePredicate.builder().of(require).build(), 4))
            .toBlock(result(block))
            .save(provider);
    }
}
