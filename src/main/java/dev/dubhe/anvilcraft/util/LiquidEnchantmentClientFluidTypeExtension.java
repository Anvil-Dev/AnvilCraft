package dev.dubhe.anvilcraft.util;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.Optional;

public class LiquidEnchantmentClientFluidTypeExtension extends ModClientFluidTypeExtensionImpl {
    private static final int[] BASE_LAYER = {0xFFFFFFFF};
    private static final int LAYER_ALPHA = 0x80;
    private static final int CURSE_LAYER = (LAYER_ALPHA << 24) | 0x00FF0000;

    public LiquidEnchantmentClientFluidTypeExtension(ResourceLocation texture) {
        super(texture, texture);
    }

    /** 原贴图、魔咒色与诅咒红色按顺序作为独立渲染层。 */
    public int[] getLayerColors(FluidStack stack) {
        Optional<Holder<Enchantment>> enchantment = LiquidEnchantmentUtil.getEnchantment(stack);
        if (enchantment.isEmpty()) return BASE_LAYER;
        int enchantmentLayer = (LAYER_ALPHA << 24) | LiquidEnchantmentUtil.getColor(enchantment.get());
        return enchantment.get().is(EnchantmentTags.CURSE)
            ? new int[]{0xFFFFFFFF, enchantmentLayer, CURSE_LAYER}
            : new int[]{0xFFFFFFFF, enchantmentLayer};
    }

    @Override
    public int getTintColor(FluidStack stack) {
        int result = 0xFFFFFFFF;
        int[] layers = this.getLayerColors(stack);
        for (int i = 1; i < layers.length; i++) {
            result = layers.length == 3 && i == 2
                ? overlay(result, layers[i])
                : softLight(result, layers[i]);
        }
        return result;
    }

    private static int softLight(int background, int foreground) {
        return blendByMode(background, foreground, false);
    }

    private static int overlay(int background, int foreground) {
        return blendByMode(background, foreground, true);
    }

    private static int blendByMode(int background, int foreground, boolean useOverlay) {
        int alpha = foreground >>> 24;
        if (alpha == 0) return background;
        int red = blendChannel(background >> 16 & 0xFF, foreground >> 16 & 0xFF, alpha, useOverlay);
        int green = blendChannel(background >> 8 & 0xFF, foreground >> 8 & 0xFF, alpha, useOverlay);
        int blue = blendChannel(background & 0xFF, foreground & 0xFF, alpha, useOverlay);
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    private static int blendChannel(int background, int foreground, int alpha, boolean useOverlay) {
        int result;
        if (useOverlay) {
            result = background < 128
                ? 2 * background * foreground / 255
                : 255 - 2 * (255 - background) * (255 - foreground) / 255;
        } else {
            // Tint composition starts from white, so the layer color must be the soft-light base.
            result = softLightChannel(foreground, background);
        }
        return (result * alpha + background * (255 - alpha)) / 255;
    }

    private static int softLightChannel(int background, int foreground) {
        float base = background / 255.0f;
        float blend = foreground / 255.0f;
        if (blend <= 0.5f) {
            return Math.round((base - (1.0f - 2.0f * blend) * base * (1.0f - base)) * 255.0f);
        }
        float softenedBase = base <= 0.25f
            ? ((16.0f * base - 12.0f) * base + 4.0f) * base
            : (float) Math.sqrt(base);
        return Math.round((base + (2.0f * blend - 1.0f) * (softenedBase - base)) * 255.0f);
    }
}
