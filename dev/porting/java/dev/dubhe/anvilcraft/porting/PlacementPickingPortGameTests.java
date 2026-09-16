package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.BlockPlacementPicking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class PlacementPickingPortGameTests {
    private static final BlockPos TARGET = new BlockPos(2, 2, 4);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_picking_contexts", PlacementPickingPortGameTests::contexts,
        "port_picking_correction", PlacementPickingPortGameTests::correction,
        "port_picking_permissions", PlacementPickingPortGameTests::permissions,
        "port_picking_air", PlacementPickingPortGameTests::air,
        "port_picking_entity", PlacementPickingPortGameTests::entity
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_picking"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_empty"), 100, 0, true)
        )));
    }

    private static Player player(GameTestHelper helper) {
        helper.setBlock(TARGET, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 eye = helper.absolutePos(TARGET).getCenter().add(0, 0, -3);
        player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
        player.setYRot(0);
        player.setXRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT, 3));
        return player;
    }

    private static UseOnContext click(GameTestHelper helper, Player player) {
        BlockPos pos = helper.absolutePos(TARGET);
        return new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.getCenter(), Direction.UP, pos, true));
    }

    private static void contexts(GameTestHelper helper) {
        Player player = player(helper);
        UseOnContext original = click(helper, player);
        helper.assertTrue(((BlockPlacementPicking.PlayerClick) original).anvilcraft$isPlayerClick(), "玩家构造器必须标记真实点击");
        BlockPos pos = helper.absolutePos(TARGET);
        UseOnContext synthetic = new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, player.getMainHandItem(),
            new BlockHitResult(pos.getCenter(), Direction.UP, pos, true));
        helper.assertTrue(BlockPlacementPicking.forPlacement(synthetic) == synthetic, "机器显式构造的放置上下文不能被玩家射线覆盖");
        UseOnContext surface = new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.getCenter().add(0, 0, -0.5), Direction.UP, pos, true));
        helper.assertTrue(BlockPlacementPicking.forPlacement(surface) == surface, "原形状表面的调用方朝向和 inside 必须保留");
        UseOnContext outside = new UseOnContext(player, InteractionHand.MAIN_HAND,
            new BlockHitResult(pos.getCenter().add(2, 0, 0), Direction.DOWN, pos, false));
        helper.assertTrue(BlockPlacementPicking.forPlacement(outside) == outside, "格外编码的辅助放置朝向必须保留");
        helper.setBlock(TARGET, Blocks.AIR);
        helper.assertTrue(BlockPlacementPicking.forPlacement(original) == original, "直接指定可替换格的放置必须保留");
        helper.succeed();
    }

    private static void correction(GameTestHelper helper) {
        Player player = player(helper);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.COBBLESTONE, 2));
        BlockPos pos = helper.absolutePos(TARGET);
        UseOnContext original = new UseOnContext(player, InteractionHand.OFF_HAND,
            new BlockHitResult(pos.getCenter(), Direction.UP, pos, true));
        UseOnContext corrected = BlockPlacementPicking.forPlacement(original);
        helper.assertTrue(corrected.getClickedFace() == Direction.NORTH
            && corrected.getClickLocation().distanceToSqr(pos.getCenter().add(0, 0, -0.5)) < 1.0E-10,
            "模型内部命中必须校正到原方块表面");
        helper.assertTrue(corrected.getHand() == InteractionHand.OFF_HAND && corrected.getItemInHand() == player.getOffhandItem(),
            "校正必须保留副手与其物品");
        BlockPlaceContext place = new BlockPlaceContext(original);
        helper.assertTrue(place.getClickedPos().equals(pos.north()), "原生放置上下文必须使用校正后的落点");
        var result = player.getOffhandItem().useOn(original);
        helper.assertTrue(result.consumesAction() && helper.getBlockState(TARGET.north()).is(Blocks.COBBLESTONE),
            "实际副手放置必须与校正后的落点一致");
        helper.assertTrue(player.getOffhandItem().getCount() == 1, "实际放置必须只消耗一个物品");
        helper.succeed();
    }

    private static void permissions(GameTestHelper helper) {
        Player player = player(helper);
        player.getAbilities().mayBuild = false;
        UseOnContext blocked = BlockPlacementPicking.forPlacement(click(helper, player));
        helper.assertTrue(!((BlockPlacementPicking.PlayerClick) blocked).anvilcraft$hasBlockHit(), "冒险权限拒绝必须产生 MISS");
        helper.assertTrue(!new BlockPlaceContext(blocked).canPlace(), "MISS 不能在空的相邻格放置");
        helper.assertTrue(BlockPlacementPicking.findAirPlacementHit(player.getMainHandItem(), helper.getLevel(), player) == null,
            "空气放置也必须检查冒险模式权限");
        player.getAbilities().mayBuild = true;
        player.setPos(player.position().add(0, 100, 0));
        helper.assertTrue(!new BlockPlaceContext(click(helper, player)).canPlace(), "超出交互距离的模型点击不能放置");
        var fake = FakePlayerFactory.getMinecraft(helper.getLevel());
        UseOnContext fakeContext = click(helper, fake);
        helper.assertTrue(BlockPlacementPicking.forPlacement(fakeContext) == fakeContext, "不能修改自动化假玩家的上下文");
        helper.assertTrue(BlockPlacementPicking.findAirPlacementHit(new ItemStack(Items.DIRT), helper.getLevel(), fake) == null,
            "假玩家不能触发空气补偿");
        helper.succeed();
    }

    private static void air(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack stack = player.getMainHandItem();
        var hit = BlockPlacementPicking.findAirPlacementHit(stack, helper.getLevel(), player);
        helper.assertTrue(hit != null && hit.getBlockPos().equals(helper.absolutePos(TARGET)), "空气操作必须找到原方块轮廓");
        var result = stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(result.consumesAction() && helper.getBlockState(TARGET.north()).is(Blocks.DIRT) && stack.getCount() == 2,
            "原生 ItemStack.use 必须执行补偿放置并只消耗一个物品");
        Player spectator = helper.makeMockPlayer(GameType.SPECTATOR);
        spectator.setPos(player.position());
        helper.assertTrue(BlockPlacementPicking.findAirPlacementHit(stack, helper.getLevel(), spectator) == null,
            "旁观者不能触发空气放置");
        helper.succeed();
    }

    private static void entity(GameTestHelper helper) {
        Player player = player(helper);
        var stand = helper.spawn(EntityType.ARMOR_STAND, TARGET.north(2).below());
        stand.setPos(helper.absolutePos(TARGET).getCenter().add(0, -1, -2));
        helper.assertTrue(BlockPlacementPicking.findAirPlacementHit(player.getMainHandItem(), helper.getLevel(), player) == null,
            "命中实体时不能越过实体补偿放置");
        stand.discard();
        helper.assertTrue(BlockPlacementPicking.findAirPlacementHit(player.getMainHandItem(), helper.getLevel(), player) != null,
            "实体移走后应恢复补偿放置");
        helper.succeed();
    }
}
