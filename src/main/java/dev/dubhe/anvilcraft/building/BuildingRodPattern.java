package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.RandomSupport;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 过滤槽仅描述材质和纹理，真实材料仍由建筑杖的统一提交过程消耗。 */
final class BuildingRodPattern {
    private static final int COLUMNS = 6;
    private static final int SLOTS = 18;
    private final List<ItemStack> slots;
    private final List<ItemStack> candidates = new ArrayList<>();
    private final boolean tiled;
    private final int minColumn;
    private final int minRow;
    private final int width;
    private final int height;

    BuildingRodPattern(FilterContent content) {
        this.slots = content.list();
        this.tiled = content.includeComponents();
        int left = COLUMNS;
        int top = SLOTS / COLUMNS;
        int right = -1;
        int bottom = -1;
        for (int slot = 0; slot < Math.min(SLOTS, this.slots.size()); slot++) {
            ItemStack stack = this.slots.get(slot);
            if (!(stack.getItem() instanceof BlockItem)) continue;
            this.candidates.add(stack);
            left = Math.min(left, slot % COLUMNS);
            top = Math.min(top, slot / COLUMNS);
            right = Math.max(right, slot % COLUMNS);
            bottom = Math.max(bottom, slot / COLUMNS);
        }
        this.minColumn = left;
        this.minRow = top;
        this.width = right - left + 1;
        this.height = bottom - top + 1;
    }

    ItemStack material(BlockPos first, BlockPos pos, Direction face, long seed) {
        if (this.candidates.isEmpty()) return ItemStack.EMPTY;
        if (!this.tiled) {
            int index = Math.floorMod(RandomSupport.mixStafford13(seed ^ Mth.getSeed(pos)), this.candidates.size());
            return this.candidates.get(index).copyWithCount(1);
        }
        int column = face.getAxis() == Direction.Axis.X ? pos.getZ() - first.getZ() : pos.getX() - first.getX();
        int row = face.getAxis() == Direction.Axis.Y ? pos.getZ() - first.getZ() : first.getY() - pos.getY();
        int slot = (this.minRow + Math.floorMod(row, this.height)) * COLUMNS
            + this.minColumn + Math.floorMod(column, this.width);
        if (slot >= this.slots.size()) return ItemStack.EMPTY;
        ItemStack stack = this.slots.get(slot);
        return stack.getItem() instanceof BlockItem ? stack.copyWithCount(1) : ItemStack.EMPTY;
    }

    @Nullable
    static List<BuildingPlan.Group> plan(Player player, ItemStack filter, BlockPos first, BlockPos last,
                                              Direction face, long seed) {
        BuildingRodPattern pattern = new BuildingRodPattern(filter.getOrDefault(ModComponents.FILTER_CONTENT, new FilterContent()));
        List<BuildingPlan.Group> groups = new ArrayList<>();
        Set<BlockPos> occupied = new HashSet<>();
        for (BlockPos cursor : BlockPos.betweenClosed(first, last)) {
            BlockPos pos = cursor.immutable();
            if (!BuildingCommit.canModify(player, pos)) return null;
            if (occupied.contains(pos) || !player.level().getBlockState(pos).canBeReplaced()) continue;
            ItemStack material = pattern.material(first, pos, face, seed);
            if (material.isEmpty()) continue;
            List<BuildingPlan.Group> planned = BuildingBlockPlanner.planBlocks(player, material, pos, pos, face, null);
            if (planned == null) return null;
            for (var group : planned) {
                if (group.cells.stream().anyMatch(cell -> occupied.contains(cell.pos()))) continue;
                for (var cell : group.cells) occupied.add(cell.pos());
                if (occupied.size() > BlueprintPlacement.MAX_ENTRIES) return null;
                groups.add(group);
            }
        }
        return groups;
    }
}
