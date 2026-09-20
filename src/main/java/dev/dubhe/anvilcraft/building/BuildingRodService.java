package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.block.LargeCakeBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import dev.dubhe.anvilcraft.block.UseItemOnBlock;
import dev.dubhe.anvilcraft.block.cfa.CelestialForgingAnvilAmplifierBlock;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

public final class BuildingRodService {
    public static final int MAX_BLOCKS = 4000;
    private static final Map<ServerPlayer, Selection> SELECTIONS = new WeakHashMap<>();
    private static final Map<ServerPlayer, Confirmation> CONFIRMATIONS = new WeakHashMap<>();

    private record Confirmation(ResourceKey<Level> dimension, ItemStack disk, ItemStack rod, List<Cell> cells,
                                List<CompoundTag> entities, List<ItemStack> materials, boolean partial, long expiresAt) {
        boolean matches(
            ServerPlayer player, List<Cell> planned, List<EntityBuildAdapter.Planned> plannedEntities,
            List<ItemStack> supplied, boolean allowPartial
        ) {
            if (this.dimension != player.level().dimension() || this.rod != BuildingRodItem.heldRod(player)
                || this.partial != allowPartial || player.level().getGameTime() > this.expiresAt
                || !ItemStack.isSameItemSameComponents(this.disk, BuildingRodItem.material(player))
                || this.cells.size() != planned.size() || this.entities.size() != plannedEntities.size()
                || this.materials.size() != supplied.size()) return false;
            for (int i = 0; i < planned.size(); i++) {
                Cell expected = this.cells.get(i);
                Cell current = planned.get(i);
                if (!expected.pos().equals(current.pos()) || !expected.state().equals(current.state())
                    || !expected.config().equals(current.config()) || expected.contents().size() != current.contents().size()) return false;
                for (int slot = 0; slot < expected.contents().size(); slot++) {
                    var first = expected.contents().get(slot);
                    var second = current.contents().get(slot);
                    if (first.slot() != second.slot() || !ItemStack.matches(first.stack(), second.stack())) return false;
                }
            }
            for (int i = 0; i < plannedEntities.size(); i++) {
                if (!this.entities.get(i).equals(plannedEntities.get(i).entityNbt())) return false;
            }
            for (int i = 0; i < supplied.size(); i++) {
                if (!ItemStack.matches(this.materials.get(i), supplied.get(i))) return false;
            }
            return true;
        }
    }

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
        final Map<BlockPos, ItemStack> blockMaterials = new LinkedHashMap<>();
        final List<ItemStack> returned = new ArrayList<>();
        boolean separateContents;
        boolean componentMismatch;
        boolean missingContents;
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
            group.blockMaterials.put(pos, held.copyWithCount(1));
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
            StructureSnapshot snapshot = BlueprintNormalizer.load(tag, player.registryAccess(), disk.direction(), disk.upsideDown());
            BlueprintPlacement placement = new BlueprintPlacement(anchor, rotation, mirror);
            BlockPos sourceOrigin = BlueprintBlockConfiguration.sourceOrigin(snapshot);
            Map<BlockPos, Group> groups = new LinkedHashMap<>();
            List<BlueprintMultiblocks.PlacedBlock> blueprint = BlueprintMultiblocks.expand(snapshot, placement, -1);
            Map<BlockPos, BlockState> declared = new LinkedHashMap<>();
            blueprint.forEach(entry -> declared.put(entry.pos(), entry.state()));
            for (BlueprintMultiblocks.PlacedBlock entry : blueprint) {
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
                BlockPos core = BlueprintMultiblocks.core(pos, state);
                Group group = groups.computeIfAbsent(core, ignored -> new Group());
                group.separateContents = true;
                BuildingBlockMaterial blockMaterial = BuildingBlockMaterial.extract(state,
                    entry.nbt().orElse(null), player.level());
                if (blockMaterial.requiresOperator() && !player.canUseGameMasterBlocks()) {
                    message(player, "blocked");
                    return;
                }
                blockMaterial = blockMaterial.withConfiguration(state, blockMaterial.config(), player.registryAccess());
                BlueprintBlockConfiguration.transform(blockMaterial.config(), placement, sourceOrigin, snapshot);
                group.materials.addAll(BlueprintBlockConfiguration.materials(blockMaterial.config()));
                group.cells.add(new Cell(pos, state, blockMaterial.config(), blockMaterial.contents()));
                if (SignDecorationAdapter.isSign(state)) {
                    var decoration = SignDecorationAdapter.extract(state, blockMaterial.config(), player.registryAccess());
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
                ItemStack upgrade = UseItemOnBlock.materialFor(state);
                if (!upgrade.isEmpty()) group.materials.add(upgrade);
                ItemStack material = blockMaterial.stack().copyWithCount(materialCount(state));
                if (material.isEmpty() && !player.isCreative()) {
                    message(player, "unsupported");
                    return;
                }
                if (!material.isEmpty()) group.blockMaterials.put(pos, material);
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
                Entity probe = type.create(player.serverLevel());
                if (probe == null) {
                    message(player, "unsupported");
                    return;
                }
                if ((type == EntityType.COMMAND_BLOCK_MINECART || probe.onlyOpCanSetNbt())
                    && !player.canUseGameMasterBlocks()) {
                    message(player, "blocked");
                    return;
                }
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
                plan = new EntityBuildAdapter.Planned(plan.material(), plan.returned(),
                    BuildingEntityTransform.sanitize(probe, plan.entityNbt()), plan.contents(), plan.fluids(), false);
                Group group = new Group();
                group.separateContents = probe instanceof Container;
                group.materials.add(plan.material());
                if (!group.separateContents) plan.contents().forEach(content -> group.materials.add(content.stack()));
                plan.fluids().forEach(fluid -> group.fluids.add(fluid.fluid()));
                group.entities.add(plan);
                allGroups.add(group);
            }
            boolean complete = commit(player, allGroups, partial, true, declared);
            if (complete) PacketDistributor.sendToPlayer(player, new BuildingRodResultPacket(true));
        } catch (ConstructionBlueprintException | IllegalArgumentException exception) {
            message(player, "invalid_structure");
        }
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
        return commit(player, groups, partial, quiet, Map.of());
    }

    private static boolean commit(
        ServerPlayer player, List<Group> groups, boolean partial, boolean quiet, Map<BlockPos, BlockState> declared
    ) {
        if (!quiet && groups.stream().mapToInt(group -> group.cells.size()).sum() > MAX_BLOCKS) {
            message(player, "too_many");
            return false;
        }
        if (BuildingRodObstructions.reject(player, groups.stream().flatMap(group -> group.cells.stream()).toList())) return false;
        BuildingMaterials exactMaterials = new BuildingMaterials(player);
        boolean allowMismatch = groups.stream().anyMatch(group -> exactMaterials.reserve(group, false) == null);
        BuildingMaterials materials = new BuildingMaterials(player);
        List<Cell> cells = new ArrayList<>();
        List<EntityBuildAdapter.Planned> entities = new ArrayList<>();
        boolean missing = false;
        List<Group> placedGroups = new ArrayList<>();
        for (Group group : groups) {
            Group allocated = materials.reserve(group, allowMismatch);
            if (allocated == null) {
                missing = true;
                if (!partial) {
                    message(player, "missing_blocks");
                    if (quiet) BuildingRodMaterialBook.give(player, new BuildingMaterials(player).missing(groups));
                    return false;
                }
                continue;
            }
            placedGroups.add(allocated);
            cells.addAll(allocated.cells);
            entities.addAll(allocated.entities);
        }
        if (!declared.isEmpty()) {
            Map<BlockPos, BlockState> available = new LinkedHashMap<>();
            declared.forEach((pos, state) -> {
                BlockState existing = player.level().getBlockState(pos);
                available.put(pos, existing.equals(state) ? state : Blocks.AIR.defaultBlockState());
            });
            cells.forEach(cell -> available.put(cell.pos(), cell.state()));
            var reached = BlueprintFluids.reachable(available);
            cells.removeIf(cell -> BlueprintFluids.isFlowing(cell.state()) && !reached.contains(cell.pos()));
            for (Group group : placedGroups) {
                group.cells.removeIf(cell -> BlueprintFluids.isFlowing(cell.state()) && !reached.contains(cell.pos()));
            }
            placedGroups.removeIf(group -> group.cells.isEmpty() && group.entities.isEmpty());
        }
        List<Component> shortages = missing && quiet ? new BuildingMaterials(player).missing(groups) : List.of();
        if (missing) message(player, "missing_blocks");
        if (cells.isEmpty() && entities.isEmpty()) {
            BuildingRodMaterialBook.give(player, shortages);
            return !missing;
        }
        Confirmation previous = CONFIRMATIONS.remove(player);
        if (placedGroups.stream().anyMatch(group -> group.componentMismatch)) {
            List<ItemStack> supplied = new ArrayList<>();
            groups.forEach(group -> group.blockMaterials.values().forEach(stack -> supplied.add(stack.copy())));
            placedGroups.forEach(group -> group.materials.forEach(stack -> supplied.add(stack.copy())));
            if (previous == null || !previous.matches(player, cells, entities, supplied, partial)) {
                CONFIRMATIONS.put(player, new Confirmation(player.level().dimension(), BuildingRodItem.material(player).copy(),
                    BuildingRodItem.heldRod(player), List.copyOf(cells), entities.stream().map(plan -> plan.entityNbt().copy()).toList(),
                    supplied, partial, player.level().getGameTime() + 60));
                message(player, placedGroups.stream().anyMatch(group -> group.missingContents)
                    ? "contents_missing" : "component_mismatch");
                return false;
            }
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
        Map<BlockPos, ItemStack> placedMaterials = new LinkedHashMap<>();
        placedGroups.forEach(group -> placedMaterials.putAll(group.blockMaterials));
        Runnable place = () -> {
            for (Cell cell : cells) BuildingCommit.set(player.level(), cell.pos(), cell.state());
            for (Cell cell : cells) {
                BlockEntity blockEntity = player.level().getBlockEntity(cell.pos());
                if (blockEntity != null) {
                    if (!quiet && !cell.config().isEmpty() && !(blockEntity instanceof CommandBlockEntity)) {
                        blockEntity.loadWithComponents(cell.config(), player.registryAccess());
                    }
                    ItemStack supplied = placedMaterials.get(cell.pos());
                    if (supplied != null) {
                        BlockItem.updateCustomBlockEntityTag(player.level(), player, cell.pos(), supplied);
                        blockEntity.applyComponentsFromItemStack(supplied);
                    }
                    if (quiet && !SignDecorationAdapter.isSign(cell.state())) {
                        BlueprintBlockConfiguration.apply(blockEntity, cell.config(), player);
                    }
                    if (quiet && SignDecorationAdapter.isSign(cell.state())) {
                        blockEntity.loadWithComponents(
                            SignDecorationAdapter.sanitize(cell.state(), cell.config(), player.registryAccess()), player.registryAccess());
                    }
                    BlockEntityContentAdapter.insert(blockEntity, cell.contents(), player.registryAccess());
                    if (quiet) BlueprintBlockConfiguration.afterContents(blockEntity, cell.config(), player);
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
                if (!player.isCreative() && !plan.returned().isEmpty()) {
                    player.getInventory().placeItemBackInInventory(plan.returned().copy());
                }
            }
        };
        if (quiet) BuildingCommit.quietly(player.level(), place);
        else place.run();
        undo.finish(player);
        BuildingRodMaterialBook.give(player, shortages);
        finishPlacement(player, cells.size());
        if (quiet || !placedMaterials.isEmpty()) playPlacementSounds(player, cells, quiet);
        return !missing;
    }

    private static void playPlacementSounds(ServerPlayer player, List<Cell> cells, boolean blueprint) {
        Level level = player.level();
        if (blueprint) {
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }
        Set<Block> sounded = new HashSet<>();
        for (Cell cell : cells) {
            BlockState state = cell.state();
            if (!sounded.add(state.getBlock())) continue;
            SoundType sound = state.getSoundType(level, cell.pos(), player);
            level.playSound(null, cell.pos(), sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0f) / 2.0f, sound.getPitch() * 0.8f);
        }
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
