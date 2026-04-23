package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.Lazy;
import dev.anvilcraft.lib.v2.util.Util;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;

public class CompatUtil {
    public static final Lazy<Boolean> HAS_JADE = new Lazy<>(() -> Util.isLoaded("jade") || Util.isLoaded("wthit"));
    /**
     * 用于余烬砂轮和浮霜砂轮。
     * 将会使用列表内的数据组件类型从输入的物品中获取魔咒并加入魔咒备选列表。
     */
    public static final List<DataComponentType<ItemEnchantments>> ENCHANTMENTS_TYPES = new ArrayList<>();
}
