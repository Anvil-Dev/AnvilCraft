package dev.dubhe.anvilcraft.block.state;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

public enum ButtonCap implements StringRepresentable {
    RGB,
    ANVIL,
    ROYAL_ANVIL,
    EMBER_ANVIL,
    TRANSCENDENCE_ANVIL,
    FROST_ANVIL,
    LARGE_ANVIL,
    LARGE_ROYAL_ANVIL,
    LARGE_EMBER_ANVIL,
    LARGE_TRANSCENDENCE_ANVIL,
    LARGE_FROST_ANVIL,
    GIANT_ANVIL,
    BASE;

    public static final float PRESSED_HEIGHT_SCALE = 0.61f;
    public static final float PRESSED_WIDTH_SCALE = 1.18f;
    public static final Codec<ButtonCap> CODEC = StringRepresentable.fromEnum(ButtonCap::values);
    public static final StreamCodec<ByteBuf, ButtonCap> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(ButtonCap.class);

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public boolean isHammerCap() {
        return switch (this) {
            case ANVIL, ROYAL_ANVIL, EMBER_ANVIL, TRANSCENDENCE_ANVIL, FROST_ANVIL -> true;
            default -> false;
        };
    }

    public static @Nullable ButtonCap forHammer(ItemStack hammer) {
        if (hammer.is(ModItems.ANVIL_HAMMER)) {
            return ANVIL;
        }
        if (hammer.is(ModItems.ROYAL_ANVIL_HAMMER)) {
            return ROYAL_ANVIL;
        }
        if (hammer.is(ModItems.EMBER_ANVIL_HAMMER)) {
            return EMBER_ANVIL;
        }
        if (hammer.is(ModItems.FROST_ANVIL_HAMMER)) {
            return FROST_ANVIL;
        }
        return hammer.is(ModItems.TRANSCENDENCE_ANVIL_HAMMER) ? TRANSCENDENCE_ANVIL : null;
    }

    public @Nullable Block mountedBlock() {
        return this.isHammerCap() ? null : this.sourceBlock();
    }

    public @Nullable Block sourceBlock() {
        return switch (this) {
            case RGB -> ModBlocks.CHROMATIC_STONE.get();
            case ANVIL, LARGE_ANVIL -> Blocks.ANVIL;
            case ROYAL_ANVIL, LARGE_ROYAL_ANVIL -> ModBlocks.ROYAL_ANVIL.get();
            case EMBER_ANVIL, LARGE_EMBER_ANVIL -> ModBlocks.EMBER_ANVIL.get();
            case TRANSCENDENCE_ANVIL, LARGE_TRANSCENDENCE_ANVIL -> ModBlocks.TRANSCENDENCE_ANVIL.get();
            case FROST_ANVIL, LARGE_FROST_ANVIL -> ModBlocks.FROST_ANVIL.get();
            case GIANT_ANVIL -> ModBlocks.GIANT_ANVIL.get();
            default -> null;
        };
    }
}
