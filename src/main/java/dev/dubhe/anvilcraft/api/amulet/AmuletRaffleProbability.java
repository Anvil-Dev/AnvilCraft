package dev.dubhe.anvilcraft.api.amulet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.amulet.def.IAmuletDefinition;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Holder;

import java.util.Map;
import java.util.function.Function;

/**
 * 存储了玩家目前护符抽取的额外概率
 *
 * @param map 存储概率，键为类型，值为该类型目前的概率
 */
public record AmuletRaffleProbability(Object2IntMap<Holder<IAmuletDefinition>> map) {
    public static final AmuletRaffleProbability EMPTY = new AmuletRaffleProbability(new Object2IntOpenHashMap<>());
    public static final MapCodec<AmuletRaffleProbability> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        Codec.unboundedMap(IAmuletDefinition.CODEC, Codec.INT)
            .fieldOf("probabilities")
            .xmap(AmuletRaffleProbability::fromMap, Function.identity())
            .forGetter(AmuletRaffleProbability::map)
    ).apply(inst, AmuletRaffleProbability::new));

    /// 获取该类型在此处存储的概率。
    ///
    /// @param type 类型
    /// @return 概率
    public int getProbability(Holder<IAmuletDefinition> type) {
        int probability = this.map.getInt(type);
        if (probability <= 0) {
            probability = 20;
        }
        return probability;
    }

    /// 向此处存储该类型的概率。
    ///
    /// @param type        类型
    /// @param probability 新概率
    /// @return 旧概率
    @SuppressWarnings("UnusedReturnValue")
    public int setProbability(Holder<IAmuletDefinition> type, int probability) {
        return this.map.put(type, probability);
    }

    private static Object2IntMap<Holder<IAmuletDefinition>> fromMap(Map<Holder<IAmuletDefinition>, Integer> map) {
        return new Object2IntOpenHashMap<>(map);
    }
}
