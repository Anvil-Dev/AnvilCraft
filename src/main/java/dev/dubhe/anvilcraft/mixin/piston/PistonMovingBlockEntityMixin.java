package dev.dubhe.anvilcraft.mixin.piston;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.block.piston.IMoveableEntityBlock;
import dev.dubhe.anvilcraft.util.mixin.magic.PistonMovingBlockEntityInjector;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
abstract class PistonMovingBlockEntityMixin implements PistonMovingBlockEntityInjector {
    @Shadow
    private BlockState movedState;
    @Unique
    private CompoundTag anvilcraft$nbt = new CompoundTag();

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
            shift = At.Shift.AFTER,
            ordinal = 1
        )
    )
    private static void tick(
        @NotNull Level level,
        BlockPos pos,
        BlockState state,
        PistonMovingBlockEntity blockEntity,
        CallbackInfo ci,
        @Local(ordinal = 1) BlockState moveState
    ) {
        if (level.isClientSide()) return;
        if (!(blockEntity instanceof PistonMovingBlockEntityInjector blockEntity1)) return;
        if (!(moveState.getBlock() instanceof IMoveableEntityBlock entityBlock)) return;
        entityBlock.setData(level, pos, blockEntity1.anvilcraft$clearData());
    }

    @Override
    public CompoundTag anvilcraft$clearData() {
        CompoundTag nbt = this.anvilcraft$nbt;
        this.anvilcraft$nbt = new CompoundTag();
        return nbt;
    }

    @Override
    public void anvilcraft$setData(CompoundTag nbt) {
        if (nbt == null) return;
        this.anvilcraft$nbt.merge(nbt);
    }

    @Override
    public BlockState anvilcraft$getMoveState() {
        return this.movedState;
    }
}
