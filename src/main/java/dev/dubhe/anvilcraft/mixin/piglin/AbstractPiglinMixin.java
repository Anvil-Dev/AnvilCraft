package dev.dubhe.anvilcraft.mixin.piglin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static dev.dubhe.anvilcraft.init.ModDataAttachments.ZOMBIFICATED_BY_CURSE;

@Mixin(AbstractPiglin.class)
public class AbstractPiglinMixin {

    @WrapOperation(
        method = "finishConversion",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/monster/piglin/AbstractPiglin;convertTo(Lnet/minecraft/world/entity/EntityType;Z)Lnet/minecraft/world/entity/Mob;"
        )
    )
    private Mob punishmentForGreed(
        AbstractPiglin piglin,
        EntityType<ZombifiedPiglin> type,
        boolean transferInventory,
        Operation<ZombifiedPiglin> original) {
        boolean cursed = piglin.getData(ZOMBIFICATED_BY_CURSE);
        Mob zombifiedPiglin = original.call(piglin, type, transferInventory);
        if (cursed) {
            zombifiedPiglin.setData(ZOMBIFICATED_BY_CURSE, true);
        }
        return zombifiedPiglin;
    }
}
