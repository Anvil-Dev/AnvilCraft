package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.event.AmuletAbilitiesEventListener;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemStack.class)
abstract class ItemStackFoodEffectMixin {
    @WrapMethod(method = "finishUsingItem")
    private ItemStack anvilcraft$foodEffects(Level level, LivingEntity entity, Operation<ItemStack> original) {
        return AmuletAbilitiesEventListener.withFoodConsumption(entity, (ItemStack) (Object) this, () -> original.call(level, entity));
    }
}
