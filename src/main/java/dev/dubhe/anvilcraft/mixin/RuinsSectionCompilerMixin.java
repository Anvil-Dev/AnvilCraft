package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.client.renderer.blockentity.RuinsBlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SectionCompiler.class)
abstract class RuinsSectionCompilerMixin {
    @WrapOperation(method = "handleBlockEntity", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;shouldRenderOffScreen()Z"))
    private boolean anvilcraft$ruinsVisibility(BlockEntityRenderer<?, ?> renderer, Operation<Boolean> original,
                                             @Local(argsOnly = true) BlockEntity entity) {
        return renderer instanceof RuinsBlockEntityRenderer display && entity instanceof RuinsBlockEntity ruins
            ? display.shouldRenderOffScreen(ruins) : original.call(renderer);
    }
}
