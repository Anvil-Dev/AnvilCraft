package dev.dubhe.anvilcraft.recipe.transform;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import lombok.Getter;

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

    public static MobTransformInput of(LivingEntity entity) {
        return new MobTransformInput(entity);
    }
}
