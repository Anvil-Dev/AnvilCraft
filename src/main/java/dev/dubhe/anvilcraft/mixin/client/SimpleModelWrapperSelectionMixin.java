package dev.dubhe.anvilcraft.mixin.client;

import dev.dubhe.anvilcraft.client.selection.ModelSelectionCapture;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.SimpleModelWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SimpleModelWrapper.class)
abstract class SimpleModelWrapperSelectionMixin {
    @Inject(
        method = "bake(Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/resources/model/ResolvedModel;"
            + "Lnet/minecraft/client/renderer/block/dispatch/ModelState;)"
            + "Lnet/minecraft/client/renderer/block/dispatch/BlockStateModelPart;",
        at = @At("RETURN")
    )
    private static void anvilcraft$captureSelection(
        ModelBaker baker, ResolvedModel model, ModelState state, CallbackInfoReturnable<BlockStateModelPart> callback
    ) {
        if (callback.getReturnValue() != baker.missingBlockModelPart()) {
            ModelSelectionCapture.remember(model, state, callback.getReturnValue());
        }
    }
}
