package dev.dubhe.anvilcraft.mixin.accessor;

import dev.anvilcraft.lib.v2.recipe.cache.item.ItemResourceHandlerCacheElement;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemResourceHandlerCacheElement.class)
public interface ItemResourceHandlerCacheElementAccessor {
    @Accessor("iItemHandler")
    ResourceHandler<ItemResource> getItemHandler();
}
