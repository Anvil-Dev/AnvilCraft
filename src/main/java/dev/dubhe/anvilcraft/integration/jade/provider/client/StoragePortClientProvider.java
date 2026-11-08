package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.integration.jade.provider.StoragePortProvider;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/** 保留缓存与标记仓储物品的完整计数，并沿用 Jade 的物品布局和显示数量设置。 */
public enum StoragePortClientProvider implements IClientExtensionProvider<ItemStack, ItemView> {
    INSTANCE;

    @Override
    public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
        List<ClientViewGroup<ItemView>> result = new ArrayList<>();
        for (ViewGroup<ItemStack> group : groups) {
            long[] counts = group.getExtraData().getLongArray(StoragePortProvider.COUNTS_KEY);
            List<ItemView> views = new ArrayList<>();
            for (int index = 0; index < group.views.size(); index++) {
                ItemStack stack = group.views.get(index);
                long amount = index < counts.length ? counts[index] : stack.getCount();
                ItemView view = new ItemView(stack.copyWithCount((int) Math.clamp(amount, 1, Integer.MAX_VALUE)));
                view.amountText = accessor.showDetails() ? Long.toString(amount) : FormattingUtil.toAbbrNum(amount);
                views.add(view);
            }
            ClientViewGroup<ItemView> client = new ClientViewGroup<>(views);
            client.title = Component.translatable("tooltip.anvilcraft.storage_port." + group.id);
            result.add(client);
        }
        return result;
    }

    @Override
    public ResourceLocation getUid() {
        return StoragePortProvider.UID;
    }
}
