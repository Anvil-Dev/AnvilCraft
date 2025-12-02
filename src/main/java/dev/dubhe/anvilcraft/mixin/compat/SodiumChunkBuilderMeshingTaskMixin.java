package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.api.hammer.IHasHammerEffect;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkBuilderMeshingTask.class)
abstract class SodiumChunkBuilderMeshingTaskMixin {
    @WrapOperation(
        method = "execute("
                 + "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;"
                 + "Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;"
                 + ")Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;"
                     + "renderModel(Lnet/minecraft/client/resources/model/BakedModel;"
                     + "Lnet/minecraft/world/level/block/state/BlockState;"
                     + "Lnet/minecraft/core/BlockPos;"
                     + "Lnet/minecraft/core/BlockPos;)V"
        )
    )
    void skipBlock(
        BlockRenderer instance,
        BakedModel model,
        BlockState blockState,
        BlockPos blockPos,
        BlockPos origin,
        Operation<Void> original
    ) {
        if (Minecraft.getInstance().screen instanceof IHasHammerEffect hammerEffect && hammerEffect.shouldSkipRebuildBlock()) {
            BlockPos blockPos1 = hammerEffect.renderingBlockPos();
            if (!blockPos1.equals(blockPos)) {
                original.call(
                    instance,
                    model,
                    blockState,
                    blockPos,
                    origin
                );
            }
        } else {
            original.call(
                instance,
                model,
                blockState,
                blockPos,
                origin
            );
        }
    }
}
