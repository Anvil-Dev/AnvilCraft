package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.rpc.BundleLikeClientStub;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class InvertedActionEventListener {
    private static boolean lastInverted = false;
    private static boolean lastBucketInverted = false;

    /** 所有 BundleLike 物品共用的反转状态（与客户端配置一致）。 */
    public static boolean isInverted() {
        return AnvilCraftClient.CONFIG.invertOverrideAction;
    }

    /**
     * 上一次上报时所在的连接，用于识别「进入了新的世界/服务器」。
     *
     * <p>服务端在玩家退出时清空这两份反转状态，而这里的哨兵是跨世界存活的静态字段。
     * 若只看「配置值是否变化」，「配置为 true 时退出再进入同一服务器」会永远不再上报，
     * 服务端便退回默认值、与界面判定的键位相反且无法自愈。</p>
     *
     * <p>不能靠 {@code ClientPlayerNetworkEvent.LoggingOut} 复位：{@code Minecraft#disconnect}
     * 先发该事件、随后还会跑一次 {@code runTick}，此时 {@code player} 尚未置空，
     * 本次 tick 会立刻把哨兵重新写成「已上报」。改为按连接实例判断，
     * 只要连接对象变了（登出置空、或进入下一局新建）就重新上报。</p>
     */
    @Nullable
    private static ClientPacketListener lastConnection;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != InvertedActionEventListener.lastConnection) {
            // 换了一局：服务端那份状态是新的，本地哨兵必须作废以触发重新上报
            InvertedActionEventListener.lastConnection = connection;
            InvertedActionEventListener.lastInverted = false;
            InvertedActionEventListener.lastBucketInverted = false;
        }
        if (connection == null) {
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
