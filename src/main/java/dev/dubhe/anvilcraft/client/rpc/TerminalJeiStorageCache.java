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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 客户端缓存玩家绑定存储站的物品代表列表（含数量），
 * 供 JEI 转移的检查阶段判断存储站是否满足配方需求。
 */
public final class TerminalJeiStorageCache {
    private static final Map<UUID, List<ItemStack>> CACHE = new HashMap<>();
    private static final Map<UUID, CompletableFuture<List<ItemStack>>> PENDING = new HashMap<>();

    private TerminalJeiStorageCache() {
    }

    /** 玩家是否持有指向任意存储站的绑定终端。 */
    public static @Nullable UUID boundStorage(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            UUID id = TerminalJeiStorageCache.storageOf(stack);
            if (id != null) {
                return id;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            UUID id = TerminalJeiStorageCache.storageOf(stack);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    /** 获取缓存的存储站物品列表（可能为 null 表示尚未加载）。 */
    public static @Nullable List<ItemStack> get(UUID storageId) {
        return TerminalJeiStorageCache.CACHE.get(storageId);
    }

    /** 异步加载存储站物品列表到缓存；若已在加载则复用进行中的 future。 */
    public static CompletableFuture<List<ItemStack>> ensure(UUID storageId) {
        synchronized (TerminalJeiStorageCache.class) {
            if (TerminalJeiStorageCache.CACHE.containsKey(storageId)) {
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
                return items;
            }).whenComplete((items, error) -> TerminalJeiStorageCache.PENDING.remove(storageId));
            TerminalJeiStorageCache.PENDING.put(storageId, pending);
            return pending;
        }
    }

    public static void clear() {
        TerminalJeiStorageCache.CACHE.clear();
        TerminalJeiStorageCache.PENDING.clear();
    }

    private static @Nullable UUID storageOf(ItemStack stack) {
        if (!stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            return null;
        }
        TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
        if (binding == null || binding.id().isEmpty()) {
            return null;
        }
        return binding.id().get();
    }
}
