package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.api.event.BlockEntityEvent;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChutePlates;
import dev.dubhe.anvilcraft.building.BuildingRodUndo;
import dev.dubhe.anvilcraft.util.EnchantedGoldBlockPositions;
import dev.dubhe.anvilcraft.util.MonolithBlockPositions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow
    public abstract Level getLevel();

    @Shadow
    public abstract Map<BlockPos, BlockEntity> getBlockEntities();

    @WrapOperation(
        method = "setBlockState",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onRemove("
            + "Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)V")
    )
    private void transferChutePlates(
        BlockState previous, Level level, BlockPos pos, BlockState next, boolean moving, Operation<Void> original
    ) {
        ChutePlates plates = ChutePlates.isConversion(previous.getBlock(), next.getBlock()) ? ChutePlates.at(level, pos) : null;
        original.call(previous, level, pos, next, moving);
        if (plates == null || plates.getPlates().isEmpty()) {
            return;
        }
        LevelChunk chunk = (LevelChunk) (Object) this;
        if (chunk.getBlockState(pos).is(next.getBlock())
            && chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE) instanceof ChutePlates.Holder target) {
            plates.transferTo(target);
        } else {
            plates.dropAfterFailedConversion();
        }
    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void trackBuildingRodReplacement(BlockPos pos, BlockState state, boolean moving,
                                             CallbackInfoReturnable<BlockState> cir) {
        BlockState previous = cir.getReturnValue();
        if (previous != null) BuildingRodUndo.replaced(this.getLevel(), pos, previous, state);
    }

    @Inject(method = "setBlockEntity", at = @At("TAIL"))
    private void onLoadBlockEntity(BlockEntity entity, CallbackInfo ci) {
        if (this.getLevel().isClientSide) return;
        // 能力提供者可能再次查询世界，必须等实体注册和旧实体卸载完成后再通知。
        if (this.getBlockEntities().get(entity.getBlockPos()) != entity) return;
        NeoForge.EVENT_BUS.post(new BlockEntityEvent.ServerLoad(this.getLevel(), entity));
    }

    @Inject(
        method = "setBlockEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;setRemoved()V")
    )
    private void onRemoveBlockEntity(
        BlockEntity entity,
        CallbackInfo ci,
        @Local(ordinal = 1) BlockEntity be
    ) {
        if (this.getLevel().isClientSide) return;
        NeoForge.EVENT_BUS.post(new BlockEntityEvent.ServerUnload(this.getLevel(), be));
    }

    @WrapOperation(
        method = "getBlockEntity("
                 + "Lnet/minecraft/core/BlockPos;"
                 + "Lnet/minecraft/world/level/chunk/LevelChunk$EntityCreationType;"
                 + ")"
                 + "Lnet/minecraft/world/level/block/entity/BlockEntity;",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;",
            ordinal = 1
        )
    )
    private @Nullable <K, V> V onRemoveBlockEntity(Map<K, V> instance, Object key, Operation<V> original) {
        final V removed = original.call(instance, key);
        if (!this.getLevel().isClientSide && removed != null) {
            if (removed instanceof BlockEntity entity) {
                NeoForge.EVENT_BUS.post(new BlockEntityEvent.ServerUnload(this.getLevel(), entity));
            }
        }
        return removed;
    }

    @Inject(
        method = "removeBlockEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;setRemoved()V")
    )
    private void onRemoveBlockEntity(BlockPos pos, CallbackInfo ci, @Local @Nullable BlockEntity removed) {
        if (this.getLevel().isClientSide()) return;
        if (removed != null) {
            NeoForge.EVENT_BUS.post(new BlockEntityEvent.ServerUnload(this.getLevel(), removed));
        }
    }

    @Shadow
    @Nullable
    public abstract BlockEntity getBlockEntity(BlockPos pos);

    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    void onBlockEntityRemoved(BlockPos pos, CallbackInfo ci) {
        if (!this.getLevel().isClientSide()) return;
        BlockEntity be = this.getBlockEntity(pos);
        if (be instanceof BaseLaserBlockEntity laserStateAccess) {
            CacheableBERenderingPipeline.getInstance().blockRemoved(laserStateAccess);
        }
    }

    @Inject(
        method = "setBlockState(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Z)"
                 + "Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("HEAD")
    )
    private void trackEnchantedGoldBlock(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir) {
        if (!this.getLevel().isClientSide) {
            return;
        }
        BlockState previous = this.getBlockState(pos);
        EnchantedGoldBlockPositions.onBlockChanged(pos, previous, state);
        MonolithBlockPositions.onBlockChanged((LevelChunk) (Object) this, pos, previous, state);
    }
}
