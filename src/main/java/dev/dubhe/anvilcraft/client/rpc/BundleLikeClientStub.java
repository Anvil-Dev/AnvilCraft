package dev.dubhe.anvilcraft.client.rpc;

import dev.anvilcraft.lib.v2.rpc.RPC;
import dev.anvilcraft.lib.v2.rpc.RpcTarget;
import dev.dubhe.anvilcraft.rpc.BundleLikeServerStub;
import net.minecraft.client.Minecraft;

public class BundleLikeClientStub {
    public static void updateInverted(boolean inverted) {
        RPC.call(
            RpcTarget.server(),
            BundleLikeServerStub::updateInverted,
            Minecraft.getInstance().getGameProfile().getId(),
            inverted
        );
    }
}
