package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.dubhe.anvilcraft.util.MutableValue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

@Getter
@Setter
public class AutoEnchantingTableBlockEntityRenderState extends BlockEntityRenderState {
    public float time;
    public float rotY;
    public final MutableValue<Integer> amount = new MutableValue<>(0);
    public Fluid fluid;
    public FluidStack fluidStack;
    public FluidResource fluidResource;
}
