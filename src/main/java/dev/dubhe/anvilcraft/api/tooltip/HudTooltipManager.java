package dev.dubhe.anvilcraft.api.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.tooltip.impl.AffectRangeProviderImpl;
import dev.dubhe.anvilcraft.api.tooltip.impl.HeliostatsTooltip;
import dev.dubhe.anvilcraft.api.tooltip.impl.HeliostatsTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.InjectedBlockEntityTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.InjectedBlockTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.PowerComponentTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.RubyPrismTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.SpaceOvercompressorTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.IAffectRangeProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.IBlockEntityTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHandHeldItemTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper.renderOutline;
import static dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper.renderTooltipWithItemIcon;

public class HudTooltipManager {
    public static final HudTooltipManager INSTANCE = new HudTooltipManager();
    private static final int BACKGROUND_COLOR = 0xCC100010;
    private static final int BORDER_COLOR_TOP = 0x505000ff;
    private static final int BORDER_COLOR_BOTTOM = 0x5028007f;
    private final List<ITooltipProvider.BlockTooltipProvider> blockProviders = new ArrayList<>();
    private final List<ITooltipProvider.BlockEntityTooltipProvider> blockEntityProviders = new ArrayList<>();
    private final List<IAffectRangeProvider> affectRangeProviders = new ArrayList<>();
    private final List<IHandHeldItemTooltipProvider> handItemProviders = new ArrayList<>();

    static {
        INSTANCE.registerBlockEntityTooltip(new PowerComponentTooltipProvider());
        INSTANCE.registerAffectRange(new AffectRangeProviderImpl());
        INSTANCE.registerBlockEntityTooltip(new RubyPrismTooltipProvider());
        INSTANCE.registerHandHeldItemTooltip(new HeliostatsTooltip());
        INSTANCE.registerBlockEntityTooltip(new HeliostatsTooltipProvider());
        INSTANCE.registerBlockEntityTooltip(new SpaceOvercompressorTooltipProvider());
        INSTANCE.registerHandHeldItemTooltip(ModItems.STRUCTURE_TOOL.get());
        INSTANCE.registerBlockTooltip(new InjectedBlockTooltipProvider());
        INSTANCE.registerBlockEntityTooltip(new InjectedBlockEntityTooltipProvider());
    }

    private void registerAffectRange(AffectRangeProviderImpl affectRangeProvider) {
        affectRangeProviders.add(affectRangeProvider);
    }

    private void registerBlockTooltip(ITooltipProvider.BlockTooltipProvider provider) {
        blockProviders.add(provider);
    }

    @SuppressWarnings("removal")
    @Deprecated(since = "1.4.2", forRemoval = true)
    private void registerBlockEntityTooltip(IBlockEntityTooltipProvider provider) {
        blockEntityProviders.add(provider.toNewImplementation());
    }

    private void registerBlockEntityTooltip(ITooltipProvider.BlockEntityTooltipProvider provider) {
        blockEntityProviders.add(provider);
    }

    private void registerHandHeldItemTooltip(IHandHeldItemTooltipProvider provider) {
        handItemProviders.add(provider);
    }

    /**
     * 渲染方块的tooltip
     */
    public void renderTooltip(
        GuiGraphics guiGraphics,
        BlockState state,
        float partialTick,
        int screenWidth,
        int screenHeight
    ) {
        if (state == null) return;
        final int tooltipPosX = screenWidth / 2 + 10;
        final int tooltipPosY = screenHeight / 2 + 10;
        Font font = Minecraft.getInstance().font;
        ITooltipProvider.BlockTooltipProvider currentProvider = determineBlockTooltipProvider(state);
        if (currentProvider == null) return;
        List<Component> tooltip = currentProvider.tooltip(state);
        if (tooltip == null || tooltip.isEmpty()) return;
        renderTooltipWithItemIcon(
            guiGraphics,
            font,
            currentProvider.icon(state),
            tooltip,
            tooltipPosX,
            tooltipPosY,
            BACKGROUND_COLOR,
            BORDER_COLOR_TOP,
            BORDER_COLOR_BOTTOM);
    }

