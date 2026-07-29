package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.api.block.IDamagingHeater;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class HeaterUtil {
    /// 若给定方块是正在工作的加热器，则对实体造成烧伤
    ///
    /// @return 是否实际造成了伤害判定
    public static boolean hurtEntity(Level level, BlockState state, Entity entity) {
        if (!(state.getBlock() instanceof IDamagingHeater heater)
            || !heater.isActive(state)
            || entity.isSteppingCarefully()
            || !(entity instanceof LivingEntity)) {
            return false;
        }
        entity.hurt(ModDamageTypes.heaterBurn(level), 4.0F);
        return true;
    }
}
