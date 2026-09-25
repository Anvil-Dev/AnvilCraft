package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class BuildingRodHandItemState extends BuildingRodModelParts.ModelState {
    private boolean custom;
    private BuildingRodItemRenderer.@Nullable HeldState held;

    public BuildingRodHandItemState() {
        this.capture = false;
    }

    public static void begin(LivingEntity entity, ArmedEntityRenderState state) {
        if (state.rightHandItemState instanceof BuildingRodHandItemState right) {
            right.capture = entity.getItemHeldByArm(HumanoidArm.RIGHT).is(ModItems.BUILDING_ROD);
        }
        if (state.leftHandItemState instanceof BuildingRodHandItemState left) {
            left.capture = entity.getItemHeldByArm(HumanoidArm.LEFT).is(ModItems.BUILDING_ROD);
        }
    }

    @Override
    public void clear() {
        super.clear();
        this.custom = false;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline) {
        if (this.custom && this.held != null) BuildingRodItemRenderer.submitHeld(this.held, pose, collector, light, overlay, outline);
        else super.submit(pose, collector, light, overlay, outline);
    }

    public static void extract(LivingEntity entity, ArmedEntityRenderState state) {
        prepare(entity, state.rightHandItemState, state.rightHandItemStack, HumanoidArm.RIGHT);
        prepare(entity, state.leftHandItemState, state.leftHandItemStack, HumanoidArm.LEFT);
    }

    private static void prepare(LivingEntity entity, ItemStackRenderState item, ItemStack stack, HumanoidArm arm) {
        InteractionHand hand = arm == entity.getMainArm() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        if (BuildingRodItemRenderer.hideHand(entity, hand)) {
            item.clear();
        } else if (stack.is(ModItems.BUILDING_ROD) && item instanceof BuildingRodHandItemState rod) {
            if (rod.held == null) rod.held = new BuildingRodItemRenderer.HeldState(rod);
            boolean left = arm == HumanoidArm.LEFT;
            BuildingRodItemRenderer.extractHeld(entity, stack, left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, left, rod.held, false);
            rod.custom = true;
        }
    }
}
