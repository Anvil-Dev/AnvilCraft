package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端缓存玩家绑定存储站的物品代表列表（含数量），
 * 供 JEI 转移的检查阶段判断存储站是否满足配方需求。
 */
public final class TerminalJeiStorageCache {
    /** 缓存有效期（毫秒）：存储站内容可能被其他玩家/自动化改动，定期刷新避免误判。 */
    private static final long TTL_MILLIS = 60_000L;

    // ConcurrentHashMap：渲染线程（mixin 检查阶段 get）与 RPC 完成线程（thenApply 写入）
    // 并发读写，普通 HashMap 有数据损坏风险。
    private static final Map<UUID, List<ItemStack>> CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> TIMESTAMPS = new ConcurrentHashMap<>();
    private static final Map<UUID, CompletableFuture<List<ItemStack>>> PENDING = new ConcurrentHashMap<>();
    /**
     * 客户端主线程的"JEI 补库中"标志，防止 transferRecipe 递归重入。
     * 断线时补库 RPC 可能永不完成（排队的 execute 任务被登出流程丢弃），
     * 标志位会泄漏到下一次会话；由 {@link #clear()}（断线清理）统一复位。
     */
    private static final ThreadLocal<Boolean> RESTOCKING = ThreadLocal.withInitial(() -> false);

    private TerminalJeiStorageCache() {
    }

    public static boolean isRestocking() {
        return TerminalJeiStorageCache.RESTOCKING.get();
    }

    public static void setRestocking(boolean restocking) {
        TerminalJeiStorageCache.RESTOCKING.set(restocking);
    }

    /** 玩家身上全部终端（超维绑定 / 本地 / 潜影）的存储标识，去重；无终端返回空列表。 */
    public static List<UUID> boundStorages(Player player) {
        List<UUID> ids = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            TerminalJeiStorageCache.collect(TerminalJeiStorageCache.storageOf(stack), ids);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            TerminalJeiStorageCache.collect(TerminalJeiStorageCache.storageOf(stack), ids);
        }
        return ids;
    }

    private static void collect(@Nullable UUID id, List<UUID> ids) {
        if (id != null && !ids.contains(id)) {
            ids.add(id);
        }
    }

    /** 获取缓存的存储站物品列表（可能为 null 表示尚未加载或已过期）。 */
    public static @Nullable List<ItemStack> get(UUID storageId) {
        Long timestamp = TerminalJeiStorageCache.TIMESTAMPS.get(storageId);
        if (timestamp == null || System.currentTimeMillis() - timestamp > TerminalJeiStorageCache.TTL_MILLIS) {
            return null;
        }
        return TerminalJeiStorageCache.CACHE.get(storageId);
    }

    /** 异步加载存储站物品列表到缓存；若已在加载则复用进行中的 future。 */
    public static CompletableFuture<List<ItemStack>> ensure(UUID storageId) {
        synchronized (TerminalJeiStorageCache.class) {
            if (TerminalJeiStorageCache.get(storageId) != null) {
                return CompletableFuture.completedFuture(TerminalJeiStorageCache.CACHE.get(storageId));
            }
            CompletableFuture<List<ItemStack>> pending = TerminalJeiStorageCache.PENDING.get(storageId);
            if (pending != null) {
                return pending;
            }
            Player player = Minecraft.getInstance().player;
            if (player == null) {
                return CompletableFuture.completedFuture(List.of());
            }
            pending = RPC.invoke(
                RpcTarget.server(),
                StorageServerStub::getStorageItems,
                player.getGameProfile().getId(),
                storageId
            ).thenApply(items -> {
                TerminalJeiStorageCache.CACHE.put(storageId, items);
                TerminalJeiStorageCache.TIMESTAMPS.put(storageId, System.currentTimeMillis());
                return items;
            }).whenComplete((items, error) -> TerminalJeiStorageCache.PENDING.remove(storageId));
            TerminalJeiStorageCache.PENDING.put(storageId, pending);
            return pending;
        }
    }

    public static void clear() {
        TerminalJeiStorageCache.CACHE.clear();
        TerminalJeiStorageCache.TIMESTAMPS.clear();
        TerminalJeiStorageCache.PENDING.clear();
        TerminalJeiStorageCache.RESTOCKING.remove();
    }

    private static @Nullable UUID storageOf(ItemStack stack) {
        if (stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
            if (binding == null || binding.id().isEmpty()) {
                return null;
            }
            return binding.id().get();
        }
        if (stack.is(ModItems.LOCAL_TERMINAL)) {
            return StorageTerminalClientStub.localTerminalId();
        }
        if (stack.is(ModItems.SHULKER_TERMINAL)) {
            return StorageTerminalClientStub.shulkerTerminalId();
        }
        return null;
    }
}
