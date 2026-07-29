package dev.dubhe.anvilcraft.util;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * 方块状态注入
 */
public class BlockStateUtil {
    public static class BlockHolderLookup implements HolderLookup<Block>, HolderOwner<Block> {
        @Override
        public Stream<Holder.Reference<Block>> listElements() {
            return BuiltInRegistries.BLOCK.stream()
                .map(BuiltInRegistries.BLOCK::getResourceKey)
                .filter(Optional::isPresent)
                .map(key -> BuiltInRegistries.BLOCK.getHolderOrThrow(key.get()));
        }

        @Override
        public Stream<HolderSet.Named<Block>> listTags() {
            return BuiltInRegistries.BLOCK.getTags().map(Pair::getSecond);
        }

        @Override
        public Optional<Holder.Reference<Block>> get(ResourceKey<Block> resourceKey) {
            return Optional.of(BuiltInRegistries.BLOCK.getHolderOrThrow(resourceKey));
        }

        @Override
        public Optional<HolderSet.Named<Block>> get(TagKey<Block> tagKey) {
            return BuiltInRegistries.BLOCK.getTag(tagKey);
        }
    }
}
