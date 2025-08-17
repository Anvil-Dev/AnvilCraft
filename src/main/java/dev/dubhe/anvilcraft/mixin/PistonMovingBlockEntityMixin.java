package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.block.sliding.ActivatorSlidingRailBlock;
import dev.dubhe.anvilcraft.block.sliding.ISlidingRail;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.piston.MovingPistonBlock;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
abstract class PistonMovingBlockEntityMixin {
    @Shadow
    public abstract Direction getDirection();

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
        Direction facing = state.getValue(MovingPistonBlock.FACING);
        switch (facing) {
            case UP, DOWN -> {
                return;
            }
        }
        BlockPos belowPos = pos.below();
        BlockState below = level.getBlockState(belowPos);
        if (!below.is(ModBlockTags.SLIDING_RAILS)) return;
        if (below.getBlock() instanceof ActivatorSlidingRailBlock && !blockEntity.isSourcePiston()) {
            level.setBlock(belowPos, below.setValue(ActivatorSlidingRailBlock.FACING, facing), Block.UPDATE_CLIENTS);
        }
        MinecraftServer server = level.getServer();
        if (server == null) return;
        ISlidingRail.PistonPushInfo p = new ISlidingRail.PistonPushInfo(pos, blockEntity.getDirection());
        p.extending = blockEntity.isExtending();
        if (ISlidingRail.MOVING_PISTON_MAP.containsKey(belowPos)) {
            ISlidingRail.MOVING_PISTON_MAP.get(belowPos).extending = p.extending;
        } else ISlidingRail.MOVING_PISTON_MAP.put(belowPos, p);
        ISlidingRail.MOVING_PISTON_MAP.get(belowPos).isSourcePiston = blockEntity.isSourcePiston();
    }
}