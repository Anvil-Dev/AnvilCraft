package dev.dubhe.anvilcraft.init.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.MeltGemFluid;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.fluid.GasFluid;
import dev.dubhe.anvilcraft.fluid.HoneyFluid;
import dev.dubhe.anvilcraft.fluid.LiquidEnchantmentFluid;
import dev.dubhe.anvilcraft.fluid.PowderSnowFluid;
import dev.dubhe.anvilcraft.fluid.PrimordialMatterFluid;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.ColorUtil;
import dev.dubhe.anvilcraft.util.LiquidEnchantmentClientFluidTypeExtension;
import dev.dubhe.anvilcraft.util.ModClientFluidTypeExtensionImpl;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry.InteractionInformation;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

public class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(
        NeoForgeRegistries.FLUID_TYPES, AnvilCraft.MOD_ID
    );
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, AnvilCraft.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> EXP_FLUID_TYPE = FLUID_TYPES
        .register(
        "exp_fluid",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.anvilcraft.exp_fluid")
            .density(1000)
            .viscosity(500)
            .fallDistanceModifier(0)
            .motionScale(0.01)
            .supportsBoating(true)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
        )
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid> EXP_FLUID = FLUIDS.register(
        "exp_fluid",
        () -> new BaseFlowingFluid.Source(ModFluids.EXP_FLUID_PROPERTIES)
    );

    public static final DeferredHolder<Fluid, BaseFlowingFluid> FLOWING_EXP_FLUID = FLUIDS.register(
        "flowing_exp_fluid",
        () -> new BaseFlowingFluid.Flowing(ModFluids.EXP_FLUID_PROPERTIES)
    );

    public static final BaseFlowingFluid.Properties EXP_FLUID_PROPERTIES = new BaseFlowingFluid.Properties(
        EXP_FLUID_TYPE,
        EXP_FLUID,
        FLOWING_EXP_FLUID
    )
        .bucket(ModItems.EXP_BUCKET)
        .block(ModBlocks.EXP_FLUID)
        .tickRate(60)
        .slopeFindDistance(2)
        .levelDecreasePerBlock(3)
        .explosionResistance(100);

    public static final DeferredHolder<FluidType, FluidType> OIL_TYPE = FLUID_TYPES.register(
        "oil",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.anvilcraft.oil")
            .density(2000)
            .viscosity(4000)
            .fallDistanceModifier(0)
            .supportsBoating(true)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
        )
    );
    public static final DeferredHolder<Fluid, BaseFlowingFluid> OIL = FLUIDS
        .register(
            "oil",
            () -> new BaseFlowingFluid.Source(ModFluids.OIL_PROPERTIES)
        );
    public static final DeferredHolder<Fluid, BaseFlowingFluid> FLOWING_OIL = FLUIDS
        .register(
            "flowing_oil",
            () -> new BaseFlowingFluid.Flowing(ModFluids.OIL_PROPERTIES)
        );
    public static final BaseFlowingFluid.Properties OIL_PROPERTIES = new BaseFlowingFluid.Properties(OIL_TYPE, OIL, FLOWING_OIL)
        .bucket(ModItems.OIL_BUCKET)
        .block(ModBlocks.OIL)
        .tickRate(10)
        .slopeFindDistance(3)
        .explosionResistance(100);

    // === Gases ===

    public static final FluidType HYDROGEN_FLUID_TYPE = createHydrogenType();
    public static final DeferredHolder<FluidType, FluidType> HYDROGEN_TYPE = FLUID_TYPES.register(
        "hydrogen", () -> ModFluids.HYDROGEN_FLUID_TYPE
    );
    public static final DeferredHolder<Fluid, GasFluid> HYDROGEN = FLUIDS.register(
        "hydrogen", () -> new GasFluid(ModFluids.HYDROGEN_FLUID_TYPE)
    );

    public static final FluidType OXYGEN_FLUID_TYPE = createOxygenType();
    public static final DeferredHolder<FluidType, FluidType> OXYGEN_TYPE = FLUID_TYPES.register(
        "oxygen", () -> ModFluids.OXYGEN_FLUID_TYPE
    );
    public static final DeferredHolder<Fluid, GasFluid> OXYGEN = FLUIDS.register(
        "oxygen", () -> new GasFluid(ModFluids.OXYGEN_FLUID_TYPE)
    );

    public static final FluidType HELIUM_FLUID_TYPE = createHeliumType();
    public static final DeferredHolder<FluidType, FluidType> HELIUM_TYPE = FLUID_TYPES.register(
        "helium", () -> ModFluids.HELIUM_FLUID_TYPE
    );
    public static final DeferredHolder<Fluid, GasFluid> HELIUM = FLUIDS.register(
        "helium", () -> new GasFluid(ModFluids.HELIUM_FLUID_TYPE)
    );

    public static final FluidType DEUTERIUM_FLUID_TYPE = createDeuteriumType();
    public static final DeferredHolder<FluidType, FluidType> DEUTERIUM_TYPE = FLUID_TYPES.register(
        "deuterium", () -> ModFluids.DEUTERIUM_FLUID_TYPE
    );
    public static final DeferredHolder<Fluid, GasFluid> DEUTERIUM = FLUIDS.register(
        "deuterium", () -> new GasFluid(ModFluids.DEUTERIUM_FLUID_TYPE)
    );

    public static final FluidType XENON_FLUID_TYPE = createXenonType();
    public static final DeferredHolder<FluidType, FluidType> XENON_TYPE = FLUID_TYPES.register(
        "xenon", () -> ModFluids.XENON_FLUID_TYPE
    );
    public static final DeferredHolder<Fluid, GasFluid> XENON = FLUIDS.register(
        "xenon", () -> new GasFluid(ModFluids.XENON_FLUID_TYPE)
    );

    public static final FluidType KRYPTON_FLUID_TYPE = createKryptonType();
    public static final DeferredHolder<FluidType, FluidType> KRYPTON_TYPE = FLUID_TYPES.register(
        "krypton", () -> ModFluids.KRYPTON_FLUID_TYPE
    );
    public static final DeferredHolder<Fluid, GasFluid> KRYPTON = FLUIDS.register(
        "krypton", () -> new GasFluid(ModFluids.KRYPTON_FLUID_TYPE)
    );

    private static FluidType createHydrogenType() {
        return new FluidType(FluidType.Properties.create()
            .descriptionId("block.anvilcraft.hydrogen")
            .density(-1000)
            .viscosity(100)
            .fallDistanceModifier(0)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
        );
    }

    private static FluidType createOxygenType() {
        return new FluidType(FluidType.Properties.create()
            .descriptionId("block.anvilcraft.oxygen")
            .density(-1000)
            .viscosity(100)
            .fallDistanceModifier(0)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
        );
    }

    private static FluidType createHeliumType() {
        return new FluidType(FluidType.Properties.create()
            .descriptionId("block.anvilcraft.helium")
            .density(-1000)
            .viscosity(100)
            .fallDistanceModifier(0)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
        );
    }

    private static FluidType createDeuteriumType() {
        return new FluidType(FluidType.Properties.create()
            .descriptionId("block.anvilcraft.deuterium")
            .density(-1000)
            .viscosity(100)
            .fallDistanceModifier(0)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
        );
    }

    private static FluidType createXenonType() {
        return new FluidType(FluidType.Properties.create()
            .descriptionId("block.anvilcraft.xenon")
            .density(-1000)
            .viscosity(100)
            .fallDistanceModifier(0)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
        );
    }

    private static FluidType createKryptonType() {
        return new FluidType(FluidType.Properties.create()
            .descriptionId("block.anvilcraft.krypton")
            .density(-1000)
            .viscosity(100)
            .fallDistanceModifier(0)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
        );
    }

    public static final Object2ObjectMap<Color, DeferredHolder<FluidType, FluidType>> CEMENT_TYPES = registerAllCementTypes();
    public static final Object2ObjectMap<Color, DeferredHolder<Fluid, BaseFlowingFluid>> SOURCE_CEMENTS = registerAllSourceCement();
    public static final Object2ObjectMap<Color, DeferredHolder<Fluid, BaseFlowingFluid>> FLOWING_CEMENTS = registerAllFlowingCement();
    public static final Object2ObjectMap<Color, BaseFlowingFluid.Properties> CEMENT_PROPERTIES = createAllCementProperties();

    private static Object2ObjectMap<Color, DeferredHolder<FluidType, FluidType>> registerAllCementTypes() {
        Object2ObjectMap<Color, DeferredHolder<FluidType, FluidType>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var type = registerCementType(color);
            map.put(color, type);
        }
        return map;
    }

    private static DeferredHolder<FluidType, FluidType> registerCementType(Color color) {
        return FLUID_TYPES.register(
            "%s_cement".formatted(color), () -> new FluidType(FluidType.Properties.create()
                .descriptionId("block.anvilcraft.%s_cement".formatted(color))
                .fallDistanceModifier(0)
                .canExtinguish(true)
                .supportsBoating(true)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
            )
        );
    }

    private static Object2ObjectMap<Color, DeferredHolder<Fluid, BaseFlowingFluid>> registerAllSourceCement() {
        Object2ObjectMap<Color, DeferredHolder<Fluid, BaseFlowingFluid>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var holder = registerSourceCement(color);
            map.put(color, holder);
        }
        return map;
    }

    private static DeferredHolder<Fluid, BaseFlowingFluid> registerSourceCement(Color color) {
        return FLUIDS.register("%s_cement".formatted(color), () -> new BaseFlowingFluid.Source(ModFluids.CEMENT_PROPERTIES.get(color)));
    }

    private static Object2ObjectMap<Color, DeferredHolder<Fluid, BaseFlowingFluid>> registerAllFlowingCement() {
        Object2ObjectMap<Color, DeferredHolder<Fluid, BaseFlowingFluid>> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var holder = registerFlowingCement(color);
            map.put(color, holder);
        }
        return map;
    }

    private static DeferredHolder<Fluid, BaseFlowingFluid> registerFlowingCement(Color color) {
        return FLUIDS.register(
            "flowing_%s_cement".formatted(color),
            () -> new BaseFlowingFluid.Flowing(ModFluids.CEMENT_PROPERTIES.get(color))
        );
    }

    private static Object2ObjectMap<Color, BaseFlowingFluid.Properties> createAllCementProperties() {
        Object2ObjectMap<Color, BaseFlowingFluid.Properties> map = new Object2ObjectLinkedOpenHashMap<>();
        for (Color color : Color.values()) {
            var properties = createCementProperties(color);
            map.put(color, properties);
        }
        return map;
    }

    private static BaseFlowingFluid.Properties createCementProperties(Color color) {
        return new BaseFlowingFluid.Properties(CEMENT_TYPES.get(color), SOURCE_CEMENTS.get(color), FLOWING_CEMENTS.get(color))
            .bucket(ModItems.CEMENT_BUCKETS.get(color))
            .block(ModBlocks.CEMENTS.get(color))
            .explosionResistance(100);
    }

    public static final DeferredHolder<FluidType, FluidType> MELT_GEM_TYPE = FLUID_TYPES.register(
        "melt_gem",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.anvilcraft.melt_gem")
            .canSwim(false)
            .canDrown(false)
            .pathType(PathType.LAVA)
            .adjacentPathType(null)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_LAVA)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_LAVA)
            .lightLevel(15)
            .density(3000)
            .viscosity(6000)
            .temperature(1300)
        )
    );
    public static final DeferredHolder<Fluid, MeltGemFluid> MELT_GEM = FLUIDS.register(
        "melt_gem",
        () -> new MeltGemFluid.Source(ModFluids.MELT_GEM_PROPERTIES)
    );
    public static final DeferredHolder<Fluid, MeltGemFluid> FLOWING_MELT_GEM = FLUIDS.register(
        "flowing_melt_gem",
        () -> new MeltGemFluid.Flowing(ModFluids.MELT_GEM_PROPERTIES)
    );
    public static final BaseFlowingFluid.Properties MELT_GEM_PROPERTIES = new BaseFlowingFluid.Properties(
        MELT_GEM_TYPE,
        MELT_GEM,
        FLOWING_MELT_GEM
    )
        .block(ModBlocks.MELT_GEM)
        .bucket(ModItems.MELT_GEM_BUCKET)
        .tickRate(20)
        .explosionResistance(100);

    // === Honey（不可放置流体） ===

    public static final DeferredHolder<FluidType, FluidType> HONEY_TYPE = FLUID_TYPES.register(
        "honey", () -> HoneyFluid.TYPE
    );

    public static final DeferredHolder<Fluid, HoneyFluid> HONEY = FLUIDS.register(
        "honey", HoneyFluid::new
    );

    // === Primordial Matter（不可放置流体） ===

    public static final DeferredHolder<FluidType, FluidType> PRIMORDIAL_MATTER_TYPE = FLUID_TYPES.register(
        "primordial_matter", () -> PrimordialMatterFluid.TYPE
    );

    public static final DeferredHolder<Fluid, PrimordialMatterFluid> PRIMORDIAL_MATTER = FLUIDS.register(
        "primordial_matter", PrimordialMatterFluid::new
    );

    public static final DeferredHolder<FluidType, FluidType> LIQUID_ENCHANTMENT_TYPE = FLUID_TYPES.register(
        "liquid_enchantment", () -> LiquidEnchantmentFluid.TYPE
    );

    public static final DeferredHolder<Fluid, LiquidEnchantmentFluid> LIQUID_ENCHANTMENT = FLUIDS.register(
        "liquid_enchantment", LiquidEnchantmentFluid::new
    );

    public static final DeferredHolder<FluidType, FluidType> POWDER_SNOW_TYPE = DeferredHolder.create(
        NeoForgeRegistries.FLUID_TYPES.key(),
        ResourceLocation.withDefaultNamespace("powder_snow")
    );
    public static final DeferredHolder<Fluid, PowderSnowFluid> POWDER_SNOW = DeferredHolder.create(
        Registries.FLUID,
        ResourceLocation.withDefaultNamespace("powder_snow")
    );

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }

    public static final Block[] FLOWING_MELT_GEM_CONVERTIBLE = {
        Blocks.DIORITE,
        Blocks.GRANITE,
        Blocks.ANDESITE
    };

    public static void registerFluidInteractions(FMLCommonSetupEvent ignore) {
        FluidInteractionRegistry.addInteraction(
            MELT_GEM.get().getFluidType(),
            new InteractionInformation(
                (level, currentPos, relativePos, currentState) ->
                    level.getFluidState(relativePos).getFluidType() == Fluids.WATER.getFluidType(),
                (level, currentPos, relativePos, currentState) -> {
                    Block block;
                    if (level.getFluidState(currentPos).isSource()) {
                        block = ModBlocks.CHROMATIC_STONE.get();
                    } else {
                        block = FLOWING_MELT_GEM_CONVERTIBLE[level.getRandom().nextInt(3)];
                    }
                    level.setBlockAndUpdate(
                        currentPos,
                        EventHooks.fireFluidPlaceBlockEvent(level, currentPos, currentPos, block.defaultBlockState())
                    );
                    level.levelEvent(1501, currentPos, 0);
                }
            )
        );
    }

    @SuppressWarnings("CodeBlock2Expr")
    public static void registerVanilla(RegisterEvent event) {
        event.register(NeoForgeRegistries.FLUID_TYPES.key(), helper -> {
            helper.register(ModFluids.POWDER_SNOW_TYPE.getId(), new FluidType(
                FluidType.Properties.create()
                    .canExtinguish(true)
                    .descriptionId(Blocks.POWDER_SNOW.getDescriptionId())
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_POWDER_SNOW)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_POWDER_SNOW)
            ));
        });
        event.register(Registries.FLUID, helper -> {
            helper.register(ModFluids.POWDER_SNOW.getId(), new PowderSnowFluid());
        });
    }

    public static void onRegisterFluidType(RegisterClientExtensionsEvent e) {
        e.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                AnvilCraft.of("block/exp_fluid"),
                AnvilCraft.of("block/exp_fluid_flow"),
                0xC1E8A9,
                1.0f,
                0xFFFFFFFF,
                false
            ), EXP_FLUID_TYPE
        );
        e.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                AnvilCraft.of("block/oil"),
                AnvilCraft.of("block/oil_flow"),
                0x1B061F,
                1.0f,
                0xFFFFFFFF,
                false
            ), OIL_TYPE
        );
        e.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0xC9E4F7,
                2.0f,
                0xFFC9E4F7,
                false
            ), HYDROGEN_TYPE
        );
        e.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0x9CCCF8,
                2.0f,
                0xFF9CCCF8,
                false
            ), OXYGEN_TYPE
        );
        e.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0xF0C8E0,
                2.0f,
                0xFFF0C8E0,
                false
            ), HELIUM_TYPE
        );
        e.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0xA8E8DC,
                2.0f,
                0xFFA8E8DC,
                false
            ), DEUTERIUM_TYPE
        );
        e.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0xC9C2F0,
                2.0f,
                0xFFC9C2F0,
                false
            ), XENON_TYPE
        );
        e.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0xB0E8A8,
                2.0f,
                0xFFB0E8A8,
                false
            ), KRYPTON_TYPE
        );

        for (Color color : Color.values()) {
            e.registerFluidType(
                new ModClientFluidTypeExtensionImpl(
                    AnvilCraft.of("block/%s_cement".formatted(color)),
                    AnvilCraft.of("block/%s_cement".formatted(color)),
                    ColorUtil.mulValue(((DyeItem) color.dyeItem()).getDyeColor().getTextColor(), 0.6f),
                    1.0f,
                    0xFFFFFFFF,
                    false
                ), CEMENT_TYPES.get(color)
            );
        }
        e.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                AnvilCraft.of("block/melt_gem"),
                AnvilCraft.of("block/melt_gem_flow"),
                0xB7EEDE,
                2.0f,
                0xFFFFFFFF,
                false
            ), MELT_GEM_TYPE
        );
        e.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/honey_block_top"),
                ResourceLocation.withDefaultNamespace("block/honey_block_top"),
                0xFFB82E,
                2.0f,
                0xFFFFFFFF,
                false
            ), HONEY_TYPE
        );
        e.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/water_still"),
                ResourceLocation.withDefaultNamespace("block/water_flow"),
                0xE6CFFF,
                0.5f
            ), PRIMORDIAL_MATTER_TYPE
        );
        e.registerFluidType(
            new LiquidEnchantmentClientFluidTypeExtension(AnvilCraft.of("block/liquid_enchantment")),
            LIQUID_ENCHANTMENT_TYPE
        );
        e.registerFluidType(
            new ModClientFluidTypeExtensionImpl(
                ResourceLocation.withDefaultNamespace("block/powder_snow"),
                ResourceLocation.withDefaultNamespace("block/powder_snow")
            ), POWDER_SNOW_TYPE
        );
    }

}
