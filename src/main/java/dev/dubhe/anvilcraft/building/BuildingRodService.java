package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import dev.dubhe.anvilcraft.block.UseItemOnBlock;
import dev.dubhe.anvilcraft.building.BuildingPlan.Cell;
import dev.dubhe.anvilcraft.building.BuildingPlan.Group;
import dev.dubhe.anvilcraft.entity.AnimateAscendingBlockEntity;
import dev.dubhe.anvilcraft.entity.CauldronOutletEntity;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

public final class BuildingRodService {
    public static final int MAX_BLOCKS = BlueprintPlacement.MAX_ENTRIES;
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
        return BuildingBlockPlanner.planBlocks(player, held, first, last, face, hit);
    }

    public static List<Cell> singlePlacement(UseOnContext use) {
        return BuildingBlockPlanner.singlePlacement(use);
    }

    public static void blueprint(ServerPlayer player, BlockPos anchor, Rotation rotation, Mirror mirror, boolean partial) {
        blueprints(player, anchor, anchor, rotation, mirror, partial);
    }

    public static void blueprints(
        ServerPlayer player, BlockPos anchor, BlockPos last, Rotation rotation, Mirror mirror, boolean partial
    ) {
        if (!BuildingRodItem.isHeld(player) || !BuildingRodItem.material(player).is(ModItems.STRUCTURE_DISK)) return;
        var disk = BuildingRodItem.material(player).get(ModComponents.STRUCTURE_DISK_DATA);
        if (disk == null || !withinReach(player, anchor, 26) || !withinReach(player, last, 26)) return;
        CompoundTag tag = StructureLoadUtil.readStructureFileOnServer(player.level(), disk.file());
        if (tag == null) return;
        try {
            StructureSnapshot snapshot = BlueprintNormalizer.load(tag, player.registryAccess(), disk.direction(), disk.upsideDown());
            BlueprintPlacement placement = new BlueprintPlacement(anchor, rotation, mirror);
            List<BlueprintPlacement> placements = placement.tile(snapshot, last);
            if (placements.isEmpty()) {
                message(player, "too_many");
                return;
            }
            List<Group> allGroups = new ArrayList<>();
            Map<BlockPos, BlockState> declared = new LinkedHashMap<>();
            List<BlueprintTicks.Entry> ticks = new ArrayList<>();
            for (BlueprintPlacement copy : placements) {
                snapshot.ticks().forEach(tick -> ticks.add(tick.at(copy.worldOf(tick.pos()))));
                if (!planBlueprint(player, snapshot, copy, allGroups, declared)) return;
            }
            var undoBounds = placements.getFirst().bounds(snapshot.size());
            undoBounds.encapsulate(placements.getLast().bounds(snapshot.size()));
            boolean complete = commit(player, allGroups, partial, true, declared, ticks, undoBounds);
            if (complete) message(player, "placed");
        } catch (ConstructionBlueprintException | IllegalArgumentException exception) {
            message(player, "invalid_structure");
        }
    }

    private static boolean planBlueprint(
        ServerPlayer player, StructureSnapshot snapshot, BlueprintPlacement placement,
        List<Group> allGroups, Map<BlockPos, BlockState> declared
    ) {
        BlockPos sourceOrigin = BlueprintBlockConfiguration.sourceOrigin(snapshot);
        Map<BlockPos, Group> groups = new LinkedHashMap<>();
        List<BlueprintMultiblocks.PlacedBlock> blueprint = BlueprintMultiblocks.expand(snapshot, placement, -1);
        blueprint.forEach(entry -> declared.put(entry.pos(), entry.state()));
        Map<BlockPos, BlockPos> portalCores = BlueprintIgnition.portalCores(declared);
        Map<BlockPos, BlockPos> localPositions = new LinkedHashMap<>();
        snapshot.blocks().forEach(block -> localPositions.put(placement.worldOf(block.pos()), block.pos()));
        for (BlueprintMultiblocks.PlacedBlock entry : blueprint) {
            BlockState state = entry.state();
            if (OrdinaryBlockAdapter.mapping(state) == OrdinaryBlockAdapter.Mapping.AIR) continue;
            BlockPos pos = entry.pos();
            if (!canModify(player, pos)) {
                message(player, "blocked");
                return false;
            }
            if (player.level().getBlockState(pos).equals(state)) continue;
            if (!player.level().getBlockState(pos).canBeReplaced()) {
                message(player, "blocked");
                return false;
            }
            BlockPos core = portalCores.getOrDefault(pos, BlueprintMultiblocks.core(pos, state));
            Group group = groups.computeIfAbsent(core, ignored -> new Group());
            group.separateContents = true;
            if (state.is(Blocks.MOVING_PISTON)) {
                CompoundTag source = entry.nbt().orElseThrow(() -> new IllegalArgumentException("Moving piston has no runtime data"));
                if (!"minecraft:piston".equals(source.getStringOr("id", "")) || !Float.isFinite(source.getFloatOr("progress", 0))) {
                    throw new IllegalArgumentException("Invalid moving piston runtime data");
                }
                BlockState moved = NbtUtils.readBlockState(player.registryAccess().lookupOrThrow(Registries.BLOCK),
                    source.getCompoundOrEmpty("blockState"));
                if (BuildingBlockMaterial.extract(moved, null, player.level()).requiresOperator() && !player.canUseGameMasterBlocks()) {
                    message(player, "blocked");
                    return false;
                }
                CompoundTag config = source.copy();
                BlueprintBlockConfiguration.transform(config, placement, sourceOrigin, snapshot);
                group.cells.add(new Cell(pos, state, config, List.of()));
                if (!moved.is(Blocks.PISTON_HEAD)) {
                    ItemStack material = OrdinaryBlockAdapter.material(moved);
                    if (material.isEmpty() && !player.isCreative()) {
                        unsupported(player, state.getBlock().getName());
                        return false;
                    }
                    if (!material.isEmpty()) group.materials.add(material);
                }
                continue;
            }
            if (BlueprintIgnition.isIgnition(state)) {
                group.cells.add(new Cell(pos, state, new CompoundTag(), List.of()));
                group.ignitions = 1;
                continue;
            }
            BuildingBlockMaterial blockMaterial = BuildingBlockMaterial.extract(state,
                entry.nbt().orElse(null), player.level());
            if (blockMaterial.requiresOperator() && !player.canUseGameMasterBlocks()) {
                message(player, "blocked");
                return false;
            }
            blockMaterial = blockMaterial.withConfiguration(state, blockMaterial.config(), player.registryAccess());
            BlueprintBlockConfiguration.transform(blockMaterial.config(), placement, sourceOrigin, snapshot);
            BlueprintRuntimeData.restoreClock(blockMaterial.config(), snapshot,
                localPositions.getOrDefault(pos, BlockPos.ZERO), player.level().getGameTime());
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
            if (state.is(Blocks.BUBBLE_COLUMN)) {
                group.fluids.add(new FluidStack(Fluids.WATER, 1000));
                continue;
            }
            if (FluidBuildAdapter.isFilledCauldron(state)) {
                group.materials.add(new ItemStack(Items.CAULDRON));
                group.fluids.add(FluidBuildAdapter.cauldronFluidOf(state));
                continue;
            }
            if (!core.equals(pos)) continue;
            group.materials.addAll(BlueprintSpecialBlocks.extra(state));
            ItemStack upgrade = UseItemOnBlock.materialFor(state);
            if (!upgrade.isEmpty()) group.materials.add(upgrade);
            ItemStack material = blockMaterial.stack().copyWithCount(materialCount(state));
            if (material.isEmpty() && !player.isCreative()) {
                unsupported(player, state.getBlock().getName());
                return false;
            }
            if (!material.isEmpty()) group.blockMaterials.put(pos, material);
        }
        for (var entry : groups.entrySet()) {
            if (entry.getValue().cells.stream().noneMatch(cell -> cell.pos().equals(entry.getKey()))
                && player.level().getBlockState(entry.getKey()).isAir()) {
                message(player, "invalid_structure");
                return false;
            }
        }
        allGroups.addAll(groups.values());
        var identities = BlueprintEntities.identities(snapshot, placement);
        for (var entry : snapshot.entities()) {
            EntityType<?> type = EntityBuildAdapters.typeOf(entry.nbt()).orElse(null);
            if (type == null || EntityBuildAdapters.isTransient(type)) continue;
            Entity probe = type.create(player.level(), EntitySpawnReason.LOAD);
            if (probe == null) {
                unsupported(player, type.getDescription());
                return false;
            }
            if (EntityBuildAdapters.isTransient(probe)) continue;
            if (probe instanceof AnimateAscendingBlockEntity && !entry.nbt().contains("BlockState")) continue;
            if ((type == EntityType.COMMAND_BLOCK_MINECART || probe.getType().onlyOpCanSetNbt()
                && !(probe instanceof FallingBlockEntity) && !(probe instanceof SlidingBlockEntity))
                && !player.canUseGameMasterBlocks()) {
                message(player, "blocked");
                return false;
            }
            CompoundTag transformed = BuildingEntityTransform.transform(entry, placement);
            BlueprintEntities.relocateMemories(transformed, entry, snapshot, placement, player.level().dimension().identifier().toString());
            var leashFence = BlueprintLeashes.attachment(transformed);
            if (leashFence.isPresent() && (!canModify(player, leashFence.get())
                || !declared.getOrDefault(leashFence.get(), player.level().getBlockState(leashFence.get())).is(BlockTags.FENCES))) {
                message(player, "blocked");
                return false;
            }
            if (!player.canUseGameMasterBlocks() && DynamicBuildingEntities.requiresOperator(transformed, player.level())) {
                message(player, "blocked");
                return false;
            }
            var position = BlueprintNbt.list(transformed, "Pos", Tag.TAG_DOUBLE);
            Vec3 world = new Vec3(position.getDoubleOr(0, 0), position.getDoubleOr(1, 0), position.getDoubleOr(2, 0));
            if (!canModify(player, BlockPos.containing(world))) {
                message(player, "blocked");
                return false;
            }
            if (!player.level().getEntitiesOfClass(Entity.class, AABB.ofSize(world, 0.25, 0.25, 0.25),
                entity -> entity.getType() == type && entity.position().distanceToSqr(world) < 0.02).isEmpty()) {
                continue;
            }
            EntityBuildAdapter adapter = EntityBuildAdapters.find(probe, transformed).orElse(null);
            if (adapter == null) {
                unsupported(player, type.getDescription());
                return false;
            }
            var plan = probe instanceof Mob && !player.isCreative()
                ? new EntityBuildAdapter.Planned(ItemStack.EMPTY, ItemStack.EMPTY, transformed.copy(), List.of(), List.of(), false)
                : adapter.plan(player.level(), entry, transformed);
            if (plan.unsupported()) {
                unsupported(player, type.getDescription());
                return false;
            }
            plan = new EntityBuildAdapter.Planned(plan.material(), plan.returned(),
                BuildingEntityTransform.sanitize(probe, plan.entityNbt()), plan.contents(), plan.fluids(), false);
            if (transformed.get("Motion") instanceof net.minecraft.nbt.ListTag) {
                plan.entityNbt().put("Motion", BlueprintNbt.list(transformed, "Motion", Tag.TAG_DOUBLE).copy());
            }
            BlueprintEntities.scope(plan.entityNbt(), entry.nbt(), identities);
            Group group = null;
            if (probe instanceof MagnetizedNodeEntity || probe instanceof CauldronOutletEntity) {
                String key = probe instanceof MagnetizedNodeEntity ? "BlockPos" : "CauldronPos";
                BlockPos support = plan.entityNbt().read(key, BlockPos.CODEC).orElseThrow();
                BlockState state = declared.get(support);
                if (state == null) {
                    message(player, "invalid_structure");
                    return false;
                }
                group = groups.get(BlueprintMultiblocks.core(support, state));
            }
            if (group == null) {
                group = new Group();
                allGroups.add(group);
            }
            group.separateContents |= probe instanceof Container;
            ItemStack tool = adapter.requiredTool();
            if (!tool.isEmpty()) group.tools.add(tool.getItem());
            group.hammer |= adapter.requiresHammer();
            if (probe instanceof Leashable && BlueprintLeashes.hasLeash(plan.entityNbt())) group.leads = 1;
            if (probe instanceof Mob) group.creature = type;
            else group.materials.add(plan.material());
            if (!group.separateContents) {
                for (var content : plan.contents()) group.materials.add(content.stack());
            }
            for (var fluid : plan.fluids()) group.fluids.add(fluid.fluid());
            group.entities.add(plan);
        }
        return true;
    }

    static int materialCount(BlockState state) {
        return BuildingBlockMaterial.materialCount(state);
    }

    static boolean commit(ServerPlayer player, List<Group> groups, boolean partial, boolean quiet) {
        return commit(player, groups, partial, quiet, Map.of(), List.of(), null);
    }

    private static boolean commit(
        ServerPlayer player, List<Group> groups, boolean partial, boolean quiet,
        Map<BlockPos, BlockState> declared, List<BlueprintTicks.Entry> ticks,
        @Nullable BoundingBox undoBounds
    ) {
        if (!quiet && groups.stream().mapToInt(group -> group.cells.size()).sum() > MAX_BLOCKS) {
            message(player, "too_many");
            return false;
        }
        if (BuildingRodObstructions.reject(player, groups.stream().flatMap(group -> group.cells.stream()).toList())) return false;
        BuildingMaterials exactMaterials = new BuildingMaterials(player, BuildingRodItem.material(player));
        boolean allowMismatch = groups.stream().anyMatch(group -> exactMaterials.reserve(group, false) == null);
        BuildingMaterials materials = new BuildingMaterials(player, BuildingRodItem.material(player));
        List<Cell> cells = new ArrayList<>();
        List<EntityBuildAdapter.Planned> entities = new ArrayList<>();
        boolean missing = false;
        List<Group> placedGroups = new ArrayList<>();
        for (Group group : groups) {
            boolean missingFence = group.entities.stream().map(entity -> BlueprintLeashes.attachment(entity.entityNbt()))
                .flatMap(Optional::stream).anyMatch(pos -> !player.level().getBlockState(pos).is(BlockTags.FENCES)
                    && cells.stream().noneMatch(cell -> cell.pos().equals(pos) && cell.state().is(BlockTags.FENCES)));
            Group allocated = missingFence ? null : materials.reserve(group, allowMismatch);
            if (allocated == null) {
                missing = true;
                if (!partial) {
                    message(player, "missing_blocks");
                    if (quiet) {
                        var missingMaterials = new BuildingMaterials(player, BuildingRodItem.material(player)).missing(groups);
                        BuildingRodMaterialBook.give(player, missingMaterials);
                    }
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
        List<Component> shortages = missing && quiet
            ? new BuildingMaterials(player, BuildingRodItem.material(player)).missing(groups) : List.of();
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
        final BuildingRodUndo undo = new BuildingRodUndo(player, placedGroups, undoBounds);
        if (!materials.consume()) {
            message(player, "missing_blocks");
            if (quiet) {
                BuildingRodMaterialBook.give(player, new BuildingMaterials(player, BuildingRodItem.material(player)).missing(groups));
            }
            return false;
        }
        undo.consumed();
        Map<BlockPos, ItemStack> placedMaterials = new LinkedHashMap<>();
        placedGroups.forEach(group -> placedMaterials.putAll(group.blockMaterials));
        List<Map.Entry<Entity, CompoundTag>> spawned = new ArrayList<>();
        Runnable place = () -> {
            for (Cell cell : cells) BuildingCommit.set(player.level(), cell.pos(), cell.state());
            for (Cell cell : cells) {
                BlockEntity blockEntity = player.level().getBlockEntity(cell.pos());
                if (blockEntity == null && cell.state().is(Blocks.MOVING_PISTON)) {
                    blockEntity = BlueprintBlockEntities.create(player.level(), cell.pos(), cell.state(), cell.config());
                    if (blockEntity != null) player.level().setBlockEntity(blockEntity);
                }
                if (blockEntity != null) {
                    if (!quiet && !cell.config().isEmpty() && !(blockEntity instanceof CommandBlockEntity)) {
                        blockEntity.loadWithComponents(
                            TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), cell.config()));
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
                        blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(),
                            SignDecorationAdapter.sanitize(cell.state(), cell.config(), player.registryAccess())));
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
                    RedstoneWireNetworkManager.topologyChanged(player.level(), cell.pos());
                }
            }
            if (quiet) RedstoneWireNetworkManager.restoreBlueprint(player.level(), wires);
            for (var plan : entities) {
                Entity entity = EntityBuildAdapters.spawn(player.level(), new BuildingEntityOp(plan.entityNbt(), plan.returned()));
                if (entity != null) {
                    EntityBuildAdapters.insertContents(entity, plan.contents(), player.registryAccess());
                    spawned.add(Map.entry(entity, plan.entityNbt()));
                    undo.recordEntity(plan, entity);
                }
                if (!player.isCreative() && !plan.returned().isEmpty()) {
                    player.getInventory().placeItemBackInInventory(plan.returned().copy());
                }
            }
        };
        if (quiet) BuildingCommit.quietly(player.level(), place);
        else place.run();
        BlueprintEntities.link(spawned).forEach(undo::recordAuxiliary);
        undo.finish(player);
        if (quiet) BuildingCommit.activate(player.level(), cells, ticks);
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
    }

    static boolean canModify(Player player, BlockPos pos) {
        return BuildingCommit.canModify(player, pos);
    }

    private static boolean withinReach(ServerPlayer player, BlockPos pos, int allowance) {
        return player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= Math.pow(player.blockInteractionRange() + allowance + 1, 2);
    }

    private static void unsupported(ServerPlayer player, Component type) {
        player.sendSystemMessage(Component.translatable("message.anvilcraft.building_rod.unsupported_type", type), true);
    }

    public static void message(ServerPlayer player, String key) {
        player.sendSystemMessage(Component.translatable("message.anvilcraft.building_rod." + key), true);
    }
}
