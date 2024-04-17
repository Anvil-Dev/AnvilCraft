package dev.dubhe.anvilcraft.mixin.forge;

import dev.dubhe.anvilcraft.api.event.forge.BlockEntityEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {
    @Shadow
    public abstract Level getLevel();

    @Inject(
        method = "setBlockEntity",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"
        )
    )
    private void onLoadBlockEntity(BlockEntity entity, CallbackInfo ci) {
        if (this.getLevel().isClientSide) return;
        MinecraftForge.EVENT_BUS.post(new BlockEntityEvent.ServerLoad(this.getLevel(), entity));
    }

    @Inject(
        method = "setBlockEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;setRemoved()V"
        ),
        locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void onRemoveBlockEntity(BlockEntity entity, CallbackInfo ci, BlockPos blockPos, BlockEntity entity2) {
        if (this.getLevel().isClientSide) return;
        MinecraftForge.EVENT_BUS.post(new BlockEntityEvent.ServerUnload(this.getLevel(), entity2));
    }

    @Redirect(
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
    private <K, V> V onRemoveBlockEntity(@NotNull Map<K, V> map, K key) {
        @Nullable final V removed = map.remove(key);
        if (!this.getLevel().isClientSide && removed != null) {
            if (removed instanceof BlockEntity entity) {
                MinecraftForge.EVENT_BUS.post(new BlockEntityEvent.ServerUnload(this.getLevel(), entity));
            }
        }
        return removed;
    }

    @Inject(
        method = "removeBlockEntity",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/BlockEntity;setRemoved()V"
        ),
        locals = LocalCapture.CAPTURE_FAILEXCEPTION
    )
    private void onRemoveBlockEntity(BlockPos pos, CallbackInfo ci, BlockEntity removed) {
        if (this.getLevel().isClientSide) return;
        if (removed != null) {
            MinecraftForge.EVENT_BUS.post(new BlockEntityEvent.ServerUnload(this.getLevel(), removed));
        }
    }
}
