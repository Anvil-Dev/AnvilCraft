package dev.dubhe.anvilcraft.client.support;

import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.DefinitionSerialization;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPattern;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPredicateWithState;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockUtil;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class LevelLikeDisplaySupport {
    private static final Map<LevelLike, List<TagSlot>> TAG_SLOTS = new WeakHashMap<>();

    public static List<List<ItemStack>> tagIngredients(MultiblockDefinition definition) {
        List<List<ItemStack>> result = new ArrayList<>();
        MultiblockUtil.tagIngredientCounts(definition).forEach((tag, count) -> {
            var items = BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, tag.location()));
            result.add(items.map(holders -> holders.stream()
                .map(holder -> new ItemStack(holder.value(), count)).toList()).orElse(List.of()));
        });
        return result;
    }

    public static void cycleTags(LevelLike level) {
        cycleTags(level, (int) (Util.getMillis() / 1000));
    }

    public static void cycleTags(LevelLike level, int variant) {
        for (var slot : TAG_SLOTS.getOrDefault(level, List.of())) {
            if (slot.states.isEmpty()) continue;
            int index = Math.floorMod(variant, slot.states.size());
            var state = slot.states.get(index);
            if (level.getBlockState(slot.pos) != state) level.setBlockState(slot.pos, state);
        }
    }

    private static List<BlockState> tagStates(BlockStatePredicate predicate) {
        return predicate.getBlocks().stream().map(holder -> {
            var block = holder.value();
            var state = MultiblockUtil.getDefaultState(predicate, block);
            return predicate.testWithoutEntity(state) ? state : block.getStateDefinition().getPossibleStates().stream()
                .filter(predicate::testWithoutEntity).findFirst().orElse(state);
        }).filter(predicate::testWithoutEntity).toList();
    }

    private record TagSlot(BlockPos pos, List<BlockState> states) {
    }

    public static LevelLike asLevelLike(MultiblockDefinition pattern) {
        var levelLike = new LevelLike(Minecraft.getInstance().level);
        var data = DefinitionSerialization.fromDefinition(pattern);
        int size = data.grid().length;
        List<TagSlot> tags = new ArrayList<>();
        for (int y = 0; y < size; y++) {
            for (int z = 0; z < data.grid()[y].length; z++) {
                for (int x = 0; x < data.grid()[y][z].length(); x++) {
                    var predicate = data.mapping().get(data.grid()[y][z].charAt(x));
                    var state = predicate == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                        : MultiblockUtil.getDefaultState(predicate);
                    var pos = new BlockPos(x - size / 2, y - size / 2, z - size / 2);
                    levelLike.setBlockState(pos, state);
                    if (predicate != null && predicate.getBlocks() instanceof HolderSet.Named<?>) {
                        tags.add(new TagSlot(pos, tagStates(predicate)));
                    }
                }
            }
        }
        TAG_SLOTS.put(levelLike, tags);
        return levelLike;
    }

    // @OnlyIn(Dist.CLIENT)
    public static LevelLike asLevelLike(BlockPattern pattern) {
        @SuppressWarnings("DataFlowIssue")
        LevelLike levelLike = new LevelLike(Minecraft.getInstance().level);

        int size = pattern.getSize();
        for (int y = size - 1; y >= 0; y--) {
            for (int x = size - 1; x >= 0; x--) {
                for (int z = size - 1; z >= 0; z--) {
                    BlockPredicateWithState predicate = pattern.getPredicate(x, y, z);
                    BlockState state = predicate.getDefaultState();
                    if (state.isAir() && Math.max(levelLike.horizontalSize(), levelLike.verticalSize()) >= size) {
                        continue;
                    }
                    levelLike.setBlockState(new BlockPos(x - size / 2, y - size / 2, z - size / 2), state);
                }
            }
        }

        return levelLike;
    }
}
