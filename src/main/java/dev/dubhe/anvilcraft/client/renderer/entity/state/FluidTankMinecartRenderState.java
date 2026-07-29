package dev.dubhe.anvilcraft.client.renderer.entity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.entity.state.MinecartRenderState;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

/// 储罐矿车渲染所需的罐内流体内容
@Getter
@Setter
public class FluidTankMinecartRenderState extends MinecartRenderState {
    private @Nullable FluidResource resource;
    private float fill;
}
