package dev.dubhe.anvilcraft.recipe.anvil.predicate.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.api.recipe.CauldronProcessing;
import dev.dubhe.anvilcraft.init.recipe.ModRecipePredicateTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * 按注册的加工类别匹配锅下方块，使已有配方可复用附属模组提供的加工方块。
 *
 * @param offset 相对于配方位置的方块偏移
 * @param category 加工类别
 */
public record HasCauldronProcessor(Vec3 offset, ResourceLocation category) implements IRecipePredicate<HasCauldronProcessor> {
    public static final MapCodec<HasCauldronProcessor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Vec3.CODEC.fieldOf("offset").forGetter(HasCauldronProcessor::offset),
        ResourceLocation.CODEC.fieldOf("category").forGetter(HasCauldronProcessor::category)
    ).apply(instance, HasCauldronProcessor::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HasCauldronProcessor> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.VEC3, HasCauldronProcessor::offset,
        ResourceLocation.STREAM_CODEC, HasCauldronProcessor::category,
        HasCauldronProcessor::new
    );

    @Override
    public boolean test(InWorldRecipeContext context) {
        BlockPos pos = BlockPos.containing(context.getPos().add(this.offset));
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        return CauldronProcessing.matches(this.category, cache.getBlockState(pos));
    }

    @Override
    public Type getType() {
        return ModRecipePredicateTypes.HAS_CAULDRON_PROCESSOR.get();
    }

    /** 加工方块条件的编解码类型。 */
    public static class Type implements IRecipePredicate.Type<HasCauldronProcessor> {
        @Override
        public MapCodec<HasCauldronProcessor> codec() {
            return HasCauldronProcessor.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HasCauldronProcessor> streamCodec() {
            return HasCauldronProcessor.STREAM_CODEC;
        }
    }
}
