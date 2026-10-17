package dev.dubhe.anvilcraft.integration.jei.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * JEI 展示用的矿物涌泉配方：同「维度 + 优先级 + 材料方块 + 周围需求」的多条配方合并为一条，结果同格轮换。
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

    /** 全部结果的方块状态；流体产出本身也是方块，一并轮换。 */
    public List<BlockState> outcomeStates() {
        return this.outcomes.stream().map(Outcome::state).toList();
    }

    /** 把周围需求摊到四个底座位置上；数量之和通常为 4，不足时剩余位置为空。 */
    public List<SideRequirement> sideSlots() {
        List<SideRequirement> slots = new java.util.ArrayList<>(4);
        for (SideRequirement requirement : this.sideRequirements) {
            for (int i = 0; i < requirement.count() && slots.size() < SIDE_SLOT_COUNT; i++) {
                slots.add(requirement);
            }
        }
        return List.copyOf(slots);
    }

    public static final int SIDE_SLOT_COUNT = 4;

    /** 条目整体的单次产出概率。 */
    public double totalChance() {
        return this.outcomes.stream().mapToDouble(Outcome::chance).sum();
    }

    /** 结果方块的最终概率（已含权重占比）；未收录时返回 {@code -1}。 */
    public double chanceOf(BlockState state) {
        for (Outcome outcome : this.outcomes) {
            if (outcome.state().is(state.getBlock())) return outcome.chance();
        }
        return -1;
    }

    public record Outcome(BlockState state, double chance) {
    }

    /**
     * 一项底座需求：四个相邻面中需要 {@code count} 个 {@code states} 中的方块。
     * 谓词命中多个方块（如标签）时 {@code states} 有多项，展示时轮换。
     */
    public record SideRequirement(List<BlockState> states, int count) {
        public SideRequirement {
            states = List.copyOf(states);
        }
    }
}
