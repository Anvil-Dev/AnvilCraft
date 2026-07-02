package dev.dubhe.anvilcraft.client.event;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import dev.dubhe.anvilcraft.api.thought.ThoughtManager;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.init.ModAtlasIds;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.client.init.ModTextureAtlases;
import dev.dubhe.anvilcraft.client.support.AmuletSelectorSupport;
import dev.dubhe.anvilcraft.client.support.FilterSelectorSupport;
import dev.dubhe.anvilcraft.client.support.SeismicBounceManager;
import dev.dubhe.anvilcraft.client.support.StructureDiskPreviewSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.state.StorageMenuState;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.UsePillBoxPacket;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import dev.dubhe.anvilcraft.util.BlockHighlightUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterTextureAtlasesEvent;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderItemInFrameEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ClientEventListener {
    @SubscribeEvent
    public static void on(SubmitCustomGeometryEvent event) {
        if (BlockHighlightUtil.SUBCHUNKS.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockHighlightUtil.render(
            level,
            event.getSubmitNodeCollector(),
            event.getPoseStack(),
            event.getLevelRenderState().cameraRenderState
        );
    }

    @SubscribeEvent
    public static void on(RenderItemInFrameEvent event) {
        PoseStack poseStack = event.getPoseStack();
        if (!AnvilCraftClient.CONFIG.verticalItemFrame) return;
        Direction direction = event.getItemFrameRenderState().direction;
        if (direction == Direction.UP) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        } else if (direction == Direction.DOWN) {
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
    }

    @SubscribeEvent
    public static void on(RegisterTextureAtlasesEvent event) {
        event.register(
            new AtlasManager.AtlasConfig(
                ModTextureAtlases.LOCATION_LASER,
                ModAtlasIds.LASER,
                false
            )
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
        RecipesRecord.CLIENTSIDE = null;
        StorageMenuState.clear();
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
        SeismicBounceManager.getInstance().tick();
        dev.dubhe.anvilcraft.client.support.ScreenShakeManager.getInstance().tick();
        long lastThoughtTime = ThoughtManager.getLastThoughtTime();
        if (lastThoughtTime < 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long curTime = minecraft.gui.getGuiTicks();
        long deltaTime = curTime - lastThoughtTime;
        if (deltaTime > ThoughtManager.getMaxSeconds() * 20) {
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
    public static void renderContainerScreenEvent(ContainerScreenEvent.Render.Foreground event) {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        Slot slot = screen.getHoveredSlot();
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
        GuiGraphicsExtractor graphics = event.getGraphics();
        int x = event.getX();
        int y = event.getY();

        ItemStack itemStack = event.getItemStack();
        if (itemStack.is(ModItems.AMULET_BOX)) {
            event.setY(y + 13);
            AmuletSelectorSupport.setCurrentHoveringItemStack(itemStack);
            AmuletSelectorSupport.render(graphics, x, y);
        } else if (itemStack.is(ModItems.PILL_BOX)) {
            event.setY(y + 13);
            AnvilCraftClient.pillSelectorSupport.setPillBox(itemStack);
            AnvilCraftClient.pillSelectorSupport.render(graphics, x, y);
        } else if (itemStack.is(ModItems.FILTER)) {
            event.setY(y + 13);
            FilterSelectorSupport.setCurrentFilterStack(itemStack);
            FilterSelectorSupport.render(graphics, x, y);
        } else if (itemStack.is(ModItems.STRUCTURE_DISK)) {
            StructureDiskPreviewSupport.renderPreviewAt(graphics, itemStack, x, y);
        } else {
            AmuletSelectorSupport.setCurrentHoveringItemStack(ItemStack.EMPTY);
            AnvilCraftClient.pillSelectorSupport.setPillBox(ItemStack.EMPTY);
            FilterSelectorSupport.setCurrentFilterStack(ItemStack.EMPTY);
        }
    }
}
