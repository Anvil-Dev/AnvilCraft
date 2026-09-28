package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.RuinsBlock;
import dev.dubhe.anvilcraft.block.RuinsBlockView;
import dev.dubhe.anvilcraft.block.entity.RuinsBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.block.RuinsBlockItem;
import dev.dubhe.anvilcraft.network.RuinsUpdatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class RuinsTests {
    private static final BlockPos POS = new BlockPos(8, 3, 3);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_ruins_snapshot", RuinsTests::snapshot,
        "port_ruins_physics", RuinsTests::physics,
        "port_ruins_multipart", RuinsTests::multipart,
        "port_ruins_fragile", RuinsTests::fragile,
        "port_ruins_loot", RuinsTests::loot,
        "port_ruins_permissions", RuinsTests::permissions,
        "port_ruins_piston", RuinsTests::piston
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_ruins"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 200, 0, true))));
    }

    private static ServerPlayer player(GameTestHelper helper, boolean creative) {
        var player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "ruins-test"));
        player.setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL);
        var pos = helper.absolutePos(POS.south(3));
        player.setPos(pos.getCenter());
        return player;
    }

    private static RuinsBlockEntity convert(GameTestHelper helper, Block block) {
        helper.setBlock(POS, block);
        RuinsBlockItem.convert(helper.getLevel(), helper.absolutePos(POS));
        return helper.getBlockEntity(POS, RuinsBlockEntity.class);
    }

    private static void snapshot(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.CHEST);
        var chest = helper.getBlockEntity(POS, ChestBlockEntity.class);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 7));
        RuinsBlockItem.convert(helper.getLevel(), helper.absolutePos(POS));
        var ruins = helper.getBlockEntity(POS, RuinsBlockEntity.class);
        helper.assertTrue(ruins.getDisplayState().is(Blocks.CHEST) && !ruins.canMove(),
            "Snapshot preserves original state and movement rule");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(POS)).inflate(1)).isEmpty(),
            "Replacing the original container must not spill its contents");
        helper.assertTrue(helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(POS), null) == null,
            "A ruins snapshot does not expose the original machine inventory");
        var tag = ruins.getUpdateTag(helper.getLevel().registryAccess());
        var restored = new ChestBlockEntity(helper.absolutePos(POS), ruins.getDisplayState());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(),
            tag.getCompoundOrEmpty("displayData")));
        helper.assertTrue(restored.getItem(0).is(Items.DIAMOND) && restored.getItem(0).getCount() == 7, "Snapshot includes container data");
        helper.assertTrue(ruins.getDisplayEntity() == null, "Server must never instantiate a working display entity");
        helper.succeed();
    }

    private static void physics(GameTestHelper helper) {
        for (Block block : java.util.List.of(Blocks.STONE, Blocks.OAK_SLAB, Blocks.GLASS, Blocks.GLOWSTONE)) {
            var ruins = convert(helper, block);
            var state = helper.getBlockState(POS);
            var original = block.defaultBlockState();
            var view = new RuinsBlockView(helper.getLevel());
            var pos = helper.absolutePos(POS);
            helper.assertTrue(!Shapes.joinIsNotEmpty(state.getCollisionShape(helper.getLevel(), pos),
                original.getCollisionShape(view, pos), BooleanOp.NOT_SAME), "Original collision shape");
            helper.assertTrue(state.getLightDampening() == original.getLightDampening()
                && state.propagatesSkylightDown() == original.propagatesSkylightDown(), "Native cached light properties follow disguise");
            helper.assertTrue(state.getLightEmission(helper.getLevel(), pos) == original.getLightEmission(view, pos),
                "Original light emission");
            helper.assertTrue(ruins.canMove() && state.getValue(RuinsBlock.MOVABLE), "Ordinary disguised blocks remain movable");
        }
        helper.succeed();
    }

    private static RuinsBlockEntity group(GameTestHelper helper) {
        var block = ModBlocks.LARGE_CRATE.get();
        var state = block.defaultBlockState();
        for (var part : block.getParts()) {
            helper.getLevel().setBlock(helper.absolutePos(POS).offset(block.offsetFrom(state, part)), block.placedState(part, state),
                Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
        RuinsBlockItem.convert(helper.getLevel(), helper.absolutePos(POS.above().east()));
        for (var part : Cube3x3PartHalf.values()) {
            var ruins = helper.getBlockEntity(POS.offset(part.getOffset()), RuinsBlockEntity.class);
            helper.assertTrue(ruins.getDisplayState().is(block), "Every original multipart member is converted");
        }
        return helper.getBlockEntity(POS, RuinsBlockEntity.class);
    }

    private static void multipart(GameTestHelper helper) {
        var main = group(helper);
        main.setDrops(Identifier.withDefaultNamespace("blocks/diamond_block"));
        helper.getLevel().destroyBlock(helper.absolutePos(POS.above().east()), true);
        assertRemoved(helper);
        int drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(POS)).inflate(4)).stream()
            .map(ItemEntity::getItem).filter(stack -> stack.is(Items.DIAMOND_BLOCK)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(drops == 1, "Breaking any member drops the configured main loot exactly once");
        helper.succeed();
    }

    private static void assertRemoved(GameTestHelper helper) {
        for (var part : Cube3x3PartHalf.values()) {
            helper.assertTrue(!helper.getBlockState(POS.offset(part.getOffset())).is(ModBlocks.RUINS_BLOCK), "Multipart cleanup");
        }
    }

    private static void fragile(GameTestHelper helper) {
        var main = group(helper);
        main.setDrops(Identifier.withDefaultNamespace("blocks/diamond_block"));
        var player = player(helper, false);
        var pos = helper.absolutePos(POS.above().east());
        var hit = new BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false);
        helper.getLevel().getBlockState(pos).useItemOn(ItemStack.EMPTY, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(helper.getBlockState(POS).is(ModBlocks.RUINS_BLOCK),
            "Inert mode consumes interaction without changing structure");
        main.setFragile(true);
        helper.getLevel().getBlockState(pos).useItemOn(ItemStack.EMPTY, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
        assertRemoved(helper);
        int drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(POS)).inflate(4)).stream()
            .map(ItemEntity::getItem).filter(stack -> stack.is(Items.DIAMOND_BLOCK)).mapToInt(ItemStack::getCount).sum();
        helper.assertTrue(drops == 1, "Fragile interaction yields configured loot once");
        helper.succeed();
    }

    private static LootParams.Builder params(GameTestHelper helper, RuinsBlockEntity ruins) {
        return new LootParams.Builder(helper.getLevel()).withParameter(LootContextParams.ORIGIN, ruins.getBlockPos().getCenter())
            .withParameter(LootContextParams.TOOL, ItemStack.EMPTY).withParameter(LootContextParams.BLOCK_ENTITY, ruins);
    }

    private static void loot(GameTestHelper helper) {
        var ruins = convert(helper, Blocks.STONE);
        helper.assertTrue(ruins.getDrops(params(helper, ruins)).isEmpty(), "Default minecraft:empty drops nothing");
        var table = LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(3))
            .add(LootItem.lootTableItem(Items.DIAMOND))).build();
        var data = ruins.saveWithFullMetadata(helper.getLevel().registryAccess());
        data.put("drops", LootTable.DIRECT_CODEC.encodeStart(helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE),
            table).getOrThrow());
        ruins.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), data));
        helper.assertTrue(ruins.getDropsId().isEmpty() && ruins.getDrops(params(helper, ruins)).stream()
            .mapToInt(ItemStack::getCount).sum() == 3,
            "Inline loot table survives loading");
        helper.assertTrue(!ruins.getUpdateTag(helper.getLevel().registryAccess()).contains("drops"), "Loot remains server-side");
        var player = player(helper, true);
        player.setItemInHand(InteractionHand.MAIN_HAND, ModBlocks.RUINS_BLOCK.asStack());
        new RuinsUpdatePacket(ruins.getBlockPos(), "", true).handleOnServer(player);
        helper.assertTrue(ruins.isFragile() && ruins.getDropsId().isEmpty(), "Mode-only editing retains inline loot");
        helper.succeed();
    }

    private static void permissions(GameTestHelper helper) {
        var ruins = convert(helper, Blocks.STONE);
        var player = player(helper, false);
        player.setItemInHand(InteractionHand.MAIN_HAND, ModBlocks.RUINS_BLOCK.asStack());
        var update = new RuinsUpdatePacket(ruins.getBlockPos(), "minecraft:blocks/diamond_block", true);
        update.handleOnServer(player);
        helper.assertTrue(!ruins.isFragile(), "Survival editor update denied");
        player.setGameMode(GameType.CREATIVE);
        new RuinsUpdatePacket(ruins.getBlockPos(), "missing:loot_table", true).handleOnServer(player);
        helper.assertTrue(!ruins.isFragile(), "Unknown loot table denied atomically");
        update.handleOnServer(player);
        helper.assertTrue(ruins.isFragile() && ruins.getDropsId().equals("minecraft:blocks/diamond_block"), "Creative held-tool edit");
        player.setPos(ruins.getBlockPos().getCenter().add(100, 0, 0));
        new RuinsUpdatePacket(ruins.getBlockPos(), "minecraft:empty", false).handleOnServer(player);
        helper.assertTrue(ruins.isFragile(), "Distant update denied");
        helper.succeed();
    }

    private static void piston(GameTestHelper helper) {
        var ruins = convert(helper, Blocks.STONE);
        ruins.setFragile(true);
        ruins.setDrops(Identifier.withDefaultNamespace("blocks/diamond_block"));
        helper.setBlock(POS.west(), Blocks.PISTON.defaultBlockState()
            .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING, net.minecraft.core.Direction.EAST));
        helper.setBlock(POS.west(2), Blocks.REDSTONE_BLOCK);
        helper.succeedWhen(() -> {
            var moved = helper.getBlockEntity(POS.east(), RuinsBlockEntity.class);
            helper.assertTrue(moved.getDisplayState().is(Blocks.STONE) && moved.isFragile()
                && moved.getDropsId().equals("minecraft:blocks/diamond_block"), "Piston movement preserves the entire snapshot");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(POS)).inflate(3)).isEmpty(), "Piston movement does not yield ruins loot");
        });
    }
}
