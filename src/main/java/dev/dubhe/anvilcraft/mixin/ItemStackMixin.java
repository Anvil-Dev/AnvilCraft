package dev.dubhe.anvilcraft.mixin;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.event.AppendCustomHoverTextEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @Inject(method = "addDetailsToTooltip", at = @At("HEAD"))
    private void appendCustomHoverText(
        Item.TooltipContext context,
        TooltipDisplay display,
        @Nullable Player player,
        TooltipFlag tooltipFlag,
        Consumer<Component> builder,
        CallbackInfo ci
    ) {
        NeoForge.EVENT_BUS.post(new AppendCustomHoverTextEvent(
            Util.cast(this),
            context,
            display,
            player,
            tooltipFlag,
            builder
        ));
    }
}
