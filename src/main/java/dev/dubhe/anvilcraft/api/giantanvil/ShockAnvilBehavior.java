package dev.dubhe.anvilcraft.api.giantanvil;

import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/** 描述一个边框铁砧在撼地破坏模式中的挖掘与掉落行为。 */
public record ShockAnvilBehavior(BlockMiningEffect miningEffect, ShockDropBehavior dropBehavior) {
    public static final ShockAnvilBehavior NORMAL = new ShockAnvilBehavior(BlockMiningEffect.NORMAL);

    public ShockAnvilBehavior {
        Objects.requireNonNull(miningEffect, "miningEffect");
        Objects.requireNonNull(dropBehavior, "dropBehavior");
    }

    public ShockAnvilBehavior(BlockMiningEffect miningEffect) {
        this(miningEffect, ShockDropBehavior.DEFAULT);
    }

    /** 将本体方块铁砧的挖掘效果转换为默认掉落行为。 */
    public static ShockAnvilBehavior fromMiningEffect(BlockMiningEffect effect) {
        if (effect.equals(BlockMiningEffect.NORMAL)) return ShockAnvilBehavior.NORMAL;
        return new ShockAnvilBehavior(effect);
    }

    /** 判断两个边框位置是否可以组成同一套撼地破坏结构。 */
    public boolean isCompatibleWith(@Nullable ShockAnvilBehavior other) {
        return other != null
            && this.miningEffect.equals(other.miningEffect)
            && this.dropBehavior.id().equals(other.dropBehavior.id());
    }
}
