package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.building.BuildingRodPlacementAnimation;
import net.caffeinemc.mods.sodium.client.world.LevelSlice;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 区块内的流体与模型使用同一隐藏视图，使相邻水面也能随区块重建。 */
@Mixin(value = LevelSlice.class, remap = false)
abstract class SodiumPlacementSliceMixin {
    @ModifyReturnValue(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("RETURN"))
    private BlockState anvilcraft$hideState(BlockState original, int x, int y, int z) {
        return BuildingRodPlacementAnimation.hidesBlock(x, y, z) ? Blocks.AIR.defaultBlockState() : original;
    }
}
