package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import dev.dubhe.anvilcraft.mixin.accessor.BlueprintBlockEventsAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockEventData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.CommonHooks;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

final class BuildingRegionSnapshot {
    private final ServerLevel level;
    private final BoundingBox bounds;
    private final long capturedAt;
    private final List<SavedBlock> blocks = new ArrayList<>();
    private final List<SavedEntity> entities = new ArrayList<>();
    private final List<BlueprintTicks.Entry> ticks;
    private final List<BlockEventData> events;

    private record SavedBlock(BlockPos pos, BlockState state, @Nullable CompoundTag data) implements BuildingCommit.PlacedCell {
    }

    private record SavedEntity(UUID uuid, CompoundTag data, Entity original) {
    }

    BuildingRegionSnapshot(ServerPlayer player, BoundingBox bounds) {
        this.level = player.level();
        this.bounds = BoundingBox.fromCorners(new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()),
            new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ()));
        this.capturedAt = this.level.getGameTime();
        for (BlockPos cursor : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(),
            bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            BlockPos pos = cursor.immutable();
            if (!BuildingCommit.canModify(player, pos)) throw new IllegalArgumentException("Undo area is unavailable");
            BlockEntity entity = this.level.getBlockEntity(pos);
            this.blocks.add(new SavedBlock(pos, this.level.getBlockState(pos),
                entity == null ? null : entity.saveWithFullMetadata(this.level.registryAccess())));
        }
        for (Entity entity : this.level.getEntities((Entity) null, AABB.of(bounds), entity -> !(entity instanceof Player))) {
            CompoundTag tag = new CompoundTag();
            if (!BlueprintLeashes.save(entity, tag)) continue;
            tag.remove("Passengers");
            if (entity.getVehicle() != null) tag.store(BlueprintEntities.VEHICLE, UUIDUtil.CODEC, entity.getVehicle().getUUID());
            this.entities.add(new SavedEntity(entity.getUUID(), tag, entity));
        }
        this.ticks = BlueprintTicks.capture(this.level, bounds);
        this.events = ((BlueprintBlockEventsAccessor) this.level).anvilcraft$getBlockEvents().stream()
            .filter(event -> bounds.isInside(event.pos())).toList();
    }

    boolean contains(BlockPos pos) {
        return this.bounds.isInside(pos);
    }

    boolean containsEntity(UUID uuid) {
        return this.entities.stream().anyMatch(entity -> entity.uuid().equals(uuid));
    }

    boolean canRestore(ServerPlayer player) {
        for (SavedBlock block : this.blocks) {
            if (!BuildingCommit.canModify(player, block.pos())) return false;
            BlockState current = this.level.getBlockState(block.pos());
            if (current != block.state() && !current.isAir() && CommonHooks.fireBlockBreak(this.level,
                player.gameMode.getGameModeForPlayer(), player, block.pos(), current).isCanceled()) {
                return false;
            }
        }
        for (SavedEntity saved : this.entities) {
            Entity entity = find(this.level, saved.uuid());
            if (entity != null) {
                if (!entity.level().mayInteract(player, entity.blockPosition())) return false;
            } else {
                Entity.RemovalReason reason = saved.original().getRemovalReason();
                if (reason == null || !reason.shouldDestroy()) return false;
            }
        }
        return true;
    }

    List<ItemStack> restoredMaterials() {
        List<ItemStack> result = new ArrayList<>();
        for (SavedBlock block : this.blocks) {
            if (block.data() == null) continue;
            List<ItemStack> before = BlockEntityContentAdapter.extract(block.state(), block.data(), this.level.registryAccess())
                .contents().stream().map(content -> content.stack().copy()).collect(Collectors.toCollection(ArrayList::new));
            BlockEntity current = this.level.getBlockEntity(block.pos());
            if (current != null) {
                var contents = BlockEntityContentAdapter.extract(current, this.level.registryAccess(), this.level);
                for (var content : contents.contents()) subtract(before, content.stack());
            }
            before.stream().filter(stack -> !stack.isEmpty()).forEach(result::add);
        }
        return result;
    }

    static int subtract(List<ItemStack> stacks, ItemStack required) {
        int remaining = required.getCount();
        for (ItemStack stack : stacks) {
            if (!ItemStack.isSameItemSameComponents(stack, required)) continue;
            int take = Math.min(stack.getCount(), remaining);
            stack.shrink(take);
            remaining -= take;
            if (remaining == 0) break;
        }
        return required.getCount() - remaining;
    }

    void restore() {
        this.level.clearBlockEvents(this.bounds);
        this.level.getBlockTicks().clearArea(this.bounds);
        this.level.getFluidTicks().clearArea(this.bounds);
        BuildingCommit.quietly(this.level, () -> {
            for (SavedBlock block : this.blocks) {
                this.level.removeBlockEntity(block.pos());
                BuildingCommit.set(this.level, block.pos(), block.state());
            }
            for (SavedBlock block : this.blocks) {
                if (block.data() == null) continue;
                CompoundTag data = block.data().copy();
                BlueprintRuntimeData.rebase(data, this.capturedAt, this.level.getGameTime());
                BlockEntity entity = BlueprintBlockEntities.create(this.level, block.pos(), block.state(), data);
                if (entity != null) {
                    this.level.setBlockEntity(entity);
                    entity.setChanged();
                }
                this.level.sendBlockUpdated(block.pos(), block.state(), block.state(), Block.UPDATE_CLIENTS);
            }
        });
        List<Map.Entry<Entity, CompoundTag>> restored = new ArrayList<>();
        for (SavedEntity saved : this.entities) {
            Entity entity = find(this.level, saved.uuid());
            if (entity instanceof Player) continue;
            if (entity != null && entity.level() != this.level) {
                entity.discard();
                entity = null;
            }
            if (entity == null) {
                entity = EntityBuildAdapters.create(saved.data().copy(), this.level).orElse(null);
                if (entity == null || !this.level.addFreshEntity(entity)) continue;
            } else {
                entity.stopRiding();
                entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), saved.data().copy()));
            }
            CompoundTag link = saved.data().copy();
            link.store(BlueprintEntities.SOURCE, UUIDUtil.CODEC, saved.uuid());
            restored.add(Map.entry(entity, link));
        }
        BlueprintEntities.link(restored);
        Map<BlockPos, BlockState> wires = new LinkedHashMap<>();
        this.blocks.stream().filter(cell -> cell.state().getBlock() instanceof RedstoneWireBlock)
            .forEach(cell -> wires.put(cell.pos(), cell.state()));
        RedstoneWireNetworkManager.restoreBlueprint(this.level, wires);
        BuildingCommit.activate(this.level, this.blocks, this.ticks);
        for (BlockEventData event : this.events) {
            if (this.level.getBlockState(event.pos()).is(event.block())) {
                this.level.blockEvent(event.pos(), event.block(), event.paramA(), event.paramB());
            }
        }
    }

    @Nullable
    static Entity find(ServerLevel level, UUID uuid) {
        for (ServerLevel world : level.getServer().getAllLevels()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null) return entity;
        }
        return null;
    }
}
