package dev.dubhe.anvilcraft.data.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.block.HasBlock;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;

@Getter
public class RecipeBlock {
    private final Block block;
    private final TagKey<Block> blockTagKey;
    private final Map.Entry<Property<?>, Comparable<?>>[] stateEntries;
    private final BlockState blockState;

    /**
     * 配方方块
     */
    public RecipeBlock(
            Block block,
            TagKey<Block> blockTagKey,
            Map.Entry<Property<?>, Comparable<?>>[] stateEntries,
            BlockState blockState
    ) {
        this.block = block;
        this.blockTagKey = blockTagKey;
        this.stateEntries = stateEntries;
        this.blockState = blockState;
    }

    public static RecipeBlock of(Block block) {
        return new RecipeBlock(block, null, null, null);
    }

    @SafeVarargs
    public static RecipeBlock of(Block block, Map.Entry<Property<?>, Comparable<?>>... stateEntries) {
        return new RecipeBlock(block, null, stateEntries, null);
    }

    public static RecipeBlock of(TagKey<Block> blockTagKey) {
        return new RecipeBlock(null, blockTagKey, null, null);
    }

    @SafeVarargs
    public static RecipeBlock of(TagKey<Block> blockTagKey, Map.Entry<Property<?>, Comparable<?>>... stateEntries) {
        return new RecipeBlock(null, blockTagKey, stateEntries, null);
    }

    public static RecipeBlock of(BlockState blockState) {
        return new RecipeBlock(null, null, null, blockState);
    }

    /**
     * 判断是否有状态检测, 一般用于{@link HasBlock hasBlock}
     */
    public boolean isHasStates() {
        return this.stateEntries != null;
    }

    /**
     * 判断是否为方块状态, 一般用于{@link dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SetBlock setBlock}
     */
    public boolean isBlockStates() {
        return this.blockState != null;
    }

    public boolean isTag() {
        return this.blockTagKey != null;
    }

    /**
     *  获取key
     *
     * @return key
     */
    public String getKey() {
        return this.block == null
                ? this.blockTagKey == null
                ? ""
                : blockTagKey.location().getNamespace() + "_" + blockTagKey.location().getPath()
                : BuiltInRegistries.BLOCK
                .getKey(this.block).getPath();
    }
}
