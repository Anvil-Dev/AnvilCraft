package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.block.ITooltipBlock;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements DataComponentHolder {
    @Shadow
    public abstract boolean is(Predicate<Holder<Item>> item);

    @Shadow
    public abstract Item getItem();

    @Definition(
        id = "addToTooltip",
        method = "Lnet/minecraft/world/item/ItemStack;"
                 + "addToTooltip(Lnet/minecraft/core/component/DataComponentType;"
                 + "Lnet/minecraft/world/item/Item$TooltipContext;"
                 + "Lnet/minecraft/world/item/component/TooltipDisplay;"
                 + "Ljava/util/function/Consumer;"
                 + "Lnet/minecraft/world/item/TooltipFlag;)V"
    )
    @Definition(
        id = "ENCHANTMENTS",
        field = "Lnet/minecraft/core/component/DataComponents;ENCHANTMENTS:Lnet/minecraft/core/component/DataComponentType;"
    )
    @Expression("this.addToTooltip(ENCHANTMENTS, ?, ?, ?, ?)")
    @Inject(method = "addDetailsToTooltip", at = @At(value = "MIXINEXTRAS:EXPRESSION", shift = At.Shift.AFTER))
    private void addMercilessToTooltip(
        Item.TooltipContext context,
        TooltipDisplay display,
        @Nullable Player player,
        TooltipFlag tooltipFlag,
        Consumer<Component> builder,
        CallbackInfo ci
    ) {
        this.addToTooltip(
            ModComponents.MERCILESS_ENCHANTMENTS,
            context,
            display,
            tooltip -> builder.accept(tooltip.copy().withColor(0x5F93A3)),
            tooltipFlag
        );
    }

    @Inject(method = "addDetailsToTooltip", at = @At(value = "HEAD", shift = At.Shift.AFTER))
    private void addDetailsForBlock(
        Item.TooltipContext context,
        TooltipDisplay display,
        @Nullable Player player,
        TooltipFlag tooltipFlag,
        Consumer<Component> builder,
        CallbackInfo ci
    ) {
        if (this.getItem() instanceof BlockItem bi && bi instanceof ITooltipBlock itb) {
            itb.appendHoverText(Util.cast(this), context, builder, tooltipFlag);
        }
    }
}
