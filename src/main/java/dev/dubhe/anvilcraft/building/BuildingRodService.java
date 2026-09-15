package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.block.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
import dev.dubhe.anvilcraft.block.entity.PulseGeneratorBlockEntity;
import dev.dubhe.anvilcraft.block.item.FlexibleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.item.LargeCakeBlockItem;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.SimpleMultiPartBlock;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import dev.dubhe.anvilcraft.network.BuildingRodResultPacket;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.piston.PistonHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

public final class BuildingRodService {
    public static final int MAX_BLOCKS = 4000;
    private static final Map<ServerPlayer, Selection> SELECTIONS = new WeakHashMap<>();

    private record Selection(BlockPos first, Direction face, @Nullable BlockHitResult hit,
                             ResourceKey<Level> dimension, ItemStack material, long seed) {
    }

    public static void start(ServerPlayer player, BlockPos first, Direction face, @Nullable BlockHitResult hit) {
        start(player, first, face, hit, 0);
    }

    public static void start(ServerPlayer player, BlockPos first, Direction face, @Nullable BlockHitResult hit, long seed) {
        SELECTIONS.remove(player);
        if (!BuildingRodItem.isHeld(player) || !withinReach(player, first, 0) || !canModify(player, first)) return;
        if (hit != null && (!withinReach(player, hit.getBlockPos(), 1)
            || !Double.isFinite(hit.getLocation().lengthSqr())
            || player.getEyePosition().distanceToSqr(hit.getLocation()) > Math.pow(player.blockInteractionRange() + 1, 2))) {
            return;
        }
        SELECTIONS.put(player, new Selection(first.immutable(), face, hit, player.level().dimension(),
            BuildingRodItem.material(player).copyWithCount(1), seed));
    }

    private BuildingRodService() {
    }

    public record Cell(BlockPos pos, BlockState state, CompoundTag config, List<BlockEntityContentAdapter.SlotStack> contents) {
    }

    static final class Group {
        final List<Cell> cells = new ArrayList<>();
        final List<ItemStack> materials = new ArrayList<>();
        final List<FluidStack> fluids = new ArrayList<>();
        final List<EntityBuildAdapter.Planned> entities = new ArrayList<>();
    }

    public static long volume(BlockPos first, BlockPos last) {
        return (Math.abs((long) first.getX() - last.getX()) + 1)
            * (Math.abs((long) first.getY() - last.getY()) + 1)
            * (Math.abs((long) first.getZ() - last.getZ()) + 1);
    }

    public static void box(ServerPlayer player, BlockPos first, BlockPos last, Direction face) {
        box(player, first, last, face, null);
    }

    public static void box(ServerPlayer player, BlockPos first, BlockPos last, Direction face, @Nullable BlockHitResult hit) {
        box(player, first, last, face, hit, 0);
    }

    public static void box(ServerPlayer player, BlockPos first, BlockPos last, Direction face, @Nullable BlockHitResult hit, long seed) {
        ItemStack held = BuildingRodItem.material(player);
        if (!BuildingRodItem.isHeld(player) || !BuildingRodItem.isPlacementMaterial(held)) return;
        Selection selection = SELECTIONS.remove(player);
        if (!withinReach(player, last, 0)) return;
        if (selection != null) {
            if (!selection.first().equals(first) || selection.dimension() != player.level().dimension()
                || !ItemStack.isSameItemSameComponents(selection.material(), held)) return;
            face = selection.face();
            hit = selection.hit();
            seed = selection.seed();
        } else {
            if (!withinReach(player, first, 0)) return;
            if (hit != null && (!withinReach(player, hit.getBlockPos(), 1)
                || !Double.isFinite(hit.getLocation().lengthSqr())
                || player.getEyePosition().distanceToSqr(hit.getLocation()) > Math.pow(player.blockInteractionRange() + 1, 2))) {
                return;
            }
        }
        if (volume(first, last) > MAX_BLOCKS) {
            message(player, "too_many");
            return;
        }
        if (!(held.getItem() instanceof BlockItem) && !held.is(ModItems.FILTER)) {
            BuildingRodFluids.place(player, first, last);
            return;
        }
        List<Group> groups = held.is(ModItems.FILTER) ? BuildingRodPattern.plan(player, held, first, last, face, seed)
            : planBlocks(player, held, first, last, face, hit);
        if (groups == null) {
            message(player, "blocked");
            return;
        }
        commit(player, groups, false, false);
    }

    public static List<Cell> preview(Player player, BlockPos first, BlockPos last, Direction face, @Nullable BlockHitResult hit) {
        return preview(player, first, last, face, hit, 0);
    }

