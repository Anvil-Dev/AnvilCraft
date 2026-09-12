package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.network.BigRedButtonHoldPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
    private static final float HELD_SWING_PROGRESS = 0.125f;
    private static @Nullable ClientLevel animationLevel;
    private static float previousSwingProgress;
    private static float swingProgress;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUse(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem()) return;
        Minecraft minecraft = Minecraft.getInstance();
        BlockPos pos = targetedButton(minecraft);
        if (pos == null) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (pos.equals(heldPos) && minecraft.level == heldLevel) return;
        release();
        heldPos = pos;
        heldLevel = minecraft.level;
        heartbeatTicks = 0;
        PacketDistributor.sendToServer(new BigRedButtonHoldPacket(pos, true));
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
        if (minecraft.level != heldLevel || !minecraft.options.keyUse.isDown()
            || !heldPos.equals(targetedButton(minecraft))) {
            release();
            return;
        }
        if (++heartbeatTicks >= 5) {
            heartbeatTicks = 0;
            PacketDistributor.sendToServer(new BigRedButtonHoldPacket(heldPos, true));
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
        swingProgress = Mth.approach(swingProgress, heldPos == null ? 0 : HELD_SWING_PROGRESS, HELD_SWING_PROGRESS / 2);
    }

    public static float getHandSwingProgress(InteractionHand hand, float partialTick, float vanillaProgress) {
        if (hand != InteractionHand.MAIN_HAND || (swingProgress == 0 && previousSwingProgress == 0)) {
            return vanillaProgress;
        }
        return Mth.lerp(partialTick, previousSwingProgress, swingProgress);
    }

    @Nullable
    private static BlockPos targetedButton(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null || !minecraft.isWindowActive()
            || !minecraft.player.isAlive() || minecraft.player.isSpectator()) return null;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return null;
        return minecraft.level.getBlockState(hit.getBlockPos()).is(ModBlocks.BIG_RED_BUTTON) ? hit.getBlockPos() : null;
    }

    private static void release() {
        Minecraft minecraft = Minecraft.getInstance();
        if (heldPos != null && minecraft.level == heldLevel && minecraft.getConnection() != null) {
            PacketDistributor.sendToServer(new BigRedButtonHoldPacket(heldPos, false));
        }
        heldPos = null;
        heldLevel = null;
        heartbeatTicks = 0;
    }
}
