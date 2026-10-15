package dev.dubhe.anvilcraft.integration.jei.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * JEI 展示用的矿物涌泉配方。
 *
 * <p>同一「维度 + 优先级 + 材料方块 + 周围需求」下的多条配方会合并为一个条目，
 * 由 {@code outcomes} 描述各自的结果与概率，JEI 会在同一格内轮换显示这些结果。</p>
 *
 * @param id                条目 id，取该组中第一条配方
 * @param sideRequirements  周围（底座）方块需求，含各自数量
 * @param fromBlocks        上方材料方块
 * @param outcomes          该组内所有可能的结果，按权重顺序排列
 * @param fluids            该组内声明的产出流体，仅用于 JEI 的流体查找，不额外占格
 * @param dimension         限定维度；为空表示任意维度
 * @param replacementChance 替换概率；非替换配方为 {@code null}
 * @param replacementTarget 被替换的主动配方名；为空表示可替换任意主动配方
 */
public record MineralFountainJeiRecipe(
    ResourceLocation id,
    List<SideRequirement> sideRequirements,
    List<BlockState> fromBlocks,
    List<Outcome> outcomes,
    List<FluidStack> fluids,
    @Nullable ResourceLocation dimension,
    @Nullable Double replacementChance,
    @Nullable Component replacementTarget
) {
    public MineralFountainJeiRecipe {
        sideRequirements = List.copyOf(sideRequirements);
        fromBlocks = List.copyOf(fromBlocks);
        outcomes = List.copyOf(outcomes);
        fluids = List.copyOf(fluids);
    }

    /**
     * 全部可能结果对应的方块状态，供槽位轮换与方块渲染使用。
     *
     * <p>流体产出本身也是方块（水、经验流体等），因此和普通方块一起轮换，
     * 渲染出来的就是轮换的流体方块。</p>
     */
    public List<BlockState> outcomeStates() {
        return this.outcomes.stream().map(Outcome::state).toList();
    }

    /**
     * 把周围需求摊到四个底座位置上，用于直观展示各需要几个。
     *
     * <p>数量之和通常等于 4（四个相邻面）；不足时剩余位置为空。
     * 例如「4× 粗铜块」四个位置都是粗铜块，「2× 熔岩 + 2× 熔融宝石」则各占两个位置。</p>
     */
    public List<SideRequirement> sideSlots() {
        List<SideRequirement> slots = new java.util.ArrayList<>(4);
        for (SideRequirement requirement : this.sideRequirements) {
            for (int i = 0; i < requirement.count() && slots.size() < SIDE_SLOT_COUNT; i++) {
                slots.add(requirement);
            }
        }
        return List.copyOf(slots);
    }

    /** 底座位置数量，即矿物涌泉的四个水平相邻面。 */
    public static final int SIDE_SLOT_COUNT = 4;

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

    /**
     * 一项底座（周围）需求：四个相邻面中需要 {@code count} 个 {@code states} 中的方块。
     *
     * <p>谓词命中多个方块（如标签）时 {@code states} 会有多项，展示时轮换。</p>
     *
     * @param states 可满足该需求的方块状态
     * @param count  所需数量
     */
    public record SideRequirement(List<BlockState> states, int count) {
        public SideRequirement {
            states = List.copyOf(states);
        }
    }
}
