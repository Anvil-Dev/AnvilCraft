package dev.dubhe.anvilcraft.mixin.piston;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.api.injection.block.entity.IPistonMovingBlockEntityExtension;
import dev.dubhe.anvilcraft.block.piston.IMoveableEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
abstract class PistonMovingBlockEntityMixin extends BlockEntity implements IPistonMovingBlockEntityExtension {
    @Shadow
    private BlockState movedState;
    @Unique
    private CompoundTag anvilcraft$nbt = new CompoundTag();

    public PistonMovingBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public CompoundTag anvilcraft$clearData() {
        CompoundTag nbt = this.anvilcraft$nbt;
        this.anvilcraft$nbt = new CompoundTag();
        return nbt;
    }

    @Override
    public void anvilcraft$setData(@Nullable CompoundTag nbt) {
        if (nbt == null) return;
        this.anvilcraft$nbt.merge(nbt);
    }

    @Override
    public BlockState anvilcraft$getMoveState() {
        return this.movedState;
    }

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
        Level level,
        BlockPos pos,
        BlockState state,
        PistonMovingBlockEntity blockEntity,
        CallbackInfo ci,
        @Local(ordinal = 1) BlockState moveState
    ) {
        if (level.isClientSide()) return;
        //noinspection ConstantValue
        if (!(blockEntity instanceof IPistonMovingBlockEntityExtension blockEntity1)) return;
        if (!(moveState.getBlock() instanceof IMoveableEntityBlock entityBlock)) return;
        CompoundTag tag = blockEntity1.anvilcraft$clearData();
        if (tag != null) {
            entityBlock.setData(level, pos, tag);
        }
    }

    @Inject(
        method = "finalTick", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
        shift = At.Shift.AFTER
    )
    )
    private void finalTick(CallbackInfo ci, @Local BlockState moveState) {
        if (this.level == null || this.level.isClientSide()) return;
        //noinspection ConstantValue
        if (!(this instanceof IPistonMovingBlockEntityExtension blockEntity1)) return;
        if (!(moveState.getBlock() instanceof IMoveableEntityBlock entityBlock)) return;
        CompoundTag tag = blockEntity1.anvilcraft$clearData();
        //noinspection ConstantValue
        if (tag != null) {
            entityBlock.setData(level, this.worldPosition, tag);
        }
    }
}
