package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsBlockEntityRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ChunkBuilderMeshingTask.class, remap = false)
abstract class SodiumRuinsMeshingMixin {
    @WrapOperation(method = "execute(Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildContext;"
        + "Lnet/caffeinemc/mods/sodium/client/util/task/CancellationToken;)"
        + "Lnet/caffeinemc/mods/sodium/client/render/chunk/compile/ChunkBuildOutput;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRenderOffScreen()Z"))
    private boolean anvilcraft$ruinsVisibility(BlockEntityRenderer<?, ?> renderer, Operation<Boolean> original,
                                             @Local BlockEntity entity) {
        return renderer instanceof RuinsBlockEntityRenderer display && entity instanceof RuinsBlockEntity ruins
            ? display.shouldRenderOffScreen(ruins) : original.call(renderer);
    }
}
