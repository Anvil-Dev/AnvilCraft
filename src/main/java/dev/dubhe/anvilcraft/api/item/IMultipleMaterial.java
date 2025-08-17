package dev.dubhe.anvilcraft.api.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 多合一配方的材料
 */
public interface IMultipleMaterial {
    /**
     * 获取在余烬锻造台内鼠标悬停时显示的tooltip
     *
     * @return 需要显示的tooltip
     */
    Component getInputTooltip(ItemStack template, List<ItemStack> inputs);

    /**
     * 获取在余烬锻造台内对应空槽位显示的纹理集
     *
     * @param template 作为模板的物品
     * @param id       槽位id
     * @param inputs   其它槽位内的物品
     * @return 该槽位应显示的纹理集
     */
    List<ResourceLocation> getEmptySlotTextures(ItemStack template, int id, List<ItemStack> inputs);
}
