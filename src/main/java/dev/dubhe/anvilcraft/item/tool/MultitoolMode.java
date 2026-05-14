package dev.dubhe.anvilcraft.item.tool;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.init.item.ModItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

public enum MultitoolMode implements StringRepresentable {
    ALL_MODE,
    SHEARS_MODE(Items.SHEARS),
    FLINT_AND_STEEL_MODE(Items.FLINT_AND_STEEL),
    BRUSH_MODE(Items.BRUSH),
    SPYGLASS_MODE(Items.SPYGLASS),
    MAGNET_MODE(ModItems.MAGNET.get()),
    FISHING_ROD_MODE(Items.FISHING_ROD),
    CARROT_ON_A_STICK_MODE(Items.CARROT_ON_A_STICK),
    WARPED_FUNGUS_ON_A_STICK_MODE(Items.WARPED_FUNGUS_ON_A_STICK),
    ;

    public static final Codec<MultitoolMode> CODEC = StringRepresentable.fromEnum(MultitoolMode::values);
    public static final StreamCodec<ByteBuf, MultitoolMode> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(MultitoolMode.class);
    private final @Nullable Item acting;

    MultitoolMode() {
        this(null);
    }

    MultitoolMode(@Nullable Item acting) {
        this.acting = acting;
    }

    public boolean isActing(Item item) {
        return item == this.acting;
    }

    public boolean isActing(ItemStack stack) {
        return stack.is(this.acting);
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
