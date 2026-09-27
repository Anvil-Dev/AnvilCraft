package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** 为双版本持握对照固定光照输入，不改变生产渲染。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class ReferenceHandLighting {
    private static final Method RENDER = findRender();
    private static final Field USE_REMAINING = findUseRemaining();

    private static Field findUseRemaining() {
        try {
            var field = LivingEntity.class.getDeclaredField("useItemRemaining");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Missing reference use clock", error);
        }
    }

    private static void useRemaining(LivingEntity entity, int ticks) {
        try {
            USE_REMAINING.setInt(entity, ticks);
        } catch (IllegalAccessException error) {
            throw new IllegalStateException("Reference use clock failed", error);
        }
    }

    private static Method findRender() {
        try {
            var method = ItemInHandRenderer.class.getDeclaredMethod("renderArmWithItem", AbstractClientPlayer.class,
                float.class, float.class, InteractionHand.class, float.class, ItemStack.class, float.class,
                PoseStack.class, SubmitNodeCollector.class, int.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Missing reference hand entry point", error);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void render(RenderHandEvent event) {
        if (!Boolean.getBoolean("anvilcraft.portCrabClawScene")) return;
        var client = Minecraft.getInstance();
        event.setCanceled(true);
        if (CrabClawClientScene.hidingHands()) return;
        int remaining = client.player.getUseItemRemainingTicks();
        boolean using = client.player.isUsingItem() && client.player.getUsedItemHand() == event.getHand();
        try {
            if (using) useRemaining(client.player, Math.max(1, event.getItemStack().getUseDuration(client.player) - 20));
            RENDER.invoke(client.gameRenderer.itemInHandRenderer, client.player, 0.0F, event.getInterpolatedPitch(),
                event.getHand(), event.getSwingProgress(), event.getItemStack(), event.getEquipProgress(),
                event.getPoseStack(), event.getSubmitNodeCollector(), 0xF000F0);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Reference hand rendering failed", error);
        } finally {
            if (using) useRemaining(client.player, remaining);
        }
    }
}
