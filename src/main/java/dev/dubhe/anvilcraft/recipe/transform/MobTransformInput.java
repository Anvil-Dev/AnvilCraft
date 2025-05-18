package dev.dubhe.anvilcraft.recipe.transform;

import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@Getter
public class MobTransformInput implements RecipeInput {
    private final LivingEntity inputEntity;

    MobTransformInput(LivingEntity inputEntity) {
        this.inputEntity = inputEntity;
    }

    @Override
    public ItemStack getItem(int i) {
        return ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    public static MobTransformInput of(LivingEntity entity) {
        return new MobTransformInput(entity);
    }
}
