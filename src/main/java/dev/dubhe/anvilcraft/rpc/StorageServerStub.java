package dev.dubhe.anvilcraft.rpc;

import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageServerStub {
    private static final Map<UUID, StorageServerStub> STUBS = new HashMap<>();
    private final int version = 0; // 预留
    private IntList order;

    @RemoteCallable
    public static int getVersion(@CallableParam(clazz = UUIDUtil.class, field = "STREAM_CODEC") UUID id) {
        return StorageServerStub.STUBS.computeIfAbsent(id, _ -> new StorageServerStub()).version;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static double getFullness(
        @CallableParam(clazz = UUIDUtil.class, field = "STREAM_CODEC") UUID playerId,
        long sourcePos
    ) {
        return StorageServerStub.getStorage(playerId, sourcePos).getItems().getFullness();
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static void reorder(
        @CallableParam(clazz = UUIDUtil.class, field = "STREAM_CODEC") UUID playerId,
        long sourcePos
    ) {
    }

    public static final class StorageAccessValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(@NonNull IPayloadContext ctx, @NonNull Method method, Object @NonNull [] args) {
            if (
                !(ctx.player() instanceof ServerPlayer player)
                || args.length != 2
                || !(args[0] instanceof UUID playerId)
                || !player.getGameProfile().id().equals(playerId)
                || !(args[1] instanceof Long sourcePos)
            ) {
                return false;
            }
            BlockPos pos = BlockPos.of(sourcePos);
            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            return blockEntity instanceof StorageBlockEntity storage
                && storage.getId() != null
                && Storages.get().get(storage.getId()).isPresent()
                && AbstractContainerMenu.stillValid(
                    ContainerLevelAccess.create(player.level(), pos),
                    player,
                    storage.getBlockState().getBlock()
                );
        }
    }

    private static BaseStorage getStorage(UUID playerId, long sourcePos) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("Cannot access storage without a running server");
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            throw new IllegalStateException("Cannot access storage without a server player");
        }
        BlockEntity blockEntity = player.level().getBlockEntity(BlockPos.of(sourcePos));
        if (!(blockEntity instanceof StorageBlockEntity storage) || storage.getId() == null) {
            throw new IllegalStateException("Cannot access storage without a storage block entity");
        }
        return Storages.get().get(storage.getId()).orElseThrow();
    }
}
