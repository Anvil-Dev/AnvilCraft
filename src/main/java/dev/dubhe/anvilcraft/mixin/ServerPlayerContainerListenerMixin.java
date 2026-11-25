package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import dev.dubhe.anvilcraft.init.ModStats;
import dev.dubhe.anvilcraft.util.PowerComponentUtil;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.server.level.ServerPlayer$2")
public abstract class ServerPlayerContainerListenerMixin {
    @Shadow
    @Final
    ServerPlayer this$0;

    /// 上一次统计到的背包内电网组件数量；-1 表示还没建立基准
    @Unique
    private int anvilcraft$powerComponentCount = -1;

    @Inject(
        method = "slotChanged",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancements/criterion/InventoryChangeTrigger;"
                     + "trigger("
                     + "Lnet/minecraft/server/level/ServerPlayer;"
                     + "Lnet/minecraft/world/entity/player/Inventory;"
                     + "Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void anvilcraft$trackPowerComponentObtained(
        AbstractContainerMenu container,
        int slotIndex,
        ItemStack changedItem,
        CallbackInfo ci
    ) {
        int count = PowerComponentUtil.countPowerComponents(this.this$0.getInventory());
        int previous = this.anvilcraft$powerComponentCount;
        this.anvilcraft$powerComponentCount = count;
        if (previous < 0 || count <= previous) return;
        this.this$0.awardStat(ModStats.OBTAIN_POWER_COMPONENT, count - previous);
        TriggerUtil.powerGridRhythm(this.this$0);
    }

    @Inject(
        method = "slotChanged",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/advancements/criterion/InventoryChangeTrigger;"
                     + "trigger("
                     + "Lnet/minecraft/server/level/ServerPlayer;"
                     + "Lnet/minecraft/world/entity/player/Inventory;"
                     + "Lnet/minecraft/world/item/ItemStack;)V"
        )
    )
    private void triggerElectricAllergy(AbstractContainerMenu container, int slotIndex, ItemStack changedItem, CallbackInfo ci) {
        ModCriterionTriggers.ELECTRIC_ALLERGY.get().trigger(this.this$0, changedItem);
    }
}
