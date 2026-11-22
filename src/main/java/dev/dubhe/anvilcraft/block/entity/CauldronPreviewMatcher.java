package dev.dubhe.anvilcraft.block.entity;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.block.HasBlockBase;
import dev.dubhe.anvilcraft.api.recipe.CauldronProcessing;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasAnvil;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldronProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** 按世界内配方的顺序检查预览条件，在独立的库存和流体快照中尝试冲突原料的不同分配。 */
final class CauldronPreviewMatcher {
    private CauldronPreviewMatcher() {
    }

    /** 每次尝试独立保留当前可用的原料与流体，成功分支直接供展示使用。 */
    interface State<S extends State<S>> {
        S copy();

        boolean apply(IRecipePredicate<?> predicate);
    }

    static boolean matchesWorld(IRecipePredicate<?> predicate, Level level, Vec3 contextPos, BlockState anvil) {
        return switch (predicate) {
            case HasCauldronProcessor processor -> {
                BlockPos pos = BlockPos.containing(contextPos.add(processor.offset()));
                yield CauldronProcessing.matches(processor.category(), level.getBlockState(pos));
            }
            case HasBlockBase<?> block -> {
                BlockPos pos = BlockPos.containing(contextPos.add(block.getOffset()));
                yield block.getPredicate().test(level, level.getBlockState(pos), level.getBlockEntity(pos));
            }
            case HasAnvil required -> {
                boolean matches = required.anvil().map(condition -> condition.test(level, anvil, null))
                    .orElseGet(() -> anvil.is(BlockTags.ANVIL));
                yield matches != required.inverted();
            }
            default -> false;
        };
    }

    static <S extends State<S>> @Nullable S match(InWorldRecipe recipe, S state) {
        for (var predicate : recipe.nonConflicting()) {
            if (!state.apply(predicate)) {
                return null;
            }
        }
        var conflicts = recipe.conflicting();
        if (recipe.compatible() || conflicts.size() <= 1) {
            for (var predicate : conflicts) {
                if (!state.apply(predicate)) {
                    return null;
                }
            }
            return state;
        }
        return matchOrder(conflicts, new boolean[conflicts.size()], conflicts.size(), state);
    }

    private static <S extends State<S>> @Nullable S matchOrder(
        List<IRecipePredicate<?>> predicates, boolean[] used, int remaining, S state
    ) {
        for (int i = 0; i < predicates.size(); i++) {
            if (used[i]) {
                continue;
            }
            if (remaining == 1) {
                return state.apply(predicates.get(i)) ? state : null;
            }
            var branch = state.copy();
            if (branch.apply(predicates.get(i))) {
                used[i] = true;
                var result = matchOrder(predicates, used, remaining - 1, branch);
                used[i] = false;
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
