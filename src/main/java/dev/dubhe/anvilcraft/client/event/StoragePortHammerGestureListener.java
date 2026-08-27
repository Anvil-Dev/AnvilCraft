package dev.dubhe.anvilcraft.client.event;

import dev.anvilcraft.lib.v2.wheel.api.WheelMenuBuilder;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.client.input.WheelScreenController;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.StoragePortBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.StoragePortUnmarkPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;
import javax.annotation.Nullable;

/**
 * 仓储端口的铁砧锤手势：手持铁砧锤对已标记的端口长按右键会打开轮盘，
 * 轮盘中只有一个渲染为屏障的「清除标记」选项，选中即清除标记。
 * 普通铁砧锤右键（短按）不会调整任何状态。
 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class StoragePortHammerGestureListener {
    /** 长按右键多少 tick 后呼出轮盘 */
    private static final long OPEN_WHEEL_DELAY = 4;

    private static final WheelScreenController CONTROLLER = new WheelScreenController();

    private static long pressTick = -1L;
    private static @Nullable BlockPos pressPos = null;
    private static boolean wheelOpened = false;

    private StoragePortHammerGestureListener() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        if (!client.options.keyUse.matchesMouse(event.getButton())) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (client.screen != null || StoragePortHammerGestureListener.wheelOpened) {
                return;
            }
            BlockPos targetedPort = StoragePortHammerGestureListener.targetedMarkedPort(client.player);
            if (targetedPort == null) {
                return;
            }
            StoragePortHammerGestureListener.pressTick = client.level.getGameTime();
            StoragePortHammerGestureListener.pressPos = targetedPort;
            return;
        }
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            if (StoragePortHammerGestureListener.wheelOpened) {
                StoragePortHammerGestureListener.CONTROLLER.onHoldKeyReleased();
            }
            StoragePortHammerGestureListener.reset();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || client.player == null) {
            return;
        }
        // 轮盘打开期间每 tick 终止铁砧锤的「使用」计时（长按 40 tick 会打开便携铁砧），
        // 无论轮盘计时结束时是否仍开启，都不允许便携铁砧被呼出。
        // 仅本地 stopUsingItem 不会通知服务端：服务端的 ServerPlayer 仍会跑完 40 tick
        // 并在 finishUsingItem 里直接打开便携铁砧，因此必须发送 RELEASE_USE_ITEM 包。
        if (StoragePortHammerGestureListener.wheelOpened) {
            LocalPlayer player = client.player;
            if (player.isUsingItem()) {
                player.stopUsingItem();
                Objects.requireNonNull(client.gameMode).releaseUsingItem(player);
            }
            return;
        }
        if (StoragePortHammerGestureListener.pressTick < 0) {
            return;
        }
        if (level.getGameTime() - StoragePortHammerGestureListener.pressTick
            <= StoragePortHammerGestureListener.OPEN_WHEEL_DELAY) {
            return;
        }
        BlockPos pos = StoragePortHammerGestureListener.pressPos;
        if (pos == null) {
            StoragePortHammerGestureListener.reset();
            return;
        }
        StoragePortHammerGestureListener.wheelOpened = true;
        StoragePortHammerGestureListener.CONTROLLER.onHoldKeyPressed(StoragePortHammerGestureListener.buildClearMarkWheel(pos));
        LocalPlayer player = client.player;
        if (player.isUsingItem()) {
            player.stopUsingItem();
            Objects.requireNonNull(client.gameMode).releaseUsingItem(player);
        }
    }

    /**
     * 构建「清除标记」轮盘：仅一个选项，图标渲染为屏障方块。
     */
    private static WheelMenuModel buildClearMarkWheel(BlockPos pos) {
        return WheelMenuBuilder.create()
            .slotsPerPage(1)
            .action(
                "clear_mark",
                Component.translatable("screen.anvilcraft.storage_port.clear_mark"),
                (graphics, pose, width, height) -> graphics.renderItem(new ItemStack(Items.BARRIER), -8, -8),
                ctx -> {
                    PacketDistributor.sendToServer(new StoragePortUnmarkPacket(pos));
                    LocalPlayer player = Minecraft.getInstance().player;
                    if (player != null) {
                        // 选中后终止铁砧锤的长按使用，避免 40 tick 后打开便携铁砧
                        player.stopUsingItem();
                    }
                }
            )
            .build();
    }

    /**
     * 玩家手持铁砧锤且准星指向已标记的仓储端口时返回该端口坐标，否则返回 null。
     */
    @Nullable
    private static BlockPos targetedMarkedPort(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof AnvilHammerItem)) {
            return null;
        }
        Minecraft client = Minecraft.getInstance();
        if (!(client.hitResult instanceof BlockHitResult hitResult)) {
            return null;
        }
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = hitResult.getBlockPos();
        BlockState state = Objects.requireNonNull(client.level).getBlockState(pos);
        if (!state.is(ModBlocks.STORAGE_PORT)) {
            return null;
        }
        return state.getValue(StoragePortBlock.MARKED) ? pos : null;
    }

    private static void reset() {
        StoragePortHammerGestureListener.pressTick = -1L;
        StoragePortHammerGestureListener.pressPos = null;
        StoragePortHammerGestureListener.wheelOpened = false;
    }
}
