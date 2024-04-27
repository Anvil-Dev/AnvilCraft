package dev.dubhe.anvilcraft.mixin.fabric;

import dev.dubhe.anvilcraft.block.ResinBlock;
import net.minecraft.world.level.block.Block;

import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PistonMovingBlockEntity.class)
abstract class PistonMovingBlockEntityMixin {
    @Redirect(
        method = "moveCollidedEntities",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"
        )
    )
    private static boolean isElastic(BlockState instance, Block b) {
        Block block = instance.getBlock();
        return block instanceof SlimeBlock || block instanceof ResinBlock;
    }
}
