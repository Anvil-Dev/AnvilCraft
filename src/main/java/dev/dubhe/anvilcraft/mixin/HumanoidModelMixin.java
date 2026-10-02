package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
abstract class HumanoidModelMixin {
    @Shadow
    @Final
    public ModelPart rightArm;
    @Shadow
    @Final
    public ModelPart leftArm;

    @Inject(method = "poseRightArm", at = @At("TAIL"))
    private void anvilcraft$holdAnvilRight(HumanoidRenderState state, CallbackInfo ci) {
        HumanoidModelMixin.anvilcraft$holdAnvil(state, HumanoidArm.RIGHT, this.rightArm);
    }

    @Inject(method = "poseLeftArm", at = @At("TAIL"))
    private void anvilcraft$holdAnvilLeft(HumanoidRenderState state, CallbackInfo ci) {
        HumanoidModelMixin.anvilcraft$holdAnvil(state, HumanoidArm.LEFT, this.leftArm);
    }

    @Unique
    private static void anvilcraft$holdAnvil(HumanoidRenderState state, HumanoidArm arm, ModelPart part) {
        ItemStack stack = state.getUseItemStackForArm(arm);
        if (!stack.is(ModBlocks.CELESTIAL_FORGING_ANVIL.asItem())) return;
        part.xRot = -Mth.HALF_PI;
        part.yRot = 0.0F;
        part.zRot = 0.0F;
    }
}
