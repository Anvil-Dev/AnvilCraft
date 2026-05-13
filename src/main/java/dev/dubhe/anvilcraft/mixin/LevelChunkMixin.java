package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.entity.IExtensibleBlockEntity;
import dev.dubhe.anvilcraft.api.event.BlockEntityEvent;
import dev.dubhe.anvilcraft.api.rendering.CacheableBERenderingPipeline;
import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.util.mixin.ExtensibleBlockEntityEntry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private void onLoadBlockEntity(BlockEntity blockEntity, CallbackInfo ci) {
        if (this.getLevel().isClientSide()) return;
        NeoForge.EVENT_BUS.post(new BlockEntityEvent.ServerLoad(this.getLevel(), blockEntity));
    }

    @Inject(
        method = "setBlockEntity",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/BlockEntity;setRemoved()V")
    )
    private void onRemoveBlockEntity(
        BlockEntity blockEntity,
        CallbackInfo ci,
        @Local(name = "previousEntry") BlockEntity previousEntry
    ) {
        if (this.getLevel().isClientSide()) return;
        NeoForge.EVENT_BUS.post(new BlockEntityEvent.ServerUnload(this.getLevel(), previousEntry));
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
        if (!this.getLevel().isClientSide() && removed != null) {
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
    private void onRemoveBlockEntity(BlockPos pos, CallbackInfo ci, @Local(name = "removeThis") @Nullable BlockEntity removeThis) {
        if (this.getLevel().isClientSide()) return;
        if (removeThis != null) {
            NeoForge.EVENT_BUS.post(new BlockEntityEvent.ServerUnload(this.getLevel(), removeThis));
        }
    }

    @Shadow
    @Nullable
    public abstract BlockEntity getBlockEntity(BlockPos pos);

    @Inject(method = "removeBlockEntity", at = @At("HEAD"))
    void onBlockEntityRemoved(BlockPos pos, CallbackInfo ci) {
        if (!this.getLevel().isClientSide()) return;
        BlockEntity be = this.getBlockEntity(pos);
        if (be instanceof BaseLaserBlockEntity laserStateAccess) {
            CacheableBERenderingPipeline.getInstance().blockRemoved(laserStateAccess);
        }
    }

    @WrapOperation(
        method = "setBlockState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;"
                     + "preRemoveSideEffects("
                     + "Lnet/minecraft/core/BlockPos;"
                     + "Lnet/minecraft/world/level/block/state/BlockState;)V"
        )
    )
    private void storeValuesIfExtensible(
        BlockEntity instance,
        BlockPos pos,
        BlockState state,
        Operation<Void> original,
        @Share(namespace = AnvilCraft.MOD_ID, value = "extensible") LocalRef<ExtensibleBlockEntityEntry<?>> entry
    ) {
        if (!(instance instanceof IExtensibleBlockEntity<?> extensible)) {
            original.call(instance, pos, state);
            return;
        }
        entry.set(new ExtensibleBlockEntityEntry<>(extensible, state, original));
    }

    @WrapOperation(
        method = "setBlockState",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/EntityBlock;"
                     + "newBlockEntity("
                     + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)"
                     + "Lnet/minecraft/world/level/block/entity/BlockEntity;"
        )
    )
    private BlockEntity extendEntityIfValid(
        EntityBlock instance,
        BlockPos pos,
        BlockState state,
        Operation<BlockEntity> original,
        @Share(namespace = AnvilCraft.MOD_ID, value = "extensible") LocalRef<ExtensibleBlockEntityEntry<?>> entry
    ) {
        BlockEntity newBe = original.call(instance, pos, state);
        ExtensibleBlockEntityEntry<?> extensibleEntry = entry.get();
        if (extensibleEntry == null) return newBe;
        if (extensibleEntry.extensible().getThatType() != newBe.getType()) {
            extensibleEntry.remove();
            return newBe;
        }
        extensibleEntry.apply(Util.cast(newBe));
        return newBe;
    }
}
