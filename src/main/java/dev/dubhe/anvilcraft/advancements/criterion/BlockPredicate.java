package dev.dubhe.anvilcraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.function.Predicate;

public record BlockPredicate(Optional<HolderSet<Block>> block) implements Predicate<Block> {
    public static final Codec<BlockPredicate> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("block").forGetter(BlockPredicate::block)
    ).apply(instance, BlockPredicate::new));

    @Override
    public boolean test(Block block) {
        return this.block.isEmpty() || block.defaultBlockState().is(this.block.get());
    }

    public static class Builder {
        private HolderSet<Block> blocks = HolderSet.empty();

        private Builder() {
        }

        public static Builder block() {
            return new Builder();
        }

        @SuppressWarnings("deprecation")
        public Builder of(Block block) {
            this.blocks = HolderSet.direct(b -> b.defaultBlockState().getBlock().builtInRegistryHolder(), block);
            return this;
        }

        public Builder of(TagKey<Block> tag) {
            this.blocks = BuiltInRegistries.BLOCK.getOrThrow(tag);
            return this;
        }

        public BlockPredicate build() {
            return new BlockPredicate(Optional.of(this.blocks));
        }
    }
}
