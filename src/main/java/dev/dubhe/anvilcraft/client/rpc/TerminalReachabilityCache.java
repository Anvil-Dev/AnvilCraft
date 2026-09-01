package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;

/**
 * 客户端缓存本地 / 潜影终端“当前能否连接目标”的状态，供“+”提示与浮窗判定。
 * 超维终端没有可达性问题（绑定目标无距离限制），不会进入本缓存。
 * 潜影终端仅连接玩家身上的潜影集装箱或 64 格内最近的世界潜影集装箱。
 */
public final class TerminalReachabilityCache {
    /** 缓存有效期（毫秒）：轻微滞后可接受，避免频繁 RPC。 */
    private static final long TTL_MILLIS = 2_000L;

    private static final Map<UUID, Boolean> REACHABLE = new HashMap<>();
    private static final Map<UUID, Long> TIMESTAMPS = new HashMap<>();
    private static final Map<UUID, CompletableFuture<Boolean>> PENDING = new HashMap<>();

    private TerminalReachabilityCache() {
    }

    /** 缓存确认不可达时返回 false；未知（尚未确认）时乐观视为可达（供浮窗激活使用）。 */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isReachable(UUID terminalId) {
        Long timestamp = TerminalReachabilityCache.TIMESTAMPS.get(terminalId);
        if (timestamp != null && System.currentTimeMillis() - timestamp <= TerminalReachabilityCache.TTL_MILLIS) {
            return TerminalReachabilityCache.REACHABLE.getOrDefault(terminalId, true);
        }
        return true;
    }

    /** 返回当前缓存的可达性；无有效缓存（未知 / 过期）返回 null。供“+”提示等避免乐观闪回。 */
    public static @Nullable Boolean getReachability(UUID terminalId) {
        Long timestamp = TerminalReachabilityCache.TIMESTAMPS.get(terminalId);
        if (timestamp != null && System.currentTimeMillis() - timestamp <= TerminalReachabilityCache.TTL_MILLIS) {
            return TerminalReachabilityCache.REACHABLE.getOrDefault(terminalId, true);
        }
        return null;
    }

    /** 异步向服务端确认可达性（命中有效缓存或在途请求则跳过）。 */
    public static void ensure(UUID terminalId) {
        Long timestamp = TerminalReachabilityCache.TIMESTAMPS.get(terminalId);
        if (timestamp != null && System.currentTimeMillis() - timestamp <= TerminalReachabilityCache.TTL_MILLIS) {
            return;
        }
        synchronized (TerminalReachabilityCache.class) {
            if (TerminalReachabilityCache.PENDING.containsKey(terminalId)) {
                return;
            }
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            CompletableFuture<Boolean> pending = RPC.invoke(
                RpcTarget.server(),
                StorageServerStub::isTerminalReachable,
                player.getGameProfile().getId(),
                terminalId
            ).whenComplete((reachable, error) -> {
                TerminalReachabilityCache.REACHABLE.put(terminalId, reachable != null && reachable);
                TerminalReachabilityCache.TIMESTAMPS.put(terminalId, System.currentTimeMillis());
                TerminalReachabilityCache.PENDING.remove(terminalId);
            });
            TerminalReachabilityCache.PENDING.put(terminalId, pending);
        }
    }

    /** 断线清理，防止缓存泄漏到下次会话。 */
    public static void clear() {
        TerminalReachabilityCache.REACHABLE.clear();
        TerminalReachabilityCache.TIMESTAMPS.clear();
        TerminalReachabilityCache.PENDING.clear();
    }
}
