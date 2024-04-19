package dev.dubhe.anvilcraft.client.gui.screen.inventory;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.depository.ItemDepositorySlot;
import dev.dubhe.anvilcraft.client.gui.component.EnableFilterButton;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import dev.dubhe.anvilcraft.network.MachineEnableFilterPack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;

/**
 * 有过滤的 GUI
 */
public interface IFilterScreen {
    ResourceLocation DISABLED_SLOT = AnvilCraft.of("textures/gui/container/machine/disabled_slot.png");

    IFilterMenu getFilterMenu();

    /**
     * 获取是否开启过滤
     *
     * @return 是否开启过滤
     */
    default boolean isFilterEnabled() {
        return this.getFilterMenu().isFilterEnabled();
    }

    /**
     * 设置是否开启过滤
     *
     * @param enable 是否开启过滤
     */
    default void setFilterEnabled(boolean enable) {
        this.getFilterMenu().setFilterEnabled(enable);
    }

    /**
     * 设置指定槽位是否禁用
     *
     * @param slot    槽位
     * @param disable 是否禁用
     */
    default void setSlotDisabled(int slot, boolean disable) {
        this.getFilterMenu().setSlotDisabled(slot, disable);
    }

    /**
     * 获取指定槽位是否禁用
     *
     * @param slot 槽位
     */
    default boolean isSlotDisabled(int slot) {
        return this.getFilterMenu().isSlotDisabled(slot);
    }

    /**
     * 设置指定槽位的过滤
     *
     * @param slot   槽位
     * @param filter 过滤
     */
    default void setFilter(int slot, ItemStack filter) {
        this.getFilterMenu().setFilter(slot, filter);
    }

    /**
     * 获取指定槽位的过滤
     *
     * @param slot 槽位
     */
    default ItemStack getFilter(int slot) {
        return this.getFilterMenu().getFilter(slot);
    }

    /**
     * 刷新
     */
    default void flush() {
    }

    /**
     * 获取一个生成启用过滤按钮的生成器
     *
     * @param x 按钮 X 坐标
     * @param y 按钮 Y 坐标
     * @return 生成启用过滤按钮的生成器
     */
    default BiFunction<Integer, Integer, EnableFilterButton> getEnableFilterButtonSupplier(int x, int y) {
        return (i, j) -> new EnableFilterButton(i + x, j + y, button -> {
            if (button instanceof EnableFilterButton enableFilterButton) {
                MachineEnableFilterPack packet = new MachineEnableFilterPack(enableFilterButton.next());
                packet.send();
            }
        }, this::isFilterEnabled);
    }

    /**
     * 渲染槽位
     *
     * @param guiGraphics 画布
     * @param slot        槽位
     */
    default void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (!(slot instanceof ItemDepositorySlot crafterSlot)) return;
        if (!crafterSlot.isFilter()) return;
        if (this.isSlotDisabled(slot.getContainerSlot())) {
            this.renderDisabledSlot(guiGraphics, crafterSlot);
            return;
        }
        ItemStack filter = this.getFilter(slot.getContainerSlot());
        if (!slot.hasItem() && !filter.isEmpty()) {
            this.renderFilterItem(guiGraphics, slot, filter);
        }
    }

    /**
     * 渲染禁用的槽位
     *
     * @param guiGraphics 画布
     * @param crafterSlot 槽位
     */
    default void renderDisabledSlot(@NotNull GuiGraphics guiGraphics, @NotNull Slot crafterSlot) {
        RenderSystem.enableDepthTest();
        guiGraphics.blit(DISABLED_SLOT, crafterSlot.x, crafterSlot.y, 0, 0, 16, 16, 16, 16);
    }

    /**
     * 渲染过滤物品
     *
     * @param guiGraphics 画布
     * @param slot        槽位
     * @param stack       物品堆栈
     */
    default void renderFilterItem(@NotNull GuiGraphics guiGraphics, @NotNull Slot slot, @NotNull ItemStack stack) {
        int i = slot.x;
        int j = slot.y;
        guiGraphics.renderFakeItem(stack, i, j);
        guiGraphics.fill(i, j, i + 16, j + 16, 0x80ffaaaa);
    }
}
