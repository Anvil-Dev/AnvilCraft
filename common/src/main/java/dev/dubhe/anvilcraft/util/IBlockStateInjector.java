package dev.dubhe.anvilcraft.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.stream.Stream;

public interface IBlockStateInjector {
    @SuppressWarnings("DuplicatedCode")
    static @NotNull BlockState fromJson(JsonElement stateJson) {
        if (!stateJson.isJsonObject()) throw new JsonSyntaxException("Expected item to be object");
        JsonObject object = stateJson.getAsJsonObject();
        if (!object.has("block")) throw new JsonSyntaxException("The field block is missing");
        JsonElement blockElement = object.get("block");
        if (!blockElement.isJsonPrimitive()) throw new JsonSyntaxException("Expected item to be string");
        StringBuilder block = new StringBuilder(blockElement.getAsString());
        if (object.has("state")) {
            block.append(GsonHelper.getAsString(object,"state"));
        }
        HolderLookup<Block> blocks = new BlockHolderLookup();
        BlockStateParser.BlockResult blockResult;
        try {
            blockResult = BlockStateParser.parseForBlock(blocks, block.toString(), true);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        return blockResult.blockState();
    }

    default JsonElement anvilcraft$toJson() {
        return new JsonObject();
    }

    class BlockHolderLookup implements HolderLookup<Block>, HolderOwner<Block> {
        @Override
        public @NotNull Stream<Holder.Reference<Block>> listElements() {
            return BuiltInRegistries.BLOCK.stream().map(BuiltInRegistries.BLOCK::getResourceKey).filter(Optional::isPresent).map(key -> BuiltInRegistries.BLOCK.getHolderOrThrow(key.get()));
        }

        @Override
        public @NotNull Stream<HolderSet.Named<Block>> listTags() {
            return BuiltInRegistries.BLOCK.getTags().map(Pair::getSecond);
        }

        @Override
        public @NotNull Optional<Holder.Reference<Block>> get(@NotNull ResourceKey<Block> resourceKey) {
            return Optional.of(BuiltInRegistries.BLOCK.getHolderOrThrow(resourceKey));
        }

        @Override
        public @NotNull Optional<HolderSet.Named<Block>> get(@NotNull TagKey<Block> tagKey) {
            return BuiltInRegistries.BLOCK.getTag(tagKey);
        }
    }
}
