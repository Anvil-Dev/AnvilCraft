package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.item.CelestialForgingAnvilBlockItem;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.client.init.ModRenderTypes;
import dev.dubhe.anvilcraft.client.support.RenderModelSupport;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class BuildingRodItemRenderer {
    private static final Map<BakedModel, PayloadPose> PAYLOAD_POSES = new WeakHashMap<>();
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
        pendingPayload = player.getOffhandItem().copyWithCount(1);
        pendingUntil = time() + 5;
    }

    public static void placed() {
        var player = Minecraft.getInstance().player;
        if (player == null || !player.getMainHandItem().is(ModItems.BUILDING_ROD)
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
            && Minecraft.getInstance().options.keyAttack.matchesMouse(event.getButton())) attackCanceled = false;
    }

    @SubscribeEvent
    public static void keyReleased(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_RELEASE
            && Minecraft.getInstance().options.keyAttack.matches(event.getKey(), event.getScanCode())) attackCanceled = false;
    }

    private static double time() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0 : (mc.level.getGameTime() + (double) mc.getTimer().getGameTimeDeltaPartialTick(true)) / 20;
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
        boolean supported = hand == InteractionHand.MAIN_HAND ? isPayload(stack) : BuildingRodItem.isPlacementMaterial(stack);
        return supported ? stack : ItemStack.EMPTY;
    }

    public static boolean hideHand(LivingEntity entity, InteractionHand hand) {
        InteractionHand rod = rodHand(entity);
        if (rod == null || rod == hand) return false;
        return carriesPayload(entity) || entity == Minecraft.getInstance().player && rod == animationHand
            && time() < pushedUntil && !pushedPayload.isEmpty();
    }

    public static boolean usesToolClaw(LivingEntity entity) {
        return rodHand(entity) == InteractionHand.OFF_HAND && !entity.getMainHandItem().isEmpty()
            && !BuildingRodItem.isPlacementMaterial(entity.getMainHandItem());
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
            : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, left, pose, event.getMultiBufferSource(), event.getPackedLight());
        pose.popPose();
    }

    public static void renderHeld(
        LivingEntity entity, ItemStack stack, ItemDisplayContext context, boolean left,
        PoseStack pose, MultiBufferSource buffers, int light
    ) {
        Minecraft mc = Minecraft.getInstance();
        boolean mainHand = left == (entity.getMainArm() == HumanoidArm.LEFT);
        InteractionHand hand = mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        boolean localRod = entity == mc.player && hand == rodHand(entity);
        boolean powered = stack.getOrDefault(ModComponents.STORED_ENERGY, 0) > 0;
        boolean attacking = localRod && mc.player != null && mc.player.isCreative()
            && attackTarget != null && time() < attackUntil && mc.screen == null;
        boolean charging = localRod && mc.screen == null && mc.options.keyUse.isDown()
            && (mainHand ? BuildingRodClient.first != null : carriesPayload(entity));
        BuildingRodMotion.Pose visual = localRod ? motion.sample(time(), powered, charging, attacking)
            : new BuildingRodMotion.Pose(powered ? (float) (time() * 90 % 360) : 0, 0, 0, 0);
        pose.pushPose();
        if (context.firstPerson()) {
            pose.translate((left ? 0.08 : -0.08) * visual.aim(), 0.06 * visual.aim(), -0.3 * visual.aim());
            pose.translate((left ? 0.18 : -0.18) * visual.knock(), -0.1 * visual.knock(), -0.2 * visual.knock());
            pose.mulPose(Axis.YP.rotationDegrees((left ? -18 : 18) * visual.knock()));
            pose.mulPose(Axis.XP.rotationDegrees(-65 * visual.knock()));
        }
        ItemRenderer renderer = mc.getItemRenderer();
        BakedModel model = renderer.getModel(stack, entity.level(), entity, entity.getId());
        model = ClientHooks.handleCameraTransforms(pose, model, context, left);
        pose.translate(-0.5, -0.5, -0.5);
        if (!context.firstPerson() && visual.knock() != 0) {
            pose.translate(0.5, 2.0 / 16, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(-65 * visual.knock()));
            pose.translate(-0.5, -2.0 / 16, -0.5);
        }
        Vector3f laserTarget = localRod && mc.player != null && mc.player.isCreative() && attackTarget != null
            ? targetInPose(pose, attackTarget) : null;
        if (laserTarget != null && visual.aim() > 0.001f) {
            // 以握柄为支点前伸，杖身始终从握持侧进入画面，核心随杖头一起移动。
            Vector3f direction = new Vector3f(laserTarget).sub(0.5f, 2f / 16, 0.5f).normalize();
            Quaternionf rotation = new Quaternionf().rotationTo(new Vector3f(0, 1, 0), direction);
            pose.translate(0.5, 2.0 / 16, 0.5);
            pose.mulPose(new Quaternionf().slerp(rotation, visual.aim()));
            pose.translate(-0.5, -2.0 / 16, -0.5);
        }
        boolean green = localRod && mainHand && BuildingRodClient.locked && BuildingRodClient.hasMatchingDisk();
        BuildingRodModelParts.render(model, stack, pose, buffers, light, OverlayTexture.NO_OVERLAY,
            visual.angle(), green, powered);
        if (hand == rodHand(entity)) {
            ItemStack held = localRod && time() < pushedUntil && !pushedPayload.isEmpty()
                ? pushedPayload : payload(entity, hand);
            if (!held.isEmpty()) renderPayload(entity, held, pose, buffers, light, visual.kick());
        }
        if (attacking && attackTarget != null) {
            Vector3f end = targetInPose(pose, attackTarget);
            renderLaser(pose, buffers, end);
        }
        pose.popPose();
    }

    private static Vector3f targetInPose(PoseStack pose, Vec3 target) {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 relative = target.subtract(camera.getPosition());
        Vector3f point = relative.toVector3f();
        // 第一人称姿态栈已包含相机旋转的逆矩阵，终点必须保留相机相对世界坐标。
        return new Matrix4f(pose.last().pose()).invert().transformPosition(point);
    }

    private static void renderPayload(
        LivingEntity entity, ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, float kick
    ) {
        pose.pushPose();
        pose.translate(0.5, 18.0 / 16 + kick * 0.18, 0.5);
        if (stack.is(ModItems.STRUCTURE_DISK)) pose.mulPose(Axis.XP.rotationDegrees(-90));
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = renderer.getModel(stack, entity.level(), entity, entity.getId());
        PayloadPose fitted = PAYLOAD_POSES.computeIfAbsent(model, value -> PayloadPose.of(value, stack));
        pose.scale(fitted.scale(), fitted.heightScale(), fitted.scale());
        pose.translate(0.5 - fitted.center().x, 0.5 - fitted.center().y, 0.5 - fitted.center().z);
        renderer.render(stack, ItemDisplayContext.NONE, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, model);
        pose.popPose();
    }

    private record PayloadPose(float scale, float heightScale, Vec3 center) {
        private static PayloadPose of(BakedModel model, ItemStack stack) {
            Vector3f min = new Vector3f(Float.POSITIVE_INFINITY);
            Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY);
            RandomSource random = RandomSource.create(42);
            for (BakedModel pass : model.getRenderPasses(stack, true)) {
                for (int side = 0; side <= Direction.values().length; side++) {
                    Direction direction = side == Direction.values().length ? null : Direction.values()[side];
                    random.setSeed(42);
                    for (var quad : pass.getQuads(null, direction, random, ModelData.EMPTY, null)) {
                        for (Vector3f vertex : RenderModelSupport.getVertices(quad)) {
                            min.min(vertex);
                            max.max(vertex);
                        }
                    }
                }
            }
            boolean column = stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof AbstractMultiPartBlock<?> multipart && isColumn(multipart);
            PayloadPose fitted = fit(min, max, column);
            // 锻星砧的物品渲染器在 NONE 上下文额外抬高一格，夹持中心需合入这个变换。
            return stack.getItem() instanceof CelestialForgingAnvilBlockItem
                ? new PayloadPose(fitted.scale, fitted.heightScale, fitted.center.add(0, 1, 0)) : fitted;
        }

        private static PayloadPose fit(Vector3f min, Vector3f max, boolean column) {
            double width = Math.max(max.x - min.x, max.z - min.z);
            double height = max.y - min.y;
            double size = Math.max(width, height);
            if (!Double.isFinite(size) || size <= 0) return new PayloadPose(0.25f, 0.25f, new Vec3(0.5, 0.5, 0.5));
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

    private static void renderLaser(PoseStack pose, MultiBufferSource buffers, Vector3f end) {
        Vector3f start = new Vector3f(0.5f, 14f / 16, 0.5f);
        Vector3f direction = new Vector3f(end).sub(start);
        if (direction.lengthSquared() < 0.0001f) return;
        direction.normalize();
        Vector3f side = direction.cross(Math.abs(direction.y) < 0.9f ? new Vector3f(0, 1, 0)
            : new Vector3f(1, 0, 0), new Vector3f()).normalize();
        Vector3f up = new Vector3f(side).cross(direction).normalize();
        VertexConsumer vertices = buffers.getBuffer(ModRenderTypes.STELLAR_BEAM);
        beam(pose, vertices, start, end, side, up, 0.025f, 0x5030DAFF);
        beam(pose, vertices, start, end, side, up, 0.008f, 0xFFE0FFFF);
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
