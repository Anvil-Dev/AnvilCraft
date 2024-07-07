package dev.dubhe.anvilcraft.mixin.fabric;

import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(ModelBakery.class)
public abstract class ModelBakeryMixin {
    @Shadow @Final private Map<ResourceLocation, UnbakedModel> unbakedCache;

    @Shadow @Final private Map<ResourceLocation, UnbakedModel> topLevelModels;

    @Shadow public abstract UnbakedModel getModel(ResourceLocation modelLocation);

    @Inject(method = "loadTopLevel", at = @At("HEAD"))
    private void registerModels(ModelResourceLocation location, CallbackInfo ci) {
        if (location.getNamespace().equals("anvilcraft") && location.getPath().equals("crab_claw")) {
            putUnbakedModel(new ModelResourceLocation("anvilcraft", "crab_claw_holding_block", "inventory"));
            putUnbakedModel(new ModelResourceLocation("anvilcraft", "crab_claw_holding_item", "inventory"));
            putUnbakedModel(new ModelResourceLocation("anvilcraft", "heliostats_head", ""));
            putUnbakedModel(new ModelResourceLocation("anvilcraft", "creative_generator_cube", ""));
        }
    }

    @Unique
    private void putUnbakedModel(ModelResourceLocation location) {
        this.unbakedCache.put(location, this.getModel(location));
        this.topLevelModels.put(location, this.getModel(location));
    }
}
