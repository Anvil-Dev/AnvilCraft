package dev.dubhe.anvilcraft.data.generator;

import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

public class MyBlockTagGenerator extends FabricTagProvider<Block> {
    public MyBlockTagGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.BLOCK, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        this.getOrCreateTagBuilder(ModBlockTags.MAGNET).setReplace(false)
                .add(ModBlocks.MAGNET_BLOCK.get())
                .add(ModBlocks.HOLLOW_MAGNET_BLOCK.get())
                .add(ModBlocks.FERRITE_CORE_MAGNET_BLOCK.get());
        this.getOrCreateTagBuilder(ModBlockTags.REDSTONE_TORCH).setReplace(false)
                .add(Blocks.REDSTONE_WALL_TORCH)
                .add(Blocks.REDSTONE_TORCH);
        this.getOrCreateTagBuilder(BlockTags.ANVIL).setReplace(false)
                .add(ModBlocks.ROYAL_ANVIL.get());
        this.getOrCreateTagBuilder(ModBlockTags.MUSHROOM_BLOCK).setReplace(false)
                .add(Blocks.BROWN_MUSHROOM_BLOCK)
                .add(Blocks.RED_MUSHROOM_BLOCK)
                .add(Blocks.MUSHROOM_STEM);
        this.getOrCreateTagBuilder(ModBlockTags.CANT_BROKEN_ANVIL).setReplace(false)
                .add(ModBlocks.ROYAL_ANVIL.get());
        this.getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_PICKAXE).setReplace(false)
                .add(ModBlocks.STAMPING_PLATFORM.get())
                .add(ModBlocks.ROYAL_ANVIL.get())
                .add(ModBlocks.MAGNET_BLOCK.get())
                .add(ModBlocks.HOLLOW_MAGNET_BLOCK.get())
                .add(ModBlocks.FERRITE_CORE_MAGNET_BLOCK.get())
                .add(ModBlocks.AUTO_CRAFTER.get())
                .add(ModBlocks.CHUTE.get())
                .add(ModBlocks.ROYAL_STEEL_BLOCK.get())
                .add(ModBlocks.ROYAL_GRINDSTONE.get())
                .add(ModBlocks.ROYAL_SMITHING_TABLE.get())
                .add(ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK.get())
                .add(ModBlocks.CUT_ROYAL_STEEL_BLOCK.get())
                .add(ModBlocks.CUT_ROYAL_STEEL_SLAB.get())
                .add(ModBlocks.CUT_ROYAL_STEEL_STAIRS.get())
                .add(ModBlocks.LAVA_CAULDRON.get());
        this.getOrCreateTagBuilder(BlockTags.MINEABLE_WITH_AXE).setReplace(false)
                .add(ModBlocks.AUTO_CRAFTER.get())
                .add(ModBlocks.CHUTE.get());
        this.getOrCreateTagBuilder(BlockTags.BEACON_BASE_BLOCKS).setReplace(false)
                .add(ModBlocks.ROYAL_STEEL_BLOCK.get())
                .add(ModBlocks.CURSED_GOLD_BLOCK.get());
    }
}
