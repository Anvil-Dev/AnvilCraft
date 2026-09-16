package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.TerminalSourceManager;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class TerminalSourceTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_terminal_nearest", TerminalSourceTests::nearest,
        "port_terminal_ranges", TerminalSourceTests::ranges,
        "port_terminal_lifecycle", TerminalSourceTests::lifecycle,
        "port_terminal_restored", TerminalSourceTests::restored,
        "port_terminal_unbound", TerminalSourceTests::unbound,
        "port_terminal_unloaded", TerminalSourceTests::unloaded,
        "port_terminal_dimensions", TerminalSourceTests::dimensions
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> {
            registry.register(AnvilCraft.of(name), helper -> {
                TerminalSourceManager.clear(helper.getLevel());
                test.accept(helper);
            });
        }));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_terminal_sources"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    static <P extends Enum<P>> StorageBlockEntity place(
        GameTestHelper helper, AbstractMultiPartBlock<P> block, BlockPos relative
    ) {
        var pos = helper.absolutePos(relative);
        var state = block.defaultBlockState();
        for (P part : block.getParts()) {
            helper.getLevel().setBlock(pos.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        var entity = (StorageBlockEntity) helper.getLevel().getBlockEntity(pos);
        entity.setId(UUID.randomUUID());
        return entity;
    }

    private static BlockPos nearest(GameTestHelper helper, BlockPos pos, int range) {
        return TerminalSourceManager.nearestLargeCrate(helper.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, range);
    }

    private static void nearest(GameTestHelper helper) {
        var first = place(helper, ModBlocks.LARGE_CRATE.get(), new BlockPos(2, 1, 2));
        final var second = place(helper, ModBlocks.LARGE_CRATE.get(), new BlockPos(7, 1, 2));
        var shulker = place(helper, ModBlocks.SHULKER_CONTAINER.get(), new BlockPos(2, 1, 7));
        BlockPos query = first.getBlockPos().south(3);
        helper.assertTrue(first.getBlockPos().equals(nearest(helper, query, 32)), "大型板条箱查询必须选最近主方块，忽略其它存储类型");
        helper.assertTrue(shulker.getBlockPos().equals(TerminalSourceManager.nearestShulkerContainer(helper.getLevel(),
            query.getX() + 0.5, query.getY() + 0.5, query.getZ() + 0.5, 64)), "潜影查询不得误选更近的大型板条箱");
        helper.getLevel().setBlock(first.getBlockPos(), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        helper.assertTrue(second.getBlockPos().equals(nearest(helper, query, 32)), "破坏最近目标后应回退到仍有效的次近目标");
        helper.succeed();
    }

    private static void ranges(GameTestHelper helper) {
        var crate = place(helper, ModBlocks.LARGE_CRATE.get(), new BlockPos(2, 1, 2));
        var pos = crate.getBlockPos();
        helper.assertTrue(pos.equals(nearest(helper, pos.east(32), 32)), "32 格边界应包含在内");
        helper.assertTrue(TerminalSourceManager.nearestLargeCrate(helper.getLevel(), pos.getX() + 32.501,
            pos.getY() + 0.5, pos.getZ() + 0.5, 32) == null, "超出 32 格球形范围应拒绝");
        helper.assertTrue(nearest(helper, pos.offset(23, 0, 23), 32) == null, "范围不得退化为立方体");
        var shulker = place(helper, ModBlocks.SHULKER_CONTAINER.get(), new BlockPos(7, 1, 2));
        pos = shulker.getBlockPos();
        helper.assertTrue(pos.equals(TerminalSourceManager.nearestShulkerContainer(helper.getLevel(), pos.getX() + 64.5,
            pos.getY() + 0.5, pos.getZ() + 0.5, 64)), "64 格潜影边界应包含在内");
        helper.succeed();
    }

    private static void lifecycle(GameTestHelper helper) {
        var crate = place(helper, ModBlocks.LARGE_CRATE.get(), new BlockPos(2, 1, 2));
        var pos = crate.getBlockPos();
        crate.onChunkUnloaded();
        helper.assertTrue(nearest(helper, pos, 0) == null, "卸载回调必须注销目标");
        crate.onLoad();
        helper.assertTrue(pos.equals(nearest(helper, pos, 0)), "重新加载必须恢复目标");
        crate.setRemoved();
        helper.assertTrue(nearest(helper, pos, 0) == null, "移除回调必须注销目标");
        crate.clearRemoved();
        crate.onLoad();
        helper.assertTrue(pos.equals(nearest(helper, pos, 0)), "复用实体重新加载后索引必须恢复");
        helper.succeed();
    }

    private static void restored(GameTestHelper helper) {
        var crate = place(helper, ModBlocks.LARGE_CRATE.get(), new BlockPos(2, 1, 2));
        var pos = crate.getBlockPos();
        var saved = crate.saveCustomOnly(helper.getLevel().registryAccess());
        var restored = ModBlockEntities.LARGE_CRATE.create(pos, crate.getBlockState());
        restored.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
        helper.assertTrue(crate.getId().equals(restored.getId()), "重载必须保留目标身份");
        helper.getLevel().removeBlockEntity(pos);
        helper.getLevel().setBlockEntity(restored);
        restored.onLoad();
        helper.assertTrue(pos.equals(nearest(helper, pos, 0)), "读取 NBT 时尚未绑定世界的目标应在 onLoad 注册");
        helper.succeed();
    }

    private static void unbound(GameTestHelper helper) {
        var crate = place(helper, ModBlocks.LARGE_CRATE.get(), new BlockPos(2, 1, 2));
        var pos = crate.getBlockPos();
        var blank = ModBlockEntities.LARGE_CRATE.create(pos, crate.getBlockState());
        helper.getLevel().removeBlockEntity(pos);
        helper.getLevel().setBlockEntity(blank);
        blank.onLoad();
        helper.assertTrue(pos.equals(nearest(helper, pos, 0)) && blank.getId() == null,
            "未分配 UUID 的已加载仓储也必须可被发现，查询本身不得创建绑定身份");
        helper.succeed();
    }

    private static void unloaded(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = new BlockPos(29000000, 80, 29000000);
        helper.assertTrue(level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null, "测试目标区块必须未加载");
        var stale = ModBlockEntities.LARGE_CRATE.create(pos, ModBlocks.LARGE_CRATE.getDefaultState());
        stale.setLevel(level);
        TerminalSourceManager.registerIfApplicable(stale);
        helper.assertTrue(nearest(helper, pos, 32) == null, "过期登记不能成为有效终端目标");
        helper.assertTrue(level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null, "查询不得强制加载区块");
        helper.succeed();
    }

    private static void dimensions(GameTestHelper helper) {
        var crate = place(helper, ModBlocks.LARGE_CRATE.get(), new BlockPos(2, 1, 2));
        var pos = crate.getBlockPos();
        var other = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(TerminalSourceManager.nearestLargeCrate(other, pos.getX() + 0.5, pos.getY() + 0.5,
            pos.getZ() + 0.5, 32) == null, "不能跨维度解析世界终端目标");
        TerminalSourceManager.clear(other);
        helper.assertTrue(pos.equals(nearest(helper, pos, 0)), "清理另一个世界不能移除当前目标");
        TerminalSourceManager.clear(helper.getLevel());
        helper.assertTrue(nearest(helper, pos, 0) == null, "世界卸载必须释放索引");
        crate.onLoad();
        TerminalSourceManager.clear();
        helper.assertTrue(nearest(helper, pos, 0) == null, "服务器停止必须释放所有索引");
        helper.succeed();
    }
}
