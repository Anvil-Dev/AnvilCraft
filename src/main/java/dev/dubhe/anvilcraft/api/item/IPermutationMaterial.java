package dev.dubhe.anvilcraft.api.item;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 嬗变配方的材料
 */
public interface IPermutationMaterial {
    /**
     * 获取在浮霜锻造台内鼠标悬停时显示的tooltip
     *
     * @param material 作为材料的物品
     * @return 需要显示的tooltip
     */
    Component getInputTooltip(ItemStack material);

    /**
     * 获取在浮霜锻造台内对应空槽位显示的纹理集
     *
     * @return 该槽位应显示的纹理集
     */
    List<ResourceLocation> getEmptySlotTextures();
}
