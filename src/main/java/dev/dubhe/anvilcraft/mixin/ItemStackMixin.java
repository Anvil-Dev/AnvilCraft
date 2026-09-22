package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AppendCustomHoverTextEvent;
import dev.dubhe.anvilcraft.item.AmuletAbilities;
import dev.dubhe.anvilcraft.util.BlockPlacementPicking;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @WrapMethod(method = "finishUsingItem")
    private ItemStack anvilcraft$protectFoodEffects(Level level, LivingEntity consumer, Operation<ItemStack> original) {
        ItemStack stack = Util.cast(this);
        if (!stack.has(DataComponents.FOOD)) return original.call(level, consumer);
        return AmuletAbilities.consumeFood(consumer, () -> original.call(level, consumer));
    }

    @ModifyVariable(method = "useOn", at = @At("HEAD"), argsOnly = true)
    private UseOnContext useOriginalBlockItemTarget(UseOnContext context) {
        return context.getItemInHand().getItem() instanceof BlockItem ? BlockPlacementPicking.forPlacement(context) : context;
    }

    @WrapOperation(
        method = "use",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;use("
            + "Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;"
            + "Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;")
    )
    private InteractionResult placeThroughModelGaps(
        Item item, Level level, Player player, InteractionHand hand, Operation<InteractionResult> original
    ) {
        ItemStack stack = Util.cast(this);
        if (item instanceof BlockItem) {
            InteractionResult result = BlockPlacementPicking.tryPlaceFromAir(stack, level, player, hand);
            if (result != null && result != InteractionResult.PASS) return result;
        }
        return original.call(item, level, player, hand);
    }

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

    @WrapMethod(method = "typeHolder")
    private Holder<Item> storeStack(
        Operation<Holder<Item>> original,
        @Share(namespace = AnvilCraft.MOD_ID, value = "stack") LocalRef<ItemStack> stack
    ) {
        stack.set(Util.cast(this));
        return original.call();
    }
}
