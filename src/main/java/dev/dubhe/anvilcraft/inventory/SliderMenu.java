package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.util.Callback;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@Getter
public class SliderMenu extends AbstractContainerMenu {
    private final Callback<Integer> callback;

    public SliderMenu(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
        this.callback = null;
    }

    public SliderMenu(int containerId, Callback<Integer> callback) {
        super(ModMenuTypes.SLIDER.get(), containerId);
        this.callback = callback;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot sourceSlot = slots.get(index);
        // noinspection ConstantValue
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        return sourceSlot.getItem();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
