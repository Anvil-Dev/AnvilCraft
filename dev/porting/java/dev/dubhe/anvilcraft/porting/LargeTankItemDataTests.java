package dev.dubhe.anvilcraft.porting;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.tooltip.FluidTankItemTooltip;
import dev.dubhe.anvilcraft.block.entity.LargeFluidTankBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.UnitUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class LargeTankItemDataTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_large_tank_item_current", LargeTankItemDataTests::current,
        "port_large_tank_item_legacy", h -> flags(h, true),
        "port_large_tank_item_flags", h -> flags(h, false)
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_large_tank_item"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static FluidStack namedWater() {
        var fluid = new FluidStack(Fluids.WATER, 1575);
        fluid.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        fluid.set(DataComponents.CUSTOM_NAME, Component.literal("Component water"));
        return fluid;
    }

    private static void current(GameTestHelper h) {
        var water = namedWater();
        var stack = LargeFluidTankBlockEntity.fillItem(ModBlocks.LARGE_FLUID_TANK.asStack(),
            List.of(water, new FluidStack(Fluids.LAVA, 2000)), h.getLevel().registryAccess());
        var fluids = FluidTankItemTooltip.readMultiTankFluids(stack, h.getLevel().registryAccess());
        h.assertTrue(fluids.size() == 2 && fluids.getFirst().getAmount() == 1575 && fluids.get(1).getAmount() == 2000,
            "Current item writer is readable by the renderer/tooltip helper");
        h.assertTrue(FluidStack.isSameFluidSameComponents(water, fluids.getFirst()), "Fluid components survive registry-aware reading");
        var lines = new ArrayList<Component>();
        FluidTankItemTooltip.appendMultiTank(stack, Item.TooltipContext.of(h.getLevel()), lines::add, 512000);
        h.assertTrue(lines.stream().anyMatch(line -> line.getString().contains("1.57 B")), "Current item tooltip lists actual contents");
        h.succeed();
    }

    private static void flags(GameTestHelper h, boolean legacy) {
        var water = namedWater();
        var lava = new FluidStack(Fluids.LAVA, 12800000);
        var ops = h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        var fluids = new ListTag();
        for (var fluid : List.of(water, lava)) {
            var encoded = FluidStack.CODEC.encodeStart(ops, fluid).getOrThrow();
            if (legacy) {
                var entry = new CompoundTag();
                entry.put("Fluid", encoded);
                entry.putBoolean("Infinite", fluid.is(Fluids.LAVA));
                fluids.add(entry);
            } else {
                fluids.add(encoded);
            }
        }
        var tank = new CompoundTag();
        tank.put("Fluids", fluids);
        tank.putBoolean("Enhanced", true);
        if (!legacy) tank.store("Infinite", Codec.BOOL.listOf(), List.of(false, true));
        var stack = ModBlocks.LARGE_FLUID_TANK.asStack();
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, h.getLevel().registryAccess());
        output.store("Tank", CompoundTag.CODEC, tank);
        BlockItem.setBlockEntityData(stack, ModBlockEntities.LARGE_FLUID_TANK.get(), output);
        var read = FluidTankItemTooltip.readMultiTankFluids(stack, h.getLevel().registryAccess());
        h.assertTrue(read.size() == 2 && FluidStack.isSameFluidSameComponents(water, read.getFirst()), "Read both tank formats");
        var tooltip = new ArrayList<Component>();
        FluidTankItemTooltip.appendMultiTank(stack, Item.TooltipContext.of(h.getLevel()), tooltip::add, 512000);
        var rows = tooltip.stream().filter(line -> line.getContents().equals(Component.literal("  ").getContents()))
            .map(Component::getString).toList();
        h.assertTrue(rows.size() == 2 && rows.getFirst().contains("1.57 B") && rows.get(1).endsWith(UnitUtil.INFINITE_POWER),
            "Per-entry and parallel infinity flags preserve finite/infinite item tooltip rows: " + rows);
        h.succeed();
    }
}
