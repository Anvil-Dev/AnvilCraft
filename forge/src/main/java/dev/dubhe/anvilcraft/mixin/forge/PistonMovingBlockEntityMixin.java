package dev.dubhe.anvilcraft.mixin.forge;

import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonMovingBlockEntity.class)
abstract class PistonMovingBlockEntityMixin {
    @Redirect(
            method = "moveCollidedEntities",
            at = @At(
                    value = "INVOKE",
                    remap = false,
                    target = "Lnet/minecraft/world/level/block/state/BlockState;isSlimeBlock()Z"
            )
    )
    private static boolean isElastic(@NotNull BlockState instance) {
        return instance.getBlock() instanceof SlimeBlock;
    }
}
