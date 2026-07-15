package dev.dubhe.anvilcraft.client.gui.component;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

public class AutoEnchantingTableButton extends TriStateButton {
    @Getter
    private Holder<Enchantment> holder;
    private final OnPress onPress;

    public AutoEnchantingTableButton(
        int x,
        int y,
        int width,
        int height,
        Identifier texture,
        Holder<Enchantment> enchantmentHolder,
        int textureWidth,
        int textureHeight,
        int[] stateOffset,
        OnPress onPress,
        List<Component> tooltips
    ) {
        super(x, y, width, height, texture, textureWidth, textureHeight, stateOffset, (ignore) -> {}, List.of());
        this.onPress = onPress;
        this.holder = enchantmentHolder;
        List<Component> tooltip = new ObjectArrayList<>();
        tooltip.add(Enchantment.getFullname(this.holder, this.holder.value().getMaxLevel()));
        tooltip.addAll(tooltips);
        this.setTooltips(tooltip);
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractContents(graphics, mouseX, mouseY, a);
        ItemStack enchantedBookItem = Items.ENCHANTED_BOOK.getDefaultInstance();
        enchantedBookItem.enchant(this.holder, this.holder.value().getMaxLevel());
        int y = this.getY();
        if (this.selected) {
            y++;
        }
        graphics.item(enchantedBookItem, this.getX() + 1, y);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        this.onPress.onPress(this);
    }

    public interface OnPress {
        void onPress(AutoEnchantingTableButton btn);
    }
}
