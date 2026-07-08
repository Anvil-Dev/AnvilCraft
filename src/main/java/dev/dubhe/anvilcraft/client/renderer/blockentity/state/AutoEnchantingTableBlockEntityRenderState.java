package dev.dubhe.anvilcraft.client.renderer.blockentity.state;

import dev.dubhe.anvilcraft.util.MutableValue;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

@Getter
@Setter
public class AutoEnchantingTableBlockEntityRenderState extends BlockEntityRenderState {
    public float time;
    public float rotY;
    public float flip;
    public float open;
    public final MutableValue<Integer> amount = new MutableValue<>(0);
    public Fluid fluid;
    public FluidStack fluidStack;
    public FluidResource fluidResource;
    public ItemStack displayInputItem;
    public ItemStack displayOutputItem;
    public ItemClusterRenderState inputItemState;
    public ItemClusterRenderState outputItemState;
}
