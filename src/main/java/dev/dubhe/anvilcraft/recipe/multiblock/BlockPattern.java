package dev.dubhe.anvilcraft.recipe.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.util.CodecUtil;
import dev.dubhe.anvilcraft.util.BlockStateUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.util.ItemStackMap;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
public class BlockPattern {
    private final List<List<String>> layers;
    private final Map<Character, BlockPredicateWithState> symbols;

    public static final Codec<BlockPattern> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.listOf().listOf().fieldOf("layers").forGetter(o -> o.layers),
            Codec.unboundedMap(CodecUtil.CHAR_CODEC, BlockPredicateWithState.CODEC)
                .fieldOf("symbols")
                .forGetter(BlockPattern::getSymbols))
        .apply(ins, BlockPattern::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPattern> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).apply(ByteBufCodecs.list()),
        BlockPattern::getLayers,
        ByteBufCodecs.map(HashMap::new, CodecUtil.CHAR_STREAM_CODEC, BlockPredicateWithState.STREAM_CODEC),
        BlockPattern::getSymbols,
        BlockPattern::new);

    private BlockPattern(List<List<String>> layers, Map<Character, BlockPredicateWithState> symbols) {
        this.layers = layers;
        this.symbols = symbols;
    }

    private BlockPattern() {
        layers = new ArrayList<>();
        symbols = new HashMap<>();
    }

    @Contract(" -> new")
    public static BlockPattern create() {
        return new BlockPattern();
    }

    public BlockPattern layer(String... lines) {
        return layer(Arrays.asList(lines));
    }

    public BlockPattern layer(List<String> layer) {
        if (layer.size() % 2 != 1 && layer.size() > 15) {
            throw new IllegalArgumentException("Each layer must have an odd number of rows and cannot exceed 15.");
        }
        for (String line : layer) {
            if (line.length() != layer.size()) {
                throw new IllegalArgumentException(
                    "The number of squares in each row must be equal to the number of rows.");
            }
        }
        layers.add(layer);
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public BlockPattern symbol(char symbol, BlockPredicateWithState predicate) {
        symbols.put(symbol, predicate);
        return this;
    }

    public boolean checkSymbols() {
        Set<Character> characterSet = new HashSet<>();
        for (List<String> layer : layers) {
            for (String line : layer) {
                for (char c : line.toCharArray()) {
                    if (c == ' ') {
                        continue;
                    }
                    characterSet.add(c);
                }
            }
        }
        return symbols.keySet().containsAll(characterSet);
    }

    public BlockPredicateWithState getPredicate(int x, int y, int z) {
        char c = layers.get(y).get(z).charAt(x);
        if (c == ' ') {
            return BlockPredicateWithState.of(Blocks.AIR);
        }
        return symbols.get(c);
    }

    @Nullable
    public BlockPredicateWithState getBySymbol(char symbol) {
        return symbols.get(symbol);
    }

    public int getSize() {
        return layers.size();
    }

    public List<ItemStack> toIngredientList() {
        Object2IntMap<BlockState> states = new Object2IntOpenHashMap<>();
        for (List<String> layer : this.getLayers()) {
            for (String s : layer) {
                for (int i = 0; i < s.length(); i++) {
                    char c = s.charAt(i);
                    if (c == ' ') continue;
                    BlockPredicateWithState bySymbol = this.getBySymbol(c);
                    if (bySymbol != null) {
                        states.mergeInt(bySymbol.getDefaultState(), 1, Integer::sum);
                    }
                }
            }
        }
        Map<ItemStack, Integer> ingredients = ItemStackMap.createTypeAndTagMap();
        states.forEach((state, stateCount) -> BlockStateUtil.ingredientsForPlacement(state)
            .forEach(stack -> {
                int stackCount = stack.getCount();
                if (stackCount <= 0) return;
                stack.setCount(1);
                Integer totalCount = ingredients.computeIfAbsent(stack, ignore -> 0);
                ingredients.put(stack, totalCount + stateCount * stackCount);
            }));
        List<ItemStack> resultList = new ArrayList<>();
        ingredients.forEach((stack, count) -> {
            stack.setCount(count);
            resultList.add(stack);
        });
        return resultList;
    }
}
