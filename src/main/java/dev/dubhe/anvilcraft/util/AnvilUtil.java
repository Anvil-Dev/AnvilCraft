package dev.dubhe.anvilcraft.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnvilUtil {
    @SuppressWarnings("DataFlowIssue")
    public static void dropItems(@NotNull List<ItemStack> items, Level level, Vec3 pos) {
        for (ItemStack item : items) {
            if (item.isEmpty()) continue;
            int count = item.getCount();
            int maxStack = item.getMaxStackSize();
            for (; count >= maxStack; count -= maxStack) {
                ItemEntity entity = new ItemEntity(
                    level,
                    pos.x,
                    pos.y,
                    pos.z,
                    item.copyWithCount(maxStack),
                    0.0d,
                    0.0d,
                    0.0d
                );
                level.addFreshEntity(entity);
            }
            if (count <= 0) continue;
            ItemEntity entity = new ItemEntity(
                level,
                pos.x,
                pos.y,
                pos.z,
                item.copyWithCount(count),
                0.0d,
                0.0d,
                0.0d
            );
            entity.anvilcraft$setIsAdsorbable(false);
            level.addFreshEntity(entity);
        }
    }
}
