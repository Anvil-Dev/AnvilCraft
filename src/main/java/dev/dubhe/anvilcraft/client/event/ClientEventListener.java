package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import dev.dubhe.anvilcraft.api.thought.ThoughtManager;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.client.support.AmuletSelectorSupport;
import dev.dubhe.anvilcraft.client.support.StructureDiskPreviewSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.UsePillBoxPacket;
import dev.dubhe.anvilcraft.util.BlockHighlightUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ClientEventListener {
    @SubscribeEvent
    public static void blockHighlight(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (BlockHighlightUtil.SUBCHUNKS.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockHighlightUtil.render(
            level,
            Minecraft.getInstance().renderBuffers().bufferSource(),
            event.getPoseStack(),
            event.getCamera()
        );
    }

    @SubscribeEvent
    public static void onRenderBlockOverlay(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK
            && (event.getBlockState().is(ModBlocks.ACCELERATION_RING) || event.getBlockState().is(ModBlocks.DEFLECTION_RING))
        ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientPlayerDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        SoundHelper.INSTANCE.clear();
    }

    @SubscribeEvent
    public static void onKeyPress(InputEvent.Key event) {
        if (ModKeyMappings.TOGGLE_GOGGLE.get().isDown()) AnvilHammerItem.goggleEnabled = !AnvilHammerItem.goggleEnabled;
        if (Minecraft.getInstance().level == null) return;

        // 以下是界面部分

        if (event.getKey() == ModKeyMappings.USE_PILL_BOX.get().getKey().getValue()) {
            if (event.getAction() == InputConstants.PRESS) {
                ClientPacketListener connection = Minecraft.getInstance().getConnection();
                if (connection != null) {
                    connection.send(new UsePillBoxPacket());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onScreenKeyPressed(ScreenEvent.KeyPressed.Post event) {
        if (event.getKeyCode() == ModKeyMappings.THOUGHT.get().getKey().getValue()) {
            ThoughtManager.onThought();
        }
    }

    @SubscribeEvent
    public static void onScreenKeyReleased(ScreenEvent.KeyReleased.Post event) {
        if (event.getKeyCode() == ModKeyMappings.THOUGHT.get().getKey().getValue()) {
            ThoughtManager.onEndThought();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        long lastThoughtTime = ThoughtManager.getLastThoughtTime();
        if (lastThoughtTime < 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long curTime = minecraft.gui.getGuiTicks();
        long deltaTime = curTime - lastThoughtTime;
        if (deltaTime > ThoughtManager.getMAX_SECONDS() * 20) {
            ThoughtManager.onPostThought();
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(ScreenEvent.MouseScrolled.Pre event) {
        if (AmuletSelectorSupport.hasHoveringItem()) {
            int amount = (int) event.getScrollDeltaY();
            AmuletSelectorSupport.mouseScrolled(-amount);
            event.setCanceled(true);
        } else if (AnvilCraftClient.pillSelectorSupport.hasItem()) {
            int amount = (int) event.getScrollDeltaY();
            AnvilCraftClient.pillSelectorSupport.mouseScrolled(-amount);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void renderContainerScreenEvent(ContainerScreenEvent.Render.Background event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        Slot slot = screen.getSlotUnderMouse();
        if (slot != null) {
            ItemStack item = slot.getItem();
            if (item.is(ModItems.PILL_BOX)) {
                AnvilCraftClient.pillSelectorSupport.setPillBox(item);
                return;
            }
        }
        AnvilCraftClient.pillSelectorSupport.setPillBox(ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        GuiGraphics guiGraphics = event.getGraphics();
        int x = event.getX();
        int y = event.getY();

        ItemStack itemStack = event.getItemStack();
        if (itemStack.is(ModItems.AMULET_BOX)) {
            event.setY(y + 13);
            AmuletSelectorSupport.setCurrentHoveringItemStack(itemStack);
            AmuletSelectorSupport.render(guiGraphics, x, y);
        } else if (itemStack.is(ModItems.PILL_BOX)) {
            event.setY(y + 13);
            AnvilCraftClient.pillSelectorSupport.render(guiGraphics, x, y);
        } else {
            AmuletSelectorSupport.setCurrentHoveringItemStack(ItemStack.EMPTY);
        }
    }
    
    @SubscribeEvent
    public static void onContainerScreenRenderPost(ScreenEvent.Render.Post event) {
        // 在容器屏幕渲染完成后，渲染结构磁盘的3D预览窗口
        // 这样可以确保预览窗口在所有物品和UI之上
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> containerScreen)) {
            return;
        }
        
        // 获取鼠标悬停的slot
        Slot slot = containerScreen.getSlotUnderMouse();
        if (slot == null || !slot.hasItem()) {
            return;
        }
        
        ItemStack itemStack = slot.getItem();
        if (!itemStack.is(ModItems.STRUCTURE_DISK.get())) {
            return;
        }
        
        // 获取真实鼠标位置
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        int mouseX = (int) (
            minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth() / minecraft.getWindow().getScreenWidth());
        int mouseY = (int) (
            minecraft.mouseHandler.ypos() * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight());
        
        // 渲染预览窗口
        StructureDiskPreviewSupport.renderPreviewAt(
            event.getGuiGraphics(),
            itemStack,
            mouseX,
            mouseY
        );
    }
}
