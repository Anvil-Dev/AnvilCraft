package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class BuildingRodItemRenderer {
    private static final Map<List<Object>, PayloadPose> PAYLOAD_POSES = new LinkedHashMap<>();
    private static final HeldState FIRST_PERSON_STATE = new HeldState(new BuildingRodModelParts.ModelState());
    private static BuildingRodMotion motion = new BuildingRodMotion();
    private static boolean attackCanceled;
    private static double attackUntil;
    @Nullable private static Vec3 attackTarget;
    private static ItemStack pendingPayload = ItemStack.EMPTY;
    private static double pendingUntil;
    private static ItemStack pushedPayload = ItemStack.EMPTY;
    private static double pushedUntil;
    private static InteractionHand animationHand = InteractionHand.MAIN_HAND;

    private BuildingRodItemRenderer() {
    }

    static void clear() {
        motion = new BuildingRodMotion();
        attackCanceled = false;
        attackUntil = 0;
        attackTarget = null;
        pendingPayload = ItemStack.EMPTY;
        pushedPayload = ItemStack.EMPTY;
    }

    static void cancelAttack() {
        attackCanceled = true;
        attackUntil = 0;
        attackTarget = null;
    }

    static void attack() {
        Minecraft mc = Minecraft.getInstance();
        if (attackCanceled || mc.player == null) return;
        InteractionHand hand = rodHand(mc.player);
        if (hand == null) return;
        if (hand != animationHand) {
            clear();
            animationHand = hand;
        }
        if (!mc.player.isCreative() || mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) {
            attackTarget = null;
            attackUntil = 0;
            if (!motion.isKnocking(time())) motion.knock(time());
            return;
        }
        motion.stopKnock();
        attackTarget = mc.hitResult.getLocation();
        attackUntil = time() + 0.15;
    }

    static void preparePlacement() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        pendingPayload = BuildingRodItem.material(player).copyWithCount(1);
        pendingUntil = time() + 5;
    }

    public static void placed() {
        var player = Minecraft.getInstance().player;
        if (player == null || !BuildingRodItem.isHeld(player)
            || pendingPayload.isEmpty() || time() > pendingUntil) return;
        pushedPayload = pendingPayload;
        pendingPayload = ItemStack.EMPTY;
        pushedUntil = time() + 0.28;
        motion.placed(time());
    }

    public static void placedOffhand(ItemStack payload) {
        var player = Minecraft.getInstance().player;
        if (player == null || rodHand(player) != InteractionHand.OFF_HAND) return;
        if (!player.getMainHandItem().isEmpty() && !ItemStack.isSameItemSameComponents(player.getMainHandItem(), payload)) return;
        if (animationHand != InteractionHand.OFF_HAND) clear();
        animationHand = InteractionHand.OFF_HAND;
        pushedPayload = payload.copyWithCount(1);
        pushedUntil = time() + 0.28;
        motion.placed(time());
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.options.keyAttack.isDown()) attackCanceled = false;
        var player = mc.player;
        if (player == null) {
            clear();
            return;
        }
        InteractionHand hand = rodHand(player);
        if (hand == null) {
            clear();
            return;
        }
        if (hand != animationHand) {
            clear();
            animationHand = hand;
        }
        ItemStack other = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!other.isEmpty() && !ItemStack.isSameItemSameComponents(other, pushedPayload)) pushedPayload = ItemStack.EMPTY;
        if (hand == InteractionHand.OFF_HAND && !carriesPayload(player)) {
            attackTarget = null;
            attackUntil = 0;
            motion.stopKnock();
        } else if (mc.screen == null && mc.isWindowActive() && mc.options.keyAttack.isDown()
            && !BuildingRodClient.locked && BuildingRodClient.first == null) {
            if (mc.hitResult != null && mc.hitResult.getType() == HitResult.Type.BLOCK) attack();
        } else if (mc.screen != null || !mc.isWindowActive()) {
            attackUntil = 0;
        }
    }

    @SubscribeEvent
    public static void mouseReleased(InputEvent.MouseButton.Post event) {
        if (event.getAction() == GLFW.GLFW_RELEASE
            && Minecraft.getInstance().options.keyAttack.getKey().equals(
                com.mojang.blaze3d.platform.InputConstants.Type.MOUSE.getOrCreate(event.getButton()))) {
            attackCanceled = false;
        }
    }

    @SubscribeEvent
    public static void keyReleased(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_RELEASE
            && Minecraft.getInstance().options.keyAttack.matches(event.getKeyEvent())) attackCanceled = false;
    }

    private static double time() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : (mc.level.getGameTime() + (double) mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)) / 20;
    }

    public static boolean carriesPayload(LivingEntity entity) {
        InteractionHand hand = rodHand(entity);
        return hand != null && !payload(entity, hand).isEmpty();
    }

    @Nullable
    public static InteractionHand rodHand(LivingEntity entity) {
        if (entity.getMainHandItem().is(ModItems.BUILDING_ROD)) return InteractionHand.MAIN_HAND;
        return entity.getOffhandItem().is(ModItems.BUILDING_ROD) ? InteractionHand.OFF_HAND : null;
    }

    private static ItemStack payload(LivingEntity entity, InteractionHand hand) {
        ItemStack stack = entity.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        boolean supported = isPayload(stack);
        return supported ? stack : ItemStack.EMPTY;
    }

    public static boolean hideHand(LivingEntity entity, InteractionHand hand) {
        InteractionHand rod = rodHand(entity);
        if (rod == null || rod == hand) return false;
        return carriesPayload(entity) || entity == Minecraft.getInstance().player && rod == animationHand
            && time() < pushedUntil && !pushedPayload.isEmpty();
    }

    public static boolean usesToolClaw(LivingEntity entity) {
        return entity instanceof Player player
            && rodHand(entity) == null && BuildingRodItem.isCarried(player)
            && !entity.getMainHandItem().is(ModItems.CRAB_CLAW) && !entity.getOffhandItem().is(ModItems.CRAB_CLAW);
    }

    private static boolean isPayload(ItemStack stack) {
        return BuildingRodItem.isPlacementMaterial(stack) || stack.is(ModItems.STRUCTURE_DISK);
    }

    @SubscribeEvent
    public static void renderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        if (hideHand(player, event.getHand())) {
            event.setCanceled(true);
            return;
        }
        if (!event.getItemStack().is(ModItems.BUILDING_ROD)) return;
        event.setCanceled(true);
        boolean left = (event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm()
            : player.getMainArm().getOpposite()) == HumanoidArm.LEFT;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        // 保留装备抬手的位置，只移除建筑杖的原版挥击变换。
        pose.translate(left ? -0.56 : 0.56, -0.52 - event.getEquipProgress() * 0.6, -0.72);
        renderHeld(player, event.getItemStack(), left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, left, pose, event.getSubmitNodeCollector(), event.getPackedLight());
        pose.popPose();
    }

    static final class HeldState {
        final BuildingRodModelParts.ModelState model;
        final BuildingRodModelParts.ModelState payloadModel = new BuildingRodModelParts.ModelState();
        ItemStack stack = ItemStack.EMPTY;
        ItemDisplayContext context = ItemDisplayContext.NONE;
        BuildingRodMotion.Pose visual = new BuildingRodMotion.Pose(0, 0, 0, 0);
        PayloadPose payloadPose = new PayloadPose(0.25f, 0.25f, Vec3.ZERO);
        @Nullable Vec3 target;
        boolean left;
        boolean powered;
        boolean attacking;
        boolean green;
        boolean payloadDisk;
        boolean hasPayload;

        HeldState(BuildingRodModelParts.ModelState model) {
            this.model = model;
        }
    }

    public static void renderHeld(
        LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean left,
        PoseStack pose, SubmitNodeCollector collector, int light
    ) {
        extractHeld(entity, stack, context, left, FIRST_PERSON_STATE, true);
        submitHeld(FIRST_PERSON_STATE, pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
    }

    static void extractHeld(
        LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean left, HeldState state, boolean resolveModel
    ) {
        Minecraft mc = Minecraft.getInstance();
        InteractionHand hand = left == (entity.getMainArm() == HumanoidArm.LEFT) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        final boolean localRod = entity == mc.player && hand == rodHand(entity);
        state.stack = stack.copy();
        state.context = context;
        state.left = left;
        state.powered = stack.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value() > 0;
        state.attacking = localRod && mc.player != null && mc.player.isCreative()
            && attackTarget != null && time() < attackUntil && mc.screen == null;
        boolean charging = localRod && mc.screen == null && mc.options.keyUse.isDown() && BuildingRodClient.first != null;
        state.visual = localRod ? motion.sample(time(), state.powered, charging, state.attacking)
            : new BuildingRodMotion.Pose(state.powered ? (float) (time() * 90 % 360) : 0, 0, 0, 0);
        if (resolveModel) mc.getItemModelResolver().updateForTopItem(state.model, stack, context, entity.level(), entity, entity.getId());
        state.target = localRod && mc.player != null && mc.player.isCreative() && attackTarget != null
            ? attackTarget.subtract(mc.gameRenderer.getMainCamera().position()) : null;
        state.green = localRod && BuildingRodClient.locked && BuildingRodClient.hasMatchingDisk();
        ItemStack held = hand != rodHand(entity) ? ItemStack.EMPTY
            : localRod && time() < pushedUntil && !pushedPayload.isEmpty() ? pushedPayload : payload(entity, hand);
        state.hasPayload = !held.isEmpty();
        state.payloadDisk = held.is(ModItems.STRUCTURE_DISK);
        if (!state.hasPayload) {
            state.payloadModel.clear();
            return;
        }
        mc.getItemModelResolver().updateForTopItem(
            state.payloadModel, held, ItemDisplayContext.NONE, entity.level(), entity, entity.getId());
        if (PAYLOAD_POSES.size() >= 128) PAYLOAD_POSES.clear();
        state.payloadPose = state.payloadModel.isAnimated() ? PayloadPose.of(state.payloadModel, held)
            : PAYLOAD_POSES.computeIfAbsent(state.payloadModel.identity(), ignored -> PayloadPose.of(state.payloadModel, held));
    }

    static void submitHeld(HeldState state, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, int outline) {
        var visual = state.visual;
        pose.pushPose();
        if (state.context.firstPerson()) {
            pose.translate((state.left ? 0.08 : -0.08) * visual.aim(), 0.06 * visual.aim(), -0.3 * visual.aim());
            pose.translate((state.left ? 0.18 : -0.18) * visual.knock(), -0.1 * visual.knock(), -0.2 * visual.knock());
            pose.mulPose(Axis.YP.rotationDegrees((state.left ? -18 : 18) * visual.knock()));
            pose.mulPose(Axis.XP.rotationDegrees(-65 * visual.knock()));
        }
        state.model.applyTransform(pose);
        if (!state.context.firstPerson() && visual.knock() != 0) {
            pose.translate(0.5, 2.0 / 16, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(-65 * visual.knock()));
            pose.translate(-0.5, -2.0 / 16, -0.5);
        }
        if (state.target != null && visual.aim() > 0.001f) {
            Vector3f laserTarget = targetInPose(pose, state.target);
            Vector3f direction = new Vector3f(laserTarget).sub(0.5f, 2f / 16, 0.5f).normalize();
            Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), direction);
            pose.translate(0.5, 2.0 / 16, 0.5);
            pose.mulPose(new Quaternionf().slerp(rotation, visual.aim()));
            pose.translate(-0.5, -2.0 / 16, -0.5);
        }
        BuildingRodModelParts.render(state.model, state.stack, state.context, pose, collector, light, overlay, outline,
            visual.angle(), state.green, state.powered);
        if (state.hasPayload) {
            pose.pushPose();
            pose.translate(0.5, 18.0 / 16 + visual.kick() * 0.18, 0.5);
            if (state.payloadDisk) pose.mulPose(Axis.XP.rotationDegrees(-90));
            PayloadPose fitted = state.payloadPose;
            pose.scale(fitted.scale(), fitted.heightScale(), fitted.scale());
            pose.translate(-fitted.center().x, -fitted.center().y, -fitted.center().z);
            state.payloadModel.submit(pose, collector, light, overlay, outline);
            pose.popPose();
        }
        if (state.attacking && state.target != null) renderLaser(pose, collector, targetInPose(pose, state.target));
        pose.popPose();
    }

    private static Vector3f targetInPose(PoseStack pose, Vec3 relativeTarget) {
        return new Matrix4f(pose.last().pose()).invert().transformPosition(relativeTarget.toVector3f());
    }

    static void clearModels() {
        PAYLOAD_POSES.clear();
        BuildingRodModelParts.clearCache();
    }

    private record PayloadPose(float scale, float heightScale, Vec3 center) {
        private static PayloadPose of(BuildingRodModelParts.ModelState model, ItemStack stack) {
            Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
            Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
            model.visitExtents(vertex -> {
                min.min(vertex);
                max.max(vertex);
            });
            boolean column = stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof AbstractMultiPartBlock<?> multipart && isColumn(multipart);
            return fit(min, max, column);
        }

        private static PayloadPose fit(Vector3f min, Vector3f max, boolean column) {
            double width = Math.max(max.x - min.x, max.z - min.z);
            double height = max.y - min.y;
            double size = Math.max(width, height);
            if (!Double.isFinite(size) || size <= 0) return new PayloadPose(0.25f, 0.25f, Vec3.ZERO);
            float scale = (float) (0.25 / (column && width > 0 ? width : size));
            double centerY = column && height > width ? min.y + 0.125 / scale : (min.y + max.y) / 2;
            return new PayloadPose(scale, scale, new Vec3((min.x + max.x) / 2, centerY, (min.z + max.z) / 2));
        }

        private static <P extends Enum<P>> boolean isColumn(AbstractMultiPartBlock<P> block) {
            if (block.getParts().length <= 1) return false;
            for (P part : block.getParts()) {
                var offset = block.offsetFrom(block.defaultBlockState(), part);
                if (offset.getX() != 0 || offset.getZ() != 0) return false;
            }
            return true;
        }
    }

    private static void renderLaser(PoseStack pose, SubmitNodeCollector buffers, Vector3f end) {
        Vector3f start = new Vector3f(0.5f, 14f / 16, 0.5f);
        Vector3f direction = new Vector3f(end).sub(start);
        if (direction.lengthSquared() < 0.0001f) return;
        direction.normalize();
        Vector3f side = direction.cross(Math.abs(direction.y) < 0.9f ? new Vector3f(0, 1, 0)
            : new Vector3f(1, 0, 0), new Vector3f()).normalize();
        Vector3f up = new Vector3f(side).cross(direction).normalize();
        buffers.submitCustomGeometry(pose, ModRenderTypes.STELLAR_BEAM, (matrix, vertices) -> {
            PoseStack drawing = new PoseStack();
            drawing.last().set(matrix);
            beam(drawing, vertices, start, end, side, up, 0.025f, 0x5030DAFF);
            beam(drawing, vertices, start, end, side, up, 0.008f, 0xFFE0FFFF);
        });
    }

    private static void beam(
        PoseStack pose, VertexConsumer vertices, Vector3f start, Vector3f end, Vector3f side, Vector3f up, float width, int color
    ) {
        Vector3f[] corners = {
            new Vector3f(side).add(up).mul(width), new Vector3f(side).sub(up).mul(width),
            new Vector3f(side).negate().sub(up).mul(width), new Vector3f(up).sub(side).mul(width)
        };
        for (int index = 0; index < 4; index++) {
            Vector3f first = corners[index];
            Vector3f next = corners[(index + 1) % 4];
            vertices.addVertex(pose.last(), start.x + first.x, start.y + first.y, start.z + first.z).setColor(color);
            vertices.addVertex(pose.last(), end.x + first.x, end.y + first.y, end.z + first.z).setColor(color);
            vertices.addVertex(pose.last(), end.x + next.x, end.y + next.y, end.z + next.z).setColor(color);
            vertices.addVertex(pose.last(), start.x + next.x, start.y + next.y, start.z + next.z).setColor(color);
        }
    }
}
