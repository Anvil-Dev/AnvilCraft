package dev.dubhe.anvilcraft.data;

import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public class MyBlockTagGenerator extends FabricTagProvider<Block> {
    public MyBlockTagGenerator(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, Registries.BLOCK, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider arg) {
        this.getOrCreateTagBuilder(ModBlockTags.MAGNET).setReplace(false)
                .add(ModBlocks.MAGNET_BLOCK)
                .add(ModBlocks.HOLLOW_MAGNET_BLOCK)
                .add(ModBlocks.FERRITE_CORE_MAGNET_BLOCK);
        this.getOrCreateTagBuilder(BlockTags.ANVIL).setReplace(false)
                .add(ModBlocks.ROYAL_ANVIL);
    }
}
