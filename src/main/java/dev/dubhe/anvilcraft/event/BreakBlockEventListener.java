package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.item.ResonatorItem;
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
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber
public class BreakBlockEventListener {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void preventInfiniteFluidTankBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockPos pos = event.getPos();
        if (!InfiniteFluidTankBreakProtection.isProtected(player.serverLevel(), pos)) return;
        if (ResonatorItem.isResonanceMining(player.serverLevel(), player, pos)) {
            InfiniteFluidTankBreakProtection.clear(player);
            return;
        }
        if (InfiniteFluidTankBreakProtection.shouldCancelBreak(player, pos)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockRemoved(BlockEvent.BreakEvent event) {
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
    public static void preventTradingStationBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (!(player.level() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        if (!level.getBlockState(pos).is(ModBlocks.TRADING_STATION)) return;
        if (
            level.getBlockEntity(pos, ModBlockEntities.TRADING_STATION.get()).filter(be -> be.isOwner(player)).isEmpty()
            && !player.isShiftKeyDown()
        ) {
            player.displayClientMessage(
                Component.translatable("screen.anvilcraft.tooltip.trading_station.break_failed").withStyle(ChatFormatting.RED),
                true
            );
            event.setCanceled(true);
        }
    }
}
