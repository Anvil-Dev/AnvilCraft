package dev.dubhe.anvilcraft.mixin.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractCookingRecipe.class)
public interface HandbookCookingAccessor {
    @Invoker("furnaceIcon")
    Item anvilcraft$furnaceIcon();
}
