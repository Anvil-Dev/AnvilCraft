package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.util.ModClientFluidTypeExtensionImpl;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
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
        Object2ObjectMap<Color, DeferredHolder<FluidType, FluidType>> map = new Object2ObjectOpenHashMap<>();
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
        Object2ObjectMap<Color, DeferredHolder<Fluid, BaseFlowingFluid>> map = new Object2ObjectOpenHashMap<>();
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
        Object2ObjectMap<Color, DeferredHolder<Fluid, BaseFlowingFluid>> map = new Object2ObjectOpenHashMap<>();
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
        Object2ObjectMap<Color, BaseFlowingFluid.Properties> map = new Object2ObjectOpenHashMap<>();
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

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }

    public static void onRegisterFluidType(RegisterClientExtensionsEvent e) {
        e.registerFluidType(new ModClientFluidTypeExtensionImpl(AnvilCraft.of("block/oil")), OIL_TYPE);
        for (Color color : Color.values()) {
            e.registerFluidType(
                new ModClientFluidTypeExtensionImpl(AnvilCraft.of("block/%s_cement".formatted(color))),
                CEMENT_TYPES.get(color)
            );
        }
    }

}
