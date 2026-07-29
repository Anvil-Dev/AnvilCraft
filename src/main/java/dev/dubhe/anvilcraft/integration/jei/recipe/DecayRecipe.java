package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.storage.ExcitedStateVoidMatterBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public record DecayRecipe(
    Identifier id,
    List<Block> centers,
    List<Block> results,
    @Nullable TagKey<Block> resultTag,
    List<BlockPos> matchingNeighbors,
    Map<BlockPos, Block> fixedNeighbors
) {
    private static final BlockPos UP = new BlockPos(1, 2, 1);
    private static final BlockPos DOWN = new BlockPos(1, 0, 1);
    private static final BlockPos LEFT = new BlockPos(0, 1, 1);
    private static final BlockPos RIGHT = new BlockPos(2, 1, 1);
    private static final BlockPos FRONT = new BlockPos(1, 1, 2);
    private static final BlockPos BACK = new BlockPos(1, 1, 0);

    public DecayRecipe {
        centers = List.copyOf(centers);
        results = List.copyOf(results);
        matchingNeighbors = List.copyOf(matchingNeighbors);
        fixedNeighbors = Map.copyOf(fixedNeighbors);
        if (centers.isEmpty()) {
            throw new IllegalArgumentException("Decay recipe must have at least one center block");
        }
        if (resultTag == null && results.isEmpty()) {
            throw new IllegalArgumentException("Decay recipe must have results or a result tag");
        }
        if (resultTag == null && centers.size() > 1 && results.size() != centers.size()) {
            throw new IllegalArgumentException("Multi-center decay results must match the number of center blocks");
        }
    }

    public static List<DecayRecipe> getAllRecipes() {
        List<Block> radioactiveBlocks = List.of(
            ModBlocks.PLUTONIUM_BLOCK.get(),
            ModBlocks.URANIUM_BLOCK.get()
        );
        List<Block> decayProducts = List.of(
            ModBlocks.URANIUM_BLOCK.get(),
            ModBlocks.LEAD_BLOCK.get()
        );
        return List.of(
            new DecayRecipe(
                AnvilCraft.of("decay/void_matter"),
                List.of(ModBlocks.VOID_MATTER_BLOCK.get()),
                List.of(),
                ModBlockTags.VOID_DECAY_PRODUCTS,
                List.of(DOWN, BACK, FRONT, UP, LEFT),
                Map.of()
            ),
            new DecayRecipe(
                AnvilCraft.of("decay/radioactive_three_sides"),
                radioactiveBlocks,
                decayProducts,
                null,
                List.of(UP, DOWN, LEFT),
                Map.of()
            ),
            new DecayRecipe(
                AnvilCraft.of("decay/radioactive_four_sides_with_lead"),
                radioactiveBlocks,
                decayProducts,
                null,
                List.of(UP, DOWN, LEFT, RIGHT),
                Map.of(FRONT, ModBlocks.LEAD_BLOCK.get())
            ),
            new DecayRecipe(
                AnvilCraft.of("decay/radioactive_six_sides"),
                radioactiveBlocks,
                List.of(Blocks.LAVA, Blocks.LAVA),
                null,
                List.of(UP, DOWN, LEFT, RIGHT, FRONT, BACK),
                Map.of()
            ),
            new DecayRecipe(
                AnvilCraft.of("decay/excited_state_void_matter"),
                List.of(ModBlocks.EXCITED_STATE_VOID_MATTER_BLOCK.get()),
                ExcitedStateVoidMatterBlock.getDecayProducts(),
                null,
                List.of(RIGHT),
                Map.of()
            ),
            new DecayRecipe(
                AnvilCraft.of("decay/excited_state_void_matter_confined_anvil"),
                List.of(ModBlocks.EXCITED_STATE_VOID_MATTER_BLOCK.get()),
                ExcitedStateVoidMatterBlock.getConfinedAnvilons(),
                null,
                List.of(RIGHT),
                Map.of(LEFT, ModBlocks.CONFINEMENT_CHAMBER.get())
            ),
            new DecayRecipe(
                AnvilCraft.of("decay/void_matter_near_excited_state"),
                List.of(ModBlocks.VOID_MATTER_BLOCK.get()),
                List.of(),
                ModBlockTags.VOID_DECAY_PRODUCTS,
                List.of(),
                Map.of(RIGHT, ModBlocks.EXCITED_STATE_VOID_MATTER_BLOCK.get())
            )
        );
    }
}
