package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.SlidingBlockEvent;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockStructureResolver;
import dev.dubhe.anvilcraft.api.sliding.SlidingRailLoad;
import dev.dubhe.anvilcraft.api.sliding.SlidingStructureExtension;
import dev.dubhe.anvilcraft.api.sliding.SlidingStructureHooks;
import dev.dubhe.anvilcraft.block.entity.DetectorSlidingRailBlockEntity;
import dev.dubhe.anvilcraft.block.logistics.sliding.DetectorSlidingRailBlock;
import dev.dubhe.anvilcraft.block.logistics.sliding.ISlidingRail;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class SlidingExtensionTests {
    private static final ThreadLocal<Scope> ACTIVE = new ThreadLocal<>();
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_sliding_extension_reaction", SlidingExtensionTests::reaction,
        "port_sliding_extension_expand", SlidingExtensionTests::expand,
        "port_sliding_extension_pistons", SlidingExtensionTests::pistons,
        "port_sliding_extension_lifecycle", SlidingExtensionTests::lifecycle,
        "port_sliding_extension_loads", SlidingExtensionTests::loads,
        "port_sliding_extension_detector", SlidingExtensionTests::detector
    );

    static {
        for (int index = 0; index < 2; index++) {
            int slot = index;
            SlidingStructureHooks.register(new SlidingStructureExtension() {
                @Override
                public PushReaction modifyPushReaction(
                    Level level, BlockPos pos, BlockState state, PushReaction original, Direction direction
                ) {
                    Scope scope = ACTIVE.get();
                    return scope == null || slot >= scope.extensions.size() ? original
                        : scope.extensions.get(slot).modifyPushReaction(level, pos, state, original, direction);
                }

                @Override
                public boolean expand(SlidingBlockStructureResolver resolver) {
                    Scope scope = ACTIVE.get();
                    return scope == null || slot >= scope.extensions.size() || scope.extensions.get(slot).expand(resolver);
                }
            });
        }
        SlidingRailLoad.registerOccupant((level, pos, above) -> {
            Scope scope = ACTIVE.get();
            return scope != null && scope.occupant.test(level, pos, above);
        });
        NeoForge.EVENT_BUS.addListener(SlidingExtensionTests::onStart);
        NeoForge.EVENT_BUS.addListener(SlidingExtensionTests::onStop);
    }

    private static void onStart(SlidingBlockEvent.Start event) {
        Scope scope = ACTIVE.get();
        if (scope != null) scope.start.accept(event);
    }

    private static void onStop(SlidingBlockEvent.Stop event) {
        Scope scope = ACTIVE.get();
        if (scope != null) scope.stop.accept(event);
    }

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_sliding_extension"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static final class Scope implements AutoCloseable {
        private List<SlidingStructureExtension> extensions = List.of();
        private SlidingRailLoad.OccupantCheck occupant = (level, pos, above) -> false;
        private Consumer<SlidingBlockEvent.Start> start = event -> {};
        private Consumer<SlidingBlockEvent.Stop> stop = event -> {};

        private Scope() {
            ACTIVE.set(this);
        }

        @Override
        public void close() {
            ACTIVE.remove();
        }
    }

    private static SlidingBlockStructureResolver resolver(GameTestHelper helper, BlockPos pos) {
        return new SlidingBlockStructureResolver(helper.getLevel(), pos, Direction.EAST, true);
    }

    private static void reaction(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 15, 2));
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Blocks.DIRT.defaultBlockState());
        List<PushReaction> seen = new ArrayList<>();
        try (var scope = new Scope()) {
            scope.extensions = List.of(new SlidingStructureExtension() {
                @Override
                public PushReaction modifyPushReaction(
                    Level world, BlockPos at, BlockState state, PushReaction original, Direction direction
                ) {
                    helper.assertTrue(world == level && direction == Direction.EAST, "Hook receives world and actual movement direction");
                    seen.add(original);
                    return PushReaction.DESTROY;
                }
            }, new SlidingStructureExtension() {
                @Override
                public PushReaction modifyPushReaction(
                    Level world, BlockPos at, BlockState state, PushReaction original, Direction direction
                ) {
                    seen.add(original);
                    return original;
                }
            });
            var resolver = resolver(helper, pos);
            helper.assertTrue(resolver.resolve() && resolver.getToPush().equals(List.of(pos))
                && resolver.getToDestroy().equals(List.of(pos.east())), "Reaction override diverts the next block to destruction");
            helper.assertTrue(seen.equals(List.of(PushReaction.NORMAL, PushReaction.DESTROY)), "Hooks compose in registration order");
            helper.assertTrue(level.getBlockState(pos.east()).is(Blocks.DIRT), "Resolution does not mutate the world");
            level.setBlockAndUpdate(pos, Blocks.TORCH.defaultBlockState());
            helper.assertTrue(resolver(helper, pos).resolve(), "Initial destroy-only block also accepts extension expansion");
        }
        helper.succeed();
    }

    private static void expand(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 15, 2));
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.south(), Blocks.DIRT.defaultBlockState());
        try (var scope = new Scope()) {
            scope.extensions = List.of(new SlidingStructureExtension() {
                @Override
                public boolean expand(SlidingBlockStructureResolver resolver) {
                    helper.assertTrue(resolver.getLevel() == level && resolver.getPushDirection() == Direction.EAST,
                        "Extensions can inspect resolver context");
                    return resolver.addBlockLine(pos.south(), Direction.SOUTH) && resolver.addBranchingBlocks(pos);
                }
            });
            var resolver = resolver(helper, pos);
            helper.assertTrue(resolver.resolve() && resolver.getToPush().containsAll(List.of(pos, pos.south())),
                "Extension adds an attached branch using public resolver methods");
            scope.extensions = List.of(new SlidingStructureExtension() {
                @Override
                public boolean expand(SlidingBlockStructureResolver resolver) {
                    return false;
                }
            }, new SlidingStructureExtension() {
                @Override
                public boolean expand(SlidingBlockStructureResolver resolver) {
                    throw new IllegalStateException("Expansion must short-circuit after veto");
                }
            });
            helper.assertTrue(!ISlidingRail.moveBlocks(level, pos, Direction.EAST) && level.getBlockState(pos).is(Blocks.STONE),
                "Expansion veto prevents real movement and preserves source blocks");
        }
        helper.succeed();
    }

    private static void pistons(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 15, 2));
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        for (Block piston : List.of(Blocks.PISTON, Blocks.STICKY_PISTON)) {
            for (Direction facing : Direction.values()) {
                for (boolean extended : List.of(false, true)) {
                    var state = piston.defaultBlockState().setValue(PistonBaseBlock.FACING, facing)
                        .setValue(PistonBaseBlock.EXTENDED, extended);
                    level.setBlock(pos.east(), state, Block.UPDATE_CLIENTS);
                    var resolver = resolver(helper, pos);
                    helper.assertTrue(resolver.resolve() != extended, "Only retracted piston bases move: " + state);
                    if (!extended) helper.assertTrue(resolver.getToPush().contains(pos.east()), "Retracted piston is collected");
                }
            }
        }
        level.setBlock(pos.east(), Blocks.PISTON_HEAD.defaultBlockState(), Block.UPDATE_CLIENTS);
        helper.assertTrue(!resolver(helper, pos).resolve(), "Piston heads are always rejected");
        level.setBlockAndUpdate(pos.east(), Blocks.OBSIDIAN.defaultBlockState());
        helper.assertTrue(!resolver(helper, pos).resolve(), "Extension support does not bypass vanilla immovable blocks");
        helper.succeed();
    }

    private static void lifecycle(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 15, 2));
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(pos.east(), Blocks.DIRT.defaultBlockState());
        List<String> order = new ArrayList<>();
        List<SlidingBlockEntity> moving = new ArrayList<>();
        try (var scope = new Scope()) {
            scope.start = event -> {
                order.add("start");
                moving.add(event.getEntity());
                helper.assertTrue(event.getLevel() == level && event.getOrigin().equals(pos)
                    && event.getMovement() == Direction.EAST && event.getEntity().getBlockCount() == 2,
                    "Start receives the completed structure and original movement metadata");
                List<BlockPos> payload = new ArrayList<>();
                event.getBlocks().forEach(info -> payload.add(info.getLeft()));
                helper.assertTrue(payload.equals(List.of(pos, pos.east())) && level.getBlockState(pos).isAir(),
                    "Real rail dispatch posts Start after collecting and clearing the original blocks, matching source");
            };
            scope.stop = event -> {
                order.add("stop");
                helper.assertTrue(event.getLevel() == level && event.getEntity() == moving.getFirst()
                    && level.getBlockState(pos).is(Blocks.STONE) && level.getBlockState(pos.east()).is(Blocks.DIRT),
                    "Stop observes the restored world and same entity");
                helper.assertTrue(event.getEntity().isRemoved(), "Source section restoration discards before Stop is posted");
            };
            helper.assertTrue(ISlidingRail.moveBlocks(level, pos, Direction.EAST), "Real rail path starts movement");
            moving.getFirst().stop();
            helper.assertTrue(order.equals(List.of("start", "stop")), "One event at each lifecycle boundary");
        }
        helper.succeed();
    }

    private static void loads(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 15, 2));
        helper.assertTrue(!SlidingRailLoad.hasBlockLoad(level, pos) && !SlidingRailLoad.hasItemLoad(level, pos), "Empty rail has no load");
        var moving = new SlidingBlockEntity(level, pos.above(), Direction.EAST,
            List.of(Triple.of(pos.above(), Blocks.STONE.defaultBlockState(), Optional.empty())));
        level.addFreshEntity(moving);
        helper.assertTrue(SlidingRailLoad.hasBlockLoad(level, pos), "Sliding entities above the rail count as block load");
        moving.discard();
        var item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(Items.STONE));
        level.addFreshEntity(item);
        helper.assertTrue(SlidingRailLoad.hasItemLoad(level, pos), "Items inside the rail count as item load");
        item.discard();
        try (var scope = new Scope()) {
            scope.occupant = (world, at, above) -> world == level && at.equals(pos) && above.equals(new AABB(pos.above()));
            helper.assertTrue(SlidingRailLoad.hasBlockLoad(level, pos) && !SlidingRailLoad.hasBlockLoad(level, pos.east()),
                "Additional carriers receive the rail position and exact above-block search box");
        }
        helper.assertTrue(!SlidingRailLoad.hasBlockLoad(level, pos), "Inactive extension does not leak between tests");
        helper.succeed();
    }

    private static void detector(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(2, 15, 2));
        var block = ModBlocks.DETECTOR_SLIDING_RAIL.get();
        var state = block.defaultBlockState().setValue(DetectorSlidingRailBlock.POWERED, true);
        level.setBlockAndUpdate(pos, state);
        var entity = (DetectorSlidingRailBlockEntity) level.getBlockEntity(pos);
        entity.updatePower(5);
        try (var scope = new Scope()) {
            scope.occupant = (world, at, above) -> at.equals(pos);
            state.tick(level, pos, level.getRandom());
            helper.assertTrue(level.getBlockState(pos).getValue(DetectorSlidingRailBlock.POWERED) && entity.getPower() == 5,
                "Extra carrier preserves detector redstone and comparator output");
        }
        level.getBlockState(pos).tick(level, pos, level.getRandom());
        helper.assertTrue(!level.getBlockState(pos).getValue(DetectorSlidingRailBlock.POWERED) && entity.getPower() == 0,
            "Detector clears after the carrier leaves");
        var detached = new DetectorSlidingRailBlockEntity(entity.getType(), pos, state);
        detached.updatePower(4);
        block.notifyMoved(level, pos, state, detached);
        helper.assertTrue(detached.getPower() == 0, "Move notification clears the supplied block entity, even before attachment");
        helper.succeed();
    }
}
