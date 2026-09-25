package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.rpc.BundleLikeServerStub;
import net.minecraft.client.Minecraft;

public final class BundleLikeClientStub {
    private BundleLikeClientStub() {
    }

    public static void updateInverted(boolean inverted) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        BundleLikeServerStub.setClientInverted(inverted);
        RPC.call(RpcTarget.server(), BundleLikeServerStub::updateInverted, player.getUUID(), inverted);
    }
}
