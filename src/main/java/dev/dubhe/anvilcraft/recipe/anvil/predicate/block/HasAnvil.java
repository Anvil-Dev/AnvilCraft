package dev.dubhe.anvilcraft.recipe.anvil.predicate.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipePredicateTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;

/**
 * 铁砧条件谓词
 *
 * <p>用于检查铁砧是否符合条件</p>
 *
 * @param anvil 铁砧方块条件谓词
 */
public record HasAnvil(BlockStatePredicate anvil, boolean inverted) implements IRecipePredicate<HasAnvil> {
    public static final HasAnvil DEFAULT = new HasAnvil(BlockStatePredicate.builder().of(BlockTags.ANVIL).build(), false);
    public static final HasAnvil DEFAULT_INVERTED = new HasAnvil(HasAnvil.DEFAULT.anvil, true);
    public static final MapCodec<HasAnvil> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        BlockStatePredicate.CODEC
            .optionalFieldOf("anvil", HasAnvil.DEFAULT.anvil)
            .forGetter(HasAnvil::anvil),
        Codec.BOOL
            .optionalFieldOf("inverted", false)
            .forGetter(HasAnvil::inverted)
    ).apply(inst, HasAnvil::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HasAnvil> STREAM_CODEC = StreamCodec.composite(
        BlockStatePredicate.STREAM_CODEC,
        HasAnvil::anvil,
        ByteBufCodecs.BOOL,
        HasAnvil::inverted,
        HasAnvil::new
    );

    public HasAnvil(BlockStatePredicate.Builder anvil, boolean inverted) {
        this(anvil.build(), inverted);
    }

    public HasAnvil(BlockStatePredicate.Builder anvil) {
        this(anvil.build(), false);
    }

    public static HasAnvil frostOnly() {
        return new HasAnvil(BlockStatePredicate.builder().of(ModBlocks.FROST_ANVIL));
    }

    public static HasAnvil noFrost() {
        return new HasAnvil(BlockStatePredicate.builder().of(ModBlocks.FROST_ANVIL), true);
    }

    @Override
    public Type getType() {
        return ModRecipePredicateTypes.HAS_ANVIL.get();
    }

    @Override
    public boolean test(InWorldRecipeContext ctx) {
        if (!(ctx.getEntity() instanceof FallingBlockEntity falling)) return this.inverted;
        if (!this.anvil.test(ctx.getLevel(), falling.getBlockState(), null)) return this.inverted;
        return !this.inverted;
    }

    public static class Type implements IRecipePredicate.Type<HasAnvil> {
        @Override
        public MapCodec<HasAnvil> codec() {
            return HasAnvil.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HasAnvil> streamCodec() {
            return HasAnvil.STREAM_CODEC;
        }
    }
}
