package dev.dubhe.anvilcraft.api.amulet.ctx;

import net.minecraft.resources.Identifier;

/// 护符效果上下文的键，由 id 与值的类型共同确定
///
/// @param id    键的 id
/// @param clazz 值的类型
/// @param <T>   值的类型
public record AmuletEffectContextKey<T>(Identifier id, Class<T> clazz) {
    /// 创建一个上下文键
    ///
    /// @param id    键的 id
    /// @param clazz 值的类型
    /// @param <T>   值的类型
    /// @return 上下文键
    public static <T> AmuletEffectContextKey<T> of(Identifier id, Class<T> clazz) {
        return new AmuletEffectContextKey<>(id, clazz);
    }

    /// 创建一个值为 {@link Boolean} 的上下文键
    ///
    /// @param id 键的 id
    /// @return 值为 {@link Boolean} 的上下文键
    public static AmuletEffectContextKey<Boolean> ofBool(Identifier id) {
        return AmuletEffectContextKey.of(id, Boolean.class);
    }

    /// 创建一个值为 {@link Byte} 的上下文键
    ///
    /// @param id 键的 id
    /// @return 值为 {@link Byte} 的上下文键
    public static AmuletEffectContextKey<Byte> ofByte(Identifier id) {
        return AmuletEffectContextKey.of(id, Byte.class);
    }

    /// 创建一个值为 {@link Short} 的上下文键
    ///
    /// @param id 键的 id
    /// @return 值为 {@link Short} 的上下文键
    public static AmuletEffectContextKey<Short> ofShort(Identifier id) {
        return AmuletEffectContextKey.of(id, Short.class);
    }

    /// 创建一个值为 {@link Integer} 的上下文键
    ///
    /// @param id 键的 id
    /// @return 值为 {@link Integer} 的上下文键
    public static AmuletEffectContextKey<Integer> ofInt(Identifier id) {
        return AmuletEffectContextKey.of(id, Integer.class);
    }

    /// 创建一个值为 {@link Long} 的上下文键
    ///
    /// @param id 键的 id
    /// @return 值为 {@link Long} 的上下文键
    public static AmuletEffectContextKey<Long> ofLong(Identifier id) {
        return AmuletEffectContextKey.of(id, Long.class);
    }

    /// 创建一个值为 {@link Float} 的上下文键
    ///
    /// @param id 键的 id
    /// @return 值为 {@link Float} 的上下文键
    public static AmuletEffectContextKey<Float> ofFloat(Identifier id) {
        return AmuletEffectContextKey.of(id, Float.class);
    }

    /// 创建一个值为 {@link Double} 的上下文键
    ///
    /// @param id 键的 id
    /// @return 值为 {@link Double} 的上下文键
    public static AmuletEffectContextKey<Double> ofDouble(Identifier id) {
        return AmuletEffectContextKey.of(id, Double.class);
    }
}
