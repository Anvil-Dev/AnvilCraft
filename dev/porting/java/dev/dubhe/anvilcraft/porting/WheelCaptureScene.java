package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.wheel.client.gui.screen.WheelScreen;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.WheelBackgroundCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.lwjgl.glfw.GLFW;

import java.util.Map;

public final class WheelCaptureScene {
    private static int stage;
    private static long deadline;
    private static long next;
    private static boolean capturing;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 90000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("轮盘采样验证超时：" + stage);
        if (capturing || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.options.hideGui = true;
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 200.5 400 200.5 180 -90");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set 6000");
                });
                advance(1);
            }
            case 1 -> {
                if (client.player.getY() < 399 || client.player.getXRot() > -89) return;
                TerminalBalanceScene.key(GLFW.GLFW_PRESS);
                advance(2);
            }
            case 2 -> {
                if (!(client.screen instanceof WheelScreen)) return;
                TerminalBalanceScene.point(client, 0);
                advance(3);
            }
            case 3 -> capture(client, "early", 4);
            case 4 -> {
                advance(5);
                next += 3000;
            }
            case 5 -> capture(client, "settled", 6);
            case 6 -> {
                client.schedule(() -> GLFW.glfwSetWindowSize(client.getWindow().handle(), 1024, 768));
                advance(7);
            }
            case 7 -> {
                if (client.getMainRenderTarget().width != 1024 || client.getMainRenderTarget().height != 768
                    || !(client.screen instanceof WheelScreen)) return;
                TerminalBalanceScene.point(client, 1);
                advance(8);
            }
            case 8 -> capture(client, "resized", 9);
            case 9 -> {
                client.schedule(() -> GLFW.glfwSetWindowSize(client.getWindow().handle(), 1280, 720));
                advance(10);
            }
            case 10 -> {
                if (client.getMainRenderTarget().width != 1280 || client.getMainRenderTarget().height != 720
                    || !(client.screen instanceof WheelScreen)) return;
                TerminalBalanceScene.point(client, 0);
                advance(11);
            }
            case 11 -> capture(client, "restored", 12);
            case 12 -> {
                TerminalBalanceScene.key(GLFW.GLFW_RELEASE);
                advance(13);
            }
            case 13 -> {
                if (client.screen != null) return;
                TerminalBalanceScene.key(GLFW.GLFW_PRESS);
                advance(14);
            }
            case 14 -> {
                if (!(client.screen instanceof WheelScreen)) return;
                TerminalBalanceScene.point(client, 0);
                advance(15);
            }
            case 15 -> capture(client, "reopened", 16);
            case 16 -> {
                TerminalBalanceScene.key(GLFW.GLFW_RELEASE);
                client.options.hideGui = false;
                advance(17);
            }
            case 17 -> {
                if (client.screen != null) return;
                client.pauseGame(false);
                advance(18);
            }
            case 18 -> {
                if (!client.isPaused()) return;
                verifyDrained();
                capture(client, "normal-menu", 19);
            }
            case 19 -> {
                AnvilCraft.LOGGER.info("PORT_WHEEL_CAPTURE_PASSED: background, resize, close/reopen, native menu and drained frame queue");
                client.stop();
            }
            default -> throw new IllegalStateException("Unknown wheel capture stage " + stage);
        }
    }

    private static void verifyDrained() {
        try {
            var pending = WheelBackgroundCapture.class.getDeclaredField("PENDING");
            pending.setAccessible(true);
            if (!((Map<?, ?>) pending.get(null)).isEmpty() || WheelBackgroundCapture.isRefreshing()) {
                throw new IllegalStateException("GUI 绘制完成后不能残留本帧捕获任务");
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void advance(int value) {
        AnvilCraft.LOGGER.info("PORT_WHEEL_CAPTURE_STAGE: {} -> {}", stage, value);
        stage = value;
        next = System.currentTimeMillis() + 1000;
    }

    private static void capture(Minecraft client, String name, int nextStage) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "wheel-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(nextStage);
            }));
    }
}
