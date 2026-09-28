package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.jspecify.annotations.Nullable;

public class RuinsRenderState extends LitBlockRenderState {
    public @Nullable DisplayRenderer display;

    @FunctionalInterface
    public interface DisplayRenderer {
        void submit(PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera);
    }
}
