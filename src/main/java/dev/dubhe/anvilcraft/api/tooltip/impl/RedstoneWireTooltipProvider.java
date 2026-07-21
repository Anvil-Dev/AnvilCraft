package dev.dubhe.anvilcraft.api.tooltip.impl;

import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
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

/**
 * 为自定义红石导线提供铁砧锤 HUD 信息。
 *
 * <p>整网功率由服务端网络管理器同步到客户端缓存；防反馈使用的非红石粉输入强度按需查询，
 * 两者都不需要为每根导线创建方块实体。</p>
 */
public class RedstoneWireTooltipProvider extends ITooltipProvider.BlockTooltipProvider {
    /** 同一位置两次服务端请求之间的最小游戏刻数。 */
    private static final int REQUEST_INTERVAL = 10;
    /** 客户端最近收到的“可输出给原版红石粉”强度，-1 表示尚无响应。 */
    private static final Long2IntOpenHashMap NON_DUST_POWER = new Long2IntOpenHashMap();
    /** 客户端每个位置上次发包的游戏时间。 */
    private static final Long2LongOpenHashMap LAST_REQUEST = new Long2LongOpenHashMap();
    /** 上述位置缓存所属的客户端世界。 */
    private static Level cachedLevel;

    static {
        // 使用不会与合法红石强度重叠的哨兵值，避免额外维护 containsKey 集合。
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
            // 遵守统一兼容配置，避免 Jade 与铁砧锤 HUD 在同一位置重复显示信息。
            return List.of();
        }
        ensureLevel(level);
        long packedPos = pos.asLong();
        long gameTime = level.getGameTime();
        long lastRequest = LAST_REQUEST.get(packedPos);
        if (lastRequest == Long.MIN_VALUE || gameTime - lastRequest >= REQUEST_INTERVAL) {
            // HUD 可能每帧调用 tooltip，按游戏刻限频可显著减少客户端到服务端的小包数量。
            LAST_REQUEST.put(packedPos, gameTime);
            PacketDistributor.sendToServer(new RedstoneWirePowerRequestPacket(pos));
        }

        List<Component> lines = new ArrayList<>(3);
        lines.add(Component.translatable("tooltip.anvilcraft.redstone.title").withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable(
            "tooltip.anvilcraft.redstone.power", RedstoneWireNetworkManager.getPower(level, pos)
        ).withStyle(ChatFormatting.GRAY));
        int nonDustPower = NON_DUST_POWER.get(packedPos);
        if (nonDustPower >= 0) {
            // 收到服务端权威值之前不显示占位数字，避免把“未知”误导成真实的零输出。
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

    /** 接收服务端返回的非红石粉输入强度并更新当前位置缓存。 */
    public static void receive(Level level, BlockPos pos, int nonDustPower) {
        ensureLevel(level);
        NON_DUST_POWER.put(pos.asLong(), nonDustPower);
    }

    private static void ensureLevel(Level level) {
        if (cachedLevel == level) {
            return;
        }
        // 方块坐标在不同维度会重复，切换世界后必须整体清空，不能复用上一维度的数据和限频时间。
        cachedLevel = level;
        NON_DUST_POWER.clear();
        LAST_REQUEST.clear();
    }
}
