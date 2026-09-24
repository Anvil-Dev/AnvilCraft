package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import javax.annotation.Nullable;

/** 保留区域快照，撤销时按实际收回与恢复的资源结算。 */
@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class BuildingRodUndo {
    private static final Map<ServerPlayer, BuildingRodUndo> HISTORY = new WeakHashMap<>();
    private final ServerLevel level;
    private final BuildingRegionSnapshot region;
    private final List<Receipt> receipts = new ArrayList<>();
    private final Map<EntityBuildAdapter.Planned, Receipt> entityReceipts = new IdentityHashMap<>();
    private final Map<UUID, Entity> auxiliary = new LinkedHashMap<>();
    private final Map<UUID, Entity> derived = new LinkedHashMap<>();
    private final Set<BlockPos> changedPositions = new HashSet<>();
    private final boolean creative;
    private final List<BuildingMaterials.FluidPayment> pendingFluids = new ArrayList<>();
    private boolean restored;
    private boolean restoring;

    private static final class Receipt {
        private final List<ItemStack> materials = new ArrayList<>();
        private final List<ItemStack> returned = new ArrayList<>();
        private final List<BuildingMaterials.FluidPayment> fluids = new ArrayList<>();
        private final Map<UUID, Entity> entities = new LinkedHashMap<>();
        private final Map<UUID, Entity> drops = new LinkedHashMap<>();
    }

    BuildingRodUndo(ServerPlayer player, List<BuildingPlan.Group> planned) {
        this(player, planned, null);
    }

    BuildingRodUndo(ServerPlayer player, List<BuildingPlan.Group> planned, @Nullable BoundingBox bounds) {
        this.level = player.level();
        this.creative = player.isCreative();
        this.region = new BuildingRegionSnapshot(player, bounds == null ? bounds(planned) : bounds);
        for (var group : planned) {
            if (group.cells.isEmpty() && group.entities.isEmpty()) continue;
            Receipt receipt = new Receipt();
            if (!player.isCreative()) {
                group.materials.forEach(stack -> receipt.materials.add(stack.copy()));
                group.returned.forEach(stack -> receipt.returned.add(stack.copy()));
                group.entities.forEach(entity -> {
                    if (!entity.returned().isEmpty()) receipt.returned.add(entity.returned().copy());
                });
                group.fluidPayments.forEach(payment -> receipt.fluids.add(
                    new BuildingMaterials.FluidPayment(payment.storage(), payment.fluid().copy())));
            }
            group.entities.forEach(entity -> this.entityReceipts.put(entity, receipt));
            this.receipts.add(receipt);
        }
    }

    private static BoundingBox bounds(List<BuildingPlan.Group> planned) {
        List<BlockPos> positions = new ArrayList<>();
        planned.forEach(group -> {
            group.cells.forEach(cell -> positions.add(cell.pos()));
            group.entities.forEach(entity -> {
                var pos = BlueprintNbt.list(entity.entityNbt(), "Pos", Tag.TAG_DOUBLE);
                if (pos.size() == 3) {
                    positions.add(BlockPos.containing(pos.getDoubleOr(0, 0.0), pos.getDoubleOr(1, 0.0), pos.getDoubleOr(2, 0.0)));
                }
            });
        });
        return BoundingBox.encapsulatingPositions(positions).orElseThrow();
    }

    void finish(ServerPlayer player) {
        HISTORY.put(player, this);
    }

    void recordEntity(EntityBuildAdapter.Planned plan, Entity entity) {
        if (entity instanceof LeashFenceKnotEntity) {
            this.recordAuxiliary(entity);
            return;
        }
        Receipt receipt = this.entityReceipts.get(plan);
        if (receipt != null) receipt.entities.put(entity.getUUID(), entity);
    }

    void recordAuxiliary(Entity entity) {
        this.auxiliary.put(entity.getUUID(), entity);
    }

    public static void replaced(Level level, BlockPos pos, BlockState before, BlockState after) {
        if (level.isClientSide() || before.getBlock() == after.getBlock()) return;
        for (BuildingRodUndo undo : HISTORY.values()) {
            if (undo.level == level && !undo.restoring && !undo.restored && undo.region.contains(pos)) {
                undo.changedPositions.add(pos.immutable());
            }
        }
    }

    public static void spawnedAt(Level level, BlockPos pos, Entity spawned) {
        for (BuildingRodUndo undo : HISTORY.values()) {
            if (undo.level == level && !undo.restoring && !undo.restored && undo.region.contains(pos)) {
                undo.derived.put(spawned.getUUID(), spawned);
            }
        }
    }

    public static void spawnedBy(Entity source, Entity spawned) {
        for (BuildingRodUndo undo : HISTORY.values()) {
            if (undo.restoring) continue;
            if (!undo.restored && (undo.derived.containsKey(source.getUUID()) || undo.region.containsEntity(source.getUUID()))) {
                undo.derived.put(spawned.getUUID(), spawned);
            }
            for (Receipt receipt : undo.receipts) {
                if (!undo.restored && (receipt.entities.containsKey(source.getUUID()) || receipt.drops.containsKey(source.getUUID()))) {
                    receipt.drops.put(spawned.getUUID(), spawned);
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public static void blockDrops(BlockDropsEvent event) {
        for (BuildingRodUndo undo : HISTORY.values()) {
            if (undo.level != event.getLevel() || undo.restoring || undo.restored || !undo.region.contains(event.getPos())) continue;
            event.getDrops().forEach(drop -> undo.derived.put(drop.getUUID(), drop));
        }
    }

    @SubscribeEvent
    public static void fallingBlock(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk()) return;
        Entity entity = event.getEntity();
        BlockPos source = entity instanceof FallingBlockEntity falling ? falling.getStartPos()
            : entity instanceof PrimedTnt ? entity.blockPosition() : null;
        if (source == null) return;
        for (BuildingRodUndo undo : HISTORY.values()) {
            if (undo.level == event.getLevel() && !undo.restoring && !undo.restored && undo.changedPositions.contains(source)) {
                undo.derived.put(entity.getUUID(), entity);
            }
        }
    }

    private boolean canRemove(ServerPlayer player, Map<UUID, Entity> owned) {
        for (var entry : owned.entrySet()) {
            Entity entity = BuildingRegionSnapshot.find(this.level, entry.getKey());
            if (entity instanceof Player) return false;
            if (entity != null) {
                if (!entity.level().mayInteract(player, entity.blockPosition())) return false;
            } else {
                Entity.RemovalReason reason = entry.getValue().getRemovalReason();
                if (reason == null || !reason.shouldDestroy()) return false;
            }
        }
        return true;
    }

    private void remove(Map<UUID, Entity> owned) {
        for (UUID uuid : owned.keySet()) {
            Entity entity = BuildingRegionSnapshot.find(this.level, uuid);
            if (entity == null || entity instanceof Player) continue;
            if (entity instanceof Leashable leashable) leashable.setLeashData(null);
            entity.discard();
        }
    }

    private Map<UUID, Entity> owned() {
        Map<UUID, Entity> result = new LinkedHashMap<>(this.derived);
        for (Receipt receipt : this.receipts) {
            result.putAll(receipt.entities);
            result.putAll(receipt.drops);
        }
        result.keySet().removeIf(this.region::containsEntity);
        return result;
    }

    private void entityResources(Map<UUID, Entity> owned, BuildingUndoResources recovered) {
        for (UUID uuid : owned.keySet()) {
            Entity entity = BuildingRegionSnapshot.find(this.level, uuid);
            if (entity == null) continue;
            if (entity instanceof Mob && !entity.isAlive()) continue;
            CompoundTag data = new CompoundTag();
            if (!BlueprintLeashes.save(entity, data)) throw new IllegalArgumentException("Cannot inspect removed entity");
            if (entity instanceof Mob mob && this.mobResource(mob, data, recovered)) continue;
            recovered.entity(this.level, data);
        }
    }

    private boolean mobResource(Mob mob, CompoundTag data, BuildingUndoResources recovered) {
        for (Receipt receipt : this.receipts) {
            if (!receipt.entities.containsKey(mob.getUUID())) continue;
            for (ItemStack original : receipt.materials) {
                if (original.getItem() instanceof SpawnEggItem egg && SpawnEggItem.getType(original) == mob.getType()) {
                    recovered.entity(this.level, data, original);
                    return true;
                }
                SavedEntity saved = original.get(ModComponents.SAVED_ENTITY);
                if (saved == null || saved.type() != mob.getType()) continue;
                CompoundTag contents = data.copy();
                contents.remove("UUID");
                contents.remove("Passengers");
                contents.remove("leash");
                contents.remove("Leash");
                ItemStack current = original.copyWithCount(1);
                current.set(ModComponents.SAVED_ENTITY, new SavedEntity(saved.type(), contents, saved.isMonster()));
                if (mob.getCustomName() == null) current.remove(DataComponents.CUSTOM_NAME);
                else current.set(DataComponents.CUSTOM_NAME, mob.getCustomName());
                recovered.item(current);
                receipt.returned.forEach(stack -> recovered.containers.add(stack.copy()));
                if (data.contains("leash") || data.contains("Leash")) recovered.item(new ItemStack(Items.LEAD));
                return true;
            }
        }
        return false;
    }

    private boolean prepareFluidRefunds(ServerPlayer player, List<FluidStack> fluids) {
        Set<UUID> storages = new LinkedHashSet<>();
        this.receipts.forEach(receipt -> receipt.fluids.forEach(payment -> storages.add(payment.storage())));
        storages.addAll(StorageServerStub.buildingFluidSources(player));
        storages.removeIf(storage -> StoragePortManager.positions(storage).stream().anyMatch(this.region::contains));
        Set<ResourceHandler<FluidResource>> used = Collections.newSetFromMap(new IdentityHashMap<>());
        this.pendingFluids.clear();
        for (FluidStack fluid : fluids) {
            for (UUID storage : storages) {
                if (fluid.isEmpty()) break;
                var target = StoragePortManager.findRefillTarget(storage, fluid);
                if (target == null || !used.add(target)) continue;
                int amount;
                try (Transaction transaction = Transaction.openRoot()) {
                    amount = target.insert(FluidResource.of(fluid), fluid.getAmount(), transaction);
                }
                if (amount <= 0) continue;
                this.pendingFluids.add(new BuildingMaterials.FluidPayment(storage, fluid.copyWithAmount(amount)));
                fluid.shrink(amount);
            }
            if (!fluid.isEmpty()) {
                this.pendingFluids.clear();
                return false;
            }
        }
        return true;
    }

    private void refundFluids() {
        for (var payment : this.pendingFluids) {
            while (!payment.fluid().isEmpty()) {
                var target = StoragePortManager.findRefillTarget(payment.storage(), payment.fluid());
                if (target == null) break;
                int filled;
                try (Transaction transaction = Transaction.openRoot()) {
                    filled = target.insert(FluidResource.of(payment.fluid()), payment.fluid().getAmount(), transaction);
                    transaction.commit();
                }
                if (filled <= 0) break;
                payment.fluid().shrink(filled);
            }
        }
        this.pendingFluids.removeIf(payment -> payment.fluid().isEmpty());
    }

    static boolean hasPendingRefund(ServerPlayer player) {
        BuildingRodUndo undo = HISTORY.get(player);
        return undo != null && undo.restored && !undo.pendingFluids.isEmpty();
    }

    enum Result {
        NOTHING, BLOCKED, PARTIAL, UNDONE, CONFLICT, MISSING_MATERIALS, MISSING_CONTAINERS
    }

    public static void undo(ServerPlayer player) {
        if (!BuildingRodItem.isHeld(player)) return;
        String message = switch (restore(player)) {
            case NOTHING -> "nothing_to_undo";
            case BLOCKED -> "blocked";
            case PARTIAL -> "undo_partial";
            case UNDONE -> "undone";
            case CONFLICT -> "undo_conflict";
            case MISSING_MATERIALS -> "undo_missing_materials";
            case MISSING_CONTAINERS -> "undo_missing_containers";
        };
        player.sendSystemMessage(Component.translatable("message.anvilcraft.building_rod." + message), true);
    }

    static Result restore(ServerPlayer player) {
        BuildingRodUndo undo = HISTORY.get(player);
        if (undo == null || undo.level != player.level()) {
            return Result.NOTHING;
        }
        if (undo.restored) {
            undo.refundFluids();
            return undo.finishRefund(player);
        }
        Map<UUID, Entity> owned = undo.owned();
        if (!undo.region.canRestore(player) || !undo.canRemove(player, owned)) {
            return Result.BLOCKED;
        }
        BuildingUndoResources recovered = new BuildingUndoResources();
        BuildingUndoResources required = new BuildingUndoResources();
        BuildingMaterials recovery = new BuildingMaterials(player, ItemStack.EMPTY, false);
        recovery.excludeFluidSources(undo.region::contains);
        if (!undo.creative) {
            try {
                undo.region.resources(recovered, required);
                undo.entityResources(owned, recovered);
                BuildingUndoResources.cancel(recovered, required);
            } catch (RuntimeException exception) {
                AnvilCraft.LOGGER.debug("Cannot settle building rod undo resources", exception);
                return Result.CONFLICT;
            }
            if (!recovery.reserve(required.items, required.fluids)) {
                return Result.MISSING_MATERIALS;
            }
            recovery.reserveRefundContainers(recovered.fluids, recovered.items);
            if (!undo.prepareFluidRefunds(player, recovered.fluids)) {
                return Result.MISSING_CONTAINERS;
            }
            if (!recovery.consume()) {
                return Result.MISSING_MATERIALS;
            }
        }
        undo.restoring = true;
        try {
            undo.remove(owned);
            undo.region.restore();
            undo.restored = true;
            for (Entity original : undo.auxiliary.values()) {
                Entity knot = BuildingRegionSnapshot.find(undo.level, original.getUUID());
                if (knot == null || !knot.level().getEntities(knot, new AABB(knot.blockPosition()).inflate(16),
                    entity -> entity instanceof Leashable leashable && leashable.getLeashHolder() == knot).isEmpty()) {
                    continue;
                }
                knot.discard();
            }
            for (ItemStack stack : recovered.items) {
                while (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack.split(stack.getMaxStackSize()));
            }
            undo.refundFluids();
        } finally {
            undo.restoring = false;
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return undo.finishRefund(player);
    }

    private Result finishRefund(ServerPlayer player) {
        if (this.pendingFluids.isEmpty()) HISTORY.remove(player);
        return this.pendingFluids.isEmpty() ? Result.UNDONE : Result.PARTIAL;
    }
}
