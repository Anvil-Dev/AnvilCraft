package dev.dubhe.anvilcraft.api.entity;

import dev.dubhe.anvilcraft.util.GravityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/// 供附属扩展实体重力、磁化和碰撞合成行为的接口
public interface IAnvilCraftEntityExtension {
    /// 返回自定义重力类型，返回 null 时使用默认判定
    default @Nullable GravityType anvilcraft$getGravityType() {
        return null;
    }

    /// 返回附加在基础重力之上的额外重力
    default Vec3 anvilcraft$getAdditionalGravity(double baseGravity) {
        return Vec3.ZERO;
    }

    /// 实体当前是否处于被磁化状态
    default boolean anvilcraft$isMagnetized() {
        return false;
    }

    /// 实体是否允许触发碰撞合成
    default boolean anvilcraft$canCollisionCraft() {
        return true;
    }

    /// 实体是否接受来自玩家手持物品的磁化
    default boolean anvilcraft$acceptMagnetization(Player player, ItemStack stack) {
        return false;
    }
}
