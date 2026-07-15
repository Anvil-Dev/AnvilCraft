package dev.dubhe.anvilcraft.client.hud;

import dev.dubhe.anvilcraft.block.TradingStationBlock;
import dev.dubhe.anvilcraft.block.entity.TradingStationBlockEntity;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.inventory.container.FilterOnlyContainer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Optional;

public class TradingStationHUD {
    private static final Identifier HUD = SharedTextures.textureGui("machine/trading_station/hud");

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker partialTick) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        LocalPlayer player = mc.player;
        if (player == null) return;
        Level level = player.level();
        if (!(mc.hitResult instanceof BlockHitResult hit)) return;
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TradingStationBlock)) return;
        pos = pos.subtract(state.getValue(TradingStationBlock.HALF).getOffset(Direction.NORTH));
        Optional<TradingStationBlockEntity> beOp = level.getBlockEntity(pos, ModBlockEntities.TRADING_STATION.get());
        if (beOp.isEmpty()) return;
        TradingStationBlockEntity be = beOp.get();
        FilterOnlyContainer filters = be.getFilters();
        ItemStack provide = filters.getItem(0);
        ItemStack provide1 = filters.getItem(1);
        ItemStack request = filters.getItem(2);
        boolean noProviding = provide.isEmpty() && provide1.isEmpty();
        boolean noRequesting = request.isEmpty();
        if (noProviding && noRequesting) return;
        int left = (mc.getWindow().getGuiScaledWidth() - 84) / 2;
        int top = (mc.getWindow().getGuiScaledHeight() - 26) / 28 * 21;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TradingStationHUD.HUD, left, top, 0, 0, 41, 26, 64, 64);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TradingStationHUD.HUD, left + 42, top, 0, 26, 42, 26, 64, 64);
        TradingStationHUD.renderItem(graphics, mc.font, request, left + 4, top + 4);
        TradingStationHUD.renderItem(graphics, mc.font, provide, left + 46, top + 4);
        TradingStationHUD.renderItem(graphics, mc.font, provide1, left + 64, top + 4);
        boolean providingMultiple = TradingStationBlockEntity.isProvideMultiple(be.getFilters());
        if (providingMultiple || noRequesting) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                SharedTextures.ERROR_SPRITE,
                left + 24,
                top + 5,
                0,
                0,
                16,
                16,
                16,
                16
            );
        }
    }

    private static void renderItem(GuiGraphicsExtractor graphics, Font font, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;
        graphics.item(stack, x, y);
        graphics.itemDecorations(font, stack, x, y);
    }
}
