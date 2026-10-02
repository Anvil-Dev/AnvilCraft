package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.block.cake.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.building.BuildingPlan.Cell;
import dev.dubhe.anvilcraft.building.BuildingPlan.Group;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import dev.dubhe.anvilcraft.item.block.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.item.block.LargeCakeBlockItem;
import dev.dubhe.anvilcraft.item.block.SimpleMultiPartBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BuildingBlockPlanner {
    private BuildingBlockPlanner() {
    }

    @Nullable
    static List<Group> planBlocks(Player player, ItemStack held, BlockPos first, BlockPos last,
                                         Direction face, @Nullable BlockHitResult hit) {
        for (BlockPos pos : BlockPos.betweenClosed(first, last)) {
            if (!BuildingCommit.canModify(player, pos)) return null;
        }
        if (hit != null && !player.level().hasChunkAt(hit.getBlockPos())) return null;
        BlockItem item = (BlockItem) held.getItem();
        if (first.equals(last) && !(item.getBlock() instanceof AbstractMultiPartBlock<?>)
            && !(item.getBlock() instanceof LargeCakeBlock)) {
            BlockHitResult click = hit == null ? new BlockHitResult(Vec3.atCenterOf(first), face, first, false) : hit;
            List<Cell> cells = singlePlacement(new UseOnContext(player.level(), player, BuildingRodItem.materialHand(player), held, click));
            if (cells.isEmpty()) return null;
            Group group = new Group();
            group.cells.addAll(cells);
            group.blockMaterials.put(cells.getFirst().pos(), held.copyWithCount(1));
            return List.of(group);
        }
        Block block = item.getBlock();
        if (block instanceof AbstractMultiPartBlock<?> || block instanceof LargeCakeBlock || block instanceof DoorBlock
            || block instanceof DoublePlantBlock || block instanceof BedBlock) {
            return tile(player, held, block, first, last, face);
        }
        List<Group> groups = new ArrayList<>();
        var level = player.level();
        for (BlockPos cursor : BlockPos.betweenClosed(first, last)) {
            BlockPos pos = cursor.immutable();
            if (!level.getBlockState(pos).canBeReplaced()) continue;
            if (!BuildingCommit.canModify(player, pos)) {
                return null;
            }
            BlockPlaceContext context = new BlockPlaceContext(player, BuildingRodItem.materialHand(player), held,
                new BlockHitResult(Vec3.atCenterOf(pos), face, pos, false));
            BlockState state = item.getBlock().getStateForPlacement(context);
            if (state == null) {
                return null;
            }
            Group group = new Group();
            addBoxCells(group, pos, state);
            group.blockMaterials.put(pos, held.copyWithCount(1));
            groups.add(group);
        }
        Map<BlockPos, Cell> planned = new LinkedHashMap<>();
        List<Group> unique = new ArrayList<>();
        for (Group group : groups) {
            if (planned.containsKey(group.cells.getFirst().pos())) continue;
            for (Cell cell : group.cells) {
                if (planned.containsKey(cell.pos()) || !BuildingCommit.canModify(player, cell.pos())
                    || !level.getBlockState(cell.pos()).canBeReplaced()) {
                    return null;
                }
                planned.put(cell.pos(), cell);
            }
            unique.add(group);
            if (planned.size() > BlueprintPlacement.MAX_ENTRIES) {
                return null;
            }
        }
        return unique;
    }

    @Nullable
    private static List<Group> tile(Player player, ItemStack held, Block block, BlockPos first, BlockPos last, Direction face) {
        BlockPlaceContext context = new BlockPlaceContext(player, BuildingRodItem.materialHand(player), held,
            new BlockHitResult(Vec3.atCenterOf(first), face, first, false));
        BlockState state = block instanceof SimpleMultiPartBlock<?> multipart
            ? multipart.getPlacementState(context) : block.getStateForPlacement(context);
        if (state == null) return null;
        Group template = new Group();
        addBoxCells(template, BlockPos.ZERO, state);
        int minX = template.cells.stream().mapToInt(cell -> cell.pos().getX()).min().orElse(0);
        int minY = template.cells.stream().mapToInt(cell -> cell.pos().getY()).min().orElse(0);
        int minZ = template.cells.stream().mapToInt(cell -> cell.pos().getZ()).min().orElse(0);
        int maxX = template.cells.stream().mapToInt(cell -> cell.pos().getX()).max().orElse(0);
        int maxY = template.cells.stream().mapToInt(cell -> cell.pos().getY()).max().orElse(0);
        int maxZ = template.cells.stream().mapToInt(cell -> cell.pos().getZ()).max().orElse(0);
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        int countX = Math.abs(last.getX() - first.getX()) / width + 1;
        int countY = Math.abs(last.getY() - first.getY()) / height + 1;
        int countZ = Math.abs(last.getZ() - first.getZ()) / depth + 1;
        if ((long) countX * countY * countZ * template.cells.size() > BlueprintPlacement.MAX_ENTRIES) return null;
        int anchorX = face == Direction.EAST ? minX : face == Direction.WEST ? maxX : (minX + maxX) / 2;
        int anchorY = face == Direction.DOWN ? maxY : minY;
        int anchorZ = face == Direction.SOUTH ? minZ : face == Direction.NORTH ? maxZ : (minZ + maxZ) / 2;
        int signX = last.getX() >= first.getX() ? 1 : -1;
        int signY = last.getY() >= first.getY() ? 1 : -1;
        int signZ = last.getZ() >= first.getZ() ? 1 : -1;
        List<Group> groups = new ArrayList<>();
        for (int x = 0; x < countX; x++) {
            for (int y = 0; y < countY; y++) {
                for (int z = 0; z < countZ; z++) {
                    BlockPos origin = first.offset(x * width * signX - anchorX,
                        y * height * signY - anchorY, z * depth * signZ - anchorZ);
                    Group group = new Group();
                    for (Cell cell : template.cells) {
                        BlockPos pos = origin.offset(cell.pos());
                        if (!BuildingCommit.canModify(player, pos) || !player.level().getBlockState(pos).canBeReplaced()) {
                            return null;
                        }
                        group.cells.add(new Cell(pos, cell.state(), cell.config(), cell.contents()));
                    }
                    group.blockMaterials.put(group.cells.getFirst().pos(), held.copyWithCount(1));
                    groups.add(group);
                }
            }
        }
        return groups;
    }

    public static List<Cell> singlePlacement(UseOnContext use) {
        if (!(use.getItemInHand().getItem() instanceof BlockItem item) || use.getPlayer() == null) return List.of();
        List<Cell> cells = singleAttempt(item, use);
        if (!cells.isEmpty()) return cells;
        int distance;
        if (item instanceof SimpleMultiPartBlockItem<?> simple) {
            distance = simple.getMaxOffsetDistance(use.getClickedFace());
        } else if (item instanceof FlexibleMultiPartBlockItem<?, ?, ?> flexible) {
            BlockState state = flexible.getBlock().getPlacementState(new BlockPlaceContext(use));
            if (state == null) return List.of();
            distance = flexible.getMaxOffsetDistance(state, use.getClickedFace());
        } else {
            return List.of();
        }
        return singleAttempt(item, new UseOnContext(use.getLevel(), use.getPlayer(), use.getHand(), use.getItemInHand(),
            new BlockHitResult(use.getClickLocation().relative(use.getClickedFace(), distance), use.getClickedFace(),
                use.getClickedPos().relative(use.getClickedFace(), distance), false)));
    }

    private static List<Cell> singleAttempt(BlockItem item, UseOnContext use) {
        BlockPlaceContext context = item.updatePlacementContext(new BlockPlaceContext(use));
        if (context == null || !context.canPlace()) return List.of();
        if (item.getBlock() instanceof CelestialForgingAnvilAmplifierBlock amplifier) {
            BlockPos pos = context.getClickedPos();
            BlockPos snapped = amplifier.snapMainPos(context.getLevel(), pos);
            if (snapped != null && !snapped.equals(pos)) {
                context = new BlockPlaceContext(new UseOnContext(use.getLevel(), use.getPlayer(), use.getHand(), use.getItemInHand(),
                    new BlockHitResult(context.getClickLocation().add(Vec3.atLowerCornerOf(snapped.subtract(pos))),
                        context.getClickedFace(), snapped, false)));
            }
        }
        BlockState state = item instanceof LargeCakeBlockItem cake
            ? cake.getPlacementState(context) : item.getBlock().getStateForPlacement(context);
        if (state == null) return List.of();
        if (!item.canPlace(context, state)) {
            if (use.getPlayer() instanceof ServerPlayer player && BuildingRodItem.isHeld(player)) {
                BuildingRodObstructions.reject(player,
                    List.of(new Cell(context.getClickedPos(), state, new CompoundTag(), List.of())));
            }
            return List.of();
        }
        Group group = new Group();
        addBoxCells(group, context.getClickedPos(), state);
        for (Cell cell : group.cells) {
            Player player = use.getPlayer();
            if (player == null || !BuildingCommit.canModify(player, cell.pos())) return List.of();
            if (!cell.pos().equals(context.getClickedPos()) && !use.getLevel().getBlockState(cell.pos()).canBeReplaced()) return List.of();
        }
        return List.copyOf(group.cells);
    }

    private static void addBoxCells(Group group, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof LargeCakeBlock) {
            LargeCakeBlockItem.forEachPlacedBlock(pos, state,
                (partPos, partState) -> group.cells.add(new Cell(partPos, partState, new CompoundTag(), List.of())));
            return;
        }
        group.cells.add(new Cell(pos, state, new CompoundTag(), List.of()));
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> multipart) {
            addMultipart(group, pos, state, multipart);
        } else if (state.getBlock() instanceof DoorBlock
            || state.getBlock() instanceof DoublePlantBlock) {
            group.cells.add(new Cell(pos.above(), state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,
                DoubleBlockHalf.UPPER), new CompoundTag(), List.of()));
        } else if (state.getBlock() instanceof BedBlock) {
            group.cells.add(new Cell(pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING)),
                state.setValue(BlockStateProperties.BED_PART, BedPart.HEAD),
                new CompoundTag(), List.of()));
        }
    }

    private static <P extends Enum<P>> void addMultipart(Group group, BlockPos pos, BlockState state, AbstractMultiPartBlock<P> block) {
        for (P part : block.getParts()) {
            if (part == state.getValue(block.getPart())) continue;
            group.cells.add(new Cell(pos.offset(block.offsetFrom(state, part)), block.placedState(part, state),
                new CompoundTag(), List.of()));
        }
    }

}
