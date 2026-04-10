package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

import java.util.Map;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class WrappingItemCustomRenderedModels {
    public static final ResourceLocation SPECTRAL_WEAPON_LAUNCHER = AnvilCraft.of("spectral_weapon_launcher");
    public static final ResourceLocation SPECTRAL_WEAPON_LAUNCHER_EXHAUSTED = AnvilCraft.of("spectral_weapon_launcher_exhausted");

    @SubscribeEvent
    public static void onModelBake(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> modelRegistry = event.getModels();
        swapModels(modelRegistry, ModelResourceLocation.inventory(AnvilCraft.of("spectral_slingshot")));
        swapModels(modelRegistry, ModelResourceLocation.inventory(SPECTRAL_WEAPON_LAUNCHER));
        swapModels(modelRegistry, ModelResourceLocation.inventory(SPECTRAL_WEAPON_LAUNCHER_EXHAUSTED));
    }

    public static void swapModels(Map<ModelResourceLocation, BakedModel> modelRegistry, ModelResourceLocation modelLocation) {
        BakedModel model = modelRegistry.get(modelLocation);
        CustomRenderedModelWrapper wrapper = new CustomRenderedModelWrapper(model);
        modelRegistry.put(modelLocation, wrapper);
    }

    public static class CustomRenderedModelWrapper extends BakedModelWrapper<BakedModel> {
        public CustomRenderedModelWrapper(BakedModel originalModel) {
            super(originalModel);
        }

        @Override
        public boolean isCustomRenderer() {
            return true;
        }

        // 谢谢你，西米不比！
        // Method copied from Create Mod
        @Override
        public BakedModel applyTransform(
            ItemDisplayContext cameraItemDisplayContext,
            PoseStack mat,
            boolean leftHand
        ) {
            // Super call returns originalModel, but we want to return this, else BEWLR
            // won't be used.
            super.applyTransform(cameraItemDisplayContext, mat, leftHand);
            return this;
        }
    }
}
