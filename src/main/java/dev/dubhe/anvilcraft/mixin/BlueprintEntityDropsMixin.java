package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.building.BuildingRodUndo;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class BlueprintEntityDropsMixin {
    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At("RETURN"))
    private void anvilcraft$trackBlueprintDrops(ItemStack stack, float offset, CallbackInfoReturnable<ItemEntity> cir) {
        ItemEntity item = cir.getReturnValue();
        if (item != null && !item.level().isClientSide()) BuildingRodUndo.spawnedBy((Entity) (Object) this, item);
    }
}
