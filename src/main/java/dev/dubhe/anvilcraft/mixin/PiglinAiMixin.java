package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModItems;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static net.minecraft.world.entity.monster.piglin.PiglinAi.getBarterResponseItems;
import static net.minecraft.world.entity.monster.piglin.PiglinAi.throwItems;

@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {

    @Inject(method = "isBarterCurrency", at = @At("RETURN"), cancellable = true)
    private static void isBarterCurrency(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if(stack.is(ModItems.CURSED_GOLD_INGOT)){
            cir.setReturnValue(true);
        }
    }
    @Inject(method = "stopHoldingOffHandItem",locals = LocalCapture.CAPTURE_FAILSOFT,at = @At(value = "INVOKE",target = "Lnet/minecraft/world/entity/monster/piglin/PiglinAi;throwItems(Lnet/minecraft/world/entity/monster/piglin/Piglin;Ljava/util/List;)V",ordinal = 0,shift = At.Shift.AFTER))
    private static void stopHoldingOffHandItem(Piglin piglin, boolean shouldBarter, CallbackInfo ci, ItemStack itemStack, boolean bl) {
        if(itemStack.is(ModItems.CURSED_GOLD_INGOT)){
            throwItems(piglin, getBarterResponseItems(piglin));

            MobEffectInstance nausea = new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, true);
            MobEffectInstance slowness = new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1, false, true);
            MobEffectInstance weakness = new MobEffectInstance(MobEffects.WEAKNESS, 200, 1, false, true);

            Mob etity;
            int random = (int) (Math.random() * 10);
            if(random == 0){
                etity = piglin;
            }else{
                if(random <= 6){
                    etity = piglin.convertTo(EntityType.SLIME, true);
                }else{
                    etity = piglin.convertTo(EntityType.ZOMBIFIED_PIGLIN, true);
                }
                etity.addEffect(nausea);
                etity.addEffect(slowness);
            }
            etity.addEffect(weakness);
        }
    }
}
