package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class MultipartShapeTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_multipart_shapes_translation", MultipartShapeTests::translation,
        "port_multipart_shapes_collision", MultipartShapeTests::collision,
        "port_multipart_shapes_placement", MultipartShapeTests::placement
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_multipart_shapes"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static void translation(GameTestHelper helper) {
        translated(helper, ModBlocks.LARGE_CRATE.get());
        translated(helper, ModBlocks.SHULKER_CONTAINER.get());
        translated(helper, ModBlocks.ACCELERATION_RING.get());
        translated(helper, ModBlocks.CELESTIAL_FORGING_ANVIL_AMPLIFIER.get());
        helper.succeed();
    }

    private static <P extends Enum<P>> void translated(GameTestHelper helper, AbstractMultiPartBlock<P> block) {
        for (var state : block.getStateDefinition().getPossibleStates()) {
            var whole = block.getMultiPartShape(state);
            helper.assertTrue(whole == block.getMultiPartShape(state), "Whole shape cache is reused");
            for (P part : block.getParts()) {
                var offset = block.offsetFrom(state, part);
                var other = block.getMultiPartShape(state.setValue(block.getPart(), part));
                equal(helper, whole, other.move(offset.getX(), offset.getY(), offset.getZ()), "Whole shape has the same world position");
            }
        }
    }

    private static void collision(GameTestHelper helper) {
        for (var block : new AbstractMultiPartBlock<?>[]{ModBlocks.LARGE_CRATE.get(), ModBlocks.SHULKER_CONTAINER.get()}) {
            for (var state : block.getStateDefinition().getPossibleStates()) {
                equal(helper, state.getCollisionShape(helper.getLevel(), BlockPos.ZERO), Shapes.block(), "Containers keep solid collision");
                equal(helper, state.getShape(helper.getLevel(), BlockPos.ZERO), block.getMultiPartShape(state),
                    "Outline covers whole container");
                equal(helper, state.getOcclusionShape(), state.canOcclude() ? block.getPartShape(state) : Shapes.empty(),
                    "Occlusion remains part-local or disabled by block properties");
            }
        }
        for (var state : ModBlocks.TRADING_STATION.get().getStateDefinition().getPossibleStates()) {
            equal(helper, state.getShape(helper.getLevel(), BlockPos.ZERO), ModBlocks.TRADING_STATION.get().getPartShape(state),
                "Trading station retains its explicit part-local outline");
        }
        helper.succeed();
    }

    private static void placement(GameTestHelper helper) {
        try (var fixture = new StorageFluidRpcTests.Fixture(helper, true)) {
            var player = fixture.player();
            var state = ModBlocks.LARGE_CAULDRON.getDefaultState().setValue(LargeCauldronBlock.HALF, Cube3x3PartHalf.TOP_CENTER);
            var part = ModBlocks.LARGE_CAULDRON.get().getPartShape(state);
            player.setItemInHand(InteractionHand.MAIN_HAND, ModBlocks.GIANT_ANVIL.asStack());
            var context = CollisionContext.of(player);
            equal(helper, state.getShape(helper.getLevel(), BlockPos.ZERO, context), Shapes.block(), "Placement guide is a full cell");
            equal(helper, state.getCollisionShape(helper.getLevel(), BlockPos.ZERO, context), part,
                "Placement guide cannot become collision");
            var pos = new BlockPos(12, 3, 12);
            helper.setBlock(pos, ModBlocks.RUINS_BLOCK.get());
            var ruins = helper.getBlockEntity(pos, RuinsBlockEntity.class);
            ruins.setDisplay(state, new CompoundTag());
            equal(helper, helper.getBlockState(pos).getShape(helper.getLevel(), helper.absolutePos(pos), context), part,
                "Ruins picking uses the snapshot part, not the placement guide");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            equal(helper, state.getShape(helper.getLevel(), BlockPos.ZERO, CollisionContext.of(player)), part,
                "Cauldron ordinary picking remains part-local");
        }
        helper.succeed();
    }

    private static void equal(GameTestHelper helper, VoxelShape first, VoxelShape second, String message) {
        helper.assertTrue(!Shapes.joinIsNotEmpty(first, second, BooleanOp.NOT_SAME), message);
    }
}