    /**
     * 渲染方块实体的tooltip
     */
    public void renderTooltip(
        GuiGraphics guiGraphics,
        BlockEntity entity,
        float partialTick,
        int screenWidth,
        int screenHeight
    ) {
        if (entity == null) return;
        final int tooltipPosX = screenWidth / 2 + 10;
        final int tooltipPosY = screenHeight / 2 + 10;
        Font font = Minecraft.getInstance().font;
        ITooltipProvider.BlockEntityTooltipProvider currentProvider = determineBlockEntityTooltipProvider(entity);
        if (currentProvider == null) return;
        List<Component> tooltip = currentProvider.tooltip(entity);
        if (tooltip == null || tooltip.isEmpty()) return;
        renderTooltipWithItemIcon(
            guiGraphics,
            font,
            currentProvider.icon(entity),
            tooltip,
            tooltipPosX,
            tooltipPosY,
            BACKGROUND_COLOR,
            BORDER_COLOR_TOP,
            BORDER_COLOR_BOTTOM);
    }

    /**
     * 渲染手持物品Tooltip
     */
    public void renderHandItemLevelTooltip(
        ItemStack itemStack,
        PoseStack poseStack,
        VertexConsumer consumer,
        double camX,
        double camY,
        double camZ
    ) {
        IHandHeldItemTooltipProvider pv = determineHandHeldItemTooltipProvider(itemStack);
        if (pv == null) return;
        pv.render(poseStack, consumer, itemStack, camX, camY, camZ);
    }

    /**
     * 渲染手持物品Hud Tooltip
     */
    public void renderHandItemHudTooltip(
        GuiGraphics guiGraphics,
        ItemStack itemStack,
        float partialTick,
        int screenWidth,
        int screenHeight
    ) {
        IHandHeldItemTooltipProvider pv = determineHandHeldItemTooltipProvider(itemStack);
        if (pv == null) return;
        pv.renderTooltip(guiGraphics, screenWidth, screenHeight);
    }

    /**
     * 渲染作用范围
     */
    public void renderAffectRange(
        BlockEntity entity,
        PoseStack poseStack,
        VertexConsumer consumer,
        double camX,
        double camY,
        double camZ
    ) {
        IAffectRangeProvider currentProvider = determineAffectRangeProvider(entity);
        if (currentProvider == null) return;
        VoxelShape shape = currentProvider.affectRange(entity);
        if (shape == null) return;
        renderOutline(poseStack, consumer, camX, camY, camZ, BlockPos.ZERO, shape, 0xff00ffcc);
    }

    private IHandHeldItemTooltipProvider determineHandHeldItemTooltipProvider(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return null;
        return handItemProviders.stream()
            .filter(it -> it.accepts(itemStack))
            .min(Comparator.comparingInt(IHandHeldItemTooltipProvider::priority))
            .orElse(null);
    }

    private ITooltipProvider.BlockTooltipProvider determineBlockTooltipProvider(BlockState state) {
        if (state == null) return null;
        return blockProviders.stream()
            .filter(it -> it.accepts(state))
            .min(Comparator.comparingInt(ITooltipProvider::priority))
            .orElse(null);
    }

    private ITooltipProvider.BlockEntityTooltipProvider determineBlockEntityTooltipProvider(BlockEntity entity) {
        if (entity == null) return null;
        return blockEntityProviders.stream()
            .filter(it -> it.accepts(entity))
            .min(Comparator.comparingInt(ITooltipProvider::priority))
            .orElse(null);
    }

    private IAffectRangeProvider determineAffectRangeProvider(BlockEntity entity) {
        if (entity == null) return null;
        return affectRangeProviders.stream()
            .filter(it -> it.accepts(entity))
            .min(Comparator.comparingInt(IAffectRangeProvider::priority))
            .orElse(null);
    }
}
