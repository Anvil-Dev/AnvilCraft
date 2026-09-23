package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.event.LargeBlockPlacePreviewEventListener;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.MultiPartPreviewMode;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class PlacementPreviewClientScene {
    private static final BlockPos FLOOR = new BlockPos(8, 80, 8);
    private static final BlockPos ANVIL = new BlockPos(4, 81, 8);
    private static int stage;
    private static int frames;
    private static int verifiedStage = -1;
    private static boolean capturing;
    private static boolean ready;

    public static void frame(Minecraft mc) {
        if (mc.screen != null || !(mc.level.getBlockEntity(ANVIL) instanceof CelestialForgingAnvilBlockEntity)) return;
        if (stage >= 6) {
            AnvilCraft.LOGGER.info("PORT_PLACEMENT_PREVIEW_PASSED: outline, ghost, off, independent amplifier warning, "
                + "large cake outline/ghost");
            mc.stop();
            return;
        }
        mc.player.setPos(8.5, stage >= 4 ? 82.0 : 81.6, stage >= 4 ? 12.0 : 11.5);
        if (stage >= 4) mc.options.fov().set(90);
        mc.player.setYRot(180);
        mc.player.setXRot(36.5F);
        var item = stage >= 4 ? ModBlocks.LARGE_CAKE.asItem() : ModBlocks.SMART_BLOCK_PLACER.asItem();
        mc.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        mc.options.hideGui = true;
        AnvilCraftClient.CONFIG.multiPartPreviewMode = stage == 0 || stage == 4 ? MultiPartPreviewMode.OUTLINE
            : stage == 1 || stage == 5 ? MultiPartPreviewMode.GHOST : MultiPartPreviewMode.OFF;
        if (stage == 3) LargeBlockPlacePreviewEventListener.offerMissingAmplifierAnvil(ANVIL);
        if (stage >= 4) LargeBlockPlacePreviewEventListener.removeMissingAmplifierAnvil(ANVIL);
        ready = true;
        if (capturing || ++frames < 60 || verifiedStage != stage) return;
        capturing = true;
        Screenshot.grab(mc.gameDirectory, "placement-preview-26.1-" + stage + ".png", mc.getMainRenderTarget(), 1, message -> {
            AnvilCraft.LOGGER.info("PORT_PLACEMENT_PREVIEW_CAPTURED: {}", message.getString());
            mc.execute(() -> {
                stage++;
                frames = 0;
                ready = false;
                capturing = false;
            });
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void target(ExtractLevelRenderStateEvent event) {
        if (!Boolean.getBoolean("anvilcraft.portPlacementPreviewScene") || !ready) return;
        Minecraft.getInstance().hitResult = new BlockHitResult(new Vec3(8.5, 81, 8.5), Direction.UP, FLOOR, false);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void verify(ExtractLevelRenderStateEvent event) throws ReflectiveOperationException {
        if (!Boolean.getBoolean("anvilcraft.portPlacementPreviewScene") || !ready || frames < 10) return;
        var field = LargeBlockPlacePreviewEventListener.class.getDeclaredField("PREVIEW");
        field.setAccessible(true);
        Object snapshot = event.getRenderState().getRenderData((ContextKey<?>) field.get(null));
        if (snapshot == null) throw new IllegalStateException("缺少放置预览渲染快照");
        var outlines = snapshot.getClass().getDeclaredMethod("outlines");
        var ghosts = snapshot.getClass().getDeclaredMethod("ghosts");
        outlines.setAccessible(true);
        ghosts.setAccessible(true);
        int outlineCount = ((List<?>) outlines.invoke(snapshot)).size();
        int ghostCount = ((List<?>) ghosts.invoke(snapshot)).size();
        int expectedOutline = stage == 0 ? 1 : stage == 3 ? 4 : stage == 4 ? 27 : 0;
        int expectedGhost = stage == 1 ? 5 : stage == 5 ? 27 : 0;
        if (outlineCount != expectedOutline || ghostCount != expectedGhost) {
            throw new IllegalStateException("预览阶段 " + stage + " 数量不符: outlines=" + outlineCount + ", ghosts=" + ghostCount);
        }
        verifiedStage = stage;
    }
}
