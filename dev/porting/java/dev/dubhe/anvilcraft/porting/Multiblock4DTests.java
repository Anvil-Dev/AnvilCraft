package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.event.GiantAnvilEvent;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.event.giantanvil.GiantAnvilLandingEventListener;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModEntities;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.LaserHitRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.Multiblock4DRecipe;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import dev.dubhe.anvilcraft.util.NbtUtil;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class Multiblock4DTests {
    private static boolean cancelLand;
    private static BlockPos cancelBlock;
    private static FallingGiantAnvilEntity cancelEntity;
    private static boolean blockEventSeen;
    private static final BlockPos CENTER = new BlockPos(8, 8, 3);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_4d_sequence", Multiblock4DTests::sequence,
        "port_4d_break_refund", Multiblock4DTests::refund,
        "port_4d_recipe_reload", Multiblock4DTests::reload,
        "port_4d_removed_recipe", Multiblock4DTests::removedRecipe,
        "port_4d_packets", Multiblock4DTests::packets,
        "port_4d_events", Multiblock4DTests::events,
        "port_4d_codec", Multiblock4DTests::codec,
        "port_4d_laser_template", Multiblock4DTests::laserTemplate
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, r -> TESTS.forEach((name, test) -> r.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_4d"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static SpacetimeSupercomputerBlockEntity setup(GameTestHelper h) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) h.setBlock(CENTER.offset(x, 0, z), Blocks.CRAFTING_TABLE);
        }
        h.setBlock(CENTER, ModBlocks.SPACETIME_SUPERCOMPUTER.get());
        return h.getBlockEntity(CENTER, SpacetimeSupercomputerBlockEntity.class);
    }

    private static void fill(GameTestHelper h, Block block) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -3; y <= -1; y++) {
                for (int z = -1; z <= 1; z++) h.setBlock(CENTER.offset(x, y, z), block);
            }
        }
    }

    private static void land(GameTestHelper h) {
        var entity = new FallingGiantAnvilEntity(ModEntities.FALLING_GIANT_ANVIL.get(), h.getLevel());
        GiantAnvilLandingEventListener.handleMultiblock(new AnvilEvent.GiantOnLand(
            h.getLevel(), h.absolutePos(CENTER.above(2)), entity, 1));
    }

    private static int dropped(GameTestHelper h, Item item) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(h.absolutePos(CENTER)).inflate(4)).stream()
            .filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    private static void sequence(GameTestHelper h) {
        final var computer = setup(h);
        fill(h, Blocks.DIRT);
        land(h);
        h.assertTrue(computer.getProcessingProgress() == 1 && computer.getProcessingTotal() == 3, "First step persists progress");
        h.assertTrue(h.getBlockState(CENTER.below()).isAir() && dropped(h, Items.DIRT) == 0, "Inputs consumed without early refunds");
        fill(h, Blocks.DIRT);
        land(h);
        h.assertTrue(computer.getProcessingProgress() == 1 && h.getBlockState(CENTER.below()).is(Blocks.DIRT),
            "Wrong next structure is untouched");
        fill(h, Blocks.COBBLESTONE);
        land(h);
        h.assertTrue(computer.getProcessingProgress() == 2, "Second structure advances recorded recipe");
        fill(h, Blocks.OAK_PLANKS);
        land(h);
        h.assertTrue(computer.getProcessingProgress() == 0 && computer.getProcessingTotal() == 0, "Completion clears progress");
        h.assertTrue(dropped(h, Items.DIAMOND) == 1 && dropped(h, Items.DIRT) == 0 && dropped(h, Items.COBBLESTONE) == 0,
            "One output and no refunded intermediate materials");
        land(h);
        h.assertTrue(dropped(h, Items.DIAMOND) == 1, "Repeated landing cannot duplicate result");
        h.succeed();
    }

    private static void refund(GameTestHelper h) {
        final var computer = setup(h);
        fill(h, Blocks.DIRT);
        land(h);
        fill(h, Blocks.COBBLESTONE);
        land(h);
        h.getLevel().destroyBlock(h.absolutePos(CENTER), true);
        h.assertTrue(dropped(h, Items.DIRT) == 27 && dropped(h, Items.COBBLESTONE) == 27, "Removal refunds all completed stages once");
        computer.dropProcessingInputs();
        h.assertTrue(dropped(h, Items.DIRT) == 27, "Refund is idempotent");
        h.succeed();
    }

    private static void reload(GameTestHelper h) {
        final var computer = setup(h);
        fill(h, Blocks.DIRT);
        land(h);
        var tag = computer.saveWithFullMetadata(h.getLevel().registryAccess());
        var restored = new SpacetimeSupercomputerBlockEntity(computer.getType(), computer.getBlockPos(), computer.getBlockState());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag));
        h.getLevel().setBlockEntity(restored);
        h.assertTrue(restored.getProcessingProgress() == 1, "Deferred saved recipe resolves after level attachment");
        var manager = h.getLevel().getServer().getRecipeManager();
        var previous = manager.recipeMap();
        var holder = restored.getProcessingRecipe();
        var replacement = new Multiblock4DRecipe(holder.value().getDefinitions(), holder.value().getResult());
        try {
            manager.recipes = RecipeMap.create(List.of(new RecipeHolder<>(holder.id(), replacement)));
            fill(h, Blocks.COBBLESTONE);
            land(h);
            h.assertTrue(restored.getProcessingProgress() == 2, "Reloaded holder identity does not reset a valid recipe");
            fill(h, Blocks.OAK_PLANKS);
            land(h);
            h.assertTrue(dropped(h, Items.DIAMOND) == 1, "Reloaded recipe finishes normally");
        } finally {
            manager.recipes = previous;
        }
        h.succeed();
    }

    private static void removedRecipe(GameTestHelper h) {
        final var computer = setup(h);
        fill(h, Blocks.DIRT);
        land(h);
        var tag = computer.saveWithFullMetadata(h.getLevel().registryAccess());
        var manager = h.getLevel().getServer().getRecipeManager();
        var previous = manager.recipeMap();
        try {
            manager.recipes = RecipeMap.create(List.of());
            h.getLevel().removeBlockEntity(computer.getBlockPos());
            var restored = new SpacetimeSupercomputerBlockEntity(computer.getType(), computer.getBlockPos(), computer.getBlockState());
            restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), tag));
            h.getLevel().setBlockEntity(restored);
            h.assertTrue(restored.getProcessingProgress() == 0 && dropped(h, Items.DIRT) == 27, "Missing saved recipe refunds its inputs");
        } finally {
            manager.recipes = previous;
        }
        h.succeed();
    }

    private static void packets(GameTestHelper h) {
        var holder = h.getLevel().getServer().getRecipeManager().recipeMap().byType(ModRecipeTypes.MULTIBLOCK_4D.get()).iterator().next();
        var block = setup(h);
        var mirror = new SpacetimeSupercomputerBlockEntity(block.getType(), block.getBlockPos(), block.getBlockState());
        mirror.setCommand("time add 10");
        mirror.addHistoryCommand("time add 10");
        mirror.setProcessingState(holder, 2, 3);
        var data = mirror.saveWithFullMetadata(h.getLevel().registryAccess());
        data.putFloat("chargingProgress", 42);
        mirror.onDataPacket(null, TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), data));
        mirror.onDataPacket(null, TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), data));
        h.assertTrue(mirror.getHistoryCommands().size() == 1 && mirror.getChargingProgress() == 42,
            "Repeated full sync preserves command history");
        mirror.onDataPacket(null, TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), new CompoundTag()));
        h.assertTrue(mirror.getProcessingProgress() == 0 && mirror.getProcessingTotal() == 0,
            "Empty completion packet clears client progress");
        h.assertTrue(mirror.getCommand().equals("time add 10") && mirror.getHistoryCommands().size() == 1
            && mirror.getChargingProgress() == 42, "Processing-only sync preserves command and charging state");
        block.setCommand("time add 30");
        mirror.onDataPacket(null, TagValueInput.create(ProblemReporter.DISCARDING,
            h.getLevel().registryAccess(), block.getUpdateTag(h.getLevel().registryAccess())));
        h.assertTrue(mirror.getCommand().equals("time add 30") && mirror.getChargingProgress() == 0
            && mirror.getHistoryCommands().isEmpty(), "Full update clears zero charge and empty history explicitly");
        h.succeed();
    }

    @SubscribeEvent
    public static void cancel(GiantAnvilEvent.Multiblock event) {
        if (cancelLand) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void cancel(GiantAnvilEvent.BlockTick event) {
        if (event.getPos().equals(cancelBlock)) {
            blockEventSeen = true;
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void cancel(GiantAnvilEvent.FallingTick event) {
        if (event.getEntity() == cancelEntity) event.setCanceled(true);
    }

    private static void events(GameTestHelper h) {
        final var computer = setup(h);
        fill(h, Blocks.DIRT);
        try {
            cancelLand = true;
            land(h);
            h.assertTrue(computer.getProcessingProgress() == 0 && h.getBlockState(CENTER.below()).is(Blocks.DIRT),
                "Cancelled multiblock event preserves inputs");
            cancelBlock = h.absolutePos(CENTER);
            blockEventSeen = false;
            ModBlocks.GIANT_ANVIL.get().tick(ModBlocks.GIANT_ANVIL.getDefaultState(), h.getLevel(), cancelBlock, h.getLevel().getRandom());
            h.assertTrue(blockEventSeen, "Giant block tick publishes its cancellation event");
            cancelEntity = new FallingGiantAnvilEntity(ModEntities.FALLING_GIANT_ANVIL.get(), h.getLevel());
            cancelEntity.tick();
            h.assertTrue(cancelEntity.time == 0, "Cancelled falling tick does not advance physics time");
            var entity = cancelEntity;
            cancelEntity = null;
            entity.tick();
            h.assertTrue(entity.time == 1, "Uncancelled falling tick advances normal physics");
        } finally {
            cancelLand = false;
            cancelBlock = null;
            cancelEntity = null;
        }
        land(h);
        h.assertTrue(computer.getProcessingProgress() == 1, "Uncancelled landing executes crafting");
        computer.dropProcessingInputs();
        h.succeed();
    }

    private static void codec(GameTestHelper h) {
        var recipe = h.getLevel().getServer().getRecipeManager().recipeMap().byType(ModRecipeTypes.MULTIBLOCK_4D.get())
            .iterator().next().value();
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess());
        try {
            Multiblock4DRecipe.Serializer.STREAM_CODEC.encode(buffer, recipe);
            var restored = Multiblock4DRecipe.Serializer.STREAM_CODEC.decode(buffer);
            h.assertTrue(restored.getDefinitions().size() == 3 && restored.getResultItem().is(Items.DIAMOND),
                "Recipe stream preserves all dimensions and lazily creates its output");
        } finally {
            buffer.release();
        }
        var nbt = new CompoundTag();
        nbt.putByte("enabled", (byte) 1);
        nbt.putShort("short_value", (short) 7);
        var code = NbtUtil.toConstructString(nbt, new NbtUtil.State());
        h.assertTrue(code.contains("(byte) 1") && code.contains("(short) 7"), "NBT export narrows numeric method arguments");
        h.succeed();
    }

    private static void laserTemplate(GameTestHelper h) {
        h.setBlock(CENTER, Blocks.DIRT);
        var pos = h.absolutePos(CENTER);
        var recipe = LaserHitRecipe.find(h.getLevel(), new LaserHitRecipe.Input(pos, Blocks.DIRT.defaultBlockState(), 1, false))
            .orElseThrow().value();
        var first = recipe.createDrops(h.getLevel(), pos, BlockMiningEffect.NORMAL);
        h.assertTrue(first.size() == 1 && first.getFirst().is(Items.DIAMOND) && first.getFirst().getCount() == 2,
            "Data-loaded explicit laser outputs instantiate after item components are ready");
        first.getFirst().shrink(1);
        h.assertTrue(recipe.createDrops(h.getLevel(), pos, BlockMiningEffect.NORMAL).getFirst().getCount() == 2,
            "Output template creates independent stacks");
        h.succeed();
    }

}
