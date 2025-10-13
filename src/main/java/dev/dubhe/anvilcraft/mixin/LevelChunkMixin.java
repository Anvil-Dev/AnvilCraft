package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.api.event.BlockEntityEvent;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Map;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow
    public abstract Level getLevel();

    @Inject(
        method = "setBlockEntity",
        at =
        @At(
            value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private void onLoadBlockEntity(BlockEntity entity, CallbackInfo ci) {
        if (this.getLevel().isClientSide) return;
        NeoForge.EVENT_BUS.post(new BlockEntityEvent.ServerLoad(this.getLevel(), entity));
    }

    @Inject(
        method = "setBlockEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;setRemoved()V")
    )
    private void onRemoveBlockEntity(
        BlockEntity pBlockEntity,
        CallbackInfo ci,
        @Local(ordinal = 1) BlockEntity blockentity
    ) {
        if (this.getLevel().isClientSide) return;
        NeoForge.EVENT_BUS.post(new BlockEntityEvent.ServerUnload(this.getLevel(), blockentity));
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

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    void onBlockEntityRemoved(BlockPos pos, CallbackInfo ci) {
        if (!this.getLevel().isClientSide()) return;
        BlockEntity be = getBlockEntity(pos);
        if (be instanceof BaseLaserBlockEntity laserStateAccess) {
            CacheableBERenderingPipeline.getInstance().blockRemoved(laserStateAccess);
        }
    }
}
