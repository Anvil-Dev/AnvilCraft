package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.util.Callback;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class SliderMenu extends AbstractContainerMenu {
    private final int min;
    private final int max;
    private final Callback<Integer> callback;

    /**
     * @param menuType    菜单类型
     * @param containerId 容器ID
     */
    public SliderMenu(@Nullable MenuType<SliderMenu> menuType, int containerId) {
        super(menuType, containerId);
        this.min = 0;
        this.max = 160;
        this.callback = null;
    }

    /**
     * @param containerId 容器ID
     * @param callback    更新回调
     */
    public SliderMenu(int containerId, int min, int max, Callback<Integer> callback) {
        super(ModMenuTypes.SLIDER.get(), containerId);
        this.min = min;
        this.max = max;
        this.callback = callback;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot sourceSlot = slots.get(index);
        //noinspection ConstantValue
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        return sourceSlot.getItem();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }
}