    public static List<Cell> preview(Player player, BlockPos first, BlockPos last, Direction face,
                                     @Nullable BlockHitResult hit, long seed) {
        ItemStack held = BuildingRodItem.material(player);
        if (!BuildingRodItem.isPlacementMaterial(held) || volume(first, last) > MAX_BLOCKS) return List.of();
        if (!(held.getItem() instanceof BlockItem) && !held.is(ModItems.FILTER)) {
            return BuildingRodFluids.preview(player, first, last);
        }
        List<Group> groups = held.is(ModItems.FILTER) ? BuildingRodPattern.plan(player, held, first, last, face, seed)
            : planBlocks(player, held, first, last, face, hit);
        return groups == null ? List.of() : groups.stream().flatMap(group -> group.cells.stream()).toList();
    }

    @Nullable
    static List<Group> planBlocks(Player player, ItemStack held, BlockPos first, BlockPos last,
                                         Direction face, @Nullable BlockHitResult hit) {
        for (BlockPos pos : BlockPos.betweenClosed(first, last)) {
            if (!canModify(player, pos)) return null;
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
            group.materials.add(held.copyWithCount(1));
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
            if (!canModify(player, pos)) {
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
            group.materials.add(held.copyWithCount(1));
            groups.add(group);
        }
        Map<BlockPos, Cell> planned = new LinkedHashMap<>();
        List<Group> unique = new ArrayList<>();
        for (Group group : groups) {
            if (planned.containsKey(group.cells.getFirst().pos())) continue;
            for (Cell cell : group.cells) {
                if (planned.containsKey(cell.pos()) || !canModify(player, cell.pos())
                    || !level.getBlockState(cell.pos()).canBeReplaced()) {
                    return null;
                }
                planned.put(cell.pos(), cell);
            }
            unique.add(group);
            if (planned.size() > MAX_BLOCKS) {
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
        if ((long) countX * countY * countZ * template.cells.size() > MAX_BLOCKS) return null;
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
                        if (!canModify(player, pos) || !player.level().getBlockState(pos).canBeReplaced()) {
                            return null;
                        }
                        group.cells.add(new Cell(pos, cell.state(), cell.config(), cell.contents()));
                    }
                    group.materials.add(held.copyWithCount(1));
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
        if (state == null || !item.canPlace(context, state)) return List.of();
        Group group = new Group();
        addBoxCells(group, context.getClickedPos(), state);
        for (Cell cell : group.cells) {
            Player player = use.getPlayer();
            if (player == null || !canModify(player, cell.pos())) return List.of();
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

    public static void blueprint(ServerPlayer player, BlockPos anchor, Rotation rotation, Mirror mirror, boolean partial) {
        if (!BuildingRodItem.isHeld(player) || !BuildingRodItem.material(player).is(ModItems.STRUCTURE_DISK)) return;
        var disk = BuildingRodItem.material(player).get(ModComponents.STRUCTURE_DISK_DATA);
        if (disk == null || !withinReach(player, anchor, 26)) return;
        CompoundTag tag = StructureLoadUtil.readStructureFileOnServer(player.serverLevel(), disk.file());
        if (tag == null) return;
        try {
            StructureSnapshot snapshot = ScannerDiskNormalizer.normalize(StructureSnapshotCodec.parse(tag,
                player.registryAccess()).snapshot(),
                disk.direction(), disk.upsideDown());
            BlueprintPlacement placement = new BlueprintPlacement(anchor, rotation, mirror);
            Map<BlockPos, Group> groups = new LinkedHashMap<>();
            for (BlueprintMultiblocks.PlacedBlock entry : BlueprintMultiblocks.expand(snapshot, placement, -1)) {
                BlockState state = entry.state();
                if (OrdinaryBlockAdapter.mapping(state) == OrdinaryBlockAdapter.Mapping.AIR) continue;
                BlockPos pos = entry.pos();
                if (!canModify(player, pos)) {
                    message(player, "blocked");
                    return;
                }
                if (player.level().getBlockState(pos).equals(state)) continue;
                if (!player.level().getBlockState(pos).canBeReplaced()) {
                    message(player, "blocked");
                    return;
                }
                BlockPos core = core(pos, state);
                Group group = groups.computeIfAbsent(core, ignored -> new Group());
                BlockEntityContentAdapter.Extracted extracted = BlockEntityContentAdapter.extract(
                    state, entry.nbt().orElse(null), player.registryAccess());
                group.cells.add(new Cell(pos, state, extracted.config(), extracted.contents()));
                extracted.contents().forEach(content -> group.materials.add(content.stack()));
                FluidBuildAdapter.Extracted tanks = FluidBuildAdapter.extractTanks(state, entry.nbt().orElse(null),
                    player.registryAccess());
                tanks.tanks().forEach(tank -> group.fluids.add(tank.fluid()));
                if (SignDecorationAdapter.isSign(state)) {
                    var decoration = SignDecorationAdapter.extract(state, extracted.config(), player.registryAccess());
                    decoration.decorations().stream().map(SignDecorationAdapter.Decoration::material)
                        .filter(material -> !material.isEmpty()).forEach(group.materials::add);
                }
                if (state.hasProperty(BlockStateProperties.WATERLOGGED) && state.getValue(BlockStateProperties.WATERLOGGED)
                    && !player.level().getFluidState(pos).isSource()) group.fluids.add(new FluidStack(Fluids.WATER, 1000));
                if (FluidBuildAdapter.isLiquidBlock(state)) {
                    if (!state.getFluidState().isSource()) continue;
                    group.fluids.add(FluidBuildAdapter.liquidOf(state));
                    continue;
                }
                if (FluidBuildAdapter.isFilledCauldron(state)) {
                    group.materials.add(new ItemStack(Items.CAULDRON));
                    group.fluids.add(FluidBuildAdapter.cauldronFluidOf(state));
                    continue;
                }
                if (!core.equals(pos)) continue;
                ItemStack material = new ItemStack(state.getBlock().asItem(), materialCount(state));
                if (material.isEmpty() && !player.isCreative()) {
                    message(player, "unsupported");
                    return;
                }
                if (!material.isEmpty()) group.materials.add(material);
            }
            for (var entry : groups.entrySet()) {
                if (entry.getValue().cells.stream().noneMatch(cell -> cell.pos().equals(entry.getKey()))
                    && player.level().getBlockState(entry.getKey()).isAir()) {
                    message(player, "invalid_structure");
                    return;
                }
            }
            List<Group> allGroups = new ArrayList<>(groups.values());
            for (var entry : snapshot.entities()) {
                EntityType<?> type = EntityType.by(entry.nbt()).orElse(null);
                if (type == null || EntityBuildAdapters.isTransient(type)) continue;
                Vec3 world = placement.localOf(entry.pos(), entry.blockPos())
                    .add(anchor.getX(), anchor.getY(), anchor.getZ());
                if (!canModify(player, BlockPos.containing(world))) {
                    message(player, "blocked");
                    return;
                }
                if (!player.level().getEntitiesOfClass(Entity.class, AABB.ofSize(world, 0.25, 0.25, 0.25),
                    entity -> entity.getType() == type && entity.position().distanceToSqr(world) < 0.02).isEmpty()) {
                    continue;
                }
                CompoundTag transformed = BuildingEntityTransform.transform(entry, placement);
                EntityBuildAdapter adapter = EntityBuildAdapters.find(type, transformed).orElse(null);
                if (adapter == null) {
                    message(player, "unsupported");
                    return;
                }
                var plan = adapter.plan(player.serverLevel(), entry, transformed);
                if (plan.unsupported()) {
                    message(player, "unsupported");
                    return;
                }
                Group group = new Group();
                group.materials.add(plan.material());
                plan.contents().forEach(content -> group.materials.add(content.stack()));
                plan.fluids().forEach(fluid -> group.fluids.add(fluid.fluid()));
                group.entities.add(plan);
                allGroups.add(group);
            }
            boolean complete = commit(player, allGroups, partial, true);
            if (complete) PacketDistributor.sendToPlayer(player, new BuildingRodResultPacket(true));
        } catch (ConstructionBlueprintException | IllegalArgumentException exception) {
            message(player, "invalid_structure");
        }
    }

    private static BlockPos core(BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> multipart) return multipart.getMainPartPos(pos, state);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF)
            == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        if (state.hasProperty(BlockStateProperties.BED_PART)
            && state.getValue(BlockStateProperties.BED_PART) == BedPart.HEAD) {
            return pos.relative(state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite());
        }
        if (state.getBlock() instanceof PistonHeadBlock) {
            return pos.relative(state.getValue(BlockStateProperties.FACING).getOpposite());
        }
        return pos;
    }

    private static int materialCount(BlockState state) {
        if (state.hasProperty(BlockStateProperties.SLAB_TYPE) && state.getValue(BlockStateProperties.SLAB_TYPE) == SlabType.DOUBLE) {
            return 2;
        }
        if (state.hasProperty(BlockStateProperties.LAYERS)) return state.getValue(BlockStateProperties.LAYERS);
        if (state.hasProperty(BlockStateProperties.CANDLES)) return state.getValue(BlockStateProperties.CANDLES);
        if (state.hasProperty(BlockStateProperties.PICKLES)) return state.getValue(BlockStateProperties.PICKLES);
        return 1;
    }

    static boolean commit(ServerPlayer player, List<Group> groups, boolean partial, boolean quiet) {
        if (!quiet && groups.stream().mapToInt(group -> group.cells.size()).sum() > MAX_BLOCKS) {
            message(player, "too_many");
            return false;
        }
        BuildingMaterials materials = new BuildingMaterials(player);
        List<Cell> cells = new ArrayList<>();
        List<EntityBuildAdapter.Planned> entities = new ArrayList<>();
        boolean missing = false;
        List<Group> placedGroups = new ArrayList<>();
        for (Group group : groups) {
            if (!materials.reserve(group.materials, group.fluids)) {
                missing = true;
                if (!partial) {
                    message(player, "missing_blocks");
                    if (quiet) BuildingRodMaterialBook.give(player, new BuildingMaterials(player).missing(groups));
                    return false;
                }
                continue;
            }
            placedGroups.add(group);
            cells.addAll(group.cells);
            entities.addAll(group.entities);
        }
        List<Component> shortages = missing && quiet ? new BuildingMaterials(player).missing(groups) : List.of();
        if (missing) message(player, "missing_blocks");
        if (cells.isEmpty() && entities.isEmpty()) {
            BuildingRodMaterialBook.give(player, shortages);
            return !missing;
        }
        for (Cell cell : cells) {
            if (EventHooks.onBlockPlace(player, BlockSnapshot.create(player.level().dimension(), player.level(), cell.pos()),
                Direction.UP)) {
                message(player, "blocked");
                return false;
            }
        }
        ItemStack rod = BuildingRodItem.heldRod(player);
        if (!BuildingRodItem.ready(player, rod)) return false;
        if (!materials.consume()) {
            message(player, "missing_blocks");
            if (quiet) BuildingRodMaterialBook.give(player, new BuildingMaterials(player).missing(groups));
            return false;
        }
        final BuildingRodUndo undo = new BuildingRodUndo(player, placedGroups);
        for (Cell cell : cells) BuildingCommit.set(player.level(), cell.pos(), cell.state());
        for (Cell cell : cells) {
            BlockEntity blockEntity = player.level().getBlockEntity(cell.pos());
            if (blockEntity != null) {
                if (!cell.config().isEmpty() && !(blockEntity instanceof CommandBlockEntity)) {
                    if (quiet && blockEntity instanceof PulseGeneratorBlockEntity pulse) {
                        pulse.loadBlueprint(cell.config(), player.registryAccess());
                    } else {
                        blockEntity.loadWithComponents(cell.config(), player.registryAccess());
                    }
                }
                BlockEntityContentAdapter.insert(blockEntity, cell.contents(), player.registryAccess());
                blockEntity.setChanged();
                player.level().sendBlockUpdated(cell.pos(), cell.state(), cell.state(), Block.UPDATE_CLIENTS);
            }
            if (!quiet) player.level().updateNeighborsAt(cell.pos(), cell.state().getBlock());
        }
        Map<BlockPos, BlockState> wires = new LinkedHashMap<>();
        for (Cell cell : cells) {
            if (!(cell.state().getBlock() instanceof RedstoneWireBlock)) continue;
            if (quiet) {
                wires.put(cell.pos(), cell.state());
            } else {
                RedstoneWireNetworkManager.topologyChanged(player.serverLevel(), cell.pos());
            }
        }
        if (quiet) RedstoneWireNetworkManager.restoreBlueprint(player.serverLevel(), wires);
        for (var plan : entities) {
            Entity entity = EntityBuildAdapters.spawn(player.serverLevel(), new BuildingEntityOp(plan.entityNbt(), plan.returned()));
            if (entity != null) EntityBuildAdapters.insertContents(entity, plan.contents(), player.registryAccess());
            if (!player.isCreative() && !plan.returned().isEmpty()) player.getInventory().placeItemBackInInventory(plan.returned().copy());
        }
        undo.finish(player);
        BuildingRodMaterialBook.give(player, shortages);
        finishPlacement(player, cells.size());
        return !missing;
    }

    static void finishPlacement(ServerPlayer player, int blocks) {
        BuildingRodItem.consume(player, BuildingRodItem.heldRod(player), blocks);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        PacketDistributor.sendToPlayer(player, new BuildingRodResultPacket(false));
    }

    static boolean canModify(Player player, BlockPos pos) {
        return player.mayBuild() && player.level().isInWorldBounds(pos) && player.level().hasChunkAt(pos)
            && player.level().getWorldBorder().isWithinBounds(pos) && player.level().mayInteract(player, pos);
    }

    private static boolean withinReach(ServerPlayer player, BlockPos pos, int allowance) {
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= Math.pow(player.blockInteractionRange() + allowance + 1, 2);
    }

    public static void message(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable("message.anvilcraft.building_rod." + key), true);
    }
}
