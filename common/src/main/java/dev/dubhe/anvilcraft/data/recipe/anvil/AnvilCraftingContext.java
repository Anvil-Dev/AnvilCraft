package dev.dubhe.anvilcraft.data.recipe.anvil;

import lombok.Getter;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 铁砧合成上下文
 */
public class AnvilCraftingContext
    implements Container, StackedContentsCompatible {
    @Getter
    private final Level level;
    @Getter
    private final BlockPos pos;
    @Getter
    private final FallingBlockEntity entity;
    @Getter
    private boolean isAnvilDamage = false;
    private final Set<Map.Entry<Vec3, ItemStack>> outputs = new HashSet<>();
    private final Map<String, Object> data = new HashMap<>();

    /**
     * 初始化 AnvilCraftingContext
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
     * 设置是否强制铁砧损坏
     *
     * @param anvilDamage true 表示强制铁砧损坏，false 表示不强制铁砧损坏
     */
    public void setAnvilDamage(boolean anvilDamage) {
        isAnvilDamage = anvilDamage;
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

    /**
     * 向数据集合中添加键值对
     *
     * @param key 键，用于唯一标识数据项
     * @param value 值，可以是任意类型的数据
     * @param <T> 泛型参数，表示值的类型
     */
    public <T> void addData(String key, T value) {
        this.data.put(key, value);
    }


    /**
     * 根据键和类型从数据映射中获取数据
     * <p>
     * 此方法用于从存储对象的映射中安全地检索特定类型的数据
     * <p>
     * 如果键不存在，或者存储的值不是指定的类型，则返回 null
     *
     * @param key 要检索的数据项的键
     * @param typeOfT 要检索的数据项的类型
     * @return <T> 如果找到键且其值匹配指定类型，则返回该值；否则返回 null
     */
    public <T> T getData(String key, Class<T> typeOfT) {
        // 从映射中获取与键关联的对象
        Object o = this.data.get(key);
        // 如果对象不存在，则直接返回null
        if (o == null) return null;
        // 检查对象是否是请求的类型，如果不是，则返回null
        if (!typeOfT.isInstance(o)) return null;
        // 类型检查通过，安全地将对象转换为请求的类型并返回
        //noinspection unchecked
        return (T) o;
    }
    
    public AnvilCraftingContext clearData() {
        this.data.clear();
        return this;
    }
}
