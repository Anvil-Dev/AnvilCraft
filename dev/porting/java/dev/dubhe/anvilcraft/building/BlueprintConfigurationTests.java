package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.ItemCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BlueprintConfigurationTests {
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_blueprint_contents", BlueprintConfigurationTests::contents,
        "port_blueprint_loot_unmapped", BlueprintConfigurationTests::loot,
        "port_blueprint_handler_contents", BlueprintConfigurationTests::handler,
        "port_blueprint_sign_decoration", BlueprintConfigurationTests::sign,
        "port_blueprint_configuration_limits", BlueprintConfigurationTests::limits,
        "port_blueprint_configuration_transform", BlueprintConfigurationTests::transform,
        "port_blueprint_pulse_load", BlueprintConfigurationTests::pulse
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_blueprint_configuration"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true)
        )));
    }

    private static void contents(GameTestHelper helper) {
        var chest = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        chest.setItem(5, new ItemStack(Items.DIAMOND, 7));
        var tag = chest.saveWithFullMetadata(helper.getLevel().registryAccess());
        var before = tag.copy();
        var extracted = BlockEntityContentAdapter.extract(Blocks.CHEST.defaultBlockState(), tag, helper.getLevel().registryAccess());
        helper.assertTrue(extracted.contents().size() == 1 && extracted.contents().getFirst().slot() == 5
            && extracted.contents().getFirst().stack().getCount() == 7 && !extracted.config().contains("Items")
            && tag.equals(before), "真实物品应按槽拆出，配置不含库存且不能修改输入");
        var restored = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState());
        BlockEntityContentAdapter.insert(restored, extracted.contents(), helper.getLevel().registryAccess());
        helper.assertTrue(restored.getItem(5).is(Items.DIAMOND) && restored.getItem(5).getCount() == 7, "交付物品必须回到对应槽位");
        helper.succeed();
    }

    private static void loot(GameTestHelper helper) {
        var tag = new ChestBlockEntity(BlockPos.ZERO, Blocks.CHEST.defaultBlockState())
            .saveWithFullMetadata(helper.getLevel().registryAccess());
        tag.putString("LootTable", "minecraft:chests/simple_dungeon");
        tag.putLong("LootTableSeed", 42);
        var result = BlockEntityContentAdapter.extract(Blocks.CHEST.defaultBlockState(), tag, helper.getLevel().registryAccess());
        helper.assertTrue(result.unmapped() && result.contents().isEmpty() && !result.config().contains("LootTable")
            && !result.config().contains("LootTableSeed"), "规划战利品箱不能生成或复制战利品");
        helper.succeed();
    }

    private static void handler(GameTestHelper helper) {
        var state = ModBlocks.ITEM_COLLECTOR.getDefaultState();
        var collector = new ItemCollectorBlockEntity(ModBlockEntities.ITEM_COLLECTOR.get(), BlockPos.ZERO, state);
        var inventory = collector.getFilteredItemStackHandler();
        inventory.set(0, ItemResource.of(Items.DIAMOND), 3);
        inventory.setFilterEnabled(true);
        inventory.setFilter(2, new ItemStack(Items.EMERALD));
        inventory.setSlotLimit(2, 16);
        inventory.setSlotDisabled(1, true);
        var extracted = BlockEntityContentAdapter.extract(collector, helper.getLevel().registryAccess(), helper.getLevel());
        helper.assertTrue(extracted.contents().size() == 1 && extracted.contents().getFirst().stack().getCount() == 3,
            "过滤样本不能变成库存物品，实际库存应完整拆出");
        var settings = BlueprintBlockConfiguration.take(collector, extracted.config().copy(), helper.getLevel().registryAccess());
        var target = new ItemCollectorBlockEntity(ModBlockEntities.ITEM_COLLECTOR.get(), BlockPos.ZERO, state);
        target.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), settings));
        BlockEntityContentAdapter.insert(target, extracted.contents(), helper.getLevel().registryAccess());
        var result = target.getFilteredItemStackHandler();
        helper.assertTrue(result.getResource(0).equals(ItemResource.of(Items.DIAMOND)) && result.getAmountAsLong(0) == 3
            && result.getFilter(2).is(Items.EMERALD) && result.getSlotLimit(2) == 16 && result.isSlotDisabled(1),
            "实际交付与过滤配置必须各自恢复：" + result.getAmountAsLong(0) + "/" + result.getFilter(2)
                + "/" + result.getSlotLimit(2) + "/" + result.isSlotDisabled(1) + "/" + settings);
        helper.succeed();
    }

    private static void sign(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var text = new SignText().setMessage(0, Component.literal("visible").withStyle(style ->
            style.withBold(true).withClickEvent(new ClickEvent.RunCommand("say blueprint_test"))))
            .setColor(DyeColor.RED).setHasGlowingText(true);
        var original = new CompoundTag();
        original.put("front_text", SignText.DIRECT_CODEC
            .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), text).getOrThrow());
        original.putBoolean("is_waxed", true);
        var extracted = SignDecorationAdapter.extract(Blocks.OAK_SIGN.defaultBlockState(), original, registries);
        helper.assertTrue(extracted.decorations().size() == 4 && !extracted.config().contains("front_text")
            && !extracted.config().contains("is_waxed"), "文字、染色、发光、打蜡应拆成四项独立交付");
        var composed = SignDecorationAdapter.compose(extracted.config(), extracted.decorations(), registries);
        var result = SignText.DIRECT_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE),
            composed.getCompoundOrEmpty("front_text")).getOrThrow();
        helper.assertTrue(result.getMessage(0, false).getString().equals("visible")
            && result.getMessage(0, false).getStyle().getClickEvent() == null && !result.getMessage(0, false).getStyle().isBold()
            && result.getColor() == DyeColor.RED && result.hasGlowingText() && composed.getBooleanOr("is_waxed", false),
            "已交付效果应恢复，文本只保留可见字符");
        var pending = SignDecorationAdapter.stripPending(composed,
            SignDecorationAdapter.maskOf(SignDecorationAdapter.slotOf(SignDecorationAdapter.Aspect.TEXT, true)), registries);
        var hidden = SignText.DIRECT_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE),
            pending.getCompoundOrEmpty("front_text")).getOrThrow();
        helper.assertTrue(hidden.getMessage(0, false).getString().isEmpty() && hidden.hasGlowingText(), "未交付文字的预览不能隐藏其它已交付效果");
        helper.succeed();
    }

    private static void limits(GameTestHelper helper) {
        final var detector = new ItemDetectorBlockEntity(BlockPos.ZERO, ModBlocks.ITEM_DETECTOR.getDefaultState());
        var tag = new CompoundTag();
        tag.putInt("Range", 999);
        tag.putString("FilterMode", "ALL");
        tag.putBoolean("OutputInvert", true);
        final var filter = new CompoundTag();
        var entries = new ListTag();
        var entry = new CompoundTag();
        entry.putInt("Slot", 0);
        entry.put("Item", BlueprintNbt.writeItem(helper.getLevel().registryAccess(), new ItemStack(Items.DIAMOND, 3)));
        entries.add(entry);
        filter.put("Items", entries);
        tag.put("Filter", filter);
        var settings = BlueprintBlockConfiguration.take(detector, tag, helper.getLevel().registryAccess());
        var restored = new ItemDetectorBlockEntity(BlockPos.ZERO, ModBlocks.ITEM_DETECTOR.getDefaultState());
        restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), settings));
        helper.assertTrue(restored.getRange() == 8 && restored.getFilterMode() == ItemDetectorBlockEntity.Mode.ALL
            && restored.isOutputInvert() && restored.getFilter().getItem(0).is(Items.DIAMOND),
            "设置必须限制合法范围并兼容原生嵌套物品格式");
        var pos = helper.absolutePos(new BlockPos(2, 16, 4));
        var level = helper.getLevel();
        level.setBlock(pos, ModBlocks.ITEM_DETECTOR.getDefaultState(), Block.UPDATE_CLIENTS);
        var live = (ItemDetectorBlockEntity) level.getBlockEntity(pos);
        live.setOutputInvert(true);
        live.tick();
        helper.assertTrue(live.getOutputSignal() == 15, "空检测范围在反转模式下必须实际输出满信号");
        live.setOutputInvert(false);
        live.tick();
        helper.assertTrue(live.getOutputSignal() == 0, "关闭反转后必须恢复正常信号");
        helper.succeed();
    }

    private static void transform(GameTestHelper helper) {
        final var config = new CompoundTag();
        var valves = new ListTag();
        var valve = new CompoundTag();
        valve.putInt("Face", Direction.NORTH.get3DDataValue());
        valve.putInt("Flow", Direction.SOUTH.get3DDataValue());
        valves.add(valve);
        config.put("Valves", valves);
        config.putInt("Ix", 101);
        config.putInt("Iy", 52);
        config.putInt("Iz", 203);
        var snapshot = new StructureSnapshot(new Vec3i(4, 4, 4), List.of(), List.of(), List.of());
        BlueprintBlockConfiguration.transform(config, new BlueprintPlacement(new BlockPos(20, 30, 40), Rotation.CLOCKWISE_90,
            Mirror.NONE), new BlockPos(100, 50, 200), snapshot);
        helper.assertTrue(valve.getIntOr("Face", -1) == Direction.EAST.get3DDataValue()
            && valve.getIntOr("Flow", -1) == Direction.WEST.get3DDataValue()
            && config.getIntOr("Ix", 0) == 17 && config.getIntOr("Iy", 0) == 32 && config.getIntOr("Iz", 0) == 41,
            "阀方向与结构内部目标引用必须跟随蓝图变换");
        config.putInt("Ix", 10000);
        BlueprintBlockConfiguration.transform(config, new BlueprintPlacement(BlockPos.ZERO, Rotation.NONE, Mirror.NONE),
            BlockPos.ZERO, snapshot);
        helper.assertTrue(!config.contains("Ix"), "结构外的定日镜目标引用不能继续指向旧世界位置");
        helper.succeed();
    }

    private static void pulse(GameTestHelper helper) {
        var pos = helper.absolutePos(new BlockPos(2, 16, 4));
        var level = helper.getLevel();
        level.setBlock(pos, ModBlocks.PULSE_GENERATOR.getDefaultState(), Block.UPDATE_CLIENTS);
        final var entity = (PulseGeneratorBlockEntity) level.getBlockEntity(pos);
        final var config = new CompoundTag();
        var extra = new CompoundTag();
        extra.putByte("State", PulseGeneratorBlockEntity.State.WAITING.index());
        extra.putByte("StartMode", PulseGeneratorBlockEntity.Mode.LOOP.index());
        extra.putInt("WaitingTime", 20);
        extra.putInt("SignalDuration", 5);
        extra.putLong("PhaseStartGameTime", level.getGameTime());
        extra.putInt("PhaseDuration", 20);
        config.put("ExtraData", extra);
        entity.loadBlueprint(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), config));
        entity.onLoad();
        helper.assertTrue(entity.isProcessing() && !entity.isOutputting()
            && entity.getPhaseDuration() == 20 && entity.getPhaseStartGameTime() == level.getGameTime(),
            "蓝图恢复不得被首次加载的红石输入重判覆盖运行状态");
        helper.succeed();
    }
}
