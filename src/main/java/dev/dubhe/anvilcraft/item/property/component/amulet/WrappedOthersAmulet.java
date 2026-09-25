package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public record WrappedOthersAmulet(List<ResourceKey<IAmulet>> amulets) implements IAmulet {
    @Override
    public void inventoryTick(ServerPlayer player, ItemStack stack, boolean isEnabled) {
        for (ResourceKey<IAmulet> key : this.amulets) {
            IAmulet amulet = ModRegistries.AMULET.getValue(key);
            if (amulet != null) {
                amulet.inventoryTick(player, stack, isEnabled);
            }
        }
    }

    @Override
    public boolean shouldImmune(ServerPlayer player, ItemStack stack, DamageSource source) {
        for (ResourceKey<IAmulet> key : this.amulets) {
            IAmulet amulet = ModRegistries.AMULET.getValue(key);
            if (amulet != null && amulet.shouldImmune(player, stack, source)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<ResourceKey<IAmulet>> canActLike() {
        return this.amulets;
    }

    @Override
    public Type getType() {
        return ModAmuletTypes.WRAPPED_OTHERS.get();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this;
    }

    public static class Type implements IAmulet.Type<WrappedOthersAmulet> {
        public static final MapCodec<WrappedOthersAmulet> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            ResourceKey.codec(ModRegistryKeys.AMULET)
                .listOf()
                .fieldOf("amulets")
                .forGetter(WrappedOthersAmulet::amulets)
        ).apply(inst, WrappedOthersAmulet::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, WrappedOthersAmulet> STREAM_CODEC = StreamCodec.composite(
            ResourceKey.streamCodec(ModRegistryKeys.AMULET).apply(ByteBufCodecs.list()),
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
