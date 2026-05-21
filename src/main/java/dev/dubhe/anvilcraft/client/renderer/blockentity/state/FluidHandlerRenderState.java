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
    private float fill;
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
