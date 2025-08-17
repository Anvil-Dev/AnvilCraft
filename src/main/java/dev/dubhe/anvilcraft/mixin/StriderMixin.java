package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Strider.class)
abstract class StriderMixin extends Animal {
    protected StriderMixin(EntityType<? extends Animal> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow
    public abstract boolean isSaddled();

    @ModifyReturnValue(method = "getControllingPassenger", at = @At("RETURN"))
    private LivingEntity getControllingPassenger(LivingEntity original) {
        return this.isSaddled() && this.getFirstPassenger() instanceof Player player && (player.isHolding(Items.WARPED_FUNGUS_ON_A_STICK)
            || (player.isHolding(ModItems.MULTITOOL_ITEM.asItem())
            && (MultitoolItem.getMode(player.getMainHandItem()) == MultitoolItem.WARPED_FUNGUS_ON_A_STICK_MODE)
            || MultitoolItem.getMode(player.getOffhandItem()) == MultitoolItem.WARPED_FUNGUS_ON_A_STICK_MODE))
            ? player
            : super.getControllingPassenger();
    }
}
