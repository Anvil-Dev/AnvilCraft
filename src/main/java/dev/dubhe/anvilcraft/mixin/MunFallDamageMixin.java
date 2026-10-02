package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialTravelManager;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public class MunFallDamageMixin {
    @ModifyReturnValue(method = "calculateFallDamage", at = @At("RETURN"))
    private int anvilcraft$preserveMunFallRounding(int original, double distance, float multiplier) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (!entity.level().dimension().equals(CelestialTravelManager.MUN_LEVEL)
            || entity.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) return original;
        // 26.1 uses floor; the source Mun event feeds a distance into 1.21's float/ceil calculation.
        float excess = (float) distance - (float) entity.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        return Mth.ceil((double) (excess * multiplier) * entity.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
    }
}
