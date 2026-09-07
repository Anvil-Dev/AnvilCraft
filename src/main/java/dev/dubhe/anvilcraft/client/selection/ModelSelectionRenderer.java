package dev.dubhe.anvilcraft.client.selection;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/** 供渲染和拾取共用当前帧的模型姿态；收集过程中不执行绘制、音效或粒子逻辑。 */
public interface ModelSelectionRenderer<T extends BlockEntity> {
    default void collectSelectionModels(T blockEntity, float partialTick, PoseStack pose, ModelConsumer consumer) {
    }

    default void collectSelectionParts(T blockEntity, float partialTick, List<SelectionPart> output) {
    }

    @FunctionalInterface
    interface ModelConsumer {
        void accept(ModelResourceLocation model, PoseStack pose);
    }
}
