package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.HeavyHalberdItem;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import dev.dubhe.anvilcraft.item.ResonatorItem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements DataComponentHolder {
    @WrapOperation(
        method = "is(Lnet/minecraft/tags/TagKey;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder$Reference;is(Lnet/minecraft/tags/TagKey;)Z")
    )
    private boolean tryUseModeSpecificHolderForTag(
        Holder.Reference<Item> instance,
        TagKey<Item> tagKey,
        Operation<Boolean> original
    ) {
        return switch (instance) {
            case HeavyHalberdItem.HeavyHalberdHolder holder -> holder.is(HeavyHalberdItem.getMode(Util.cast(this)), tagKey);
            case ResonatorItem.ResonatorHolder holder -> holder.is(ResonatorItem.getMode(Util.cast(this)), tagKey);
            case MultitoolItem.MultitoolHolder holder -> holder.is(MultitoolItem.getMode(Util.cast(this)), tagKey);
            default -> original.call(instance, tagKey);
        };
    }

    @WrapOperation(
        method = "is(Lnet/minecraft/core/HolderSet;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/core/HolderSet;contains(Lnet/minecraft/core/Holder;)Z")
    )
    private boolean tryUseModeSpecificHolderForSet(
        HolderSet<Item> instance,
        Holder<Item> holder0,
        Operation<Boolean> original
    ) {
        return switch (holder0) {
            case HeavyHalberdItem.HeavyHalberdHolder holder -> holder.is(HeavyHalberdItem.getMode(Util.cast(this)), instance);
            case MultitoolItem.MultitoolHolder holder -> holder.is(MultitoolItem.getMode(Util.cast(this)), instance);
            case ResonatorItem.ResonatorHolder holder -> holder.is(ResonatorItem.getMode(Util.cast(this)), instance);
            default -> original.call(instance, holder0);
        };
    }

    @Inject(
        method = "getTooltipLines",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;"
                     + "addToTooltip("
                     + "Lnet/minecraft/core/component/DataComponentType;"
                     + "Lnet/minecraft/world/item/Item$TooltipContext;"
                     + "Ljava/util/function/Consumer;"
                     + "Lnet/minecraft/world/item/TooltipFlag;"
                     + ")V",
            ordinal = 3
        )
    )
    private void addMercilessToTooltip(
        Item.TooltipContext tooltipContext,
        Player player,
        TooltipFlag tooltipFlag,
        CallbackInfoReturnable<List<Component>> cir,
        @Local List<Component> list
    ) {
        this.addToTooltip(
            ModComponents.MERCILESS_ENCHANTMENTS,
            tooltipContext,
            tooltip -> list.add(tooltip.copy().withColor(0x5F93A3)),
            tooltipFlag
        );
        this.addToTooltip(ModComponents.STORAGE, tooltipContext, list::add, tooltipFlag);
    }
}
