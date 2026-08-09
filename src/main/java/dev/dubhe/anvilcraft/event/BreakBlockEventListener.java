package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.block.TradingStationBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.tool.trascendence.TranscendenceResonatorItem;
import dev.dubhe.anvilcraft.util.InfiniteFluidTankBreakProtection;
import dev.dubhe.anvilcraft.util.ModEnchantmentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

@EventBusSubscriber
public class BreakBlockEventListener {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventInfiniteFluidTankBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockPos pos = event.getPos();
        ServerLevel level = player.level();
        if (!InfiniteFluidTankBreakProtection.isProtected(level, pos)) return;
        if (TranscendenceResonatorItem.isResonanceMining(level, player, pos)) {
            InfiniteFluidTankBreakProtection.clear(player);
            return;
        }
        if (InfiniteFluidTankBreakProtection.shouldCancelBreak(player, pos)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockRemoved(BreakBlockEvent event) {
        Player player = event.getPlayer();
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        ItemStack stack = player.getMainHandItem();
        ModEnchantmentHelper.onPostBreakBlock(
            serverLevel,
            stack,
            player,
            EquipmentSlot.MAINHAND,
            event.getPos().getCenter(),
            event.getState()
        );
    }

    @SubscribeEvent
    public static void preventTradingStationBreak(BreakBlockEvent event) {
        Player player = event.getPlayer();
        if (!(player.level() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.TRADING_STATION)) return;
        BlockPos mainPos = state.getBlock() instanceof TradingStationBlock tradingStation
            ? tradingStation.getMainPartPos(pos, state)
            : pos;
        if (level.getBlockEntity(mainPos, ModBlockEntities.TRADING_STATION.get())
            .filter(blockEntity -> blockEntity.isOwner(player))
            .isPresent()
            || player.isShiftKeyDown()) {
            return;
        }
        player.sendOverlayMessage(
            Component.translatable("screen.anvilcraft.tooltip.trading_station.break_failed")
                .withStyle(ChatFormatting.RED)
        );
        event.setCanceled(true);
    }
}
