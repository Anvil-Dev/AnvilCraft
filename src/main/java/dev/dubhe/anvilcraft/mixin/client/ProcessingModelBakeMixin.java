package dev.dubhe.anvilcraft.mixin.client;

import dev.dubhe.anvilcraft.client.support.ProcessingModelShape;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CuboidItemModelWrapper.Unbaked.class)
abstract class ProcessingModelBakeMixin {
    @Inject(method = "bake", at = @At("RETURN"))
    private void anvilcraft$recordGeometry(
        ItemModel.BakingContext context, Matrix4fc transformation, CallbackInfoReturnable<ItemModel> cir
    ) {
        var unbaked = (CuboidItemModelWrapper.Unbaked) (Object) this;
        ResolvedModel root = context.blockModelBaker().getModel(unbaked.model());
        while (root.parent() != null) root = root.parent();
        if (cir.getReturnValue() instanceof ProcessingModelShape shape) {
            shape.anvilcraft$setThreeDimensional(!(root.wrapped() instanceof ItemModelGenerator));
        }
    }
}
