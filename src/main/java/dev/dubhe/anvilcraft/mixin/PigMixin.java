package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Pig.class)
abstract class PigMixin extends Animal {
    @Shadow
    public abstract boolean isSaddled();

    protected PigMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "registerGoals", at = @At("HEAD"))
    private void registerGoals(CallbackInfo ci) {
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.25, (itemStack) -> itemStack.is(ModItems.MULTITOOL_ITEM) && MultitoolItem.getMode(itemStack) == MultitoolItem.CARROT_ON_A_STICK_MODE, false));
    }

    @ModifyReturnValue(method = "getControllingPassenger", at = @At("RETURN"))
    private LivingEntity getControllingPassenger(LivingEntity original) {
        return this.isSaddled() && this.getFirstPassenger() instanceof Player player && (player.isHolding(Items.CARROT_ON_A_STICK)
            || (player.isHolding(ModItems.MULTITOOL_ITEM.asItem())
            && (MultitoolItem.getMode(player.getMainHandItem()) == MultitoolItem.CARROT_ON_A_STICK_MODE)
            || MultitoolItem.getMode(player.getOffhandItem()) == MultitoolItem.CARROT_ON_A_STICK_MODE))
            ? player
            : super.getControllingPassenger();
    }
}
