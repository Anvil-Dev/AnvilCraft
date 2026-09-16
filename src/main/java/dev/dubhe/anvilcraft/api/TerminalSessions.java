package dev.dubhe.anvilcraft.api;

import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.TerminalItem;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.StorageType;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import it.unimi.dsi.fastutil.ints.IntObjectBiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class TerminalSessions {
    private static final int MAX_SESSIONS = 16;
    private static final Map<UUID, LinkedHashMap<Long, Session>> SESSIONS = new HashMap<>();

    private TerminalSessions() {
    }

    public static UUID localTerminalId(UUID player) {
        return UUID.nameUUIDFromBytes(("anvilcraft:local_terminal:" + player).getBytes(StandardCharsets.UTF_8));
    }

    public static UUID shulkerTerminalId(UUID player) {
        return UUID.nameUUIDFromBytes(("anvilcraft:shulker_terminal:" + player).getBytes(StandardCharsets.UTF_8));
    }

    public static ItemStack findTerminal(ServerPlayer player, UUID target) {
        for (ItemStack stack : List.of(player.getMainHandItem(), player.getOffhandItem(), player.containerMenu.getCarried())) {
            if (matches(player, stack, target)) return stack;
        }
        for (ItemStack stack : TerminalItem.getAll(player)) {
            if (matches(player, stack, target)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean matches(ServerPlayer player, ItemStack stack, UUID target) {
        return !stack.isEmpty() && stack.getItem() instanceof TerminalItem terminal && target.equals(terminal.targetId(player, stack));
    }

    public static long open(ServerPlayer player, UUID target) {
        ItemStack terminal = findTerminal(player, target);
        if (terminal.isEmpty() || targetStorage(player, terminal, true) == null) return -1L;
        var sessions = SESSIONS.computeIfAbsent(player.getUUID(), ignored -> new LinkedHashMap<>());
        long token;
        do {
            token = new BlockPos(ThreadLocalRandom.current().nextInt(-30000000, 30000000), -2048,
                ThreadLocalRandom.current().nextInt(-30000000, 30000000)).asLong();
        } while (sessions.containsKey(token));
        if (sessions.size() == MAX_SESSIONS) sessions.remove(sessions.keySet().iterator().next());
        UUID emptyId = UUID.nameUUIDFromBytes(("anvilcraft:empty_terminal:" + player.getUUID()).getBytes(StandardCharsets.UTF_8));
        sessions.put(token, new Session(target, terminal, new EmptyStorage(emptyId)));
        return token;
    }

    public static boolean contains(UUID player, long token) {
        var sessions = SESSIONS.get(player);
        return sessions != null && sessions.containsKey(token);
    }

    public static ItemStack terminal(ServerPlayer player, long token) {
        var sessions = SESSIONS.get(player.getUUID());
        Session session = sessions == null ? null : sessions.get(token);
        if (session == null) return ItemStack.EMPTY;
        if (matches(player, session.initial, session.target)) {
            if (player.containerMenu.getCarried() == session.initial) return session.initial;
            for (ItemStack stack : TerminalItem.getAll(player)) {
                if (stack == session.initial) return stack;
            }
        }
        return findTerminal(player, session.target);
    }

    public static BaseStorage<?> storage(ServerPlayer player, long token) {
        Session session = SESSIONS.get(player.getUUID()).get(token);
        ItemStack terminal = terminal(player, token);
        BaseStorage<?> storage = terminal.isEmpty() ? null : targetStorage(player, terminal, false);
        return storage == null ? session.empty : storage;
    }

    public static @Nullable BaseStorage<?> targetStorage(ServerPlayer player, ItemStack stack, boolean grant) {
        if (!(stack.getItem() instanceof TerminalItem terminal)) return null;
        if (terminal.kind() == TerminalItem.Kind.HYPERDIMENSION) {
            UUID id = terminal.targetId(player, stack);
            return id == null ? null : Storages.get().getOrCreate(id, HyperdimensionStorage.class);
        }
        if (terminal.kind() == TerminalItem.Kind.SHULKER) {
            for (ItemStack item : TerminalItem.carriedItems(player)) {
                if (!item.is(ModBlocks.SHULKER_CONTAINER.asItem())) continue;
                StorageRef ref = item.get(ModComponents.STORAGE);
                if (ref == null || ref.type() != StorageType.SHULKER_CONTAINER) continue;
                UUID id = ref.id().orElse(null);
                if (id == null && grant) {
                    id = UUID.randomUUID();
                    item.set(ModComponents.STORAGE, new StorageRef(StorageType.SHULKER_CONTAINER, id));
                    player.getInventory().setChanged();
                    player.inventoryMenu.broadcastChanges();
                }
                return id == null ? null : Storages.get().getOrCreate(id, ShulkerContainerStorage.class);
            }
        }
        var level = player.level();
        BlockPos pos = terminal.kind() == TerminalItem.Kind.LOCAL
            ? TerminalSourceManager.nearestLargeCrate(level, player.getX(), player.getY(), player.getZ(), 32)
            : TerminalSourceManager.nearestShulkerContainer(level, player.getX(), player.getY(), player.getZ(), 64);
        if (pos == null || !(level.getBlockEntity(pos) instanceof StorageBlockEntity entity)) return null;
        UUID id = entity.getId();
        if (id == null) {
            id = UUID.randomUUID();
            entity.setId(id);
        }
        return Storages.get().getOrCreate(id, entity.getStorageType().clazz());
    }

    public static void clear(UUID player) {
        SESSIONS.remove(player);
    }

    public static void clear() {
        SESSIONS.clear();
    }

    private record Session(UUID target, ItemStack initial, EmptyStorage empty) {
    }

    private static final class EmptyStorage extends HyperdimensionStorage {
        private EmptyStorage(UUID id) {
            super(id);
        }

        @Override
        protected UnlimitedItemStacksResourceHandler constructItemHandler(IntObjectBiConsumer<UnlimitedItemStack> changed) {
            return new UnlimitedItemStacksResourceHandler(0);
        }
    }
}
