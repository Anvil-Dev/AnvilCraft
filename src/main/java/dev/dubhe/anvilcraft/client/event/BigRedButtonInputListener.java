package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.BigRedButtonBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import dev.dubhe.anvilcraft.network.BigRedButtonHoldPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class BigRedButtonInputListener {
    private static @Nullable BlockPos heldPos;
    private static @Nullable ClientLevel heldLevel;
    private static int heartbeatTicks;
    private static int holdId;
    private static boolean confirmed;
    private static final float HELD_SWING_PROGRESS = 0.125f;
    private static @Nullable ClientLevel animationLevel;
    private static float previousSwingProgress;
    private static float swingProgress;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        BlockHitResult hit = targetedButton(minecraft);
        if (hit == null) return;
        BlockPos pos = hit.getBlockPos();
        event.setCanceled(true);
        event.setSwingHand(false);
        if (pos.equals(heldPos) && minecraft.level == heldLevel) return;
        release();
        heldPos = pos;
        heldLevel = minecraft.level;
        heartbeatTicks = 0;
        PacketDistributor.sendToServer(new BigRedButtonHoldPacket(pos, hit.getLocation(), true, holdId));
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Post event) {
        if (event.getAction() == GLFW.GLFW_RELEASE
            && Minecraft.getInstance().options.keyUse.matchesMouse(event.getButton())) {
            release();
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() == GLFW.GLFW_RELEASE
            && Minecraft.getInstance().options.keyUse.matches(event.getKey(), event.getScanCode())) {
            release();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        if (heldPos == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        BlockHitResult hit = targetedButton(minecraft);
        if (minecraft.level != heldLevel || !minecraft.options.keyUse.isDown()
            || hit == null || !heldPos.equals(hit.getBlockPos())) {
            release();
            return;
        }
        if (++heartbeatTicks >= 5) {
            heartbeatTicks = 0;
            PacketDistributor.sendToServer(new BigRedButtonHoldPacket(heldPos, hit.getLocation(), true, holdId));
        }
    }

    @SubscribeEvent
    public static void onAnimationTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != animationLevel || minecraft.player == null) {
            animationLevel = minecraft.level;
            previousSwingProgress = 0;
            swingProgress = 0;
        }
        previousSwingProgress = swingProgress;
        boolean pressed = confirmed && heldPos != null && minecraft.player != null
            && minecraft.level == heldLevel && minecraft.level != null
            && minecraft.level.getBlockState(heldPos).is(ModBlocks.BIG_RED_BUTTON)
            && minecraft.level.getBlockState(heldPos).getValue(BigRedButtonBlock.PRESSED);
        swingProgress = Mth.approach(swingProgress, pressed ? HELD_SWING_PROGRESS : 0, HELD_SWING_PROGRESS / 2);
    }

    public static void handleHoldResult(BlockPos pos, int id, boolean accepted) {
        if (id != holdId || !pos.equals(heldPos) || Minecraft.getInstance().level != heldLevel) return;
        confirmed = accepted;
    }

    public static float getHandSwingProgress(InteractionHand hand, float partialTick, float vanillaProgress) {
        if (hand != InteractionHand.MAIN_HAND || (swingProgress == 0 && previousSwingProgress == 0)) {
            return vanillaProgress;
        }
        return Mth.lerp(partialTick, previousSwingProgress, swingProgress);
    }

    @Nullable
    private static BlockHitResult targetedButton(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null || !minecraft.isWindowActive()
            || !minecraft.player.isAlive() || minecraft.player.isSpectator() || minecraft.player.isShiftKeyDown()
            || BuildingRodItem.isHeld(minecraft.player)) return null;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        return minecraft.level.getBlockState(hit.getBlockPos()).is(ModBlocks.BIG_RED_BUTTON) ? hit : null;
    }

    private static void release() {
        Minecraft minecraft = Minecraft.getInstance();
        if (heldPos != null && minecraft.level == heldLevel && minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new BigRedButtonHoldPacket(heldPos, Vec3.ZERO, false, holdId));
        }
        heldPos = null;
        heldLevel = null;
        holdId++;
        confirmed = false;
        heartbeatTicks = 0;
    }
}
