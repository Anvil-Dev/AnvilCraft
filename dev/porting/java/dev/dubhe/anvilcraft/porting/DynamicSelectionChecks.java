package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.block.INegativeShapeBlock;
import dev.dubhe.anvilcraft.block.entity.HeliostatsBlockEntity;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.block.utility.redstone.AdvancedComparatorBlock;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionBlacklist;
import dev.dubhe.anvilcraft.client.selection.ModelSelectionRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DynamicSelectionChecks {
    private static int checks;

    private DynamicSelectionChecks() {
    }

    public static void run() {
        for (BlockState state : ModBlocks.SMART_BLOCK_PLACER.get().getStateDefinition().getPossibleStates()) {
            check(ModelBlockSelection.rendererParts(create(state), 0).size() == 4, "放置器底盘、大臂、小臂和钳子应全部可选择");
        }
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof INegativeShapeBlock<?>) || ModelSelectionBlacklist.usesOriginalOutline(block)) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                if (state.getRenderShape() != RenderShape.MODEL) continue;
                check(!ModelBlockSelection.multipartOutline(state).isEmpty(), "负形模型不得因渲染包装丢失选择几何: " + state);
            }
        }
        for (Block block : List.of(ModBlocks.CHARGE_COLLECTOR.get(), ModBlocks.FE_COLLECTOR.get(), ModBlocks.HEAT_COLLECTOR.get(),
            ModBlocks.VOID_ENERGY_COLLECTOR.get(), ModBlocks.INFINITE_COLLECTOR.get(), ModBlocks.CREATIVE_GENERATOR.get())) {
            BlockEntity be = create(block.defaultBlockState());
            check(ModelBlockSelection.rendererParts(be, 0.5F).size() == 1, "发电设备动态头部缺失: " + block);
            check(poses(be, false).size() == 1, "发电设备未提供模型姿态: " + block);
        }
        for (int signal = 0; signal <= 15; signal++) {
            BlockState state = ModBlocks.ADVANCED_COMPARATOR.getDefaultState().setValue(AdvancedComparatorBlock.POWER, signal);
            List<Matrix4f> poses = poses(create(state), false);
            check(poses.size() == 1, "高级比较器指针缺失");
            near(poses.getFirst().m31(), signal / 48.0, "高级比较器指针高度");
        }
        for (BlockState state : ModBlocks.PUMP.get().getStateDefinition().getPossibleStates()) {
            BlockEntity be = create(state);
            check(poses(be, false).isEmpty(), "无电网的泵不能生成工作活塞姿态");
            List<Matrix4f> preview = poses(be, true);
            check(preview.size() == 2, "泵预览必须包含两组活塞");
            Matrix4f relative = new Matrix4f(preview.getFirst()).invert().mul(preview.getLast());
            near(relative.m30(), 0, "泵预览活塞相对 X 位移");
            near(relative.m31(), 1.5 / 16, "泵预览活塞相对 Y 位移");
            near(relative.m32(), 0, "泵预览活塞相对 Z 位移");
            check(ModelBlockSelection.previewBerParts(state, BlockPos.ZERO).size() == 2, "泵预览几何缺失");
        }
        for (BlockState state : ModBlocks.CONTROL_VALVE.get().getStateDefinition().getPossibleStates()) {
            ControlValveBlockEntity be = (ControlValveBlockEntity) create(state);
            for (int rate : new int[]{0, 500, ControlValveBlockEntity.MAX_RATE}) {
                be.setMaxRate(rate);
                List<Matrix4f> poses = poses(be, false);
                check(poses.size() == 1, "控制阀手轮缺失");
                Vector3f normal = poses.getFirst().transformDirection(new Vector3f(0, 1, 0));
                near(normal.x, be.getFacing().getStepX(), "控制阀手轮 X 法向");
                near(normal.y, be.getFacing().getStepY(), "控制阀手轮 Y 法向");
                near(normal.z, be.getFacing().getStepZ(), "控制阀手轮 Z 法向");
                check(ModelBlockSelection.rendererParts(be, 0).size() == 1, "控制阀手轮选择几何缺失");
            }
        }
        HeliostatsBlockEntity heliostats = (HeliostatsBlockEntity) create(ModBlocks.HELIOSTATS.getDefaultState());
        for (Vector3f normal : List.of(new Vector3f(1, 1, 1), new Vector3f(-1, 1, 1), new Vector3f(1, 2, -1))) {
            heliostats.setNormalVector3f(normal);
            check(ModelBlockSelection.rendererParts(heliostats, 0).size() == 1, "定日镜转动模型缺失");
        }
        for (BlockState state : ModBlocks.PULSE_GENERATOR.get().getStateDefinition().getPossibleStates()) {
            List<SelectionPart> parts = ModelBlockSelection.rendererParts(create(state), 0);
            check(parts.size() == 1, "脉冲发生器的预览指针几何缺失");
        }
        AnvilCraft.LOGGER.info("PORT_DYNAMIC_SELECTION_PASSED: {} checks", checks);
    }

    private static BlockEntity create(BlockState state) {
        return Objects.requireNonNull(((EntityBlock) state.getBlock()).newBlockEntity(BlockPos.ZERO, state));
    }

    @SuppressWarnings("unchecked")
    private static List<Matrix4f> poses(BlockEntity be, boolean preview) {
        var renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(be);
        check(renderer instanceof ModelSelectionRenderer<?>, "缺少动态选择渲染器: " + be.getType());
        ModelSelectionRenderer<BlockEntity> selection = (ModelSelectionRenderer<BlockEntity>) renderer;
        List<Matrix4f> result = new ArrayList<>();
        ModelSelectionRenderer.ModelConsumer consumer = (model, pose) -> result.add(new Matrix4f(pose.last().pose()));
        if (preview) selection.collectPreviewModels(be, 0, new PoseStack(), consumer);
        else selection.collectSelectionModels(be, 0, new PoseStack(), consumer);
        for (Matrix4f pose : result) check(pose.isFinite() && pose.isAffine(), "动态姿态矩阵必须为有限仿射矩阵");
        return result;
    }

    private static void near(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1.0E-5, message + ": " + actual + " != " + expected);
    }

    private static void check(boolean passed, String message) {
        if (!passed) throw new IllegalStateException(message);
        checks++;
    }
}
