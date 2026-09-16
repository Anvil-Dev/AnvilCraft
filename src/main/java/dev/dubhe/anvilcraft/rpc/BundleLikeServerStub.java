package dev.dubhe.anvilcraft.rpc;

import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BundleLikeServerStub {
    private static final Map<UUID, Boolean> INVERTED = new HashMap<>();
    private static volatile boolean clientInverted;

    private BundleLikeServerStub() {
    }

    @RemoteCallable(validator = OwnActionValidator.class)
    public static void updateInverted(UUID playerId, boolean inverted) {
        if (inverted) INVERTED.put(playerId, true);
        else INVERTED.remove(playerId);
    }

    public static boolean isInvertedAction(Player player) {
        return player.level().isClientSide() ? clientInverted : Boolean.TRUE.equals(INVERTED.get(player.getUUID()));
    }

    public static void setClientInverted(boolean inverted) {
        clientInverted = inverted;
    }

    public static void clear(UUID playerId) {
        INVERTED.remove(playerId);
    }

    public static void clear() {
        INVERTED.clear();
    }

    public static final class OwnActionValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext context, Method method, Object[] args) {
            return context.player() instanceof ServerPlayer player && args.length == 2
                && args[0] instanceof UUID id && id.equals(player.getUUID()) && args[1] instanceof Boolean;
        }
    }
}
