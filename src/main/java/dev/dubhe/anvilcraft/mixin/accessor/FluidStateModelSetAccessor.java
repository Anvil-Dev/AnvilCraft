package dev.dubhe.anvilcraft.mixin.accessor;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(FluidStateModelSet.class)
public interface FluidStateModelSetAccessor {
    @Accessor
    Map<Fluid, FluidModel> getModelByFluid();

    @Accessor
    FluidModel getMissingModel();
}
