package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record MineralFountainJeiRecipe(
    ResourceLocation id,
    List<BlockState> sideBlocks,
    List<BlockState> fromBlocks,
    ChanceBlockState result,
    @Nullable ResourceLocation dimension
) {
    public MineralFountainJeiRecipe {
        sideBlocks = List.copyOf(sideBlocks);
        fromBlocks = List.copyOf(fromBlocks);
    }
}
