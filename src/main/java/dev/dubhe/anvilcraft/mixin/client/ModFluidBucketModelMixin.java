package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.FluidBucketGeometry;
import net.minecraft.client.renderer.block.dispatch.ModelState;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.geometry.QuadCollection;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.client.model.ExtraFaceData;
import net.neoforged.neoforge.client.model.item.DynamicFluidContainerModel;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DynamicFluidContainerModel.class)
public abstract class ModFluidBucketModelMixin {
    @Shadow
    @Final
    private DynamicFluidContainerModel.Unbaked unbakedModel;

    @WrapOperation(method = "bakeModelForFluid", at = @At(value = "INVOKE", target =
        "Lnet/neoforged/neoforge/client/model/UnbakedElementsHelper;bakeItemMaskQuads("
            + "Lnet/minecraft/client/resources/model/ModelBaker;Lnet/minecraft/client/resources/model/sprite/Material$Baked;"
            + "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;"
            + "Lnet/minecraft/client/renderer/block/dispatch/ModelState;Lnet/neoforged/neoforge/client/model/ExtraFaceData;)"
            + "Lnet/minecraft/client/resources/model/geometry/QuadCollection;"))
    private QuadCollection anvilcraft$sourceFluidGeometry(
        ModelBaker baker, Material.Baked mask, BakedQuad.MaterialInfo material, ModelState state, ExtraFaceData faceData,
        Operation<QuadCollection> original
    ) {
        if (this.unbakedModel.forceOpaqueFluid()
            || !BuiltInRegistries.FLUID.getKey(this.unbakedModel.fluid()).getNamespace().equals(AnvilCraft.MOD_ID)) {
            return original.call(baker, mask, material, state, faceData);
        }
        return FluidBucketGeometry.bake(baker, mask, material, state, faceData);
    }
}
