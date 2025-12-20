package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceBlockState;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.function.Consumer;

public class AnvilCollisionCraftRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlockTags.ANVIL_TIER_0)
            .hitBlock(ModBlocks.NEGATIVE_MATTER_BLOCK.get())
            .outputItem(ModBlocks.VOID_MATTER_BLOCK.asItem(), 8)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlocks.EMBER_ANVIL.get())
            .hitBlock(ModBlocks.FROST_METAL_BLOCK.get())
            .outputItem(ModItems.MULTIPHASE_MATTER.get(), 4)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlockTags.ANVIL_TIER_0)
            .hitBlock(ModBlocks.CORRUPTED_BEACON.get())
            .transformBlock(
                BlockStatePredicate.builder().of(ModBlocks.CONFINEMENT_CHAMBER).build(),
                ChanceBlockState.of(ModBlocks.CONFINED_TIME_ANVILON),
                8
            )
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlockTags.ANVIL_TIER_0)
            .hitBlock(ModBlocks.CORRUPTED_BEACON.get())
            .transformBlock(
                BlockStatePredicate.builder().of(ModBlocks.CONFINEMENT_CHAMBER).build(),
                ChanceBlockState.of(ModBlocks.CONFINED_ENERGY_ANVILON),
                4
            )
            .speed(128)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlockTags.ANVIL_TIER_0)
            .hitBlock(ModBlocks.SPACE_OVERCOMPRESSOR.get())
            .transformBlock(
                BlockStatePredicate.builder().of(ModBlocks.CONFINEMENT_CHAMBER).build(),
                ChanceBlockState.of(ModBlocks.CONFINED_SPACE_ANVILON),
                8
            )
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlockTags.ANVIL_TIER_0)
            .hitBlock(ModBlocks.SPACE_OVERCOMPRESSOR.get())
            .transformBlock(
                BlockStatePredicate.builder().of(ModBlocks.CONFINEMENT_CHAMBER).build(),
                ChanceBlockState.of(ModBlocks.CONFINED_ENERGY_ANVILON),
                4
            )
            .speed(128)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlockTags.ANVIL_TIER_0)
            .hitBlock(ModBlocks.GIANT_ANVIL.get())
            .transformBlock(
                BlockStatePredicate.builder().of(ModBlocks.CONFINEMENT_CHAMBER).build(),
                ChanceBlockState.of(ModBlocks.CONFINED_MASS_ANVILON),
                16
            )
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlockTags.ANVIL_TIER_0)
            .hitBlock(ModBlocks.GIANT_ANVIL.get())
            .transformBlock(
                BlockStatePredicate.builder().of(ModBlocks.CONFINEMENT_CHAMBER).build(),
                ChanceBlockState.of(ModBlocks.CONFINED_ENERGY_ANVILON),
                8
            )
            .speed(128)
            .save(provider);

        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlockTags.ANVIL_TIER_1)
            .consume(false)
            .hitBlock(Blocks.REDSTONE_BLOCK)
            .outputItem(ModItems.URANIUM_NUGGET.get(), 6)
            .outputItem(ModItems.URANIUM_NUGGET.get(), 4, 0.5f)
            .outputItem(ModItems.URANIUM_NUGGET.get(), 2, 0.25f)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlockTags.ANVIL_TIER_1)
            .consume(false)
            .hitBlock(ModBlocks.LEVITATION_POWDER_BLOCK.get())
            .outputItem(ModItems.NEGATIVE_MATTER_NUGGET.get(), 6)
            .outputItem(ModItems.NEGATIVE_MATTER_NUGGET.get(), 2, 0.5f)
            .outputItem(ModItems.NEGATIVE_MATTER_NUGGET.get(), 2, 0.25f)
            .save(provider);

        CompoundTag uraniumHeatableData = new CompoundTag();
        uraniumHeatableData.putInt("duration", 400);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlockTags.ANVIL_TIER_2)
            .consume(false)
            .hitBlock(ModBlocks.URANIUM_BLOCK.get())
            .transformBlock(
                BlockStatePredicate.builder().of(ModBlockTags.OVERHEATABLE).build(),
                ChanceBlockState.of(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK, uraniumHeatableData),
                16
            )
            .speed(256)
            .save(provider);
        CompoundTag plutoniumHeatableData = new CompoundTag();
        plutoniumHeatableData.putInt("duration", 1200);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlockTags.ANVIL_TIER_2)
            .consume(false)
            .hitBlock(ModBlocks.PLUTONIUM_BLOCK.get())
            .transformBlock(
                BlockStatePredicate.builder().of(ModBlockTags.OVERHEATABLE).build(),
                ChanceBlockState.of(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK, plutoniumHeatableData),
                16
            )
            .speed(256)
            .save(provider);
    }

    private static void forEachAnvil(Consumer<Block> block, Block... anvils) {
        for (Block anvil : anvils) {
            block.accept(anvil);
        }
    }
}