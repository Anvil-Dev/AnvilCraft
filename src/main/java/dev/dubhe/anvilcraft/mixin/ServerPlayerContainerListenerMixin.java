package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.level.ServerPlayer$2")
public abstract class ServerPlayerContainerListenerMixin {
    @Shadow
    @Final
    ServerPlayer this$0;

    @Inject(
        method = "slotChanged",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancements/critereon/InventoryChangeTrigger;"
                     + "trigger("
                     + "Lnet/minecraft/server/level/ServerPlayer;"
                     + "Lnet/minecraft/world/entity/player/Inventory;"
                     + "Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void triggerElectricAllergy(
        AbstractContainerMenu container,
        int slotIndex,
        ItemStack changedItem,
        CallbackInfo ci
    ) {
        ModCriterionTriggers.ELECTRIC_ALLERGY.get().trigger(this.this$0, changedItem);
    }
}
