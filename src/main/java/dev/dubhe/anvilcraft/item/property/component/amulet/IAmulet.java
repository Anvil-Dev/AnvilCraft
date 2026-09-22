package dev.dubhe.anvilcraft.item.property.component.amulet;

import dev.anvilcraft.lib.v2.util.ISerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/// 护符类
public interface IAmulet {
    int SMALL_AMULET_WEIGHT = 6;
    int BIG_AMULET_WEIGHT = 9;

    /// 在物品栏内时调用。<br>
    /// 用于执行护符效果。若需判断是否免疫伤害源，请参阅 {@link IAmulet#shouldImmune(ServerPlayer, ItemStack, DamageSource)}。
    ///
    /// @param player    玩家
    /// @param amulet    护符物品堆
    /// @param isEnabled 护符启用状态。`true` 为已启用，反之则为未启用
    /// @see IAmulet#shouldImmune(ServerPlayer, ItemStack, DamageSource)
    default void inventoryTick(ServerPlayer player, ItemStack amulet, boolean isEnabled) {
    }

    /// 根据给定数据判断是否免疫给定伤害源。<br>
    /// 用于判断是否免疫伤害源。若需执行护符效果，请参阅 {@link IAmulet#inventoryTick(ServerPlayer, ItemStack, boolean)}。
    ///
    /// @param player 玩家
    /// @param amulet 护符物品堆
    /// @param source 伤害源
    /// @return 是否免疫给定伤害源
    /// @see IAmulet#inventoryTick(ServerPlayer, ItemStack, boolean)
    default boolean shouldImmune(ServerPlayer player, ItemStack amulet, DamageSource source) {
        return false;
    }

    /// 获取该护符能额外充当的其它护符
    ///
    /// @return 该护符能额外充当的其它护符的资源键，不包含其自身
    default List<ResourceKey<IAmulet>> canActLike() {
        return List.of();
    }

    Type<? extends IAmulet> getType();

    interface Type<T extends IAmulet> extends ISerializer<T> {
    }
}
