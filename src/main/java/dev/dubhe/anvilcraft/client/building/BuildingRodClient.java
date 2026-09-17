package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.BuildingRodFluids;
import dev.dubhe.anvilcraft.building.BuildingRodService;
import dev.dubhe.anvilcraft.building.ConstructionBlueprintException;
import dev.dubhe.anvilcraft.building.ScannerDiskNormalizer;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.network.BuildingRodPacket;
import dev.dubhe.anvilcraft.util.BlockPlacementPicking;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class BuildingRodClient {
    @Nullable private static StructureDiskData disk;
    @Nullable private static ClientLevel sessionLevel;
    @Nullable static StructureSnapshot snapshot;
    @Nullable static BlockPos first;
    @Nullable static BlockPos target;
    @Nullable private static BlockHitResult currentHit;
    @Nullable private static BlockHitResult firstHit;
    @Nullable private static List<BuildingRodService.Cell> placementCells;
    private static ItemStack selected = ItemStack.EMPTY;
    static BlueprintPlacement placement = new BlueprintPlacement(BlockPos.ZERO, Rotation.NONE, Mirror.NONE);
    static boolean locked;
    static int layer = -1;
    private static boolean wasTraditional;
    private static int yOffset;
    private static Rotation rotationOffset = Rotation.NONE;
    private static double previewDistance = 5;
    private static boolean distanceHeld;
    private static boolean useHeld;
    private static boolean invalidDisk;
    private static Direction face = Direction.UP;
    private static final RandomSource PATTERN_RANDOM = RandomSource.create();
    private static long patternSeed = PATTERN_RANDOM.nextLong();
    private static final BuildingRodKeyRepeat KEY_REPEAT = new BuildingRodKeyRepeat();
    private static final int PLACE_COLOR = 0x55FF55;
    private static final int CANCEL_COLOR = 0xFF5555;
    private static final int MOVE_COLOR = 0xFFFF55;
    private static final int ROTATE_COLOR = 0x55FFFF;
    private static final int MIRROR_COLOR = 0xFF55FF;
    private static final int SCROLL_COLOR = 0xAAAAAA;

    private BuildingRodClient() {
    }

    public static void cancel() {
        BuildingRodTraditionalControls.clear();
        first = null;
        patternSeed = PATTERN_RANDOM.nextLong();
        locked = false;
        layer = -1;
        yOffset = 0;
        rotationOffset = Rotation.NONE;
        previewDistance = 5;
        distanceHeld = false;
        placement = new BlueprintPlacement(BlockPos.ZERO, Rotation.NONE, Mirror.NONE);
        disk = null;
        snapshot = null;
        selected = ItemStack.EMPTY;
        target = null;
        currentHit = null;
        firstHit = null;
        placementCells = null;
        invalidDisk = false;
        KEY_REPEAT.clear();
        BuildingRodRenderer.clear();
    }

    private static boolean active() {
        var player = Minecraft.getInstance().player;
        return player != null && BuildingRodItem.isHeld(player);
    }

    private static boolean holdingRod() {
        var player = Minecraft.getInstance().player;
        return player != null && BuildingRodItem.isHeld(player);
    }

    static boolean traditional() {
        return AnvilCraft.CLIENT_CONFIG.buildingRodControls == AnvilCraftClientConfig.BuildingRodControls.TRADITIONAL;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (wasTraditional != traditional()) {
            cancel();
            wasTraditional = traditional();
        }
        if (sessionLevel != mc.level) {
            cancel();
            BuildingRodItemRenderer.clear();
            sessionLevel = mc.level;
        }
        BuildingRodItemRenderer.tick();
        if (traditional() && BuildingRodTraditionalControls.isActive()
            && (!active() || mc.player == null || !ItemStack.isSameItemSameComponents(selected, BuildingRodItem.material(mc.player)))) {
            cancel();
        }
        if (!active() || mc.screen != null || mc.level == null || mc.player == null) {
            if (!locked && !(traditional() && BuildingRodTraditionalControls.isActive())) cancel();
            first = null;
            target = null;
            distanceHeld = false;
            useHeld = mc.options.keyUse.isDown();
            KEY_REPEAT.clear();
            discardControlClicks();
            return;
        }
        ItemStack other = BuildingRodItem.material(mc.player);
        // 优化模式的固定蓝图独立于手持物；传统模式的持有条件由工具会话检查。
        if (!locked) {
            if (!ItemStack.isSameItemSameComponents(selected, other)) {
                cancel();
                selected = other.copyWithCount(1);
            }
            StructureDiskData next = other.get(ModComponents.STRUCTURE_DISK_DATA);
            if (!Objects.equals(disk, next)) {
                disk = next;
                snapshot = null;
                invalidDisk = false;
                BuildingRodRenderer.clear();
            }
            if (disk != null && snapshot == null && !invalidDisk) {
                StructureLoadUtil.getStructureNbtForPreview(mc.level, disk).ifPresent(tag -> {
                    try {
                        snapshot = ScannerDiskNormalizer.normalize(StructureSnapshotCodec.parse(tag, mc.level.registryAccess()).snapshot(),
                            disk.direction(), disk.upsideDown());
                    } catch (ConstructionBlueprintException exception) {
                        invalidDisk = true;
                        mc.player.displayClientMessage(Component.translatable("message.anvilcraft.building_rod.invalid_structure"), true);
                    }
                });
            }
        }
        updateTarget();
        if (mc.options.keyUse.isDown() && !useHeld) press();
        if (!mc.options.keyUse.isDown() && useHeld) release();
        controls();
    }

    private static void updateTarget() {
        placementCells = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (traditional() && snapshot != null) {
            BuildingRodTraditionalControls.updateTarget();
            return;
        }
        HitResult result = mc.player.pick(mc.player.blockInteractionRange(), 1, false);
        boolean floating = snapshot != null && !locked && !traditional() && mc.isWindowActive()
            && (physicalModifiers(mc.getWindow().getWindow()) & GLFW.GLFW_MOD_CONTROL) != 0;
        if (floating) {
            if (!distanceHeld && result.getType() == HitResult.Type.BLOCK) {
                previewDistance = Math.clamp(mc.player.getEyePosition().distanceTo(result.getLocation()),
                    1, mc.player.blockInteractionRange());
            }
            target = BlockPos.containing(mc.player.getEyePosition().add(mc.player.getLookAngle().scale(previewDistance)));
            face = Direction.UP;
        } else if (result instanceof BlockHitResult hit && result.getType() == HitResult.Type.BLOCK) {
            currentHit = hit;
            face = hit.getDirection();
            var hitState = mc.level.getBlockState(hit.getBlockPos());
            boolean waterlogging = hitState.hasProperty(BlockStateProperties.WATERLOGGED)
                && BuildingRodFluids.isWater(BuildingRodItem.material(mc.player));
            if (snapshot == null && BuildingRodItem.isPlacementMaterial(BuildingRodItem.material(mc.player))
                && !BuildingRodItem.material(mc.player).is(ModItems.FILTER) && !waterlogging) {
                UseOnContext use = BlockPlacementPicking.forPlacement(
                    new UseOnContext(mc.player, BuildingRodItem.materialHand(mc.player), hit));
                if (use instanceof BlockPlacementPicking.PlayerClick click && !click.anvilcraft$hasBlockHit()) {
                    target = null;
                    currentHit = null;
                    return;
                }
                currentHit = new BlockHitResult(use.getClickLocation(), use.getClickedFace(), use.getClickedPos(), false);
                face = use.getClickedFace();
                target = new BlockPlaceContext(use).getClickedPos();
            } else {
                target = hitState.canBeReplaced() || waterlogging ? hit.getBlockPos() : hit.getBlockPos().relative(face);
            }
            previewDistance = Math.clamp(mc.player.getEyePosition().distanceTo(result.getLocation()), 1, mc.player.blockInteractionRange());
        } else {
            distanceHeld = false;
            target = null;
            currentHit = null;
            return;
        }
        distanceHeld = floating;
        if (snapshot == null || locked || disk == null) return;
        Rotation rotation = viewRotation().getRotated(rotationOffset);
        BlueprintPlacement local = new BlueprintPlacement(BlockPos.ZERO, rotation, placement.mirror());
        var bounds = local.bounds(snapshot.size());
        int x = bounds.minX() + bounds.getXSpan() / 2;
        final int y = face == Direction.DOWN ? bounds.maxY() : bounds.minY();
        int z = bounds.minZ() + bounds.getZSpan() / 2;
        if (face == Direction.EAST) x = bounds.minX();
        if (face == Direction.WEST) x = bounds.maxX();
        if (face == Direction.SOUTH) z = bounds.minZ();
        if (face == Direction.NORTH) z = bounds.maxZ();
        placement = new BlueprintPlacement(target.offset(-x, -y + yOffset, -z), rotation, placement.mirror());
    }

    private static Rotation viewRotation() {
        var player = Minecraft.getInstance().player;
        if (player == null || disk == null || !disk.autoRotate()) return Rotation.NONE;
        Direction source = disk.direction().getAxis().isHorizontal() ? disk.direction() : Direction.NORTH;
        for (Rotation candidate : Rotation.values()) {
            if (candidate.rotate(source) == player.getDirection()) return candidate;
        }
        return Rotation.NONE;
    }

    @SubscribeEvent
    public static void interaction(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen == null && holdingRod() && event.isAttack() && ((!traditional() && locked) || first != null)) {
            BuildingRodItemRenderer.cancelAttack();
            cancel();
            event.setCanceled(true);
            event.setSwingHand(false);
            return;
        }
        if (!active()) return;
        if (event.isAttack()) {
            event.setSwingHand(false);
            BuildingRodItemRenderer.attack();
        }
        if (event.isUseItem()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            if (!useHeld) press();
        }
    }

    private static void press() {
        useHeld = true;
        updateTarget();
        if (traditional() && disk != null) {
            BuildingRodTraditionalControls.press();
            return;
        }
        if (snapshot != null) {
            if (target == null && !locked) return;
            if (!locked) {
                locked = true;
            } else {
                confirm();
            }
        } else if (target != null && BuildingRodItem.isPlacementMaterial(selected)) {
            first = target;
            firstHit = currentHit;
            PacketDistributor.sendToServer(new BuildingRodPacket(first, first, face, false, Rotation.NONE,
                Mirror.NONE, false, firstHit, BuildingRodPacket.Action.START, patternSeed));
        }
    }

    private static void release() {
        useHeld = false;
        if (first == null) return;
        if (target != null) {
            BuildingRodItemRenderer.preparePlacement();
            PacketDistributor.sendToServer(new BuildingRodPacket(first, target, firstHit == null ? face : firstHit.getDirection(),
                false, Rotation.NONE, Mirror.NONE, false, firstHit, BuildingRodPacket.Action.PLACE, patternSeed));
        }
        first = null;
        firstHit = null;
        patternSeed = PATTERN_RANDOM.nextLong();
        placementCells = null;
    }

    public static List<BuildingRodService.Cell> placementPreview() {
        var player = Minecraft.getInstance().player;
        if (!active() || player == null || target == null || snapshot != null || Minecraft.getInstance().screen != null) return List.of();
        if (placementCells == null) {
            placementCells = BuildingRodService.preview(player, first == null ? target : first, target,
                firstHit == null ? face : firstHit.getDirection(),
                first == null ? currentHit : firstHit, patternSeed);
        }
        return placementCells;
    }

    private static KeyMapping[] controlKeys() {
        return new KeyMapping[]{ModKeyMappings.BUILDING_ROD_FORWARD.get(), ModKeyMappings.BUILDING_ROD_BACK.get(),
            ModKeyMappings.BUILDING_ROD_LEFT.get(), ModKeyMappings.BUILDING_ROD_RIGHT.get(), ModKeyMappings.BUILDING_ROD_UP.get(),
            ModKeyMappings.BUILDING_ROD_DOWN.get(), ModKeyMappings.BUILDING_ROD_CLOCKWISE.get(),
            ModKeyMappings.BUILDING_ROD_COUNTERCLOCKWISE.get(), ModKeyMappings.BUILDING_ROD_MIRROR.get()};
    }

    private static boolean canControl() {
        Minecraft mc = Minecraft.getInstance();
        return (locked || canAdjustDistance()) && active() && !traditional()
            && mc.screen == null && mc.getOverlay() == null && mc.isWindowActive();
    }

    private static boolean canAdjustDistance() {
        Minecraft mc = Minecraft.getInstance();
        return !locked && snapshot != null && active() && !traditional() && mc.screen == null
            && mc.getOverlay() == null && mc.isWindowActive()
            && (physicalModifiers(mc.getWindow().getWindow()) & GLFW.GLFW_MOD_CONTROL) != 0;
    }

    /** 在 KeyboardHandler 入口处理，避免依赖可能被其他模组取消的 NeoForge 末尾事件。 */
    public static boolean handleKeyboardInput(int key, int scanCode, int action, int modifiers) {
        Minecraft mc = Minecraft.getInstance();
        if (key == GLFW.GLFW_KEY_Z && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0
            && active() && mc.screen == null && mc.getOverlay() == null && mc.isWindowActive()) {
            if (action == GLFW.GLFW_PRESS) {
                PacketDistributor.sendToServer(new BuildingRodPacket(BlockPos.ZERO, BlockPos.ZERO, Direction.UP,
                    false, Rotation.NONE, Mirror.NONE, false, null, BuildingRodPacket.Action.UNDO));
            }
            return true;
        }
        return handleControl(InputConstants.getKey(key, scanCode), scanCode, action, modifiers);
    }

    @SubscribeEvent
    public static void mouseControl(InputEvent.MouseButton.Pre event) {
        if (handleControl(InputConstants.Type.MOUSE.getOrCreate(event.getButton()), -1, event.getAction(), event.getModifiers())) {
            event.setCanceled(true);
        }
    }

    private static boolean handleControl(InputConstants.Key pressed, int scanCode, int action, int modifiers) {
        KeyMapping[] keys = controlKeys();
        boolean enabled = canControl();
        boolean handled = false;
        for (int i = 0; i < keys.length; i++) {
            InputConstants.Key bound = keys[i].getKey();
            if (bound.equals(InputConstants.UNKNOWN)) continue;
            if (!bound.equals(pressed) && !(bound.getType() == InputConstants.Type.SCANCODE && bound.getValue() == scanCode)) continue;
            if (action == GLFW.GLFW_RELEASE) KEY_REPEAT.release(i);
            if (!enabled || (canAdjustDistance() && i >= 2) || !modifiersMatch(keys[i], modifiers)) continue;
            if (action == GLFW.GLFW_PRESS && KEY_REPEAT.press(i, System.nanoTime())) applyControl(i, 1);
            // 系统重复事件只拦截，不执行位移；连移统一使用物理状态与自己的计时。
            handled = true;
        }
        return handled;
    }

    private static boolean modifiersMatch(KeyMapping key, int modifiers) {
        return switch (key.getKeyModifier()) {
            case SHIFT -> (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
            case CONTROL -> (modifiers & (Minecraft.ON_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL)) != 0;
            case ALT -> (modifiers & GLFW.GLFW_MOD_ALT) != 0;
            case NONE -> true;
        };
    }

    private static boolean physicalKeyDown(InputConstants.Key key, int index, long window) {
        int code = key.getValue();
        return switch (key.getType()) {
            case KEYSYM -> code >= GLFW.GLFW_KEY_SPACE && code <= GLFW.GLFW_KEY_LAST && InputConstants.isKeyDown(window, code);
            case MOUSE -> code >= 0 && code <= GLFW.GLFW_MOUSE_BUTTON_LAST && GLFW.glfwGetMouseButton(window, code) == GLFW.GLFW_PRESS;
            case SCANCODE -> KEY_REPEAT.isHeld(index);
        };
    }

    private static int physicalModifiers(long window) {
        int modifiers = 0;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
            || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            modifiers |= GLFW.GLFW_MOD_SHIFT;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
            || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            modifiers |= GLFW.GLFW_MOD_CONTROL;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
            || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT)) {
            modifiers |= GLFW.GLFW_MOD_ALT;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SUPER)
            || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SUPER)) {
            modifiers |= GLFW.GLFW_MOD_SUPER;
        }
        return modifiers;
    }

    private static void discardControlClicks() {
        for (KeyMapping key : controlKeys()) {
            while (key.consumeClick()) {
                // 移动由原始按下事件和客户端计时驱动，不累积系统重复事件。
            }
        }
    }

    private static void controls() {
        discardControlClicks();
        if (!canControl()) {
            KEY_REPEAT.clear();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        int modifiers = physicalModifiers(window);
        KeyMapping[] keys = controlKeys();
        long now = System.nanoTime();
        for (int i = 0; i < keys.length; i++) {
            boolean down = (!canAdjustDistance() || i < 2)
                && physicalKeyDown(keys[i].getKey(), i, window) && modifiersMatch(keys[i], modifiers);
            int steps = KEY_REPEAT.update(i, down, now);
            if (steps > 0) applyControl(i, steps);
        }
    }

    private static void applyControl(int index, int steps) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        if (canAdjustDistance()) {
            if (index >= 2) return;
            updateTarget();
            previewDistance = Math.clamp(previewDistance + (index == 0 ? steps : -steps), 1, player.blockInteractionRange());
            updateTarget();
            return;
        }
        Direction forward = player.getDirection();
        Direction[] directions = {forward, forward.getOpposite(), forward.getCounterClockWise(), forward.getClockWise(),
            Direction.UP, Direction.DOWN};
        if (index < 6) {
            move(directions[index], steps);
        } else if (index < 8) {
            rotate(index == 6 ? 1 : -1);
        } else {
            mirror();
        }
    }

    static void move(Direction direction, int steps) {
        placement = new BlueprintPlacement(placement.anchor().relative(direction, steps), placement.rotation(), placement.mirror());
    }

    static void transform(Rotation rotation, Mirror mirror) {
        if (snapshot == null) return;
        if (!locked && !traditional()) {
            for (Rotation candidate : Rotation.values()) {
                if (viewRotation().getRotated(candidate) == rotation) rotationOffset = candidate;
            }
        }
        var before = placement.bounds(snapshot.size());
        var after = new BlueprintPlacement(placement.anchor(), rotation, mirror).bounds(snapshot.size());
        placement = new BlueprintPlacement(placement.anchor().offset(
            before.minX() + before.getXSpan() / 2 - after.minX() - after.getXSpan() / 2,
            before.minY() - after.minY(),
            before.minZ() + before.getZSpan() / 2 - after.minZ() - after.getZSpan() / 2), rotation, mirror);
    }

    static void rotate(int step) {
        BuildingRodRenderer.turn(step);
        Rotation change = step > 0 ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90;
        transform(placement.rotation().getRotated(change), placement.mirror());
    }

    private static void mirror() {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        Direction localForward = player.getDirection();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (placement.rotation().rotate(direction) == player.getDirection()) localForward = direction;
        }
        Mirror axis = localForward.getAxis() == Direction.Axis.Z ? Mirror.FRONT_BACK : Mirror.LEFT_RIGHT;
        if (placement.mirror() == axis) {
            transform(placement.rotation(), Mirror.NONE);
        } else if (placement.mirror() == Mirror.NONE) {
            transform(placement.rotation(), axis);
        } else {
            transform(placement.rotation().getRotated(Rotation.CLOCKWISE_180), Mirror.NONE);
        }
    }

    static void stepLayer(int step) {
        if (snapshot == null) return;
        int next = layer == -1 ? step > 0 ? 0 : snapshot.size().getY() - 1 : layer + step;
        layer = next < 0 || next >= snapshot.size().getY() ? -1 : next;
    }

    @SubscribeEvent
    public static void scroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || snapshot == null || mc.screen != null || !mc.isWindowActive() || mc.player == null) return;
        if (!traditional()) return;
        BuildingRodTraditionalControls.scroll(event);
    }

    static boolean hasMatchingDisk() {
        var player = Minecraft.getInstance().player;
        return active() && player != null && BuildingRodItem.material(player).is(ModItems.STRUCTURE_DISK)
            && disk != null && disk.equals(BuildingRodItem.material(player).get(ModComponents.STRUCTURE_DISK_DATA));
    }

    static String blueprintName() {
        return disk == null ? "" : disk.name();
    }

    static void confirm() {
        var player = Minecraft.getInstance().player;
        if (player == null || !hasMatchingDisk()) return;
        BuildingRodItemRenderer.preparePlacement();
        PacketDistributor.sendToServer(new BuildingRodPacket(placement.anchor(), BlockPos.ZERO, face, true,
            placement.rotation(), placement.mirror(), player.isShiftKeyDown()));
    }

    @SubscribeEvent
    public static void overlay(RenderGuiEvent.Post event) {
        if (traditional()) {
            BuildingRodTraditionalOverlay.render(event.getGuiGraphics());
            return;
        }
        if (snapshot == null || (!locked && !active())) return;
        Minecraft mc = Minecraft.getInstance();
        Component text;
        if (!locked && !traditional()) {
            text = Component.translatable("screen.anvilcraft.building_rod.distance",
                key(ModKeyMappings.BUILDING_ROD_TOOL.get(), MOVE_COLOR),
                key(ModKeyMappings.BUILDING_ROD_FORWARD.get(), MOVE_COLOR),
                key(ModKeyMappings.BUILDING_ROD_BACK.get(), MOVE_COLOR),
                key(mc.options.keyUse, PLACE_COLOR));
        } else if (locked && !hasMatchingDisk()) {
            text = Component.translatable("screen.anvilcraft.building_rod.suspended",
                key(mc.options.keyAttack, CANCEL_COLOR));
        } else {
            text = Component.translatable("screen.anvilcraft.building_rod.optimized",
                key(mc.options.keyUse, PLACE_COLOR), key(mc.options.keyAttack, CANCEL_COLOR),
                key(ModKeyMappings.BUILDING_ROD_LEFT.get(), MOVE_COLOR),
                key(ModKeyMappings.BUILDING_ROD_BACK.get(), MOVE_COLOR),
                key(ModKeyMappings.BUILDING_ROD_FORWARD.get(), MOVE_COLOR),
                key(ModKeyMappings.BUILDING_ROD_RIGHT.get(), MOVE_COLOR),
                key(ModKeyMappings.BUILDING_ROD_UP.get(), MOVE_COLOR),
                key(ModKeyMappings.BUILDING_ROD_DOWN.get(), MOVE_COLOR),
                key(ModKeyMappings.BUILDING_ROD_COUNTERCLOCKWISE.get(), ROTATE_COLOR),
                key(ModKeyMappings.BUILDING_ROD_CLOCKWISE.get(), ROTATE_COLOR),
                key(ModKeyMappings.BUILDING_ROD_MIRROR.get(), MIRROR_COLOR));
        }
        var lines = mc.font.split(text, event.getGuiGraphics().guiWidth() - 20);
        int feedbackOffset = Math.max(Math.max(mc.gui.leftHeight, mc.gui.rightHeight) + 9, 68);
        int y = event.getGuiGraphics().guiHeight() - feedbackOffset - 7 - lines.size() * 10;
        for (var line : lines) {
            int left = (event.getGuiGraphics().guiWidth() - mc.font.width(line)) / 2;
            event.getGuiGraphics().fill(left - 3, y - 1, left + mc.font.width(line) + 3, y + 10, 0x80000000);
            event.getGuiGraphics().drawCenteredString(mc.font, line, event.getGuiGraphics().guiWidth() / 2, y, 0xFFFFFF);
            y += 10;
        }
    }

    static Component traditionalHint() {
        Component scroll = key(Component.translatable("screen.anvilcraft.building_rod.traditional.scroll"));
        return Component.translatable("screen.anvilcraft.building_rod.traditional.hint",
            key(ModKeyMappings.BUILDING_ROD_TOOL.get(), MOVE_COLOR), scroll,
            key(ModKeyMappings.BUILDING_ROD_ADJUST.get(), ROTATE_COLOR), scroll,
            key(Minecraft.getInstance().options.keyUse, PLACE_COLOR), scroll);
    }

    private static Component key(KeyMapping mapping) {
        String shortName = mapping.getKey().getType() == InputConstants.Type.KEYSYM && mapping.getKeyModifier() == KeyModifier.NONE
            ? switch (mapping.getKey().getValue()) {
            case GLFW.GLFW_KEY_EQUAL -> "=";
            case GLFW.GLFW_KEY_PAGE_UP -> "PgUp";
            case GLFW.GLFW_KEY_PAGE_DOWN -> "PgDn";
            default -> "";
        } : "";
        Component label = shortName.isEmpty() ? mapping.getTranslatedKeyMessage() : Component.literal(shortName);
        return Component.literal("[").append(label).append("]");
    }

    private static Component key(KeyMapping mapping, int color) {
        return key(mapping).copy().withColor(color);
    }

    private static Component key(Component label) {
        return Component.literal("[").append(label).append("]").withColor(BuildingRodClient.SCROLL_COLOR);
    }
}
