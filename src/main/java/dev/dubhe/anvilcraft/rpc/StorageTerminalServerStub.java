package dev.dubhe.anvilcraft.rpc;

import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.dubhe.anvilcraft.api.TerminalSessions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;

public final class StorageTerminalServerStub {
    private StorageTerminalServerStub() {
    }

    @RemoteCallable(validator = TerminalAccessValidator.class)
    public static long openRemote(UUID playerId, UUID target) {
        var server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer());
        return TerminalSessions.open(Objects.requireNonNull(server.getPlayerList().getPlayer(playerId)), target);
    }

    @RemoteCallable(validator = TerminalAccessValidator.class)
    public static boolean isTerminalReachable(UUID playerId, UUID target) {
        var server = Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer());
        var player = Objects.requireNonNull(server.getPlayerList().getPlayer(playerId));
        var terminal = TerminalSessions.findTerminal(player, target);
        return !terminal.isEmpty() && TerminalSessions.targetStorage(player, terminal, false) != null;
    }

    public static final class TerminalAccessValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext context, Method method, Object[] args) {
            return context.player() instanceof ServerPlayer player && args.length == 2 && args[0] instanceof UUID id
                && player.getUUID().equals(id) && args[1] instanceof UUID target
                && !TerminalSessions.findTerminal(player, target).isEmpty();
        }
    }
}
