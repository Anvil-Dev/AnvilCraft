package dev.dubhe.anvilcraft.rpc;

import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu;
import dev.dubhe.anvilcraft.inventory.TranscendenceSmithingMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.util.UUID;

public final class SmithingServerStub {
    private SmithingServerStub() {
    }

    @RemoteCallable(validator = Validator.class)
    public static boolean selectTemplate(
        UUID playerId,
        int containerId,
        @CallableParam(clazz = ItemStack.class, field = "STREAM_CODEC") ItemStack template
    ) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return false;
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null || player.containerMenu.containerId != containerId) return false;
        AbstractContainerMenu menu = player.containerMenu;
        boolean selected;
        if (menu instanceof AdjacentSmithingMenu smithing) {
            selected = smithing.selectTemplateForTransfer(player, template);
        } else if (menu instanceof TranscendenceSmithingMenu smithing) {
            selected = smithing.selectTemplateForTransfer(player, template);
        } else {
            return false;
        }
        // 先同步模板切换造成的槽位变化，客户端收到响应后再计算材料转移。
        menu.broadcastChanges();
        return selected;
    }

    public static final class Validator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext context, Method method, Object[] args) {
            return context.player() instanceof ServerPlayer player
                && args.length == 3 && player.getUUID().equals(args[0]);
        }
    }
}
