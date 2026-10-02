package dev.dubhe.anvilcraft.client.building;

import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;

/** 移植塑料工艺的工具制部署操作，仅由建筑杖传统模式调用。 */
final class BuildingRodTraditionalControls {
    enum Tool {
        MOVE("move"), ROTATE("rotate"), FLIP("flip"), LAYER_DOWN("layer_down"),
        LAYER_UP("layer_up"), CONFIRM("confirm"), CANCEL("cancel");

        private final String id;

        Tool(String id) {
            this.id = id;
        }

        String id() {
            return this.id;
        }
    }

    private static boolean active;
    private static Tool tool = Tool.MOVE;
    private static int yOffset;

    private BuildingRodTraditionalControls() {
    }

    static boolean isActive() {
        return active;
    }

    static Tool selectedTool() {
        return tool;
    }

    static void clear() {
        active = false;
        tool = Tool.MOVE;
        yOffset = 0;
    }

    static void press() {
        if (!active) {
            active = true;
            tool = Tool.MOVE;
            yOffset = 0;
            BuildingRodClient.locked = false;
            BuildingRodClient.layer = -1;
            BuildingRodClient.placement = new BlueprintPlacement(BlockPos.ZERO, Rotation.NONE, Mirror.NONE);
            BuildingRodRenderer.clear();
            updateTarget();
            BuildingRodClient.beginBlueprintSelection();
            return;
        }
        switch (tool) {
            case MOVE -> {
                if (BuildingRodClient.locked) BuildingRodClient.locked = false;
                else BuildingRodClient.beginBlueprintSelection();
            }
            case ROTATE -> BuildingRodClient.rotate(1);
            case FLIP -> mirror(1);
            case LAYER_DOWN -> BuildingRodClient.stepLayer(-1);
            case LAYER_UP -> BuildingRodClient.stepLayer(1);
            case CONFIRM -> BuildingRodClient.confirm();
            case CANCEL -> BuildingRodClient.cancel();
            default -> {
            }
        }
    }

    static void finishSelection() {
        tool = Tool.CONFIRM;
    }

    static void updateTarget() {
        var snapshot = BuildingRodClient.snapshot;
        var player = Minecraft.getInstance().player;
        if (!active || snapshot == null || player == null) {
            BuildingRodClient.target = null;
            return;
        }
        if (BuildingRodClient.locked) return;
        HitResult hit = player.pick(player.blockInteractionRange(), 1, false);
        Vec3 base = hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
            ? Vec3.atCenterOf(blockHit.getBlockPos().relative(blockHit.getDirection()))
            : player.getEyePosition().add(player.getLookAngle().scale(8));
        BlockPos target = BlockPos.containing(base);
        BuildingRodClient.target = target;
        BlueprintPlacement placement = BuildingRodClient.placement;
        var bounds = new BlueprintPlacement(BlockPos.ZERO, placement.rotation(), placement.mirror()).bounds(snapshot.size());
        BuildingRodClient.placement = new BlueprintPlacement(target.offset(
            -bounds.minX() - bounds.getXSpan() / 2, -bounds.minY() + yOffset, -bounds.minZ() - bounds.getZSpan() / 2),
            placement.rotation(), placement.mirror());
    }

    static void scroll(InputEvent.MouseScrollingEvent event) {
        if (!active) return;
        int delta = event.getScrollDeltaY() > 0 ? 1 : event.getScrollDeltaY() < 0 ? -1 : 0;
        if (delta == 0) return;
        if (Minecraft.getInstance().hasControlDown()) {
            Tool[] tools = Tool.values();
            tool = tools[Math.floorMod(tool.ordinal() - delta, tools.length)];
        } else if (Minecraft.getInstance().hasAltDown()) {
            switch (tool) {
                case MOVE -> nudge(delta);
                case ROTATE -> BuildingRodClient.rotate(delta);
                case FLIP -> mirror(delta);
                case LAYER_DOWN, LAYER_UP -> BuildingRodClient.stepLayer(delta);
                case CONFIRM, CANCEL -> {
                }
                default -> {
                }
            }
        } else {
            return;
        }
        event.setCanceled(true);
    }

    private static void nudge(int delta) {
        if (!BuildingRodClient.locked) {
            yOffset += delta;
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        Vec3 look = player.getLookAngle();
        BuildingRodClient.move(Direction.getApproximateNearest(look), delta);
    }

    private static void mirror(int delta) {
        Mirror[] mirrors = Mirror.values();
        BlueprintPlacement placement = BuildingRodClient.placement;
        BuildingRodClient.transform(placement.rotation(), mirrors[Math.floorMod(placement.mirror().ordinal() + delta, mirrors.length)]);
    }
}
