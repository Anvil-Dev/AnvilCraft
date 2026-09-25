package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.hud.EnergyWeaponUseHUD;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.network.WeaponChargeProgressPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

import java.lang.reflect.Field;

public final class WeaponProgressScene {
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static double pausedElapsed;

    public static void frame(Minecraft client) {
        if (deadline == 0) {
            client.screen.onClose();
            deadline = System.currentTimeMillis() + 120000;
        }
        if (failure != null || System.currentTimeMillis() > deadline) {
            client.options.keyUse.setDown(false);
            throw new IllegalStateException("服务器蓄力 HUD 验证失败，阶段 " + stage, failure);
        }
        if (capturing || System.currentTimeMillis() < next) return;
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var server = client.getSingleplayerServer();
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.setNoGravity(true);
                        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                            player.getInventory().setItem(slot, ItemStack.EMPTY);
                        }
                        var weapon = ModItems.ANVIL_RAILGUN.asStack();
                        weapon.set(ModComponents.RAILGUN_AMMO, ChargedProjectiles.of(new ItemStackTemplate(Blocks.ANVIL.asItem())));
                        player.getInventory().setItem(0, weapon);
                        player.getInventory().setSelectedSlot(0);
                        player.inventoryMenu.broadcastChanges();
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 100.5 100 100.5 180 0");
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                        server.tickRateManager().setTickRate(10);
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(1, 1000);
            }
            case 1 -> {
                if (!prepared || !client.player.getInventory().getItem(0).is(ModItems.ANVIL_RAILGUN)) return;
                client.player.getInventory().setSelectedSlot(0);
                client.options.keyUse.setDown(true);
                client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
                advance(2, 0);
            }
            case 2 -> {
                var sample = sample();
                if (sample == null || sample.elapsed() < 3 || sample.duration() <= 0) return;
                require(sample.item() == BuiltInRegistries.ITEM.getId(ModItems.ANVIL_RAILGUN.get())
                    && sample.hand() == 0 && sample.tickRate() == 10 && sample.repeating(), "实际网络样本必须与当前武器、手和服务器速率一致");
                capture(client, "server-progress", 3);
            }
            case 3 -> {
                client.pauseGame(false);
                advance(4, 500);
            }
            case 4 -> {
                require((boolean) field("paused"), "暂停菜单应冻结预测时钟");
                pausedElapsed = (double) field("pausedElapsed");
                advance(5, 500);
            }
            case 5 -> {
                require((double) field("pausedElapsed") == pausedElapsed, "暂停期间预测时间不能继续累加");
                client.setScreen(null);
                client.options.keyUse.setDown(true);
                advance(6, 100);
            }
            case 6 -> {
                require(!(boolean) field("paused"), "关闭暂停菜单应恢复预测");
                client.options.keyUse.setDown(false);
                client.gameMode.releaseUsingItem(client.player);
                advance(7, 300);
            }
            case 7 -> {
                require(sample() == null, "停止使用武器后应丢弃旧进度");
                client.getSingleplayerServer().execute(() -> client.getSingleplayerServer().tickRateManager().setTickRate(20));
                AnvilCraft.LOGGER.info("PORT_WEAPON_PROGRESS_PASSED: actual 10 TPS sample, HUD rendering, pause/resume and stop cleanup");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown progress stage " + stage);
        }
    }

    private static WeaponChargeProgressPacket sample() {
        return (WeaponChargeProgressPacket) field("sample");
    }

    private static Object field(String name) {
        try {
            Field field = EnergyWeaponUseHUD.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(null);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int value, long delay) {
        AnvilCraft.LOGGER.info("PORT_WEAPON_PROGRESS_STAGE: {} -> {}", stage, value);
        stage = value;
        next = System.currentTimeMillis() + delay;
    }

    private static void capture(Minecraft client, String name, int nextStage) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "weapon-progress-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(nextStage, 0);
            }));
    }
}
