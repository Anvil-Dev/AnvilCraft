package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.utility.PillBoxItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class TooltipEventListener {
    private static final Component FIRE_REFORGING = Component.translatable("tooltip.anvilcraft.property.fire_reforging")
        .withStyle(ChatFormatting.GOLD);
    private static final Component PROVIDENCE = Component.translatable("tooltip.anvilcraft.property.providence", "Shift")
        .withColor(0xFFCB62);
    private static final Component PROVIDENCE_SHIFT = Component.translatable(
        "tooltip.anvilcraft.property.providence.shifting",
        ComponentUtils.formatList(
            List.of(
                Component.translatable("enchantment.minecraft.fortune"),
                Component.translatable("enchantment.minecraft.looting"),
                Component.translatable("enchantment.anvilcraft.beheading"),
                Component.translatable("enchantment.minecraft.thorns"),
                Component.translatable("enchantment.minecraft.luck_of_the_sea")
            ),
            ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR
        )
    ).withColor(0xFFCB62);

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        final ItemStack stack = event.getItemStack();
        final Item.TooltipContext ctx = event.getContext();
        final TooltipDisplay display = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        List<Component> tooltips = new ArrayList<>();
        final Consumer<Component> consumer = tooltips::addFirst;
        final TooltipFlag flag = event.getFlags();
        final boolean shift = flag.hasShiftDown();
        stack.addToTooltip(
            ModComponents.MERCILESS_ENCHANTMENTS,
            ctx,
            display,
            tooltip -> consumer.accept(tooltip.copy().withColor(0x5F93A3)),
            flag
        );
        stack.addToTooltip(ModComponents.CAN_TAKE_OUT_AMMO, ctx, display, consumer, flag);
        stack.addUnitComponentToTooltip(ModComponents.FIRE_REFORGING, FIRE_REFORGING, display, consumer);
        stack.addToTooltip(ModComponents.MERCILESS, ctx, display, consumer, flag);
        stack.addToTooltip(ModComponents.FEROCIOUS, ctx, display, consumer, flag);
        stack.addToTooltip(ModComponents.ETERNAL, ctx, display, consumer, flag);
        TooltipEventListener.addShiftUnitTooltip(ModComponents.PROVIDENCE, PROVIDENCE, PROVIDENCE_SHIFT, stack, shift, display, consumer);
        stack.addToTooltip(ModComponents.MULTIPHASE, ctx, display, consumer, flag);
        stack.addToTooltip(ModComponents.STORED_ENERGY, ctx, display, consumer, flag);
        stack.addToTooltip(ModComponents.FLIGHT_TIME, ctx, display, consumer, flag);
        stack.addToTooltip(ModComponents.SIGNED_PLAYERS, ctx, display, consumer, flag);
        stack.addToTooltip(ModComponents.BOX_CONTENTS, ctx, display, consumer, flag);
        stack.addToTooltip(ModComponents.OVER_LIMIT_CONTAINER, ctx, display, consumer, flag);
        TooltipEventListener.addSpecialItemTooltips(stack, ctx, display, consumer, flag);
        event.getToolTip().addAll(0, tooltips);
    }

    @SuppressWarnings("SameParameterValue")
    private static void addShiftUnitTooltip(
        DataComponentType<Unit> type,
        Component normal,
        Component shift,
        ItemStack stack,
        boolean shifting,
        TooltipDisplay display,
        Consumer<Component> consumer
    ) {
        if (!shifting) {
            stack.addUnitComponentToTooltip(type, normal, display, consumer);
        } else {
            stack.addUnitComponentToTooltip(type, shift, display, consumer);
        }
    }

    private static void addSpecialItemTooltips(
        ItemStack stack,
        Item.TooltipContext ctx,
        TooltipDisplay display,
        Consumer<Component> builder,
        TooltipFlag flag
    ) {
        switch (stack.getItem()) {
            case PillBoxItem _ -> PillBoxItem.appendHoverText(builder, flag);
            default -> {}
        }
    }
}
