package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.rpc.BundleLikeClientStub;
import dev.dubhe.anvilcraft.item.HyperdimensionTerminalItem;
import dev.dubhe.anvilcraft.item.LocalTerminalItem;
import dev.dubhe.anvilcraft.item.PillBoxItem;
import dev.dubhe.anvilcraft.item.ShulkerTerminalItem;
import dev.dubhe.anvilcraft.item.amulet.AmuletBoxItem;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class InvertedActionEventListener {
    private static final Map<ResourceLocation, BooleanSupplier> CONFIG = new HashMap<>();
    private static final Object2BooleanMap<ResourceLocation> LAST_INVERTED = new Object2BooleanOpenHashMap<>();

    static  {
        InvertedActionEventListener.register(AmuletBoxItem.CONFIG_ID, () -> AnvilCraftClient.CONFIG.amuletBoxInvertOverrideAction);
        InvertedActionEventListener.register(PillBoxItem.CONFIG_ID, () -> AnvilCraftClient.CONFIG.pillBoxInvertOverrideAction);
        InvertedActionEventListener.register(LocalTerminalItem.CONFIG_ID, () -> AnvilCraftClient.CONFIG.localTerminalInvertOverrideAction);
        InvertedActionEventListener.register(
            ShulkerTerminalItem.CONFIG_ID,
            () -> AnvilCraftClient.CONFIG.shulkerTerminalInvertOverrideAction
        );
        InvertedActionEventListener.register(
            HyperdimensionTerminalItem.CONFIG_ID,
            () -> AnvilCraftClient.CONFIG.hyperdimensionTerminalInvertOverrideAction
        );
    }

    public static void register(ResourceLocation id, BooleanSupplier config) {
        InvertedActionEventListener.CONFIG.put(id, config);
    }

    /** 客户端当前记录的指定物品反转状态（尚未与配置同步时以最近一次记录为准）。 */
    public static boolean isInverted(ResourceLocation id) {
        return InvertedActionEventListener.LAST_INVERTED.getOrDefault(id, false);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        for (Map.Entry<ResourceLocation, BooleanSupplier> entry : InvertedActionEventListener.CONFIG.entrySet()) {
            ResourceLocation id = entry.getKey();
            BooleanSupplier config = entry.getValue();
            boolean inverted = config.getAsBoolean();
            boolean lastInverted = InvertedActionEventListener.LAST_INVERTED.getOrDefault(id, false);
            if (inverted != lastInverted) {
                BundleLikeClientStub.updateInverted(id, inverted);
                InvertedActionEventListener.LAST_INVERTED.put(id, inverted);
            }
        }
    }
}
