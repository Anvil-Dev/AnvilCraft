package dev.dubhe.anvilcraft.api.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.tooltip.impl.AffectRangeProviderImpl;
import dev.dubhe.anvilcraft.api.tooltip.impl.HeliostatsTooltip;
import dev.dubhe.anvilcraft.api.tooltip.impl.HeliostatsTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.PowerComponentTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.RubyPrismTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.AffectRangeProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.BlockEntityTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.HandHeldItemTooltipProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper.renderOutline;
import static dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper.renderTooltipWithItemIcon;

public class HudTooltipManager {
    public static final HudTooltipManager INSTANCE = new HudTooltipManager();
    private static final int BACKGROUND_COLOR = 0xCC100010;
    private static final int BORDER_COLOR_TOP = 0x505000ff;
    private static final int BORDER_COLOR_BOTTOM = 0x5028007f;
    private final List<BlockEntityTooltipProvider> blockEntityProviders = new ArrayList<>();
    private final List<AffectRangeProvider> affectRangeProviders = new ArrayList<>();
    private final List<HandHeldItemTooltipProvider> handItemProviders = new ArrayList<>();

    static {
        INSTANCE.registerBlockEntityTooltip(new PowerComponentTooltipProvider());
        INSTANCE.registerAffectRange(new AffectRangeProviderImpl());
        INSTANCE.registerBlockEntityTooltip(new RubyPrismTooltipProvider());
        INSTANCE.registerHandHeldItemTooltip(new HeliostatsTooltip());
        INSTANCE.registerBlockEntityTooltip(new HeliostatsTooltipProvider());
    }

    private void registerAffectRange(AffectRangeProviderImpl affectRangeProvider) {
        affectRangeProviders.add(affectRangeProvider);
    }

    private void registerBlockEntityTooltip(BlockEntityTooltipProvider provider) {
        blockEntityProviders.add(provider);
    }

    private void registerHandHeldItemTooltip(HandHeldItemTooltipProvider provider) {
        handItemProviders.add(provider);
    }

    /**
     * 渲染
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
        BlockEntityTooltipProvider currentProvider = determineBlockEntityTooltipProvider(entity);
        if (currentProvider == null) return;
        renderTooltipWithItemIcon(
                guiGraphics,
                font,
                currentProvider.icon(entity),
                currentProvider.tooltip(entity),
                tooltipPosX,
                tooltipPosY,
                BACKGROUND_COLOR,
                BORDER_COLOR_TOP,
                BORDER_COLOR_BOTTOM
        );
    }

    /**
     * 渲染手持物品Tooltip
     */
    public void renderHandItemLevelTooltip(
            ItemStack itemStack,
            PoseStack poseStack,
            VertexConsumer consumer,
            double camX, double camY, double camZ
    ) {
        HandHeldItemTooltipProvider pv = determineHandHeldItemTooltipProvider(itemStack);
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
        HandHeldItemTooltipProvider pv = determineHandHeldItemTooltipProvider(itemStack);
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
            double camX, double camY, double camZ
    ) {
        AffectRangeProvider currentProvider = determineAffectRangeProvider(entity);
        if (currentProvider == null) return;
        VoxelShape shape = currentProvider.affectRange(entity);
        if (shape == null) return;
        renderOutline(
                poseStack,
                consumer,
                camX,
                camY,
                camZ,
                BlockPos.ZERO,
                shape,
                0xff00ffcc
        );
    }

    private HandHeldItemTooltipProvider determineHandHeldItemTooltipProvider(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) return null;
        ArrayList<HandHeldItemTooltipProvider> pv = handItemProviders.stream()
                .filter(it -> it.accepts(itemStack))
                .sorted(Comparator.comparingInt(HandHeldItemTooltipProvider::priority))
                .collect(Collectors.toCollection(ArrayList::new));
        if (pv.isEmpty()) return null;
        return pv.get(0);
    }

    private BlockEntityTooltipProvider determineBlockEntityTooltipProvider(BlockEntity entity) {
        if (entity == null) return null;
        ArrayList<BlockEntityTooltipProvider> blockEntityTooltipProviders = blockEntityProviders.stream()
                .filter(it -> it.accepts(entity))
                .sorted(Comparator.comparingInt(BlockEntityTooltipProvider::priority))
                .collect(Collectors.toCollection(ArrayList::new));
        if (blockEntityTooltipProviders.isEmpty()) return null;
        return blockEntityTooltipProviders.get(0);
    }

    private AffectRangeProvider determineAffectRangeProvider(BlockEntity entity) {
        if (entity == null) return null;
        ArrayList<AffectRangeProvider> pv = affectRangeProviders.stream()
                .filter(it -> it.accepts(entity))
                .sorted(Comparator.comparingInt(AffectRangeProvider::priority))
                .collect(Collectors.toCollection(ArrayList::new));
        if (pv.isEmpty()) return null;
        return pv.get(0);
    }

}
