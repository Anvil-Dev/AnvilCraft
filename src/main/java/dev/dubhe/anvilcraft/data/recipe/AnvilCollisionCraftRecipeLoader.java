package dev.dubhe.anvilcraft.data.recipe;

import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.collision.AnvilCollisionCraftRecipe;
import dev.dubhe.anvilcraft.recipe.elements.InputBlock;
import dev.dubhe.anvilcraft.recipe.elements.OutputBlock;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;

public class AnvilCollisionCraftRecipeLoader {
    public static void init(RegistrateRecipeProvider provider) {
        AnvilCollisionCraftRecipe.builder()
            .anvil(BlockTags.ANVIL)
            .hitBlock(ModBlocks.NEGATIVE_MATTER_BLOCK.get())
            .outputItem(ModBlocks.VOID_MATTER_BLOCK.asItem(), 8)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlocks.EMBER_ANVIL.get())
            .hitBlock(ModBlocks.FROST_METAL_BLOCK.get())
            .outputItem(ModItems.MULTIPHASE_MATTER.get(), 4)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(BlockTags.ANVIL)
            .hitBlock(ModBlocks.CORRUPTED_BEACON.get())
            .transformBlock(InputBlock.of(ModBlocks.CONFINEMENT_CHAMBER), OutputBlock.of(ModBlocks.CONFINED_TIME_ANVILON), 8)
            .transformBlock(InputBlock.of(ModBlocks.CONFINEMENT_CHAMBER), OutputBlock.of(ModBlocks.CONFINED_ENERGY_ANVILON), 4)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(BlockTags.ANVIL)
            .hitBlock(ModBlocks.SPACE_OVERCOMPRESSOR.get())
            .transformBlock(InputBlock.of(ModBlocks.CONFINEMENT_CHAMBER), OutputBlock.of(ModBlocks.CONFINED_SPACE_ANVILON), 8)
            .transformBlock(InputBlock.of(ModBlocks.CONFINEMENT_CHAMBER), OutputBlock.of(ModBlocks.CONFINED_ENERGY_ANVILON), 4)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(BlockTags.ANVIL)
            .hitBlock(ModBlocks.GIANT_ANVIL.get())
            .transformBlock(InputBlock.of(ModBlocks.CONFINEMENT_CHAMBER), OutputBlock.of(ModBlocks.CONFINED_MASS_ANVILON), 16)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlocks.EMBER_ANVIL.get())
            .consume(false)
            .hitBlock(Blocks.REDSTONE_BLOCK)
            .outputItem(ModItems.URANIUM_NUGGET.get(), 6)
            .outputItem(ModItems.URANIUM_NUGGET.get(), 4, 0.5f)
            .outputItem(ModItems.URANIUM_NUGGET.get(), 2, 0.25f)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlocks.ROYAL_ANVIL.get())
            .consume(false)
            .hitBlock(Blocks.REDSTONE_BLOCK)
            .outputItem(ModItems.URANIUM_NUGGET.get(), 6)
            .outputItem(ModItems.URANIUM_NUGGET.get(), 4, 0.5f)
            .outputItem(ModItems.URANIUM_NUGGET.get(), 2, 0.25f)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlocks.EMBER_ANVIL.get())
            .consume(false)
            .hitBlock(ModBlocks.LEVITATION_POWDER_BLOCK.get())
            .outputItem(ModItems.NEGATIVE_MATTER_NUGGET.get(), 6)
            .outputItem(ModItems.NEGATIVE_MATTER_NUGGET.get(), 2, 0.5f)
            .outputItem(ModItems.NEGATIVE_MATTER_NUGGET.get(), 2, 0.25f)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(ModBlocks.ROYAL_ANVIL.get())
            .consume(false)
            .hitBlock(ModBlocks.LEVITATION_POWDER_BLOCK.get())
            .outputItem(ModItems.NEGATIVE_MATTER_NUGGET.get(), 6)
            .outputItem(ModItems.NEGATIVE_MATTER_NUGGET.get(), 2, 0.5f)
            .outputItem(ModItems.NEGATIVE_MATTER_NUGGET.get(), 2, 0.25f)
            .save(provider);
        CompoundTag heatableData = new CompoundTag();
        heatableData.putInt("duration", 200);
        AnvilCollisionCraftRecipe.builder()
            .anvil(BlockTags.ANVIL)
            .consume(false)
            .hitBlock(ModBlocks.URANIUM_BLOCK.get())
            .transformBlock(InputBlock.of(ModBlocks.EMBER_METAL_BLOCK), OutputBlock.of(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK, heatableData), 16)
            .save(provider);
        AnvilCollisionCraftRecipe.builder()
            .anvil(BlockTags.ANVIL)
            .consume(false)
            .hitBlock(ModBlocks.PLUTONIUM_BLOCK.get())
            .transformBlock(InputBlock.of(ModBlocks.EMBER_METAL_BLOCK), OutputBlock.of(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK, heatableData), 16)
            .save(provider);
    }
}