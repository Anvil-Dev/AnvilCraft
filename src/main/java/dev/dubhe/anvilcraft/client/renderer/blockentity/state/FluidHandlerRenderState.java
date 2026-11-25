package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

@Getter
@Setter
public class FluidHandlerRenderState extends BlockEntityRenderState {
    private @Nullable FluidResource resource;
    /// 端口上被标记的流体；与 resource 独立，罐里有流体时也要画标记图标
    private @Nullable FluidResource markedResource;
    private float fill;
    private int amount;
    private float minX;
    private float minY;
    private float minZ;
    private float maxX;
    private float maxY;
    private float maxZ;

    public void setTankW(float tankW) {
        this.setTankW(0, 0, 0, 1, 1, 1, tankW);
    }

    public void setTankW(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, float tankW) {
        this.minX = minX + tankW;
        this.minY = minY + tankW;
        this.minZ = minZ + tankW;
        this.maxX = maxX - tankW;
        this.maxY = maxY - tankW;
        this.maxZ = maxZ - tankW;
    }
}
