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
        var enchantmentColors = new dev.dubhe.anvilcraft.util.LiquidEnchantmentClientFluidTypeExtension();
        FluidTintSource enchantmentTint = new FluidTintSource() {
            @Override
            public int color(net.minecraft.world.level.material.FluidState state) {
                return 0xFFFFFFFF;
            }

            @Override
            public int colorAsStack(net.neoforged.neoforge.fluids.FluidStack stack) {
                return enchantmentColors.getTintColor(stack);
            }
        };
        event.register(new FluidModel.Unbaked(
            new Material(AnvilCraft.of("block/liquid_enchantment")),
            new Material(AnvilCraft.of("block/liquid_enchantment")),
            null, enchantmentTint
        ), ModFluids.LIQUID_ENCHANTMENT);
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
            new Material(Identifier.withDefaultNamespace("block/honey_block_top")),
            new Material(Identifier.withDefaultNamespace("block/honey_block_top")),
            null, tint
        ), ModFluids.HONEY);
        event.register(new FluidModel.Unbaked(
            new Material(Identifier.withDefaultNamespace("block/water_still")),
            new Material(Identifier.withDefaultNamespace("block/water_flow")),
            null, _ -> 0xFFC9E4F7
        ), ModFluids.HYDROGEN);
        event.register(new FluidModel.Unbaked(
            new Material(Identifier.withDefaultNamespace("block/water_still")),
            new Material(Identifier.withDefaultNamespace("block/water_flow")),
            null, _ -> 0xFF9CCCF8
        ), ModFluids.OXYGEN);
        event.register(new FluidModel.Unbaked(
            new Material(Identifier.withDefaultNamespace("block/water_still")),
            new Material(Identifier.withDefaultNamespace("block/water_flow")),
            null, _ -> 0xFFF0C8E0
        ), ModFluids.HELIUM);
        event.register(new FluidModel.Unbaked(
            new Material(Identifier.withDefaultNamespace("block/water_still")),
            new Material(Identifier.withDefaultNamespace("block/water_flow")),
            null, _ -> 0xFFA8E8DC
        ), ModFluids.DEUTERIUM);
        event.register(new FluidModel.Unbaked(
            new Material(Identifier.withDefaultNamespace("block/water_still")),
            new Material(Identifier.withDefaultNamespace("block/water_flow")),
            null, _ -> 0xFFC9C2F0
        ), ModFluids.XENON);
        event.register(new FluidModel.Unbaked(
            new Material(Identifier.withDefaultNamespace("block/water_still")),
            new Material(Identifier.withDefaultNamespace("block/water_flow")),
            null, _ -> 0xFFB0E8A8
        ), ModFluids.KRYPTON);
        event.register(new FluidModel.Unbaked(
            new Material(Identifier.withDefaultNamespace("block/water_still")),
            new Material(Identifier.withDefaultNamespace("block/water_flow")),
            null, _ -> 0xFFE6CFFF
        ), ModFluids.PRIMORDIAL_MATTER);
    }
}
