package dev.dubhe.anvilcraft.init.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ClientModFluidModels {
    @SubscribeEvent
    public static void registerFluidModel(RegisterFluidModelsEvent event) {
        FluidTintSource tint = _ -> 0xFFFFFFFF;
        event.register(new FluidModel.Unbaked(
            new Material(AnvilCraft.of("block/exp_fluid")),
            new Material(AnvilCraft.of("block/exp_fluid_flow")),
            null, tint
        ), ModFluids.EXP_FLUID, ModFluids.FLOWING_EXP_FLUID);
        event.register(new FluidModel.Unbaked(
            new Material(AnvilCraft.of("block/oil")),
            new Material(AnvilCraft.of("block/oil_flow")),
            null, tint
        ), ModFluids.OIL, ModFluids.FLOWING_OIL);
        for (Color color : Color.values()) {
            event.register(new FluidModel.Unbaked(
                new Material(AnvilCraft.of("block/%s_cement".formatted(color))),
                new Material(AnvilCraft.of("block/%s_cement".formatted(color))),
                null, tint
            ), ModFluids.SOURCE_CEMENTS.get(color), ModFluids.FLOWING_CEMENTS.get(color));
        }
        event.register(new FluidModel.Unbaked(
            new Material(AnvilCraft.of("block/melt_gem")),
            new Material(AnvilCraft.of("block/melt_gem_flow")),
            null, tint
        ), ModFluids.MELT_GEM, ModFluids.FLOWING_MELT_GEM);
        event.register(new FluidModel.Unbaked(
            new Material(Identifier.withDefaultNamespace("block/powder_snow")),
            new Material(Identifier.withDefaultNamespace("block/powder_snow")),
            null, tint
        ), ModFluids.POWDER_SNOW);
        event.register(new FluidModel.Unbaked(
            new Material(Identifier.fromNamespaceAndPath("neoforge", "block/milk_still")),
            new Material(Identifier.fromNamespaceAndPath("neoforge", "block/milk_flowing")),
            null, tint
        ), ModFluids.MILK);
        event.register(new FluidModel.Unbaked(
            new Material(Identifier.withDefaultNamespace("block/honey_block_top")),
            new Material(Identifier.withDefaultNamespace("block/honey_block_top")),
            null, tint
        ), ModFluids.HONEY);
    }
}
