package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
abstract class RuinsRenderFaceMixin {
    @ModifyExpressionValue(
        method = "shouldRenderFace",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/BlockGetter;getBlockState(Lnet/minecraft/core/BlockPos;)"
            + "Lnet/minecraft/world/level/block/state/BlockState;")
    )
    private static BlockState anvilcraft$disguisedNeighbor(
        BlockState neighbor, BlockState state, BlockGetter level, BlockPos offset, Direction face, BlockPos pos
    ) {
        if (neighbor.getBlock() instanceof RuinsBlock && level.getBlockEntity(pos) instanceof RuinsBlockEntity ruins) {
            return ruins.getDisplayState();
        }
        return neighbor;
    }
}
