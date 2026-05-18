package dev.dubhe.anvilcraft.integration.jei.recipe;

import com.google.common.collect.ImmutableList;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.block.Blocks;

public class BeaconConversionRecipe {
    public final int cursedGoldBlockLayers;
    public final int cursedGoldBlockCount;
    public final float chance;
    public final ChanceItemStack corruptedBeaconOutput;
    public final ChanceItemStack beaconOutput;

    public BeaconConversionRecipe(int cursedGoldBlockLayers, float chance) {
        this.cursedGoldBlockLayers = cursedGoldBlockLayers;
        this.chance = chance;
        int count = 0;
        for (int i = 0; i < cursedGoldBlockLayers; i++) {
            count += (2 * i + 3) * (2 * i + 3);
        }
        this.cursedGoldBlockCount = count;
        this.corruptedBeaconOutput = ChanceItemStack.of(ItemStackTemplate.fromNonEmptyStack(ModBlocks.CORRUPTED_BEACON.asStack()), chance);
        this.beaconOutput = ChanceItemStack.of(new ItemStackTemplate(Blocks.BEACON.asItem(), 1), 1.0F - chance);
    }

    public static ImmutableList<BeaconConversionRecipe> getAllRecipes() {
        ImmutableList.Builder<BeaconConversionRecipe> builder = ImmutableList.builder();
        builder.add(new BeaconConversionRecipe(1, 0.02F));
        builder.add(new BeaconConversionRecipe(2, 0.05F));
        builder.add(new BeaconConversionRecipe(3, 0.2F));
        builder.add(new BeaconConversionRecipe(4, 1.0F));
        return builder.build();
    }
}
