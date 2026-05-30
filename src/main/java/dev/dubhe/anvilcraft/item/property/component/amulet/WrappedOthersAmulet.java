package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record WrappedOthersAmulet(List<IAmulet> amulets) implements IAmulet {
    public static WrappedOthersAmulet of(IAmulet... amulets) {
        return new WrappedOthersAmulet(List.of(amulets));
    }

    @Override
    public void inventoryTick(ServerPlayer player, ItemStack stack, boolean isEnabled) {
        for (IAmulet amulet : this.amulets) {
            amulet.inventoryTick(player, stack, isEnabled);
        }
    }

    @Override
    public boolean shouldImmune(ServerPlayer player, DamageSource source) {
        for (IAmulet amulet : this.amulets) {
            if (amulet.shouldImmune(player, source)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getWeight() {
        return 9;
    }

    @Override
    public boolean canActAs(IAmulet other) {
        return IAmulet.super.canActAs(other) || this.amulets.contains(other);
    }

    @Override
    public Type getType() {
        return ModAmuletTypes.WRAPPED_OTHERS.get();
    }

    public static class Type implements IAmulet.Type<WrappedOthersAmulet> {
        public static final MapCodec<WrappedOthersAmulet> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            IAmulet.CODEC
                .listOf()
                .fieldOf("amulets")
                .forGetter(WrappedOthersAmulet::amulets)
        ).apply(inst, WrappedOthersAmulet::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, WrappedOthersAmulet> STREAM_CODEC = StreamCodec.composite(
            IAmulet.STREAM_CODEC.apply(ByteBufCodecs.list()),
            WrappedOthersAmulet::amulets,
            WrappedOthersAmulet::new
        );

        @Override
        public MapCodec<WrappedOthersAmulet> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WrappedOthersAmulet> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
