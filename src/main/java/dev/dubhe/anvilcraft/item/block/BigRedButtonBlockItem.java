package dev.dubhe.anvilcraft.item.block;

import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerItem;
import dev.anvilcraft.lib.v2.registrum.util.CreativeVariantPickerRegistry;
import dev.dubhe.anvilcraft.block.state.ButtonCap;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.MountedButtonBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;

public class BigRedButtonBlockItem extends BlockItem implements CreativeVariantPickerItem {
    public BigRedButtonBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static boolean canSwitchBase(ItemStack stack) {
        if (!(stack.getItem() instanceof BigRedButtonBlockItem) || stack.has(ModComponents.MOUNTED_BLOCK)) {
            return false;
        }
        ButtonCap cap = stack.get(ModComponents.BUTTON_CAP);
        // 原始按钮也显示红色，只有染红时才会写入红色组件。
        return cap == ButtonCap.BASE || (cap == null && stack.get(DataComponents.BASE_COLOR) == DyeColor.RED);
    }

    public static void switchBase(ItemStack stack) {
        if (!canSwitchBase(stack)) {
            return;
        }
        if (stack.get(ModComponents.BUTTON_CAP) == ButtonCap.BASE) {
            stack.remove(ModComponents.BUTTON_CAP);
        } else {
            stack.set(ModComponents.BUTTON_CAP, ButtonCap.BASE);
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        MountedButtonBlock mounted = MountedButtonBlock.get(stack);
        if (mounted != null) {
            ButtonCap named = mounted.namedCap();
            if (named == ButtonCap.RGB) {
                return Component.translatable(this.getDescriptionId() + ".rgb");
            }
            String suffix = named == null || named == ButtonCap.GIANT_ANVIL ? ".mounted" : ".large";
            return Component.translatable(this.getDescriptionId() + suffix, mounted.material().create().getHoverName());
        }
        ButtonCap cap = stack.get(ModComponents.BUTTON_CAP);
        if (cap != null) {
            Block anvil = cap.sourceBlock();
            return anvil == null ? Component.translatable(this.getDescriptionId() + ".base")
                : Component.translatable(this.getDescriptionId() + ".small", anvil.getName());
        }
        DyeColor color = stack.getOrDefault(DataComponents.BASE_COLOR, DyeColor.RED);
        return color == DyeColor.RED ? super.getName(stack) : Component.translatable(this.getDescriptionId() + "." + color.getName());
    }

    @Override
    public List<ItemStack> createCreativePickerVariants(ItemStack source) {
        List<ItemStack> variants = new ArrayList<>();
        for (DyeColor color : CreativeVariantPickerRegistry.colorOrder()) {
            ItemStack stack = this.getDefaultInstance();
            if (color != DyeColor.RED) {
                stack.set(DataComponents.BASE_COLOR, color);
            }
            variants.add(stack);
        }
        return variants;
    }
}
