package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.MeltGemFluid;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.util.ColorUtil;
import dev.dubhe.anvilcraft.util.ModClientFluidTypeExtensionImpl;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
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

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ModFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, AnvilCraft.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, AnvilCraft.MOD_ID);

    public static final DeferredHolder<FluidType, FluidType> OIL_TYPE = FLUID_TYPES.register(
        "oil",
        () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.anvilcraft.oil")
            .density(2000)
            .viscosity(4000)
            .fallDistanceModifier(0)
            .motionScale(0.007)
            .supportsBoating(true)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
        ));
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
        return FLUID_TYPES.register("%s_cement".formatted(color), () -> new FluidType(FluidType.Properties.create()
            .descriptionId("block.anvilcraft.%s_cement".formatted(color))
            .fallDistanceModifier(0)
            .canExtinguish(true)
            .supportsBoating(true)
            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
            .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
        ));
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
        return FLUIDS.register("flowing_%s_cement".formatted(color), () -> new BaseFlowingFluid.Flowing(ModFluids.CEMENT_PROPERTIES.get(color)));
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
    public static final BaseFlowingFluid.Properties MELT_GEM_PROPERTIES = new BaseFlowingFluid.Properties(MELT_GEM_TYPE, MELT_GEM, FLOWING_MELT_GEM)
        .block(ModBlocks.MELT_GEM)
        .bucket(ModItems.MELT_GEM_BUCKET)
        .tickRate(20)
        .explosionResistance(100);

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }

    public static final Block[] FLOWING_MELT_GEM_CONVERTIBLE = {Blocks.DIORITE, Blocks.GRANITE, Blocks.ANDESITE};

    public static void registerFluidInteractions(FMLCommonSetupEvent event) {
        FluidInteractionRegistry.addInteraction(MELT_GEM.get().getFluidType(),
            new InteractionInformation((level, currentPos, relativePos, currentState) ->
                level.getFluidState(relativePos).getFluidType() == Fluids.WATER.getFluidType(),
                (level, currentPos, relativePos, currentState) -> {
                    Block block;
                    if (level.getFluidState(currentPos).isSource()) block = Blocks.TERRACOTTA;
                    else block = FLOWING_MELT_GEM_CONVERTIBLE[level.getRandom().nextInt(3)];
                    level.setBlockAndUpdate(currentPos, EventHooks.fireFluidPlaceBlockEvent(level, currentPos, currentPos, block.defaultBlockState()));
                    level.levelEvent(1501, currentPos, 0);
                }
            ));
    }

    public static void onRegisterFluidType(RegisterClientExtensionsEvent e) {
        e.registerFluidType(new ModClientFluidTypeExtensionImpl(
            AnvilCraft.of("block/oil"),
            AnvilCraft.of("block/oil_flow"),
            0x1B061F,
            1.0f
        ), OIL_TYPE);
        for (Color color : Color.values()) {
            e.registerFluidType(new ModClientFluidTypeExtensionImpl(
                AnvilCraft.of("block/%s_cement".formatted(color)),
                AnvilCraft.of("block/%s_cement".formatted(color)),
                ColorUtil.mulValue(((DyeItem) color.dyeItem()).getDyeColor().getTextColor(), 0.6f),
                1.0f
            ), CEMENT_TYPES.get(color));
        }
        e.registerFluidType(new ModClientFluidTypeExtensionImpl(
            AnvilCraft.of("block/melt_gem"),
            AnvilCraft.of("block/melt_gem_flow"),
            0xB7EEDE,
            2.0f
        ), MELT_GEM_TYPE);
    }

}
