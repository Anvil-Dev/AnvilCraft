package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.AppendCustomHoverTextEvent;
import dev.dubhe.anvilcraft.api.tooltip.ItemTooltipManager;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.util.Unit;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

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
    public static void onTooltip(AppendCustomHoverTextEvent event) {
        final ItemStack stack = event.getStack();
        final Item.TooltipContext ctx = event.getContext();
        final TooltipDisplay display = event.getDisplay();
        final Consumer<Component> builder = event.getBuilder();
        final TooltipFlag flag = event.getTooltipFlag();
        final boolean shift = flag.hasShiftDown();

        ItemTooltipManager.addTooltip(stack, builder, flag);
        stack.addToTooltip(
            ModComponents.MERCILESS_ENCHANTMENTS,
            ctx,
            display,
            tooltip -> builder.accept(tooltip.copy().withColor(0x5F93A3)),
            flag
        );
        stack.addToTooltip(ModComponents.CAN_TAKE_OUT_AMMO, ctx, display, builder, flag);
        stack.addUnitComponentToTooltip(ModComponents.FIRE_REFORGING, FIRE_REFORGING, display, builder);
        stack.addToTooltip(ModComponents.MERCILESS, ctx, display, builder, flag);
        stack.addToTooltip(ModComponents.FEROCIOUS, ctx, display, builder, flag);
        stack.addToTooltip(ModComponents.ETERNAL, ctx, display, builder, flag);
        TooltipEventListener.addShiftUnitTooltip(ModComponents.PROVIDENCE, PROVIDENCE, PROVIDENCE_SHIFT, stack, shift, display, builder);
        stack.addToTooltip(ModComponents.MULTIPHASE, ctx, display, builder, flag);
        stack.addToTooltip(ModComponents.STORED_ENERGY, ctx, display, builder, flag);
        stack.addToTooltip(ModComponents.FLIGHT_TIME, ctx, display, builder, flag);
        stack.addToTooltip(ModComponents.AMULET, ctx, display, builder, flag);
        stack.addToTooltip(ModComponents.BOX_CONTENTS, ctx, display, builder, flag);
        stack.addToTooltip(ModComponents.OVER_LIMIT_CONTAINER, ctx, display, builder, flag);
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

}
