package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record MineralFountainJeiRecipe(
    Identifier id,
    List<BlockState> sideBlocks,
    List<BlockState> fromBlocks,
    ChanceBlockState result,
    @Nullable Identifier dimension
) {
    public MineralFountainJeiRecipe {
        sideBlocks = List.copyOf(sideBlocks);
        fromBlocks = List.copyOf(fromBlocks);
    }
}
