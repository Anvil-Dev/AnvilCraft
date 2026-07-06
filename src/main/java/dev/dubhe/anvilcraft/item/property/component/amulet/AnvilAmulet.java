package dev.dubhe.anvilcraft.item.property.component.amulet;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypeTags;
import dev.dubhe.anvilcraft.init.entity.ModEntityTypeTags;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;

public record AnvilAmulet() implements IAmulet {
    private static final AnvilAmulet INSTANCE = new AnvilAmulet();

    @Override
    public boolean shouldImmune(ServerPlayer player, DamageSource source) {
        if (!source.is(ModDamageTypeTags.ANVIL_AMULET_VALID)) {
            return false;
        }
        HolderSet.Named<EntityType<?>> valid = BuiltInRegistries.ENTITY_TYPE.getOrThrow(ModEntityTypeTags.ANVIL_AMULET_VALID);
        if (!source.getDirectEntity().is(valid)) {
            return source.getDirectEntity() instanceof Player && source.getWeaponItem().is(ModItemTags.ANVIL_HAMMER);
        }
        return !(source.getEntity() instanceof FallingBlockEntity falling)
               || falling.getBlockState().is(BlockTags.ANVIL)
               || falling.getBlockState().is(ModBlockTags.GIANT_ANVIL);
    }

    @Override
    public Type getType() {
        return ModAmuletTypes.ANVIL.get();
    }

    public static class Type implements IAmulet.Type<AnvilAmulet> {
        public static final MapCodec<AnvilAmulet> CODEC = MapCodec.unit(AnvilAmulet.INSTANCE);
        public static final StreamCodec<ByteBuf, AnvilAmulet> STREAM_CODEC = StreamCodec.unit(AnvilAmulet.INSTANCE);

        @Override
        public MapCodec<AnvilAmulet> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, AnvilAmulet> streamCodec() {
            return Type.STREAM_CODEC.cast();
        }
    }
}
