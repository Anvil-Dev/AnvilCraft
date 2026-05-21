package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.client.renderer.item.WrappingItemCustomRenderedModels;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

@Mixin(ItemOverrides.class)
abstract class ItemOverridesMixin {
    @WrapOperation(
        method = "bakeModel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/ModelBaker;bake("
                     + "Lnet/minecraft/resources/ResourceLocation;"
                     + "Lnet/minecraft/client/resources/model/ModelState;"
                     + "Ljava/util/function/Function;)"
                     + "Lnet/minecraft/client/resources/model/BakedModel;"
        )
    )
    private BakedModel wrapSpecific(
        ModelBaker instance,
        ResourceLocation location,
        ModelState transform,
        Function<Material, TextureAtlasSprite> sprites,
        Operation<BakedModel> original
    ) {
        if (
            location.equals(WrappingItemCustomRenderedModels.SPECTRAL_WEAPON_LAUNCHER.withPrefix("item/"))
            || location.equals(WrappingItemCustomRenderedModels.SPECTRAL_WEAPON_LAUNCHER_EXHAUSTED.withPrefix("item/"))
        ) {
            return new WrappingItemCustomRenderedModels.CustomRenderedModelWrapper(original.call(instance, location, transform, sprites));
        } else {
            return original.call(instance, location, transform, sprites);
        }
    }
}
