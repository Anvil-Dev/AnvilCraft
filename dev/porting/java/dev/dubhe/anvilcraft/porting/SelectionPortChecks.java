package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.BigRedButtonBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RedstoneDiceBlockEntity;
import dev.dubhe.anvilcraft.block.utility.redstone.BigRedButtonBlock;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.client.selection.ModelCubeGeometry;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionBlacklist;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.CuboidRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

public final class SelectionPortChecks {
    private static int checks;

    private SelectionPortChecks() {
    }

    public static void run() {
        checkGeometry();
        check(ModelSelectionBlacklist.usesOriginalOutline(ModBlocks.LARGE_CAULDRON.get()), "坩埚必须保留原有描边");
        for (Direction facing : Direction.values()) {
            BlockState state = ModBlocks.BIG_RED_BUTTON.getDefaultState().setValue(BigRedButtonBlock.FACING, facing);
            check(!ModelBlockSelection.multipartOutline(state).isEmpty(), "按钮底座选择几何缺失");
            List<SelectionPart> released = ModelBlockSelection.rendererParts(new BigRedButtonBlockEntity(BlockPos.ZERO, state), 1);
            List<SelectionPart> pressed = ModelBlockSelection.rendererParts(
                new BigRedButtonBlockEntity(BlockPos.ZERO, state.setValue(BigRedButtonBlock.PRESSED, true)), 1
            );
            check(released.size() == 1 && pressed.size() == 1, "按钮盖动态模型缺失");
            Vec3 delta = pressed.getFirst().bounds().getCenter().subtract(released.getFirst().bounds().getCenter());
            near(delta.x, -facing.getStepX() * 0.125, "按钮盖 X 行程");
            near(delta.y, -facing.getStepY() * 0.125, "按钮盖 Y 行程");
            near(delta.z, -facing.getStepZ() * 0.125, "按钮盖 Z 行程");
        }
        RedstoneDiceBlockEntity dice = new RedstoneDiceBlockEntity(BlockPos.ZERO, ModBlocks.REDSTONE_DICE.getDefaultState());
        for (int a = 1; a <= 6; a++) {
            for (int b = 1; b <= 6; b++) {
                for (int c = 1; c <= 6; c++) {
                    CompoundTag tag = new CompoundTag();
                    tag.putInt("Faces", a * 100 + b * 10 + c);
                    tag.putInt("PreviousFaces", a * 100 + b * 10 + c);
                    dice.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag));
                    List<SelectionPart> parts = ModelBlockSelection.rendererParts(dice, 1);
                    check(parts.size() == 3, "三个骰子的动态选择几何必须独立存在");
                    for (SelectionPart part : parts) {
                        AABB bounds = part.bounds();
                        check(bounds.minX >= 3.0 / 16 - 1.0E-5 && bounds.maxX <= 13.0 / 16 + 1.0E-5, "骰子超出 X 边界");
                        check(bounds.minZ >= 3.0 / 16 - 1.0E-5 && bounds.maxZ <= 13.0 / 16 + 1.0E-5, "骰子超出 Z 边界");
                        near(bounds.minY, 4.0 / 16, "骰子静止时必须落在底座上");
                    }
                }
            }
        }
        check(!ModelBlockSelection.multipartOutline(ModBlocks.GIANT_ANVIL.getDefaultState()).isEmpty(), "巨型铁砧必须有整机描边");
        AnvilCraft.LOGGER.info("PORT_SELECTION_PASSED: {} geometry and dynamic-pose checks", checks);
    }

    private static void checkGeometry() {
        CuboidModelElement solid = new CuboidModelElement(new Vector3f(0, 0, 0), new Vector3f(16, 16, 16), Map.of());
        CuboidModelElement plane = new CuboidModelElement(new Vector3f(0, 0, 0), new Vector3f(16, 0, 16), Map.of());
        check(ModelCubeGeometry.decode(List.of(solid, plane)).size() == 1, "混合模型的特效平面不得产生选区");
        List<ConvexShape> thin = ModelCubeGeometry.decode(List.of(plane));
        check(thin.size() == 1 && thin.getFirst().bounds().getYsize() > 0, "纯平面部件应保留可拾取厚度");
        CuboidRotation rotation = new CuboidRotation(new Vector3f(0.5F, 0.5F, 0.5F),
            new CuboidRotation.SingleAxisRotation(Direction.Axis.Y, 45), false);
        CuboidModelElement rotated = new CuboidModelElement(new Vector3f(4, 0, 4), new Vector3f(12, 16, 12), Map.of(), rotation, true, 0);
        ConvexShape shape = ModelCubeGeometry.decode(List.of(rotated)).getFirst();
        double scale = ModelCubeGeometry.SCALE;
        ConvexShape left = ModelCubeGeometry.clip(shape, new AABB(0, 0, 0, scale / 2, scale, scale));
        check(left != null, "旋转凸体裁剪不能丢失实体");
        near(left.bounds().maxX, scale / 2, "裁剪必须停在格子边界");
        check(ModelCubeGeometry.clip(shape, new AABB(scale, scale, scale, scale * 2, scale * 2, scale * 2)) == null,
            "相离的格子必须返回空几何");
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0E-5, message + ": " + actual + " != " + expected);
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new IllegalStateException(message);
        checks++;
    }
}
