package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import snownee.jade.api.Accessor;
import snownee.jade.api.view.ClientViewGroup;
import snownee.jade.api.view.IClientExtensionProvider;
import snownee.jade.api.view.IServerExtensionProvider;
import snownee.jade.api.view.ItemView;
import snownee.jade.api.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public enum CrabTrapStorageProvider implements IServerExtensionProvider<ItemStack>, IClientExtensionProvider<ItemStack, ItemView> {
    INSTANCE;

    @Override
    public List<ViewGroup<ItemStack>> getGroups(Accessor<?> accessor) {
        if (!(accessor.getTarget() instanceof CrabTrapBlockEntity crabTrap)) return null;
        ItemStacksResourceHandler itemHandler = crabTrap.getItemHandler();
        List<ItemStack> items = new ArrayList<>(itemHandler.size());
        for (int slot = 0; slot < itemHandler.size(); slot++) {
            ItemResource resource = itemHandler.getResource(slot);
            if (!resource.isEmpty()) items.add(resource.toStack(itemHandler.getAmountAsInt(slot)));
        }
        return items.isEmpty() ? null : List.of(new ViewGroup<>(items));
    }

    @Override
    public List<ClientViewGroup<ItemView>> getClientGroups(Accessor<?> accessor, List<ViewGroup<ItemStack>> groups) {
        return ClientViewGroup.map(groups, ItemView::new, null);
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("crab_trap");
    }
}
