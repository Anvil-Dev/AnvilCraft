package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Comrades;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record ComradeAmulet() implements IAmulet {
    @Override
    public boolean shouldImmune(ServerPlayer player, ItemStack amulet, DamageSource source) {
        Comrades comrades = amulet.getOrDefault(ModComponents.COMRADES, Comrades.EMPTY);
        return Optional.ofNullable(source.getEntity())
            .flatMap(entity -> Util.castSafely(entity, Player.class))
            .map(p -> p.getGameProfile().id())
            .filter(comrades::contains)
            .isPresent();
    }

    @Override
    public Type getType() {
        return ModAmuletTypes.COMRADE.get();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ComradeAmulet;
    }

    public static class Type implements IAmulet.Type<ComradeAmulet> {
        public static final MapCodec<ComradeAmulet> CODEC = MapCodec.unit(new ComradeAmulet());
        public static final StreamCodec<ByteBuf, ComradeAmulet> STREAM_CODEC = StreamCodec.unit(new ComradeAmulet());

        @Override
        public MapCodec<ComradeAmulet> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ComradeAmulet> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
