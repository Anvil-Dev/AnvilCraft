package dev.dubhe.anvilcraft.api.itemhandler;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * 持有ItemHandler的
 */
public interface IItemHandlerHolder {
    ResourceHandler<ItemResource> getItemHandler();
}
