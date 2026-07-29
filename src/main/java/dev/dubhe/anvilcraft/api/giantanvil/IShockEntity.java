package dev.dubhe.anvilcraft.api.giantanvil;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** 提供实体参与巨型铁砧撼地行为的可选能力。 */
public interface IShockEntity {
    /**
     * 返回弹性模式下的弹起高度倍率。零表示实体不参与弹起，二表示目标高度为普通铁砧的两倍。
     */
    default double anvilcraft$getShockBounceHeightMultiplier() {
        return 0.0D;
    }

    /** 返回实体在破坏模式下代表的边框铁砧行为。 */
    default Optional<ShockAnvilBehavior> anvilcraft$getShockAnvilBehavior() {
        return Optional.empty();
    }

    /** 返回实体在弹性模式下代表的底座方块状态。 */
    default Optional<BlockState> anvilcraft$getShockBaseState() {
        return Optional.empty();
    }
}
