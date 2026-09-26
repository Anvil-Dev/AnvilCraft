package dev.dubhe.anvilcraft.api.amulet.effect;

import com.google.common.collect.ImmutableSet;
import dev.dubhe.anvilcraft.api.amulet.ctx.AmuletEffectContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;

/// 护符效果。<br>
/// 由 {@link dev.dubhe.anvilcraft.api.amulet.AmuletManager 护符管理器} 在护符生命周期的各个阶段触发，
/// 需要处理的阶段由 {@link AmuletEffectContext} 中已存在的上下文键决定，处理结果同样以上下文键的形式写回。
///
/// <p>触发分两类：生命周期触发每 tick 进行一次，上下文里必有
/// {@link dev.dubhe.anvilcraft.init.item.ModAmuletEffectContextKeys#ENABLED} 键；查询触发的上下文只带该次查询
/// 相关的键、用完即弃。因此只有需要在佩戴/脱下时增删状态的副作用效果（属性修饰符、药水效果等）才需要依据
/// {@code ENABLED} 决定是否执行，只写回结果键的效果不必关心它。</p>
public interface IAmuletEffect {
    /// 触发该效果。
    ///
    /// @param entity 佩戴护符的玩家
    /// @param amulet 提供该效果的护符物品堆，未启用时为 {@link ItemStack#EMPTY}
    /// @param ctx    本次触发的上下文
    void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx);

    /// 获取该效果展开后的效果，包含其包覆的其它护符的效果。
    ///
    /// <p>护符注册在静态注册表里，展开时不需要注册表访问器。</p>
    ///
    /// @return 该效果展开后的效果
    default @Unmodifiable Set<IAmuletEffect> flatten() {
        return ImmutableSet.of(this);
    }
}
