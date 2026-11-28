package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.client.building.BlueprintRenderView;
import dev.dubhe.anvilcraft.client.building.BuildingRodPlacementAnimation;
import dev.dubhe.anvilcraft.client.support.HammerPreviewState;
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
        if (level instanceof BlueprintRenderView view) {
            if (view.exposesFace(pos, face)) {
                return true;
            }
        } else if (BuildingRodPlacementAnimation.hidesNeighbor(pos, face) || HammerPreviewState.hidesNeighbor(pos, face)) {
            return true;
        }
        if (neighbor.getBlock() instanceof RuinsBlock) {
            neighbor = RuinsBlock.disguisedState(level, pos.relative(face), neighbor);
        }
        return original.call(level, pos, state, neighbor, face);
    }
}
