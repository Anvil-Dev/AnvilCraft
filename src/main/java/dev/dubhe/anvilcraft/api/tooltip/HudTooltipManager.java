package dev.dubhe.anvilcraft.api.tooltip;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.dubhe.anvilcraft.api.tooltip.impl.AffectRangeProviderImpl;
import dev.dubhe.anvilcraft.api.tooltip.impl.ChargerTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.DeflectionRingTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.HeatCollectorTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.HeatableBlockTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.HeliostatsTooltip;
import dev.dubhe.anvilcraft.api.tooltip.impl.HeliostatsTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.InjectedBlockEntityTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.InjectedBlockTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.PowerComponentTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.PropelPistonTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.RubyPrismTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.impl.SpaceOvercompressorTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.IAffectRangeProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHandHeldItemTooltipProvider;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper.renderOutline;
import static dev.dubhe.anvilcraft.api.tooltip.TooltipRenderHelper.renderTooltipWithItemIcon;

public class HudTooltipManager {
    public static final HudTooltipManager INSTANCE = new HudTooltipManager();
    private static final int BACKGROUND_COLOR = 0xCC100010;
    private static final int BORDER_COLOR_TOP = 0x505000Ff;
    private static final int BORDER_COLOR_BOTTOM = 0x5028007F;
    private final List<ITooltipProvider.BlockTooltipProvider> blockProviders = new ArrayList<>();
    private final List<ITooltipProvider.BlockEntityTooltipProvider> blockEntityProviders = new ArrayList<>();
    private final List<IAffectRangeProvider> affectRangeProviders = new ArrayList<>();
    private final List<IHandHeldItemTooltipProvider> handItemProviders = new ArrayList<>();

    static {
        INSTANCE.registerBlockEntityTooltip(new ChargerTooltipProvider());
        INSTANCE.registerBlockEntityTooltip(new HeatCollectorTooltipProvider());
        INSTANCE.registerBlockEntityTooltip(new PowerComponentTooltipProvider());
        INSTANCE.registerAffectRange(new AffectRangeProviderImpl());
        INSTANCE.registerBlockEntityTooltip(new RubyPrismTooltipProvider());
        INSTANCE.registerHandHeldItemTooltip(new HeliostatsTooltip());
        INSTANCE.registerBlockEntityTooltip(new HeliostatsTooltipProvider());
        INSTANCE.registerBlockEntityTooltip(new SpaceOvercompressorTooltipProvider());
        INSTANCE.registerHandHeldItemTooltip(ModItems.STRUCTURE_TOOL.get());
        INSTANCE.registerBlockTooltip(new InjectedBlockTooltipProvider());
        INSTANCE.registerBlockEntityTooltip(new InjectedBlockEntityTooltipProvider());
        INSTANCE.registerBlockEntityTooltip(new HeatableBlockTooltipProvider());
        INSTANCE.registerBlockEntityTooltip(new DeflectionRingTooltipProvider());
        INSTANCE.registerBlockEntityTooltip(new PropelPistonTooltipProvider());
    }

    public void registerAffectRange(AffectRangeProviderImpl affectRangeProvider) {
        this.affectRangeProviders.add(affectRangeProvider);
    }

    public void registerBlockTooltip(ITooltipProvider.BlockTooltipProvider provider) {
        this.blockProviders.add(provider);
    }

    public void registerBlockEntityTooltip(ITooltipProvider.BlockEntityTooltipProvider provider) {
        this.blockEntityProviders.add(provider);
    }

    public void registerHandHeldItemTooltip(IHandHeldItemTooltipProvider provider) {
        this.handItemProviders.add(provider);
    }

