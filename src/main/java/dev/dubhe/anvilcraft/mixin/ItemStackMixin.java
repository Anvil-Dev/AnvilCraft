package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.block.ITooltipBlock;
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
