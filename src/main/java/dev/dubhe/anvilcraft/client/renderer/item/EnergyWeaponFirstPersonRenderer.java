package dev.dubhe.anvilcraft.client.renderer.item;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.renderer.entity.WeaponBeamRenderer;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.weapon.CorruptedBeaconActivatorItem;
import dev.dubhe.anvilcraft.item.weapon.EnergyWeaponItem;
import dev.dubhe.anvilcraft.item.weapon.EnergyWeaponReload;
import dev.dubhe.anvilcraft.item.weapon.LaserGunItem;
import dev.dubhe.anvilcraft.item.weapon.TeslaGunItem;
import dev.dubhe.anvilcraft.util.WeaponRaycastUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class EnergyWeaponFirstPersonRenderer {
    private static final Vector3f BEAM_START = new Vector3f();
    private static final Vector3f BEAM_END = new Vector3f();
    private static final Vector3f[] HOLD_START = {new Vector3f(), new Vector3f()};
    private static final Vector3f HOLD_END = new Vector3f();
    private static boolean holdingProjectionReady;
    private static float beamFrame = -1;
    private static InteractionHand beamHand = InteractionHand.MAIN_HAND;
    private static ItemStack reloadWeapon = ItemStack.EMPTY;
    private static ItemStack reloadCapacitor = ItemStack.EMPTY;
    private static int reloadTicks;
    private static int reloadSlot = -1;
    private static double reloadAnimationTicks;
    private static long reloadLastFrame;
    private static float reloadAttackBlend;

    private EnergyWeaponFirstPersonRenderer() {
    }

    public static void updateReload(ItemStack weapon, ItemStack capacitor, int ticks, int slot) {
        final boolean starting = reloadWeapon.isEmpty() || ticks == 0 || reloadSlot != slot;
        reloadWeapon = weapon;
        reloadCapacitor = capacitor;
        reloadTicks = ticks;
        reloadSlot = slot;
        reloadAnimationTicks = ticks;
        reloadLastFrame = Util.getMillis();
        if (starting) {
            Minecraft minecraft = Minecraft.getInstance();
            InteractionHand hand = slot == Inventory.SLOT_OFFHAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            reloadAttackBlend = minecraft.player != null && attackRequested(minecraft.player, hand) ? 1 : 0;
        }
    }

    public static void captureBeam(PoseStack poseStack, Vec3 end, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix())
            .mul(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
        projection.transformProject(new Vector3f(), BEAM_START);
        projection.transformProject(end.toVector3f(), BEAM_END);
        beamFrame = minecraft.player.tickCount + partialTick;
        beamHand = minecraft.player.getUsedItemHand();
    }

    @SubscribeEvent
    public static void captureHoldingProjection(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !minecraft.options.getCameraType().isFirstPerson()) {
            holdingProjectionReady = false;
            return;
        }
        PoseStack pose = new PoseStack();
        pose.mulPose(event.getCamera().rotation());
        if (minecraft.options.bobView().get()) {
            WeaponBeamRenderer.counterViewBob(pose, minecraft.player, event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
        Matrix4f projection = new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).mul(pose.last().pose());
        for (HumanoidArm arm : HumanoidArm.values()) {
            float side = arm == HumanoidArm.RIGHT ? 1 : -1;
            projection.transformProject(new Vector3f(side * (float) WeaponRaycastUtil.MUZZLE_RIGHT_OFFSET,
                -(float) WeaponRaycastUtil.MUZZLE_DOWN_OFFSET, -(float) WeaponRaycastUtil.MUZZLE_FORWARD_OFFSET),
                HOLD_START[arm.ordinal()]);
        }
        projection.transformProject(new Vector3f(0, 0, -64), HOLD_END);
        holdingProjectionReady = true;
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        reloadWeapon = ItemStack.EMPTY;
        reloadCapacitor = ItemStack.EMPTY;
        beamFrame = -1;
        holdingProjectionReady = false;
    }

    public static boolean hasHoldingAnimation(ItemStack stack) {
        return stack.getItem() instanceof LaserGunItem || stack.getItem() instanceof CorruptedBeaconActivatorItem
            || stack.getItem() instanceof TeslaGunItem;
    }

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = minecraft.player;
        if (player == null || !player.isAlive() || player.isScoping()) return;
        if (!reloadWeapon.isEmpty() && !isReloadStillHeld(player)) {
            reloadWeapon = ItemStack.EMPTY;
            reloadCapacitor = ItemStack.EMPTY;
            player.getPersistentData().remove(EnergyWeaponReload.CLIENT_SLOT);
        }
        if (!reloadWeapon.isEmpty()) {
            event.setCanceled(true);
            InteractionHand visibleHand = player.isUsingItem() ? player.getUsedItemHand() : InteractionHand.MAIN_HAND;
            if (event.getHand() == visibleHand) renderReload(event, player);
            return;
        }
        ItemStack stack = event.getItemStack();
        if (!hasHoldingAnimation(stack)) return;
        event.setCanceled(true);
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        cameraSpace(pose);
        renderWeapon(event, player, stack, holdingPose(player, stack, event.getHand(), arm, event.getPartialTick()), arm);
        pose.popPose();
    }

    private static boolean isReloadStillHeld(AbstractClientPlayer player) {
        return (reloadSlot == Inventory.SLOT_OFFHAND || reloadSlot == player.getInventory().selected)
            && player.getInventory().getItem(reloadSlot).is(reloadWeapon.getItem());
    }

    private static Matrix4f holdingPose(
        AbstractClientPlayer player, ItemStack stack, InteractionHand hand, HumanoidArm arm, float partialTick
    ) {
        return holdingPose(player, stack, hand, arm, partialTick, 1.1F);
    }

    private static Matrix4f holdingPose(
        AbstractClientPlayer player, ItemStack stack, InteractionHand hand, HumanoidArm arm, float partialTick, float muzzleDepth
    ) {
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        Vector3f muzzle = new Vector3f(side * 0.28F, -0.2F, -1.1F).mul(muzzleDepth / 1.1F);
        Vector3f target = new Vector3f(0, 0, -64);
        if (holdingProjectionReady) {
            muzzle = unprojectAtDepth(HOLD_START[arm.ordinal()], muzzleDepth);
            target = unprojectAtDepth(HOLD_END, 64);
        }
        boolean firing = player.isUsingItem() && player.getUsedItemHand() == hand
            && stack.getItem() instanceof EnergyWeaponItem weapon && weapon.canFire(player, stack);
        if (firing && beamHand == hand && Math.abs(beamFrame - (player.tickCount + partialTick)) < 0.001F) {
            muzzle = unprojectAtDepth(BEAM_START, muzzleDepth);
            target = unprojectAtDepth(BEAM_END, 64);
        }
        float elapsed = player.getTicksUsingItem() + partialTick;
        float roll = firing ? Mth.sin(elapsed * 0.65F) * 0.3F * Math.clamp(elapsed / 5, 0, 1) : 0;
        float muzzleZ = stack.getItem() instanceof CorruptedBeaconActivatorItem ? -4.75F : -5;
        return EnergyWeaponReloadPose.holding(muzzle, target, muzzleZ, roll);
    }

    private static void cameraSpace(PoseStack pose) {
        Matrix4f camera = RenderSystem.getModelViewMatrix().invert(new Matrix4f());
        pose.last().pose().set(camera);
        pose.last().normal().set(camera).invert().transpose();
    }

    private static Vector3f unprojectAtDepth(Vector3f projected, float depth) {
        Matrix4f projection = RenderSystem.getProjectionMatrix();
        Vector3f center = projection.transformProject(new Vector3f(0, 0, -depth));
        return projection.invert(new Matrix4f()).transformProject(new Vector3f(projected.x, projected.y, center.z));
    }

    private static void renderReload(RenderHandEvent event, AbstractClientPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        InteractionHand hand = reloadSlot == Inventory.SLOT_OFFHAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        long now = Util.getMillis();
        float milliseconds = minecraft.isPaused() ? 0 : Math.clamp(now - reloadLastFrame, 0, 100);
        reloadAnimationTicks += milliseconds * player.level().tickRateManager().tickrate() / 1000;
        reloadAttackBlend = EnergyWeaponReloadPose.approachAttack(reloadAttackBlend, attackRequested(player, hand), milliseconds);
        reloadLastFrame = now;
        reloadAnimationTicks = Math.min(reloadAnimationTicks,
            reloadTicks < EnergyWeaponReload.CHARGE_TICK ? EnergyWeaponReload.CHARGE_TICK - 0.01 : EnergyWeaponReload.DURATION);
        float elapsed = (float) reloadAnimationTicks;
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        ItemStack heldWeapon = player.getItemInHand(hand);
        Matrix4f holding = holdingPose(player, heldWeapon, hand, arm, event.getPartialTick());
        Matrix4f attacking = holdingPose(player, heldWeapon, hand, arm, event.getPartialTick(),
            EnergyWeaponReloadPose.attackMuzzleDepth(elapsed));
        Matrix4f weaponPose = EnergyWeaponReloadPose.weapon(holding, attacking, elapsed, side, reloadAttackBlend);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        cameraSpace(pose);
        renderWeapon(event, player, heldWeapon, weaponPose, arm);
        if (EnergyWeaponReloadPose.showsCapacitor(elapsed)) {
            boolean regular = reloadCapacitor.is(ModItems.CAPACITOR) || reloadCapacitor.is(ModItems.CAPACITOR_EMPTY);
            Matrix4f capacitorPose = EnergyWeaponReloadPose.capacitor(holding, attacking, elapsed, side, regular, reloadAttackBlend);
            pose.pushPose();
            pose.mulPose(capacitorPose);
            renderItem(reloadCapacitor, pose, event.getMultiBufferSource(), event.getPackedLight(), player);
            pose.popPose();
            renderArm(event, player, arm.getOpposite(), EnergyWeaponReloadPose.capacitorGrip(capacitorPose, side));
        }
        pose.popPose();
    }

    private static boolean attackRequested(AbstractClientPlayer player, InteractionHand hand) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen == null && minecraft.options.keyUse.isDown()
            && (!player.isUsingItem() || player.getUsedItemHand() == hand);
    }

    private static void renderWeapon(
        RenderHandEvent event, AbstractClientPlayer player, ItemStack stack, Matrix4f weapon, HumanoidArm arm
    ) {
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.mulPose(weapon);
        renderItem(stack, pose, event.getMultiBufferSource(), event.getPackedLight(), player);
        pose.popPose();
        renderArm(event, player, arm, EnergyWeaponReloadPose.weaponGrip(weapon, arm == HumanoidArm.RIGHT ? 1 : -1));
    }

    private static void renderArm(RenderHandEvent event, AbstractClientPlayer player, HumanoidArm arm, Vector3f grip) {
        if (player.isInvisible()) return;
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        EnergyWeaponArmRenderer.render(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player, arm,
            EnergyWeaponReloadPose.arm(grip, side));
    }

    private static void renderItem(ItemStack stack, PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player) {
        Minecraft.getInstance().getItemRenderer().renderStatic(
            player, stack, ItemDisplayContext.NONE, false, pose, buffer, player.level(), light, OverlayTexture.NO_OVERLAY, player.getId());
    }
}
