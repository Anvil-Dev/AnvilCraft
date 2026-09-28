package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
abstract class RuinsRenderFaceMixin {
    @WrapMethod(method = "shouldRenderFace(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
        + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;"
        + "Lnet/minecraft/core/Direction;)Z")
    private static boolean anvilcraft$disguisedNeighbor(BlockGetter level, BlockPos pos, BlockState state, BlockState neighbor,
                                                       Direction face, Operation<Boolean> original) {
        if (neighbor.getBlock() instanceof RuinsBlock) neighbor = RuinsBlock.disguisedState(level, pos.relative(face), neighbor);
        return original.call(level, pos, state, neighbor, face);
    }
}
