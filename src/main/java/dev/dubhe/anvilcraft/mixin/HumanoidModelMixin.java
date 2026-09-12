package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
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
    private void anvilcraft$holdAnvilRight(LivingEntity entity, CallbackInfo ci) {
        anvilcraft$holdAnvil(entity, HumanoidArm.RIGHT, this.rightArm);
    }

    @Inject(method = "poseLeftArm", at = @At("TAIL"))
    private void anvilcraft$holdAnvilLeft(LivingEntity entity, CallbackInfo ci) {
        anvilcraft$holdAnvil(entity, HumanoidArm.LEFT, this.leftArm);
    }

    @Unique
    private static void anvilcraft$holdAnvil(LivingEntity entity, HumanoidArm arm, ModelPart part) {
        ItemStack stack = entity.getMainArm() == arm ? entity.getMainHandItem() : entity.getOffhandItem();
        if (!stack.is(ModBlocks.CELESTIAL_FORGING_ANVIL.asItem())) return;
        part.xRot = -Mth.HALF_PI;
        part.yRot = 0.0f;
        part.zRot = 0.0f;
    }
}
