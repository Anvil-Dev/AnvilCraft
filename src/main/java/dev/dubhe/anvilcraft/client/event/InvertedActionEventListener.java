package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.rpc.BundleLikeClientStub;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class InvertedActionEventListener {
    private static boolean lastInverted = false;
    private static boolean lastBucketInverted = false;

    /** 所有 BundleLike 物品共用的反转状态（与客户端配置一致）。 */
    public static boolean isInverted() {
        return AnvilCraftClient.CONFIG.invertOverrideAction;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        boolean inverted = AnvilCraftClient.CONFIG.invertOverrideAction;
        if (inverted != InvertedActionEventListener.lastInverted) {
            BundleLikeClientStub.updateInverted(inverted);
            InvertedActionEventListener.lastInverted = inverted;
        }
        // 流体端口的桶操作翻转在服务端执行（需真正取出流体），客户端配置要同步一份过去
        boolean bucketInverted = AnvilCraftClient.CONFIG.invertFluidPortBucketAction;
        if (bucketInverted != InvertedActionEventListener.lastBucketInverted) {
            StorageClientStub.updateInvertedBucketAction(bucketInverted);
            InvertedActionEventListener.lastBucketInverted = bucketInverted;
        }
    }
}
