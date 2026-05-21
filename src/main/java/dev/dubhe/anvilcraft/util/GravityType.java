package dev.dubhe.anvilcraft.util;

import lombok.Getter;

// 特殊物质定义不同引力类型
public enum GravityType {
    NORMAL(1), // 正常重力
    ANTI_GRAVITY(-1), // 反转重力
    MICRO_ANTI_GRAVITY(-0.005), // 略有失重感
    LOW_GRAVITY(0.5), // 低重力
    ;

    @Getter
    private final double scalar;

    GravityType(double scalar) {
        this.scalar = scalar;
    }
}
