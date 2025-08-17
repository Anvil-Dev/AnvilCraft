package dev.dubhe.anvilcraft.init.worldgen;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.OverworldBiomeBuilder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseRouterData;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class ModNoiseGeneratorSettings {
    private static final ResourceKey<DensityFunction> SHIFT_X = createDensityKey("shift_x");
    private static final ResourceKey<DensityFunction> SHIFT_Z = createDensityKey("shift_z");
    private static final ResourceKey<DensityFunction> SLOPED_CHEESE = createDensityKey("overworld/sloped_cheese");
    private static final ResourceKey<DensityFunction> ENTRANCES = createDensityKey("overworld/caves/entrances");
    private static final ResourceKey<DensityFunction> NOODLE = createDensityKey("overworld/caves/noodle");
    private static final ResourceKey<DensityFunction> PILLARS = createDensityKey("overworld/caves/pillars");
    private static final ResourceKey<DensityFunction> SPAGHETTI_ROUGHNESS_FUNCTION = createDensityKey(
        "overworld/caves/spaghetti_roughness_function");
    private static final ResourceKey<DensityFunction> SPAGHETTI_2D = createDensityKey("overworld/caves/spaghetti_2d");

    private static ResourceKey<DensityFunction> createDensityKey(String location) {
        return ResourceKey.create(Registries.DENSITY_FUNCTION, ResourceLocation.withDefaultNamespace(location));
    }

    public static final ResourceKey<NoiseGeneratorSettings> MUN = key("mun");

    private static BootstrapContext<NoiseGeneratorSettings> context;

    public static void bootstrap(BootstrapContext<NoiseGeneratorSettings> context) {
        ModNoiseGeneratorSettings.context = context;
        context.register(MUN, mun());
        ModNoiseGeneratorSettings.context = null;
    }

    public static NoiseGeneratorSettings mun() {
        DensityFunction shiftX = getFunction(SHIFT_X);
        DensityFunction shiftZ = getFunction(SHIFT_Z);
        DensityFunction depth = getFunction(NoiseRouterData.DEPTH);
        DensityFunction noiseGradientDensity = DensityFunctions.mul(
            DensityFunctions.constant(4.0),
            DensityFunctions.mul(depth, DensityFunctions.cache2d(getFunction(NoiseRouterData.FACTOR))).quarterNegative()
        );
        DensityFunction slopedCheese = getFunction(SLOPED_CHEESE);
        NoiseRouter noiseRouter = new NoiseRouter(
            DensityFunctions.noise(getNoise(Noises.AQUIFER_BARRIER), 0.5),
            DensityFunctions.noise(getNoise(Noises.AQUIFER_FLUID_LEVEL_FLOODEDNESS), 0.67),
            DensityFunctions.noise(getNoise(Noises.AQUIFER_FLUID_LEVEL_SPREAD), 0.7142857142857143),
            DensityFunctions.noise(getNoise(Noises.AQUIFER_LAVA)),
            DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, getNoise(Noises.TEMPERATURE)),
            DensityFunctions.shiftedNoise2d(shiftX, shiftZ, 0.25, getNoise(Noises.VEGETATION)),
            getFunction(NoiseRouterData.CONTINENTS),
            getFunction(NoiseRouterData.EROSION),
            depth,
            getFunction(NoiseRouterData.RIDGES),
            DensityFunctions.lerp(
                DensityFunctions.yClampedGradient(-64, -40, 0.0, 1.0),
                0.1171875,
                DensityFunctions.lerp(
                    DensityFunctions.yClampedGradient(240, 256, 1.0, 0.0),
                    -0.078125,
                    DensityFunctions.add(noiseGradientDensity, DensityFunctions.constant(-0.703125)).clamp(-64.0, 64.0)
                )
            ),
            DensityFunctions.min(
                DensityFunctions.mul(
                    DensityFunctions.interpolated(DensityFunctions.blendDensity(DensityFunctions.lerp(
                        DensityFunctions.yClampedGradient(-64, -40, 0.0, 1.0),
                        0.1171875,
                        DensityFunctions.lerp(
                            DensityFunctions.yClampedGradient(240, 256, 1.0, 0.0),
                            -0.078125,
                            DensityFunctions.rangeChoice(
                                slopedCheese,
                                -1000000.0,
                                1.5625,
                                DensityFunctions.min(
                                    slopedCheese,
                                    DensityFunctions.mul(DensityFunctions.constant(5.0), getFunction(ENTRANCES))
                                ),
                                underground(slopedCheese)
                            )
                        )
                    ))),
                    DensityFunctions.constant(0.64)
                ).squeeze(),
                getFunction(NOODLE)
            ),
            DensityFunctions.constant(-1),
            DensityFunctions.constant(-1),
            DensityFunctions.zero()
        );
        return new NoiseGeneratorSettings(
            NoiseSettings.create(-64, 384, 1, 2),
            ModBlocks.MUN_ROCK.getDefaultState(),
            Blocks.AIR.defaultBlockState(),
            noiseRouter,
            munRule(),
            new OverworldBiomeBuilder().spawnTarget(),
            0,
            false,
            true,
            true,
            false
        );
    }

    private static DensityFunction underground(DensityFunction input) {
        DensityFunction pillars = getFunction(PILLARS);
        return DensityFunctions.max(
            DensityFunctions.min(
                DensityFunctions.min(
                    DensityFunctions.add(
                        DensityFunctions.mul(
                            DensityFunctions.constant(4.0), DensityFunctions.noise(getNoise(Noises.CAVE_LAYER), 8.0).square()
                        ),
                        DensityFunctions.add(
                            DensityFunctions.add(
                                DensityFunctions.constant(0.27),
                                DensityFunctions.noise(getNoise(Noises.CAVE_CHEESE), 0.6666666666666666)
                            ).clamp(-1.0, 1.0),
                            DensityFunctions.add(
                                DensityFunctions.constant(1.5), DensityFunctions.mul(DensityFunctions.constant(-0.64), input)
                            ).clamp(0.0, 0.5)
                        )
                    ), getFunction(ENTRANCES)
                ), DensityFunctions.add(
                    getFunction(SPAGHETTI_2D), getFunction(SPAGHETTI_ROUGHNESS_FUNCTION))
            ), DensityFunctions.rangeChoice(
                pillars, -1000000.0, 0.03, DensityFunctions.constant(-1000000.0), pillars
            )
        );
    }

    public static SurfaceRules.RuleSource munRule() {
        return SurfaceRules.sequence(
            SurfaceRules.ifTrue(
                SurfaceRules.verticalGradient("bedrock_floor", VerticalAnchor.bottom(), VerticalAnchor.aboveBottom(5)),
                SurfaceRules.state(Blocks.BEDROCK.defaultBlockState())
            ),
            SurfaceRules.ifTrue(
                SurfaceRules.abovePreliminarySurface(),
                SurfaceRules.ifTrue(
                    SurfaceRules.stoneDepthCheck(-1, true, 0, CaveSurface.FLOOR),
                    SurfaceRules.state(ModBlocks.MUN_SOIL.getDefaultState())
                )
            )
        );
    }

    private static Holder<NormalNoise.NoiseParameters> getNoise(ResourceKey<NormalNoise.NoiseParameters> key) {
        return ModNoiseGeneratorSettings.context.lookup(Registries.NOISE).getOrThrow(key);
    }

    private static DensityFunction getFunction(ResourceKey<DensityFunction> key) {
        return new DensityFunctions.HolderHolder(ModNoiseGeneratorSettings.context.lookup(Registries.DENSITY_FUNCTION).getOrThrow(key));
    }

    private static ResourceKey<NoiseGeneratorSettings> key(String id) {
        return ResourceKey.create(Registries.NOISE_SETTINGS, AnvilCraft.of(id));
    }
}
