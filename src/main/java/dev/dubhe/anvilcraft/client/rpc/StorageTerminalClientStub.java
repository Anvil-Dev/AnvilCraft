package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.item.TerminalItem;
import dev.dubhe.anvilcraft.rpc.StorageTerminalServerStub;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StorageTerminalClientStub {
    private StorageTerminalClientStub() {
    }

    public static CompletableFuture<Long> openRemote(UUID target) {
        return RPC.invoke(RpcTarget.server(), StorageTerminalServerStub::openRemote, Minecraft.getInstance().player.getUUID(), target);
    }

    public static void open(Player player, ItemStack stack, TerminalItem.Kind kind) {
        if (!(stack.getItem() instanceof TerminalItem terminal)) return;
        UUID target = terminal.targetId(player, stack);
        if (target == null) {
            player.sendOverlayMessage(Component.translatable("message.anvilcraft.hyperdimension_terminal.not_bound"));
            return;
        }
        String name = switch (kind) {
            case LOCAL -> "local_terminal";
            case SHULKER -> "shulker_terminal";
            case HYPERDIMENSION -> "hyperdimension_terminal";
        };
        String title = switch (kind) {
            case LOCAL -> "large_crate";
            case SHULKER -> "shulker_container";
            case HYPERDIMENSION -> "hyperdimension_storage_station";
        };
        openRemote(target).whenCompleteAsync((token, error) -> {
            if (Minecraft.getInstance().player != player) return;
            if (error != null || token == null || token == -1L) {
                player.sendOverlayMessage(Component.translatable("message.anvilcraft." + name + ".not_found"));
                return;
            }
            StorageScreen.openScreen(BlockPos.of(token), Component.translatable("block.anvilcraft." + title));
        }, Minecraft.getInstance());
    }
}
