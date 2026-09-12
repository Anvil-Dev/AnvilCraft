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

    /**
     * 放置预览用：默认与 {@link #collectSelectionModels} 一致。
     *
     * <p>渲染依赖服务端同步状态的方块（如泵的 {@code working} 与电网）在预览时拿不到这些状态
     * —— 落点上没有实体，临时实体也从未被同步 —— 于是不会产出任何模型。这类渲染器覆写本方法，
     * 按放置状态给出静止姿态即可。</p>
     */
    default void collectPreviewModels(T blockEntity, float partialTick, PoseStack pose, ModelConsumer consumer) {
        this.collectSelectionModels(blockEntity, partialTick, pose, consumer);
    }

    default void collectSelectionParts(T blockEntity, float partialTick, List<SelectionPart> output) {
    }

    @FunctionalInterface
    interface ModelConsumer {
        void accept(ModelResourceLocation model, PoseStack pose);
    }
}
