package dev.dubhe.anvilcraft.porting;

import com.mojang.authlib.GameProfile;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.BigRedButtonBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RedstoneDiceBlockEntity;
import dev.dubhe.anvilcraft.block.utility.redstone.BigRedButtonBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class RedstonePortGameTests {
    private static final BlockPos POS = new BlockPos(3, 2, 3);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_dice_outcomes", RedstonePortGameTests::diceOutcomes,
        "port_dice_timing_reload", RedstonePortGameTests::diceTimingReload,
        "port_button_holders", RedstonePortGameTests::buttonHolders,
        "port_button_timeout_reload", RedstonePortGameTests::buttonTimeoutReload,
        "port_button_sneak_rod", RedstonePortGameTests::buttonSneakRod
    );

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void registerTests(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_redstone"));
        TESTS.forEach((name, test) -> event.registerTest(
            AnvilCraft.of(name),
            new FunctionGameTestInstance(
                ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
                new TestData<>(environment, AnvilCraft.of("port_empty"), 100, 0, true)
            )
        ));
    }

    private static RedstoneDiceBlockEntity placeDice(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.REDSTONE_DICE.get());
        return helper.getBlockEntity(POS, RedstoneDiceBlockEntity.class);
    }

    private static void loadDice(GameTestHelper helper, RedstoneDiceBlockEntity dice, CompoundTag tag) {
        dice.loadCustomOnly(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), tag));
    }

    private static void diceOutcomes(GameTestHelper helper) {
        RedstoneDiceBlockEntity dice = placeDice(helper);
        int[] frequencies = new int[16];
        for (int a = 1; a <= 6; a++) {
            for (int b = 1; b <= 6; b++) {
                for (int c = 1; c <= 6; c++) {
                    CompoundTag tag = new CompoundTag();
                    tag.putInt("Faces", a * 100 + b * 10 + c);
                    tag.putBoolean("Rolling", true);
                    tag.putLong("RollStart", helper.getLevel().getGameTime() - 8);
                    loadDice(helper, dice, tag);
                    dice.finishRoll();
                    helper.assertTrue(dice.getOutput() == a + b + c - 3, "真实骰子点数必须对应红石强度");
                    frequencies[dice.getOutput()]++;
                    CompoundTag saved = dice.saveWithoutMetadata(helper.getLevel().registryAccess());
                    RedstoneDiceBlockEntity restored = new RedstoneDiceBlockEntity(dice.getBlockPos(), dice.getBlockState());
                    loadDice(helper, restored, saved);
                    helper.assertTrue(restored.getOutput() == dice.getOutput(), "存档必须保留输出");
                    for (int index = 0; index < 3; index++) {
                        helper.assertTrue(restored.getFace(index, false) == dice.getFace(index, false), "存档必须保留各骰子点数");
                    }
                }
            }
        }
        int[] expected = {1, 3, 6, 10, 15, 21, 25, 27, 27, 25, 21, 15, 10, 6, 3, 1};
        for (int output = 0; output < 16; output++) {
            helper.assertTrue(frequencies[output] == expected[output], "真实模式分布错误");
        }
        dice.setUniform(false);
        CompoundTag saved = dice.saveWithoutMetadata(helper.getLevel().registryAccess());
        loadDice(helper, dice, saved);
        helper.assertTrue(!dice.isUniform(), "模式必须保存在磁盘中");
        helper.succeed();
    }

    private static void diceTimingReload(GameTestHelper helper) {
        RedstoneDiceBlockEntity dice = placeDice(helper);
        dice.roll();
        CompoundTag initial = dice.saveWithoutMetadata(helper.getLevel().registryAccess());
        dice.roll();
        helper.assertTrue(initial.equals(dice.saveWithoutMetadata(helper.getLevel().registryAccess())), "投掷期间不能再次投掷");
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(dice.getOutput() == 0, "8 tick 前不能更新输出");
            CompoundTag saved = dice.saveWithoutMetadata(helper.getLevel().registryAccess());
            RedstoneDiceBlockEntity restored = new RedstoneDiceBlockEntity(dice.getBlockPos(), dice.getBlockState());
            loadDice(helper, restored, saved);
            helper.getLevel().setBlockEntity(restored);
            restored.onLoad();
        });
        helper.runAfterDelay(7, () -> {
            RedstoneDiceBlockEntity restored = helper.getBlockEntity(POS, RedstoneDiceBlockEntity.class);
            helper.assertTrue(restored.getOutput() == 0, "恢复投掷时不能提前更新输出");
        });
        helper.runAfterDelay(9, () -> {
            RedstoneDiceBlockEntity restored = helper.getBlockEntity(POS, RedstoneDiceBlockEntity.class);
            helper.assertTrue(restored.getOutput() == restored.getScenario(false), "恢复后必须完成原来的投掷");
            helper.assertTrue(!restored.saveWithoutMetadata(helper.getLevel().registryAccess()).getBooleanOr("Rolling", true),
                "投掷结束必须解锁");
            helper.succeed();
        });
    }

    private static ServerPlayer playerNear(GameTestHelper helper) {
        ServerPlayer player = FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.randomUUID(), "PortTester"));
        player.setPos(helper.absolutePos(POS).getCenter());
        helper.getLevel().addNewPlayer(player);
        return player;
    }

    private static void assertPressed(GameTestHelper helper, boolean pressed) {
        helper.assertTrue(helper.getBlockState(POS).getValue(BigRedButtonBlock.PRESSED) == pressed, "按钮按压状态错误");
        int signal = helper.getBlockState(POS).getSignal(helper.getLevel(), helper.absolutePos(POS), net.minecraft.core.Direction.UP);
        helper.assertTrue(signal == (pressed ? 15 : 0), "按钮红石强度错误");
    }

    private static void buttonHolders(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.BIG_RED_BUTTON.get());
        BigRedButtonBlockEntity button = helper.getBlockEntity(POS, BigRedButtonBlockEntity.class);
        ServerPlayer first = playerNear(helper);
        ServerPlayer second = playerNear(helper);
        button.press(first);
        button.press(second);
        button.release(first);
        button.checkPressed();
        assertPressed(helper, true);
        second.setPos(second.position().add(30, 0, 0));
        button.checkPressed();
        assertPressed(helper, false);
        button.press(first);
        button.release(first);
        button.checkPressed();
        assertPressed(helper, false);
        helper.getLevel().removePlayerImmediately(first, Entity.RemovalReason.DISCARDED);
        helper.getLevel().removePlayerImmediately(second, Entity.RemovalReason.DISCARDED);
        helper.succeed();
    }

    private static void buttonTimeoutReload(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.BIG_RED_BUTTON.get());
        BigRedButtonBlockEntity button = helper.getBlockEntity(POS, BigRedButtonBlockEntity.class);
        ServerPlayer player = playerNear(helper);
        button.press(player);
        helper.runAfterDelay(19, () -> {
            assertPressed(helper, true);
            button.press(player);
        });
        helper.runAfterDelay(22, () -> assertPressed(helper, true));
        helper.runAfterDelay(41, () -> {
            button.checkPressed();
            assertPressed(helper, false);
            button.press(player);
            BigRedButtonBlockEntity restored = new BigRedButtonBlockEntity(button.getBlockPos(), button.getBlockState());
            helper.getLevel().setBlockEntity(restored);
            restored.onLoad();
        });
        helper.runAfterDelay(44, () -> {
            assertPressed(helper, false);
            helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            helper.succeed();
        });
    }

    private static void buttonSneakRod(GameTestHelper helper) {
        helper.setBlock(POS, ModBlocks.BIG_RED_BUTTON.get());
        var button = helper.getBlockEntity(POS, BigRedButtonBlockEntity.class);
        var player = playerNear(helper);
        var pos = helper.absolutePos(POS);
        var hit = new BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false);
        try {
            player.setShiftKeyDown(true);
            var state = helper.getBlockState(POS);
            helper.assertTrue(state.useWithoutItem(helper.getLevel(), player, hit) == InteractionResult.PASS,
                "Sneaking empty-hand use passes to placement");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
            helper.assertTrue(state.useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND, hit)
                == InteractionResult.PASS, "Sneaking item use passes to placement");
            button.press(player);
            assertPressed(helper, false);
            player.setShiftKeyDown(false);
            button.press(player);
            assertPressed(helper, true);
            player.setShiftKeyDown(true);
            button.checkPressed();
            assertPressed(helper, false);
            player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.BUILDING_ROD.asStack());
            button.press(player);
            assertPressed(helper, false);
            helper.assertTrue(helper.getBlockState(POS).useWithoutItem(helper.getLevel(), player, hit) == InteractionResult.PASS,
                "Building rod does not trigger block use");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            button.press(player);
            assertPressed(helper, true);
            player.setItemInHand(InteractionHand.MAIN_HAND, ModItems.BUILDING_ROD.asStack());
            button.checkPressed();
            assertPressed(helper, false);
            helper.setBlock(POS.above(2), ModBlocks.CELESTIAL_FORGING_ANVIL_PORTAL.getDefaultState());
            helper.assertTrue(!AnvilHammerItem.ableToUseAnvilHammer(helper.getLevel(), pos.above(2), player),
                "Celestial anvil portal cannot be rotated by an anvil hammer");
        } finally {
            helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
        }
        helper.succeed();
    }
}
