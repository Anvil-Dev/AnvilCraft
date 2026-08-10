package dev.dubhe.anvilcraft.util;

import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

public class LiquidEnchantmentClientFluidTypeExtension extends ModClientFluidTypeExtensionImpl {
    private static final int[] BASE_LAYER = {0xFFFFFFFF};
    private static final int ENCHANTMENT_ALPHA = 0x70;
    private static final int CURSE_LAYER = 0x60FF0000;

    public LiquidEnchantmentClientFluidTypeExtension() {
        super();
    }

    /** 原贴图、魔咒色与诅咒红色按顺序作为独立渲染层。 */
    public int[] getLayerColors(FluidStack stack) {
        Optional<Holder<Enchantment>> enchantment = LiquidEnchantmentUtil.getEnchantment(stack);
        if (enchantment.isEmpty()) return LiquidEnchantmentClientFluidTypeExtension.BASE_LAYER;
        int enchantmentLayer = (LiquidEnchantmentClientFluidTypeExtension.ENCHANTMENT_ALPHA << 24) | LiquidEnchantmentUtil.getColor(
            enchantment.get());
        return enchantment.get().is(EnchantmentTags.CURSE)
               ? new int[] {0xFFFFFFFF, enchantmentLayer, LiquidEnchantmentClientFluidTypeExtension.CURSE_LAYER}
               : new int[] {0xFFFFFFFF, enchantmentLayer};
    }

    public int getTintColor(FluidStack stack) {
        int result = 0xFFFFFFFF;
        int[] layers = this.getLayerColors(stack);
        for (int i = 1; i < layers.length; i++) result = LiquidEnchantmentClientFluidTypeExtension.blend(result, layers[i]);
        return result;
    }

    private static int blend(int background, int foreground) {
        int alpha = foreground >>> 24;
        int inverseAlpha = 255 - alpha;
        int red = ((foreground >> 16 & 0xFF) * alpha + (background >> 16 & 0xFF) * inverseAlpha) / 255;
        int green = ((foreground >> 8 & 0xFF) * alpha + (background >> 8 & 0xFF) * inverseAlpha) / 255;
        int blue = ((foreground & 0xFF) * alpha + (background & 0xFF) * inverseAlpha) / 255;
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }
}
