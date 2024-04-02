package dev.dubhe.anvilcraft.data.generator;

import dev.dubhe.anvilcraft.init.ModBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.world.item.Items;

public class MyBlockLootGenerator extends FabricBlockLootTableProvider {

    protected MyBlockLootGenerator(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generate() {
        this.dropSelf(ModBlocks.ROYAL_ANVIL.get());
        this.dropSelf(ModBlocks.ROYAL_GRINDSTONE.get());
        this.dropSelf(ModBlocks.ROYAL_SMITHING_TABLE.get());
        this.dropSelf(ModBlocks.MAGNET_BLOCK.get());
        this.dropSelf(ModBlocks.HOLLOW_MAGNET_BLOCK.get());
        this.dropSelf(ModBlocks.FERRITE_CORE_MAGNET_BLOCK.get());
        this.dropSelf(ModBlocks.AUTO_CRAFTER.get());
        this.dropSelf(ModBlocks.CHUTE.get());
        this.dropSelf(ModBlocks.ROYAL_STEEL_BLOCK.get());
        this.dropSelf(ModBlocks.SMOOTH_ROYAL_STEEL_BLOCK.get());
        this.dropSelf(ModBlocks.CUT_ROYAL_STEEL_BLOCK.get());
        this.add(ModBlocks.CUT_ROYAL_STEEL_SLAB.get(), this::createSlabItemTable);
        this.dropSelf(ModBlocks.CUT_ROYAL_STEEL_STAIRS.get());
        this.dropSelf(ModBlocks.STAMPING_PLATFORM.get());
        this.dropOther(ModBlocks.LAVA_CAULDRON.get(), Items.CAULDRON);
    }
}
