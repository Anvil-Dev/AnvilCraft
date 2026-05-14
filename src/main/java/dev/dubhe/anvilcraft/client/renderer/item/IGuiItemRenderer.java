package dev.dubhe.anvilcraft.client.renderer.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

public interface IGuiItemRenderer {
    void renderItem(
        @Nullable LivingEntity entity,
        @Nullable Level level,
        ItemStack stack,
        int x,
        int y,
        int seed
    );
}
