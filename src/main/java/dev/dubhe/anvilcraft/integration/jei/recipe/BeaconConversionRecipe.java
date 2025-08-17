package dev.dubhe.anvilcraft.integration.jei.recipe;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.components.ChanceItemStack;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

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
        this.corruptedBeaconOutput = ChanceItemStack.of(ModBlocks.CORRUPTED_BEACON.asStack(), chance);
        this.beaconOutput = ChanceItemStack.of(Blocks.BEACON.asItem().getDefaultInstance(), 1.0f - chance);
    }

    public static @NotNull ImmutableList<BeaconConversionRecipe> getAllRecipes() {
        ImmutableList.Builder<BeaconConversionRecipe> builder = ImmutableList.builder();
        builder.add(new BeaconConversionRecipe(1, 0.02f));
        builder.add(new BeaconConversionRecipe(2, 0.05f));
        builder.add(new BeaconConversionRecipe(3, 0.2f));
        builder.add(new BeaconConversionRecipe(4, 1.0f));
        return builder.build();
    }
}
