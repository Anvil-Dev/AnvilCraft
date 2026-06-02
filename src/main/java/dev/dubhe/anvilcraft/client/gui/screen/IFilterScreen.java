package dev.dubhe.anvilcraft.client.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.api.itemhandler.SlotItemHandlerWithFilter;
import dev.dubhe.anvilcraft.client.gui.component.EnableFilterButton;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import dev.dubhe.anvilcraft.network.MachineEnableFilterPacket;
import dev.dubhe.anvilcraft.network.SlotDisableChangePacket;
import dev.dubhe.anvilcraft.network.SlotFilterChangePacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

/**
 * 有过滤的 GUI
 */
public interface IFilterScreen<T extends AbstractContainerMenu & IFilterMenu> extends IGhostIngredientScreen {
    Component SCROLL_WHEEL_TO_CHANGE_STACK_LIMIT_TOOLTIP = Component.translatable(
        "screen.anvilcraft.filter.scroll_wheel_to_change_stack_limit")
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
    Component SHIFT_TO_SCROLL_FASTER_TOOLTIP = Component.translatable("screen.anvilcraft.filter.shift_to_scroll_faster")
            .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);

    T getFilterMenu();

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
    @SuppressWarnings("UnusedReturnValue")
    default boolean setFilter(int slot, ItemStack filter) {
        return this.getFilterMenu().setFilter(slot, filter);
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
     * 获取指定槽位的物品上限
     *
     * @param slot 槽位
     */
    default int getSlotLimit(int slot) {
        return this.getFilterMenu().getSlotLimit(slot);
    }

    /**
     * 设置指定槽位的物品上限
     *
     * @param slot  槽位
     * @param limit 物品上限
     */
    default void setSlotLimit(int slot, int limit) {
        this.getFilterMenu().setSlotLimit(slot, limit);
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
        return (i, j) -> new EnableFilterButton(
            i + x,
            j + y,
            button -> {
                if (button instanceof EnableFilterButton enableFilterButton) {
                    MachineEnableFilterPacket packet = new MachineEnableFilterPacket(enableFilterButton.next());
                    PacketDistributor.sendToServer(packet);
                }
            },
            this::isFilterEnabled);
    }

    /**
     * 渲染槽位
     *
     * @param guiGraphics 画布
     * @param slot        槽位
     */
    default void renderSlot(GuiGraphics guiGraphics, Slot slot) {
        if (!(slot instanceof SlotItemHandlerWithFilter crafterSlot)) return;
        if (!crafterSlot.isFilter()) return;
        if (this.isSlotDisabled(slot.getContainerSlot())) {
            this.renderDisabledSlot(guiGraphics, crafterSlot);
            return;
        }
        ItemStack filter = this.getFilter(slot.getContainerSlot());
        if (!slot.hasItem() && !filter.isEmpty()) {
            this.renderFilterItem(guiGraphics, slot, filter);
        }
        this.renderSlotLimit(guiGraphics, slot);
    }

    /**
     * 渲染禁用的槽位
     *
     * @param guiGraphics 画布
     * @param crafterSlot 槽位
     */
    default void renderDisabledSlot(GuiGraphics guiGraphics, Slot crafterSlot) {
        RenderSystem.enableDepthTest();
        guiGraphics.blit(SharedTextures.DISABLED_SLOT, crafterSlot.x, crafterSlot.y, 0, 0, 16, 16, 16, 16);
    }

    /**
     * 渲染过滤物品
     *
     * @param guiGraphics 画布
     * @param slot        槽位
     * @param stack       物品堆叠
     */
    default void renderFilterItem(GuiGraphics guiGraphics, Slot slot, ItemStack stack) {
        int i = slot.x;
        int j = slot.y;
        RenderSupport.renderItemWithTransparency(stack, guiGraphics.pose(), i, j, 0.52f);
        guiGraphics.fill(i, j, i + 16, j + 16, 0x60ffaaaa);
    }

    default void renderSlotLimit(GuiGraphics guiGraphics, Slot slot) {
        if (!(slot instanceof SlotItemHandlerWithFilter filterSlot) || !filterSlot.isFilter()) return;
        int slotIndex = slot.getContainerSlot();
        int limit = this.getSlotLimit(slotIndex);
        if (limit == 64) return;
        String text = String.valueOf(limit);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        float scale = 0.6f;
        guiGraphics.pose().scale(scale, scale, 1.0f);
        int width = Minecraft.getInstance().font.width(text);
        int height = Minecraft.getInstance().font.lineHeight;
        int x = (int) ((slot.x + 16.25 - width * scale) / scale);
        int y = (int) ((slot.y + 14 - height * 2 * scale + 1) / scale);
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            text,
            x,
            y,
            0xFFA0A0,
            true
        );
        guiGraphics.pose().popPose();
    }

    default int getOffsetY() {
        return 0;
    }

    default int getOffsetX() {
        return 0;
    }

    @Override
    default Collection<Integer> getGhostSlots() {
        if (!this.getFilterMenu().isFilterEnabled()) return List.of();
        return IGhostIngredientScreen.range(36, 45, 1);
    }

    @Override
    default void acceptGhost(Slot slot, ItemStack ingredient) {
        if (!this.getFilterMenu().isFilterEnabled()) return;
        int slotIndex = slot.getSlotIndex();
        PacketDistributor.sendToServer(new SlotDisableChangePacket(slotIndex, false));
        PacketDistributor.sendToServer(new SlotFilterChangePacket(slotIndex, ingredient.copyWithCount(1)));
        this.getFilterMenu().setFilter(slotIndex, ingredient.copyWithCount(1));
    }
}
