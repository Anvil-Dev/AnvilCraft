package dev.dubhe.anvilcraft.inventory.component;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * 成书输出槽位(只允许取出,不允许放入),支持条件可见性
 *
 * <p>可指定一个「消耗源」容器：取出产物时消耗其对应槽位的一件物品。
 * 材料清单书由输入书生成，若取出时不消耗输入书，同一本书可反复产出成书。
 */
public class WrittenBookOnlySlot extends Slot {
    @Nullable
    private final BooleanSupplier visibilityCondition;
    /** 取出产物时要一并消耗的容器；为 null 表示不消耗 */
    private final @Nullable Container consumeSource;
    private final int consumeSlot;

    public WrittenBookOnlySlot(Container container, int slot, int x, int y,
                              @Nullable BooleanSupplier visibilityCondition) {
        this(container, slot, x, y, visibilityCondition, null, 0);
    }

    public WrittenBookOnlySlot(
        Container container,
        int slot,
        int x,
        int y,
        @Nullable BooleanSupplier visibilityCondition,
        @Nullable Container consumeSource,
        int consumeSlot
    ) {
        super(container, slot, x, y);
        this.visibilityCondition = visibilityCondition;
        this.consumeSource = consumeSource;
        this.consumeSlot = consumeSlot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        // 输出槽位：禁止放入任何物品
        return false;
    }

    /**
     * 取出产物时消耗输入物，避免同一份输入反复产出产物。
     *
     * <p>{@code shrink} 会触发容器的 {@code setChanged}，进而重新计算输出，
     * 因此输入被清空后输出槽会保持为空。
     */
    @Override
    public void onTake(Player player, ItemStack stack) {
        if (this.consumeSource != null) {
            this.consumeSource.removeItem(this.consumeSlot, 1);
        }
        super.onTake(player, stack);
    }

    @Override
    public boolean isActive() {
        // 如果有可见性条件,检查条件;否则默认激活
        return visibilityCondition == null || visibilityCondition.getAsBoolean();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
