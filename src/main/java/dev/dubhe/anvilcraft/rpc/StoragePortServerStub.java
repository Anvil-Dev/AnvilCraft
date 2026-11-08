package dev.dubhe.anvilcraft.rpc;

import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;

/** 为当前查看的仓储端口提供服务端计数，避免持续向所有玩家同步仓库库存。 */
public final class StoragePortServerStub {
    protected static final StreamCodec<ByteBuf, BlockPos> POS_STREAM_CODEC = ByteBufCodecs.VAR_LONG
        .map(BlockPos::of, BlockPos::asLong);

    private StoragePortServerStub() {
    }

    @RemoteCallable(validator = Validator.class)
    public static long getStoredCount(
        @CallableParam(clazz = StoragePortServerStub.class, field = "POS_STREAM_CODEC") BlockPos pos
    ) {
        ServerLevel level = Validator.LEVEL.get();
        try {
            return level != null && level.getBlockEntity(pos) instanceof StoragePortBlockEntity port
                ? port.getStoredCount() : -1;
        } finally {
            Validator.LEVEL.remove();
        }
    }

    private static final class Validator implements IRemoteCallableValidator {
        private static final ThreadLocal<@Nullable ServerLevel> LEVEL = new ThreadLocal<>();

        @Override
        public boolean validate(IPayloadContext context, Method method, Object[] arguments) {
            Validator.LEVEL.remove();
            if (context.flow() != PacketFlow.SERVERBOUND || !(context.player() instanceof ServerPlayer player)
                || arguments.length != 1 || !(arguments[0] instanceof BlockPos pos)) {
                return false;
            }
            ServerLevel level = player.level();
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof StoragePortBlockEntity port)
                || !AbstractContainerMenu.stillValid(
                    ContainerLevelAccess.create(level, pos), player, port.getBlockState().getBlock())) {
                return false;
            }
            Validator.LEVEL.set(level);
            return true;
        }
    }
}
