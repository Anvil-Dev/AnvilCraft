package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluidtank.CreativeFluidHandler;
import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredFluids;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class CreativeTankItemDataTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_creative_tank_item_component", h -> read(h, "component"),
        "port_creative_tank_item_current", h -> read(h, "current"),
        "port_creative_tank_item_legacy", h -> read(h, "legacy")
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_creative_tank_item"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static void read(GameTestHelper h, String format) {
        var fluid = new FluidStack(Fluids.WATER, 1000);
        fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Named creative water"));
        var stack = ModBlocks.CREATIVE_FLUID_TANK.asStack();
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        if (format.equals("legacy")) {
            output.child("infinityFluid").store("Fluid", FluidStack.CODEC, fluid);
        } else {
            var handler = new CreativeFluidHandler();
            handler.replaceStacks(List.of(fluid));
            handler.serialize(output);
        }
        BlockItem.setBlockEntityData(stack, ModBlockEntities.CREATIVE_FLUID_TANK.get(), output);
        if (format.equals("component")) {
            fluid = new FluidStack(Fluids.LAVA, 2000);
            stack.set(ModComponents.CREATIVE_TANK_FLUIDS, new StoredFluids(List.of(fluid)));
        }
        var actual = FluidTankItemTooltip.readCreativeTank(stack, h.getLevel().registryAccess());
        h.assertTrue(FluidStack.matches(actual, fluid), "Creative fluid item decodes " + format + " with component precedence");
        var empty = FluidTankItemTooltip.readCreativeTank(ModBlocks.CREATIVE_FLUID_TANK.asStack(), h.getLevel().registryAccess());
        h.assertTrue(empty.isEmpty(),
            "Empty creative tank stays empty");
        h.succeed();
    }
}
