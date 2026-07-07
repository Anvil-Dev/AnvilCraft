package dev.dubhe.anvilcraft.integration.jade.provider.client;

import dev.dubhe.anvilcraft.integration.jade.provider.CreativeCrateProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public enum CreativeCrateClientProvider implements IClientExtensionProvider<ItemStack, ItemView> {
    INSTANCE;

    @Override
    public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
        List<ClientViewGroup<ItemView>> list = new ArrayList<>();
        for (ViewGroup<ItemStack> itemStackViewGroup : groups) {
            List<ItemView> itemViewList = new ArrayList<>();
            for (ItemStack view : itemStackViewGroup.views) {
                ItemView itemView = new ItemView(view);
                itemView.amountText = "∞";
                itemViewList.add(itemView);
            }
            list.add(new ClientViewGroup<>(itemViewList));
        }
        return list;
    }

    @Override
    public Identifier getUid() {
        return CreativeCrateProvider.UID;
    }
}
