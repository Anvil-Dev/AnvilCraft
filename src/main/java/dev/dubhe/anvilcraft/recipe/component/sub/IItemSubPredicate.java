package dev.dubhe.anvilcraft.recipe.component.sub;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.recipe.anvil.util.ISerializer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public interface IItemSubPredicate {
    boolean matches(ItemStack var1);

    record Type<T extends IItemSubPredicate>(
        MapCodec<T> codec,
        StreamCodec<RegistryFriendlyByteBuf, T> streamCodec
    ) implements ISerializer<T> {
    }
}
