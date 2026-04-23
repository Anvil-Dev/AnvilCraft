package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.Lazy;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompatUtil {
    public static final Lazy<Boolean> HAS_JADE = new Lazy<>(() -> Util.isLoaded("jade") || Util.isLoaded("wthit"));
    /**
     * 用于余烬砂轮和浮霜砂轮。
     * 将会使用列表内的数据组件类型从输入的物品中获取魔咒并加入魔咒备选列表。
     */
    public static final List<DataComponentType<ItemEnchantments>> ENCHANTMENTS_TYPES = new ArrayList<>();
    public static final Map<Portal, Map.Entry<BlockState, CompoundTag>> PORTAL_DEFAULT_CONVERSION = new HashMap<>(Map.of(
        Util.cast(Blocks.END_PORTAL), Map.entry(ModBlocks.END_DUST.getDefaultState(), new CompoundTag()),
        Util.cast(Blocks.NETHER_PORTAL), Map.entry(ModBlocks.NETHER_DUST.getDefaultState(), new CompoundTag())
    ));
}
