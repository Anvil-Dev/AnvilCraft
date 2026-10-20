package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.api.injection.tooltip.ITooltipProviderExtension;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(RedStoneWireBlock.class)
public abstract class RedstoneWireBlockMixin implements ITooltipProviderExtension {
    @Shadow
    @Final
    public static IntegerProperty POWER;

    @Shadow
    protected abstract BlockState getConnectionState(BlockGetter level, BlockState state, BlockPos pos);

    @Shadow
    private static boolean isDot(BlockState state) {
        throw new AssertionError();
    }

    @Inject(method = "onPlace", at = @At("TAIL"))
    private void anvilcraft$refreshPlacedDustConnections(
        BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo callback
    ) {
        if (level.isClientSide() || oldState.is(state.getBlock()) || RedstoneWireBlockMixin.isDot(state)) return;
        BlockState current = level.getBlockState(pos);
        if (!current.is(state.getBlock())) return;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!(level.getBlockState(pos.relative(direction).below()).getBlock() instanceof RedstoneWireBlock)) continue;
            BlockState connected = this.getConnectionState(level, current, pos);
            if (connected != current) level.setBlock(pos, connected, Block.UPDATE_ALL);
            return;
        }
    }

    @Override
    public List<Component> anvilcraft$getTooltip(BlockState state) {
        final ArrayList<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.title").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.power", state.getValue(
            RedstoneWireBlockMixin.POWER)).withStyle(ChatFormatting.GRAY));
        return lines;
    }

    @WrapOperation(
        method = "getConnectingSide(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/core/Direction;Z)Lnet/minecraft/world/level/block/state/properties/RedstoneSide;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;canRedstoneConnectTo("
            + "Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z")
    )
    private boolean anvilcraft$checkDiagonalWireConnection(
        BlockState state, BlockGetter level, BlockPos wirePos, @Nullable Direction direction,
        Operation<Boolean> original, @Local(argsOnly = true) BlockPos dustPos
    ) {
        if (direction == null && state.getBlock() instanceof RedstoneWireBlock
            && !wirePos.relative(state.getValue(RedstoneWireBlock.ATTACHMENT)).above().equals(dustPos)) {
            return false;
        }
        return original.call(state, level, wirePos, direction);
    }

    /// 把爬到方块顶面的模组导线信号并入原版红石粉的方块输入强度。
    ///
    /// <p>原版求解器把这里的返回值当作不衰减的方块信号，与模组导线自身的网络强度语义一致。</p>
    @Inject(method = "getBlockSignal", at = @At("RETURN"), cancellable = true)
    private void anvilcraft$includeUpwardWireSignal(
        Level level, BlockPos pos, CallbackInfoReturnable<Integer> callback
    ) {
        callback.setReturnValue(Math.max(callback.getReturnValue(), RedstoneWireBlock.getUpwardDustSignal(level, pos)));
    }
}
