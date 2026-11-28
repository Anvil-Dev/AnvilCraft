package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.building.BuildingRodPlacementAnimation;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 区块内的流体与模型使用同一隐藏视图，使相邻水面也能随区块重建。 */
@Mixin(RenderSectionRegion.class)
abstract class PlacementRenderRegionMixin {
    @ModifyReturnValue(method = "getBlockState", at = @At("RETURN"))
    private BlockState anvilcraft$hideState(BlockState original, BlockPos pos) {
        return BuildingRodPlacementAnimation.hidesBlock(pos) ? Blocks.AIR.defaultBlockState() : original;
    }

    @ModifyReturnValue(method = "getFluidState", at = @At("RETURN"))
    private FluidState anvilcraft$hideFluid(FluidState original, BlockPos pos) {
        return BuildingRodPlacementAnimation.hidesBlock(pos) ? Fluids.EMPTY.defaultFluidState() : original;
    }
}
