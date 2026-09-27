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
    /// @param entity 佩戴护符的实体
    /// @param amulet 提供该效果的护符物品堆，未启用时为 {@link ItemStack#EMPTY}
    /// @param ctx    本次触发的上下文
    void trigger(LivingEntity entity, ItemStack amulet, AmuletEffectContext ctx);

    /// 是否应当忽略重复。
    ///
    /// 在 {@link dev.dubhe.anvilcraft.api.amulet.AmuletManager#getActiveEffects(LivingEntity)} 内非第一次找到该效果时调用。
    ///
    /// @param entity 佩戴护符的实体
    /// @param amulet 提供该效果的护符物品堆
    /// @return 需要忽略重复（将该效果的护符物品堆加入结果）时返回 `true`
    default boolean shouldIgnoreRepetition(LivingEntity entity, ItemStack amulet) {
        return true;
    }

    /// 获取该效果展开后的效果，包含其包覆的其它护符的效果。
    ///
    /// 护符注册在静态注册表里，展开时不需要注册表访问器。
    ///
    /// @return 该效果展开后的效果
    /// @apiNote 由于此方法实现上可能有缓存机制，不允许在注册完成前调用！
    default @Unmodifiable Set<IAmuletEffect> flatten() {
        return ImmutableSet.of(this);
    }
}
