package dev.dubhe.anvilcraft.recipe.mulitblock;

import dev.dubhe.anvilcraft.util.CodecUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.Blocks;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
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
        if (lines.length != 3) {
            throw new IllegalArgumentException("Must have 3 line in block pattern");
        }
        for (String line : lines) {
            if (line.length() != 3) {
                throw new IllegalArgumentException("Must have 3 block in line");
            }
        }
        layers.add(Arrays.asList(lines));
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
}
