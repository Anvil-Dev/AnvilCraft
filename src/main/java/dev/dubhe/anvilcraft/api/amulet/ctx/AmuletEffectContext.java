package dev.dubhe.anvilcraft.api.amulet.ctx;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;

public class AmuletEffectContext {
    private final Map<AmuletEffectContextKey<?>, Object> ctx = new HashMap<>();

    /// 写入上下文的值
    ///
    /// @param key   上下文键
    /// @param value 上下文值
    /// @param <T>   上下文值的类型
    public <T> void set(AmuletEffectContextKey<T> key, T value) {
        this.ctx.put(key, value);
    }

    /// 移除上下文的值
    ///
    /// @param key 上下文键
    /// @param <T> 上下文值的类型
    /// @return 被移除的上下文值，不存在时为 null
    public <T> @Nullable T remove(AmuletEffectContextKey<T> key) {
        return key.clazz().cast(this.ctx.remove(key));
    }

    public <T> Optional<T> get(AmuletEffectContextKey<T> key) {
        return Optional.ofNullable(this.ctx.get(key)).map(key.clazz()::cast);
    }

    public <T> T getOrDefault(AmuletEffectContextKey<T> key, T defaultValue) {
        return this.get(key).orElse(defaultValue);
    }

    public <T> T getOrThrow(AmuletEffectContextKey<T> key) {
        return key.clazz().cast(Objects.requireNonNull(this.ctx.get(key), () -> "Cannot find context for key " + key.id()));
    }
}
