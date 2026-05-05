package dev.dubhe.anvilcraft.init.block;

import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.multiblock.init.LibRegistries;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.state.DirectionCube232PartHalf;
import net.minecraft.core.Direction;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class ModMultiblockDefinitions {
    public static final ResourceKey<MultiblockDefinition> CELESTIAL_FORGING_ANVIL = key(AnvilCraft.of("cfa"));
    public static final ResourceKey<MultiblockDefinition> FLUID_TANK = key(AnvilCraft.of("fluid_tank"));
    public static final ResourceKey<MultiblockDefinition> LARGE_FLUID_TANK = key(AnvilCraft.of("large_fluid_tank"));

    public static void bootstrap(BootstrapContext<MultiblockDefinition> ctx) {
        ctx.register(
            ModMultiblockDefinitions.CELESTIAL_FORGING_ANVIL,
            MultiblockDefinition.seriaBuilder()
                .layer(
                    "A   B",
                    "     ",
                    "  0  ",
                    "     ",
                    "C   D"
                )
                .mapController(ModBlocks.CELESTIAL_FORGING_ANVIL.get())
                .map(
                    'A',
                    BlockStatePredicate.builder()
                        .of(ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER)
                        .with(CelestialForgingAnvilAmplifierBlock.FACING, Direction.NORTH)
                        .with(CelestialForgingAnvilAmplifierBlock.HALF, DirectionCube232PartHalf.BOTTOM_PART)
                )
                .map(
                    'B',
                    BlockStatePredicate.builder()
                        .of(ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER)
                        .with(CelestialForgingAnvilAmplifierBlock.FACING, Direction.EAST)
                        .with(CelestialForgingAnvilAmplifierBlock.HALF, DirectionCube232PartHalf.BOTTOM_W)
                )
                .map(
                    'C',
                    BlockStatePredicate.builder()
                        .of(ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER)
                        .with(CelestialForgingAnvilAmplifierBlock.FACING, Direction.WEST)
                        .with(CelestialForgingAnvilAmplifierBlock.HALF, DirectionCube232PartHalf.BOTTOM_S)
                )
                .map(
                    'D',
                    BlockStatePredicate.builder()
                        .of(ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER)
                        .with(CelestialForgingAnvilAmplifierBlock.FACING, Direction.SOUTH)
                        .with(CelestialForgingAnvilAmplifierBlock.HALF, DirectionCube232PartHalf.BOTTOM_WS)
                )
                .build()
        );
        ctx.register(
            ModMultiblockDefinitions.FLUID_TANK,
            MultiblockDefinition.seriaBuilder()
                .layer("AAA", "A A", "AAA")
                .layer("A A", " 0 ", "A A")
                .layer("AAA", "A A", "AAA")
                .mapController(ModBlocks.FLUID_TANK.get())
                .map('A', ModBlocks.MENGER_SPONGE.get())
                .build()
        );
        ctx.register(
            ModMultiblockDefinitions.LARGE_FLUID_TANK,
            MultiblockDefinition.seriaBuilder()
                .layer("AAAAAAAAA", "A AA AA A", "AAAAAAAAA", "AAA   AAA", "A A   A A", "AAA   AAA", "AAAAAAAAA", "A AA AA A", "AAAAAAAAA")
                .layer("A AA AA A", "         ", "A AA AA A", "A A   A A", "         ", "A A   A A", "A AA AA A", "         ", "A AA AA A")
                .layer("AAAAAAAAA", "A AA AA A", "AAAAAAAAA", "AAA   AAA", "A A   A A", "AAA   AAA", "AAAAAAAAA", "A AA AA A", "AAAAAAAAA")
                .layer("AAA   AAA", "A A   A A", "AAA   AAA", "         ", "         ", "         ", "AAA   AAA", "A A   A A", "AAA   AAA")
                .layer("A A   A A", "         ", "A A   A A", "         ", "    0    ", "         ", "A A   A A", "         ", "A A   A A")
                .layer("AAA   AAA", "A A   A A", "AAA   AAA", "         ", "         ", "         ", "AAA   AAA", "A A   A A", "AAA   AAA")
                .layer("AAAAAAAAA", "A AA AA A", "AAAAAAAAA", "AAA   AAA", "A A   A A", "AAA   AAA", "AAAAAAAAA", "A AA AA A", "AAAAAAAAA")
                .layer("A AA AA A", "         ", "A AA AA A", "A A   A A", "         ", "A A   A A", "A AA AA A", "         ", "A AA AA A")
                .layer("AAAAAAAAA", "A AA AA A", "AAAAAAAAA", "AAA   AAA", "A A   A A", "AAA   AAA", "AAAAAAAAA", "A AA AA A", "AAAAAAAAA")
                .mapController(ModBlocks.LARGE_FLUID_TANK.get())
                .map('A', ModBlocks.MENGER_SPONGE.get())
                .build()
        );
    }

    private static ResourceKey<MultiblockDefinition> key(ResourceLocation id) {
        return ResourceKey.create(LibRegistries.DEFINITIONS_KEY, id);
    }
}
