package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.ExpCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.power.consumer.ExpCollectorBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class ExperienceCollectorSourceTests {
    private static final BlockPos POS = new BlockPos(4, 3, 4);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_exp_source_scan", ExperienceCollectorSourceTests::scan,
        "port_exp_source_spawn", ExperienceCollectorSourceTests::spawn,
        "port_exp_source_invalid", ExperienceCollectorSourceTests::invalid,
        "port_exp_source_capacity", ExperienceCollectorSourceTests::capacity,
        "port_exp_source_disabled", ExperienceCollectorSourceTests::disabled,
        "port_exp_source_large_count", ExperienceCollectorSourceTests::largeCount
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_exp_source"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true))));
    }

    private static ExpCollectorBlockEntity collector(GameTestHelper h, BlockPos pos, boolean instant, int fluid) {
        h.setBlock(pos, ModBlocks.EXP_COLLECTOR.get());
        var be = h.getBlockEntity(pos, ExpCollectorBlockEntity.class);
        be.setGrid(new PowerGrid(h.getLevel()));
        be.getCooldown().fromIndex(instant ? 0 : 1);
        be.tick(h.getLevel(), be.getBlockPos());
        try (Transaction tx = Transaction.openRoot()) {
            be.getInternalFluidHandler().insert(FluidResource.of(ModFluids.EXP_FLUID.get()), fluid, tx);
            tx.commit();
        }
        return be;
    }

    private static ExperienceOrb orb(GameTestHelper h, Vec3 pos, int value, int count) {
        var orb = new ExperienceOrb(h.getLevel(), pos.x, pos.y, pos.z, value);
        orb.count = count;
        h.getLevel().addFreshEntity(orb);
        return orb;
    }

    private static long remaining(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(3)).stream()
            .mapToLong(orb -> (long) orb.getValue() * orb.count).sum();
    }

    private static void scan(GameTestHelper h) {
        var be = collector(h, POS, false, 3900);
        final var orb = orb(h, be.getBlockPos().getCenter(), 7, 3);
        be.gridTick();
        be.gridTick();
        h.assertTrue(be.getInternalFluidHandler().getAmountAsInt(0) == 4000, "Absorb five XP even when one orb unit is seven");
        h.assertTrue(orb.count == 2 && !orb.isRemoved() && remaining(h, be.getBlockPos()) == 16,
            "Retain two seven-XP units and return two XP to world");
        h.succeed();
    }

    private static void spawn(GameTestHelper h) {
        var be = collector(h, POS, true, 3900);
        final var orb = orb(h, be.getBlockPos().getCenter(), 7, 3);
        h.assertTrue(be.getInternalFluidHandler().getAmountAsInt(0) == 4000 && orb.count == 2,
            "Actual entity-add hook absorbs partial orb immediately");
        h.assertTrue(remaining(h, be.getBlockPos()) == 16, "Spawn path conserves unabsorbed XP including returned orbs");
        h.succeed();
    }

    private static void invalid(GameTestHelper h) {
        for (boolean instant : new boolean[]{false, true}) {
            var be = collector(h, POS, instant, 3900);
            var zero = orb(h, be.getBlockPos().getCenter(), 0, 1);
            var negative = orb(h, be.getBlockPos().getCenter(), -5, 1);
            var badCount = orb(h, be.getBlockPos().getCenter(), 7, -2);
            if (!instant) {
                be.gridTick();
                be.gridTick();
            }
            h.assertTrue(zero.isRemoved() && negative.isRemoved(), "Nonpositive XP values are discarded");
            h.assertTrue(badCount.isRemoved() && remaining(h, be.getBlockPos()) == 2,
                "Nonpositive count becomes one unit, five XP absorbed and two returned");
            h.assertTrue(be.getInternalFluidHandler().getAmountAsInt(0) == 4000, "Invalid orb guards cannot create negative fluid");
            h.getLevel().getEntitiesOfClass(ExperienceOrb.class, new AABB(be.getBlockPos()).inflate(3)).forEach(Entity::discard);
            h.setBlock(POS, Blocks.AIR);
        }
        h.succeed();
    }

    private static void capacity(GameTestHelper h) {
        var first = collector(h, POS, true, 4000);
        var second = collector(h, POS.east(2), true, 3981);
        Vec3 middle = first.getBlockPos().getCenter().add(1, 0, 0);
        var orb = orb(h, middle, 7, 0);
        h.assertTrue(!orb.isRemoved() && orb.count == 1 && second.getInternalFluidHandler().getAmountAsInt(0) == 3981,
            "Less than twenty mB free preserves the normalized orb");
        try (Transaction tx = Transaction.openRoot()) {
            second.getInternalFluidHandler().extract(FluidResource.of(ModFluids.EXP_FLUID.get()), 81, tx);
            tx.commit();
        }
        var added = orb(h, middle, 7, 1);
        h.assertTrue(added.isRemoved() && second.getInternalFluidHandler().getAmountAsInt(0) == 4000,
            "A full collector does not prevent another in range from absorbing five XP");
        h.assertTrue(remaining(h, first.getBlockPos()) == 9, "Seven original XP plus two returned XP remain");
        h.succeed();
    }

    private static void disabled(GameTestHelper h) {
        var be = collector(h, POS, true, 0);
        be.setGrid(null);
        var noPower = orb(h, be.getBlockPos().getCenter(), 3, 1);
        be.setGrid(new PowerGrid(h.getLevel()));
        h.getLevel().setBlockAndUpdate(be.getBlockPos(), be.getBlockState().setValue(ExpCollectorBlock.POWERED, true));
        var powered = orb(h, be.getBlockPos().getCenter(), 3, 1);
        h.assertTrue(!noPower.isRemoved() && !powered.isRemoved() && be.getInternalFluidHandler().getAmountAsInt(0) == 0,
            "Grid power and redstone-disable guards still apply");
        h.getLevel().setBlockAndUpdate(be.getBlockPos(), be.getBlockState().setValue(ExpCollectorBlock.POWERED, false));
        var outside = orb(h, be.getBlockPos().getCenter().add(4, 0, 0), 3, 1);
        h.assertTrue(!outside.isRemoved(), "Instant collection remains limited by configured range");
        h.succeed();
    }

    private static void largeCount(GameTestHelper h) {
        var be = collector(h, POS, false, 0);
        final long total = 2477L * Integer.MAX_VALUE;
        var orb = orb(h, be.getBlockPos().getCenter(), 2477, Integer.MAX_VALUE);
        be.gridTick();
        be.gridTick();
        h.assertTrue(be.getInternalFluidHandler().getAmountAsInt(0) == 4000 && orb.count == Integer.MAX_VALUE - 1,
            "Merged count arithmetic cannot overflow before selecting the two-hundred-XP capacity");
        h.assertTrue(remaining(h, be.getBlockPos()) == total - 200, "Large merged counts conserve every XP point");
        h.succeed();
    }
}
