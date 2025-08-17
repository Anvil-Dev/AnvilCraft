package dev.dubhe.anvilcraft.item.amulet;

import dev.dubhe.anvilcraft.api.amulet.type.AmuletType;
import dev.dubhe.anvilcraft.api.item.IMultipleResult;
import dev.dubhe.anvilcraft.recipe.multiple.MultipleToOneSmithingRecipeInput;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;

public abstract class BigAmuletItem extends AmuletItem implements IMultipleResult {
    public BigAmuletItem(Properties properties) {
        super(properties);
    }

    public abstract Holder<AmuletType> getType();

    public int getWeight() {
        return 9;
    }

    @Override
    public ItemStack assemble(int id, MultipleToOneSmithingRecipeInput input, HolderLookup.Provider registries) {
        return this.getDefaultInstance();
    }
}
