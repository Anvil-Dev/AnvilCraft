package dev.dubhe.anvilcraft.data.recipe.anvil.predicate.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

@Getter
public class BlockStatePredicate implements Predicate<BlockState> {
    public static final Codec<BlockStatePredicate> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            ResourceLocation.CODEC.fieldOf("isBlock").forGetter(o -> o.isBlock),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("statePredicate")
                    .forGetter(o -> java.util.Optional.ofNullable(o.statePredicate))
    ).apply(ins, BlockStatePredicate::new));

    private final ResourceLocation isBlock;
    private final Map<String, String> statePredicate;

    public BlockStatePredicate(ResourceLocation isBlock, Map<String, String> statePredicate) {
        this.isBlock = isBlock;
        this.statePredicate = statePredicate;
    }

    public BlockStatePredicate(ResourceLocation isBlock, Optional<Map<String, String>> statePredicate) {
        this(isBlock, statePredicate.orElse(null));
    }

    /**
     * newInstance
     */
    @SafeVarargs
    public static BlockStatePredicate of(Block block, Map.Entry<Property<?>, StringRepresentable>... states) {
        Map<String, String> m = new HashMap<>();
        for (Map.Entry<Property<?>, StringRepresentable> state : states) {
            m.put(state.getKey().getName(), state.getValue().getSerializedName());
        }
        return new BlockStatePredicate(BuiltInRegistries.BLOCK.getKey(block), m);
    }

    /**
     * newInstance
     */
    public static BlockStatePredicate of(ResourceLocation block) {
        return new BlockStatePredicate(block, Optional.empty());
    }

    @Override
    public boolean test(BlockState state) {
        Block block = BuiltInRegistries.BLOCK.get(isBlock);
        AtomicBoolean matches = new AtomicBoolean(true);
        state.getValues().forEach((property, comparable) -> {
            String name = property.getName();
            if (statePredicate.containsKey(name)) {
                String value = statePredicate.get(name);
                if (comparable instanceof StringRepresentable representable) {
                    matches.set(representable.getSerializedName().equals(value));
                } else {
                    matches.set(false);
                }
            }
        });
        return state.is(block) && matches.get();
    }
}