    /**
     * 渲染方块的tooltip
     */
    public void renderTooltip(
        GuiGraphicsExtractor guiGraphics,
        Level level,
        BlockPos pos,
        BlockState state,
        float partialTick,
        int screenWidth,
        int screenHeight
    ) {
        final int tooltipPosX = screenWidth / 2 + 10;
        final int tooltipPosY = screenHeight / 2 + 10;
        Font font = Minecraft.getInstance().font;
        ITooltipProvider.BlockTooltipProvider currentProvider = determineBlockTooltipProvider(level, pos, state);
        if (currentProvider == null) return;
        List<Component> tooltip = currentProvider.tooltip(level, pos, state);
        if (tooltip.isEmpty()) return;
        renderTooltipWithItemIcon(
            guiGraphics,
            font,
            currentProvider.icon(level, pos, state),
            tooltip,
            tooltipPosX,
            tooltipPosY,
            BACKGROUND_COLOR,
            BORDER_COLOR_TOP,
            BORDER_COLOR_BOTTOM
        );
    }

    /**
     * 渲染方块实体的tooltip
     */
    public void renderTooltip(
        GuiGraphicsExtractor guiGraphics,
        BlockEntity entity,
        float partialTick,
        int screenWidth,
        int screenHeight
    ) {
        final int tooltipPosX = screenWidth / 2 + 10;
        final int tooltipPosY = screenHeight / 2 + 10;
        Font font = Minecraft.getInstance().font;
        ITooltipProvider.BlockEntityTooltipProvider currentProvider = this.determineBlockEntityTooltipProvider(entity);
        if (currentProvider == null) return;
        List<Component> tooltip = currentProvider.tooltip(entity);
        if (tooltip.isEmpty()) return;
        TooltipRenderHelper.renderTooltipWithItemIcon(
            guiGraphics,
            font,
            currentProvider.icon(entity),
            tooltip,
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
        double camX,
        double camY,
        double camZ
    ) {
        IHandHeldItemTooltipProvider pv = this.determineHandHeldItemTooltipProvider(itemStack);
        if (pv == null) return;
        pv.render(poseStack, consumer, itemStack, camX, camY, camZ);
    }

    /**
     * 渲染手持物品Hud Tooltip
     */
    public void renderHandItemHudTooltip(
        GuiGraphicsExtractor guiGraphics,
        ItemStack itemStack,
        float partialTick,
        int screenWidth,
        int screenHeight
    ) {
        IHandHeldItemTooltipProvider pv = this.determineHandHeldItemTooltipProvider(itemStack);
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
        IAffectRangeProvider currentProvider = this.determineAffectRangeProvider(entity);
        if (currentProvider == null) return;
        VoxelShape shape = currentProvider.affectRange(entity);
        renderOutline(poseStack, consumer, camX, camY, camZ, BlockPos.ZERO, shape, 0xff00Ffcc);
    }

    private @Nullable IHandHeldItemTooltipProvider determineHandHeldItemTooltipProvider(ItemStack itemStack) {
        if (itemStack.isEmpty()) return null;
        return this.handItemProviders.stream()
            .filter(it -> it.accepts(itemStack))
            .min(Comparator.comparingInt(IHandHeldItemTooltipProvider::priority))
            .orElse(null);
    }

    private ITooltipProvider.@Nullable BlockTooltipProvider determineBlockTooltipProvider(Level level, BlockPos pos, BlockState state) {
        return this.blockProviders.stream()
            .filter(it -> it.accepts(level, pos, state))
            .min(Comparator.comparingInt(ITooltipProvider::priority))
            .orElse(null);
    }

    private ITooltipProvider.@Nullable BlockEntityTooltipProvider determineBlockEntityTooltipProvider(BlockEntity entity) {
        return this.blockEntityProviders.stream()
            .filter(it -> it.accepts(entity))
            .min(Comparator.comparingInt(ITooltipProvider::priority))
            .orElse(null);
    }

    private @Nullable IAffectRangeProvider determineAffectRangeProvider(BlockEntity entity) {
        return this.affectRangeProviders.stream()
            .filter(it -> it.accepts(entity))
            .min(Comparator.comparingInt(IAffectRangeProvider::priority))
            .orElse(null);
    }
}
