package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.SlidingRailBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
abstract class PistonMovingBlockEntityMixin {

    @Shadow private Direction direction;

    @Shadow private int deathTicks;

    @Shadow public abstract Direction getDirection();

    @Shadow private boolean extending;

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;"
                            + "neighborChanged(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;"
                            + "Lnet/minecraft/core/BlockPos;)V",
                    shift = At.Shift.AFTER
            )
    )
    private static void slidingRail(Level level, BlockPos pos, BlockState state, PistonMovingBlockEntity blockEntity, CallbackInfo ci) {
        if (level.isClientSide) return;

        if (level.getBlockState(pos.below()).is(ModBlocks.SLIDING_RAIL)) {
            MinecraftServer server = level.getServer();
            if (server == null) return;
            BlockPos p0 = pos.below();
            SlidingRailBlock.PistonPushInfo p = new SlidingRailBlock.PistonPushInfo(pos, blockEntity.getDirection());
            p.extending = blockEntity.isExtending();
            if(SlidingRailBlock.MOVING_PISTON_MAP.containsKey(p0)){
                SlidingRailBlock.MOVING_PISTON_MAP.get(p0).extending = p.extending;
            }
            else SlidingRailBlock.MOVING_PISTON_MAP.put(p0, p);
            SlidingRailBlock.MOVING_PISTON_MAP.get(p0).isSourcePiston = blockEntity.isSourcePiston();
        }

    }
}
