package dev.dubhe.anvilcraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class HasMobBlockItem extends BlockItem {
    public HasMobBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static boolean hasMob(@NotNull ItemStack stack) {
        return stack.getOrCreateTag().contains("entity");
    }

    /**
     * 获取物品中的实体
     */
    public static @Nullable Entity getMobFromItem(Level level, @NotNull ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        if (!tag.contains("entity")) return null;
        CompoundTag entityTag = tag.getCompound("entity");
        Optional<EntityType<?>> optional = EntityType.by(entityTag);
        if (optional.isEmpty()) return null;
        EntityType<?> type = optional.get();
        Entity entity = type.create(level);
        if (entity == null) return null;
        entity.load(entityTag);
        return entity;
    }
}
