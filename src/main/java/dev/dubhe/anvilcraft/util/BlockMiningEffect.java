package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.block.EmberAnvilBlock;
import dev.dubhe.anvilcraft.block.FrostAnvilBlock;
import dev.dubhe.anvilcraft.block.RoyalAnvilBlock;
import dev.dubhe.anvilcraft.block.SpectralAnvilBlock;
import dev.dubhe.anvilcraft.block.TranscendenceAnvilBlock;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Describes a block mining effect by the enchantment that implements it.
 */
public record BlockMiningEffect(@Nullable ResourceKey<Enchantment> enchantment, int level) {
    private static final int NORMAL_LASER_COLOR = 0xFF0D0D;
    private static final int SILK_TOUCH_LASER_COLOR = 0x00FFBF;
    private static final int DISINTEGRATION_LASER_COLOR = 0x598CFF;
    private static final int SMELTING_LASER_COLOR = 0xFFD900;

    public static final BlockMiningEffect NORMAL = new BlockMiningEffect(null, 0);
    public static final BlockMiningEffect SILK_TOUCH = new BlockMiningEffect(Enchantments.SILK_TOUCH, 1);
    public static final BlockMiningEffect DISINTEGRATION =
        new BlockMiningEffect(ModEnchantments.DISINTEGRATION_KEY, 1);
    public static final BlockMiningEffect SMELTING = new BlockMiningEffect(ModEnchantments.SMELTING_KEY, 1);
    public static final BlockMiningEffect MAX_SMELTING = new BlockMiningEffect(ModEnchantments.SMELTING_KEY, 5);
    public static final BlockMiningEffect FORTUNE_5 = new BlockMiningEffect(Enchantments.FORTUNE, 5);

    public BlockMiningEffect {
        if (level < 0 || (enchantment == null && level != 0)) {
            throw new IllegalArgumentException("Invalid mining enchantment level: " + level);
        }
    }

    public ItemStack applyTo(ServerLevel level, ItemStack baseTool) {
        ItemStack tool = baseTool.copy();
        if (enchantment != null) {
            level.holderLookup(Registries.ENCHANTMENT)
                .get(enchantment)
                .ifPresent(holder -> tool.enchant(holder, this.level));
        }
        return tool;
    }

    public boolean is(ResourceKey<Enchantment> enchantment) {
        return enchantment.equals(this.enchantment);
    }

    public boolean isDisintegration() {
        return is(ModEnchantments.DISINTEGRATION_KEY);
    }

    public Component getDisplaySuffix() {
        if (enchantment == null) return Component.empty();
        return Component.literal(" ")
            .append(Component.translatable(enchantment.location().toLanguageKey("enchantment")));
    }

    public int getLaserColor() {
        if (is(Enchantments.SILK_TOUCH)) return SILK_TOUCH_LASER_COLOR;
        if (is(ModEnchantments.DISINTEGRATION_KEY)) return DISINTEGRATION_LASER_COLOR;
        if (is(ModEnchantments.SMELTING_KEY)) return SMELTING_LASER_COLOR;
        return NORMAL_LASER_COLOR;
    }

    /**
     * Returns the mining effect represented by an anvil block, or empty when the block is not a mining anvil.
     */
    public static Optional<BlockMiningEffect> fromAnvil(@Nullable Block block) {
        return switch (block) {
            case RoyalAnvilBlock ignored -> Optional.of(SILK_TOUCH);
            case FrostAnvilBlock ignored -> Optional.of(DISINTEGRATION);
            case EmberAnvilBlock ignored -> Optional.of(SMELTING);
            case TranscendenceAnvilBlock ignored -> Optional.of(FORTUNE_5);
            case SpectralAnvilBlock ignored -> Optional.of(NORMAL);
            case AnvilBlock ignored -> Optional.of(NORMAL);
            case null, default -> Optional.empty();
        };
    }
}
