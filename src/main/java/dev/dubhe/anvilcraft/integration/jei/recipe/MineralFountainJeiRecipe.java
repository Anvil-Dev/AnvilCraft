package dev.dubhe.anvilcraft.integration.jei.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * JEI 展示用的矿物涌泉配方。
 *
 * <p>同一「维度 + 优先级 + 材料方块 + 周围需求」下的多条配方会合并为一个条目，
 * 由 {@code outcomes} 描述各自的结果与概率，JEI 会在同一格内轮换显示这些方块。</p>
 *
 * @param id                条目 id，取该组中第一条配方
 * @param sideBlocks        周围方块需求（主动生产）或空
 * @param fromBlocks        上方材料方块
 * @param outcomes          该组内所有可能的结果，按权重顺序排列
 * @param dimension         限定维度；为空表示任意维度
 * @param replacementChance 替换概率；非替换配方为 {@code null}
 * @param replacementTarget 被替换的主动配方名；为空表示可替换任意主动配方
 */
public record MineralFountainJeiRecipe(
    ResourceLocation id,
    List<BlockState> sideBlocks,
    List<BlockState> fromBlocks,
    List<Outcome> outcomes,
    @Nullable ResourceLocation dimension,
    @Nullable Double replacementChance,
    @Nullable Component replacementTarget
) {
    public MineralFountainJeiRecipe {
        sideBlocks = List.copyOf(sideBlocks);
        fromBlocks = List.copyOf(fromBlocks);
        outcomes = List.copyOf(outcomes);
    }

    /** 全部可能结果对应的方块状态，供渲染与槽位轮换使用。 */
    public List<BlockState> outcomeStates() {
        return this.outcomes.stream().map(Outcome::state).toList();
    }

    /** 条目整体的单次产出概率，即各组内结果概率之和。 */
    public double totalChance() {
        return this.outcomes.stream().mapToDouble(Outcome::chance).sum();
    }

    /**
     * 单个结果的最终产出概率：结果方块概率 × 同优先级并列时的权重占比。
     *
     * @param state 结果方块状态
     * @return 对应概率；未收录该方块时返回 {@code -1}
     */
    public double chanceOf(BlockState state) {
        for (Outcome outcome : this.outcomes) {
            if (outcome.state().is(state.getBlock())) return outcome.chance();
        }
        return -1;
    }

    /**
     * 一个可能的结果。
     *
     * @param state  结果方块
     * @param chance 最终产出概率（已含权重占比）
     */
    public record Outcome(BlockState state, double chance) {
    }
}
