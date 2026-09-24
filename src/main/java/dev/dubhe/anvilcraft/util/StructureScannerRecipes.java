package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.building.BlueprintNormalizer;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StructureScannerRecipes {
    private StructureScannerRecipes() {
    }

    public static String fileName(Identifier recipe) {
        String name = recipe.getPath().replace('/', '_');
        return name.substring(0, Math.min(name.length(), 120)) + ".nbt";
    }

    public static StructureSnapshot snapshot(Recipe<?> recipe) {
        MultiblockDefinition pattern = switch (recipe) {
            case MultiblockRecipe crafting -> crafting.getPattern();
            case MultiblockConversionRecipe conversion -> conversion.getInputPattern();
            default -> throw new IllegalArgumentException("Recipe has no supported multiblock input");
        };
        var definition = pattern.toGlobal(BlockPos.ZERO);
        if (definition.isEmpty()) throw new IllegalArgumentException("Recipe structure is empty");
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : definition.keySet()) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        if ((long) maxX - minX >= StructureSnapshotCodec.MAX_AXIS || (long) maxY - minY >= StructureSnapshotCodec.MAX_AXIS
            || (long) maxZ - minZ >= StructureSnapshotCodec.MAX_AXIS) {
            throw new IllegalArgumentException("Recipe structure exceeds 16x16x16");
        }
        List<BlockState> palette = new ArrayList<>();
        List<StructureSnapshot.BlockEntry> blocks = new ArrayList<>();
        BlockPos origin = new BlockPos(minX, minY, minZ);
        for (var entry : definition.entrySet()) {
            var predicate = entry.getValue();
            BlockState state = matchingState(predicate);
            if (!palette.contains(state)) palette.add(state);
            Optional<CompoundTag> nbt = predicate.getNbts().isEmpty() ? Optional.empty()
                : Optional.of(predicate.getNbts().getFirst().tag().copy());
            blocks.add(new StructureSnapshot.BlockEntry(entry.getKey().subtract(origin), palette.indexOf(state), nbt));
        }
        return BlueprintNormalizer.normalize(new StructureSnapshot(new Vec3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1),
            palette, blocks, List.of())).snapshot();
    }

    private static BlockState matchingState(BlockStatePredicate predicate) {
        BlockState preferred = MultiblockUtil.getDefaultState(predicate);
        if (predicate.testWithoutEntity(preferred)) return preferred;
        return predicate.getBlocks().stream().flatMap(block -> block.value().getStateDefinition().getPossibleStates().stream())
            .filter(predicate::testWithoutEntity).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Recipe ingredient has no matching block state"));
    }
}
