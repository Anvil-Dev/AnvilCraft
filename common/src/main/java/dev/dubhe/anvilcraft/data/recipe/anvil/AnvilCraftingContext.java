package dev.dubhe.anvilcraft.data.recipe.anvil;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.StackedContentsCompatible;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 铁砧合成上下文
 */
@Getter
public class AnvilCraftingContext
    implements Container, StackedContentsCompatible {
    private final Level level;
    private final BlockPos pos;
    private final FallingBlockEntity entity;
    @Setter
    private boolean isAnvilDamage = false;
    private final Set<Map.Entry<Vec3, ItemStack>> outputs = new HashSet<>();

    /**
     * 初始化 AnvilCraftingContainer
     *
     * @param level  世界
     * @param pos    位置
     * @param entity 铁砧实体
     */
    public AnvilCraftingContext(Level level, BlockPos pos, FallingBlockEntity entity) {
        this.level = level;
        this.pos = pos;
        this.entity = entity;
    }

    @Override
    public int getContainerSize() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
    }

    @Override
    public void fillStackedContents(@NotNull StackedContents contents) {
    }

    /**
     * 向指定位置添加输出物品
     *
     * @param pos   相对坐标
     * @param stack 物品
     * @return 添加是否成功
     */
    public boolean addOutputsItem(Vec3 pos, ItemStack stack) {
        stack = stack.copy();
        for (Map.Entry<Vec3, ItemStack> entry : this.outputs) {
            if (stack.isEmpty()) continue;
            if (entry.getKey().distanceTo(pos) > 1) continue;
            ItemStack value = entry.getValue();
            if (!ItemStack.isSameItemSameTags(stack, value)) continue;
            int maxAdd = Math.min(value.getMaxStackSize() - value.getCount(), stack.getCount());
            maxAdd = Math.max(maxAdd, 0);
            stack.shrink(maxAdd);
            value.grow(maxAdd);
        }
        if (stack.isEmpty()) return false;
        return this.outputs.add(Map.entry(pos, stack));
    }

    /**
     * 向世界中输出物品实体
     */
    public void spawnItemEntity() {
        for (Map.Entry<Vec3, ItemStack> entry : this.outputs) {
            ItemStack stack = entry.getValue();
            if (stack.isEmpty()) continue;
            Vec3 center = this.pos.getCenter();
            Vec3 pos = entry.getKey().add(center.x(), center.y(), center.z());
            ItemEntity entity = new ItemEntity(this.level, pos.x(), pos.y(), pos.z(), stack, 0.0, 0.0, 0.0);
            this.level.addFreshEntity(entity);
        }
    }
}
