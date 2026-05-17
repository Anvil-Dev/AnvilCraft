package dev.dubhe.anvilcraft.mixin.piglin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(AbstractPiglin.class)
abstract class AbstractPiglinMixin {

    @WrapOperation(
        method = "finishConversion",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/piglin/AbstractPiglin;"
                     + "convertTo("
                     + "Lnet/minecraft/world/entity/EntityType;"
                     + "Lnet/minecraft/world/entity/ConversionParams;"
                     + "Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)"
                     + "Lnet/minecraft/world/entity/Mob;"
        )
    )
    private <T extends Mob> T punishmentForGreed(
        AbstractPiglin instance,
        EntityType<T> entityType,
        ConversionParams conversionParams,
        ConversionParams.AfterConversion<T> afterConversion,
        Operation<T> original
    ) {
        boolean cursed = instance.getData(ModDataAttachments.ZOMBIFICATED_BY_CURSE);
        T zombifiedPiglin = original.call(instance, entityType, conversionParams, afterConversion);
        if (cursed) {
            zombifiedPiglin.setData(ModDataAttachments.ZOMBIFICATED_BY_CURSE, true);
        }
        return zombifiedPiglin;
    }
}
