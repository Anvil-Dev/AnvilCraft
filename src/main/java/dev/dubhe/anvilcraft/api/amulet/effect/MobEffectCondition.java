package dev.dubhe.anvilcraft.api.amulet.effect;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import net.minecraft.world.entity.player.Player;

/// 护符效果的触发条件
public enum MobEffectCondition {
    ALWAYS,
    ON_FIRE,
    NOT_ON_FIRE,
    IN_WATER_OR_BREATHING,
    SNEAKING,
    NOT_SNEAKING;

    public static final Codec<MobEffectCondition> CODEC = CodecUtil.enumCodecInLowerName(MobEffectCondition.class);

    public boolean test(Player player) {
        return switch (this) {
            case ALWAYS -> true;
            case ON_FIRE -> player.isOnFire();
            case NOT_ON_FIRE -> !player.isOnFire();
            case IN_WATER_OR_BREATHING -> player.isInWater() || EquipmentAbilities.canBreathe(player);
            case SNEAKING -> player.isShiftKeyDown();
            case NOT_SNEAKING -> !player.isShiftKeyDown();
        };
    }
}
