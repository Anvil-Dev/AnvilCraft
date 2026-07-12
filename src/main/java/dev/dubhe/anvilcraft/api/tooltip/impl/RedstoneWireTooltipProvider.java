package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.network.RedstoneWirePowerRequestPacket;
import dev.dubhe.anvilcraft.util.CompatUtil;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Supplies redstone-wire hammer information without requiring one block entity per wire. */
public class RedstoneWireTooltipProvider extends ITooltipProvider.BlockTooltipProvider {
    private static final int REQUEST_INTERVAL = 10;
    private static final Long2IntOpenHashMap NON_DUST_POWER = new Long2IntOpenHashMap();
    private static final Long2LongOpenHashMap LAST_REQUEST = new Long2LongOpenHashMap();
    private static Level cachedLevel;

    static {
        NON_DUST_POWER.defaultReturnValue(-1);
        LAST_REQUEST.defaultReturnValue(Long.MIN_VALUE);
    }

    @Override
    public boolean accepts(Level level, BlockPos pos, BlockState value) {
        return value.getBlock() instanceof RedstoneWireBlock;
    }

    @Override
    public List<Component> tooltip(Level level, BlockPos pos, BlockState state) {
        if (CompatUtil.HAS_JADE.get() && AnvilCraftClient.CONFIG.doNotShowTooltipWhenJadePresent) {
            return List.of();
        }
        ensureLevel(level);
        long packedPos = pos.asLong();
        long gameTime = level.getGameTime();
        long lastRequest = LAST_REQUEST.get(packedPos);
        if (lastRequest == Long.MIN_VALUE || gameTime - lastRequest >= REQUEST_INTERVAL) {
            LAST_REQUEST.put(packedPos, gameTime);
            PacketDistributor.sendToServer(new RedstoneWirePowerRequestPacket(pos));
        }

        List<Component> lines = new ArrayList<>(3);
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.title").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable(
            "tooltip.anvilcraft.redstone.power", state.getValue(RedstoneWireBlock.POWER)
        ).withStyle(ChatFormatting.GRAY));
        int nonDustPower = NON_DUST_POWER.get(packedPos);
        if (nonDustPower >= 0) {
            lines.add(Component.translatable(
                "tooltip.anvilcraft.redstone.output_to_redstone", nonDustPower
            ).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    @Override
    public int priority() {
        return 0;
    }

    public static void receive(Level level, BlockPos pos, int nonDustPower) {
        ensureLevel(level);
        NON_DUST_POWER.put(pos.asLong(), nonDustPower);
    }

    private static void ensureLevel(Level level) {
        if (cachedLevel == level) {
            return;
        }
        cachedLevel = level;
        NON_DUST_POWER.clear();
        LAST_REQUEST.clear();
    }
}
