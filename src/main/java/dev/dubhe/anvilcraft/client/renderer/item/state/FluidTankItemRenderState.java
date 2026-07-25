package dev.dubhe.anvilcraft.client.renderer.item.state;

import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.List;

/// 物品形态储罐渲染所需的流体内容
@Getter
@Setter
public class FluidTankItemRenderState {
    @SuppressWarnings("deprecation")
    public static final RenderType FLUID_RENDER_TYPE = RenderType.create(
        AnvilCraft.of("fluid_tank_item").toString(),
        RenderSetup.builder(RenderPipelines.TRANSLUCENT_BLOCK)
            .withTexture("Sampler0", TextureAtlas.LOCATION_BLOCKS)
            .useLightmap()
            .sortOnUpload()
            .createRenderSetup()
    );

    private @Nullable FluidResource resource;
    private float fill;
    private List<Layer> layers = List.of();

    /// 大型储罐里按高度分层展示的单层流体
    public record Layer(FluidResource resource, float bottom, float top) {
    }
}
