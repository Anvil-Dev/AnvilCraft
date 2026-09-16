package dev.dubhe.anvilcraft.client.selection;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;

import java.util.List;

/** 渲染、拾取与预览共用姿态；收集过程不提交绘制，也不产生音效或粒子。 */
public interface ModelSelectionRenderer<T extends BlockEntity> {
    default void collectSelectionModels(T entity, float partialTick, PoseStack pose, ModelConsumer consumer) {
    }

    /** 需要服务端工作状态的渲染器应在此提供放置时的静止姿态。 */
    default void collectPreviewModels(T entity, float partialTick, PoseStack pose, ModelConsumer consumer) {
        this.collectSelectionModels(entity, partialTick, pose, consumer);
    }

    default void collectSelectionParts(T entity, float partialTick, List<SelectionPart> output) {
    }

    @FunctionalInterface
    interface ModelConsumer {
        void accept(SelectionModel model, PoseStack pose);

        default void accept(StandaloneModelKey<?> model, PoseStack pose) {
            this.accept(SelectionModel.standalone(model), pose);
        }

        default void accept(BlockState state, PoseStack pose) {
            this.accept(new SelectionModel.State(state), pose);
        }
    }
}
