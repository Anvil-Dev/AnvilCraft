package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.item.BuildingRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class BuildingRodFluids {
    private BuildingRodFluids() {
    }

    public static boolean isWater(ItemStack stack) {
        if (!(stack.getItem() instanceof BucketItem) || stack.getItem() instanceof MobBucketItem) return false;
        return FluidUtil.getFirstStackContained(stack).getFluid().isSame(Fluids.WATER);
    }

    static void place(ServerPlayer player, BlockPos first, BlockPos last) {
        Plan plan = plan(player, first, last);
        if (plan == null || plan.group.cells.isEmpty()) return;
        var level = player.level();
        if (plan.evaporates) {
            BuildingMaterials materials = new BuildingMaterials(player, BuildingRodItem.material(player));
            if (!materials.reserve(List.of(), plan.group.fluids)) {
                BuildingRodService.message(player, "missing_blocks");
                return;
            }
            if (!BuildingRodItem.ready(player, BuildingRodItem.heldRod(player)) || !materials.consume()) return;
            plan.fluid.getFluidType().onVaporize(null, level, first, plan.fluid);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, first.getX() + 0.5, first.getY() + 0.5, first.getZ() + 0.5,
                8, 0.4, 0.4, 0.4, 0);
            BuildingRodService.finishPlacement(player, plan.group.cells.size());
            return;
        }
        if (!BuildingRodService.commit(player, List.of(plan.group), false, false)) return;
        for (var cell : plan.group.cells) {
            var placed = cell.state().getFluidState().getType();
            level.scheduleTick(cell.pos(), placed, placed.getTickDelay(level));
        }
    }

    public static List<BuildingPlan.Cell> preview(Player player, BlockPos first, BlockPos last) {
        Plan plan = plan(player, first, last);
        return plan == null ? List.of() : List.copyOf(plan.group.cells);
    }

    private record Plan(BuildingPlan.Group group, FluidStack fluid, boolean evaporates) {
    }

    @Nullable
    private static Plan plan(Player player, BlockPos first, BlockPos last) {
        for (BlockPos pos : BlockPos.betweenClosed(first, last)) {
            if (!BuildingRodService.canModify(player, pos)) return null;
        }
        FluidStack fluid = FluidUtil.getFirstStackContained(BuildingRodItem.material(player));
        if (fluid.isEmpty()) return null;
        var level = player.level();
        var type = fluid.getFluidType();
        var fluidState = type.getStateForPlacement(level, first, fluid);
        if (!type.canBePlacedInLevel(level, first, fluid)) return null;
        boolean evaporates = level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, first)
            && fluidState.is(FluidTags.WATER)
            || type.isVaporizedOnPlacement(level, first, fluid);
        BuildingPlan.Group group = new BuildingPlan.Group();
        for (BlockPos cursor : BlockPos.betweenClosed(first, last)) {
            BlockPos pos = cursor.immutable();
            BlockState before = level.getBlockState(pos);
            BlockState after;
            if (fluid.getFluid().isSame(Fluids.WATER) && before.hasProperty(BlockStateProperties.WATERLOGGED)) {
                if (before.getValue(BlockStateProperties.WATERLOGGED)) continue;
                after = before.setValue(BlockStateProperties.WATERLOGGED, true);
            } else {
                if (!before.canBeReplaced()) continue;
                if (!before.getFluidState().isEmpty() && !before.getFluidState().getType().isSame(fluid.getFluid())) continue;
                after = type.getBlockForFluidState(level, pos, type.getStateForPlacement(level, pos, fluid));
            }
            if (before.equals(after)) continue;
            if (!BuildingRodService.canModify(player, pos)) {
                return null;
            }
            group.cells.add(new BuildingPlan.Cell(pos, after, new CompoundTag(), List.of()));
        }
        int buckets = level instanceof ServerLevel serverLevel && fluidState.canConvertToSource(serverLevel, first)
            ? 2 : group.cells.size();
        group.fluids.add(fluid.copyWithAmount(buckets * FluidType.BUCKET_VOLUME));
        return new Plan(group, fluid, evaporates);
    }
}
