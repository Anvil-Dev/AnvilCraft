package dev.dubhe.anvilcraft.integration.jei.recipe;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record MineralFountainJeiRecipe(
    Identifier id,
    List<SideRequirement> sideRequirements,
    List<BlockState> fromBlocks,
    List<Outcome> outcomes,
    List<FluidStack> fluids,
    @Nullable Identifier dimension,
    @Nullable Double replacementChance,
    @Nullable Component replacementTarget
) {
    public MineralFountainJeiRecipe {
        sideRequirements = List.copyOf(sideRequirements);
        fromBlocks = List.copyOf(fromBlocks);
        outcomes = List.copyOf(outcomes);
        fluids = List.copyOf(fluids);
    }

    public List<BlockState> outcomeStates() {
        return this.outcomes.stream().map(Outcome::state).toList();
    }

    public List<SideRequirement> sideSlots() {
        List<SideRequirement> slots = new ArrayList<>(4);
        for (SideRequirement requirement : this.sideRequirements) {
            for (int i = 0; i < requirement.count() && slots.size() < SIDE_SLOT_COUNT; i++) {
                slots.add(requirement);
            }
        }
        return List.copyOf(slots);
    }

    public static final int SIDE_SLOT_COUNT = 4;

    public double totalChance() {
        return this.outcomes.stream().mapToDouble(Outcome::chance).sum();
    }

    public double chanceOf(BlockState state) {
        for (Outcome outcome : this.outcomes) {
            if (outcome.state().is(state.getBlock())) return outcome.chance();
        }
        return -1;
    }

    public record Outcome(BlockState state, double chance) {
    }

    public record SideRequirement(List<BlockState> states, int count) {
        public SideRequirement {
            states = List.copyOf(states);
        }
    }
}
