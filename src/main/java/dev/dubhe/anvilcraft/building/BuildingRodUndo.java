package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.item.BuildingRodItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** 最近一次放置的区域快照与逐组材料账单。区域恢复不依赖当前方块或实体仍与蓝图相同。 */
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
    private boolean restored;
    private boolean restoring;

    private static final class Receipt {
        private final List<ItemStack> materials = new ArrayList<>();
        private final List<ItemStack> returned = new ArrayList<>();
        private final List<BuildingMaterials.FluidPayment> fluids = new ArrayList<>();
        private final Map<UUID, Entity> entities = new LinkedHashMap<>();
        private final Map<UUID, Entity> drops = new LinkedHashMap<>();
        private boolean accepted;
    }

    BuildingRodUndo(ServerPlayer player, List<BuildingPlan.Group> planned) {
        this(player, planned, null);
    }

    BuildingRodUndo(ServerPlayer player, List<BuildingPlan.Group> planned, @Nullable BoundingBox bounds) {
        this.level = player.level();
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

    void consumed() {
        List<ItemStack> restoredItems = this.region.restoredMaterials();
        for (Receipt receipt : this.receipts) {
            for (ItemStack material : receipt.materials) material.shrink(BuildingRegionSnapshot.subtract(restoredItems, material));
            receipt.materials.removeIf(ItemStack::isEmpty);
        }
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
                if (!receipt.accepted && (receipt.entities.containsKey(source.getUUID()) || receipt.drops.containsKey(source.getUUID()))) {
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

    enum Result {
        NOTHING, BLOCKED, PARTIAL, UNDONE
    }

    public static void undo(ServerPlayer player) {
        if (!BuildingRodItem.isHeld(player)) return;
        String message = switch (restore(player)) {
            case NOTHING -> "nothing_to_undo";
            case BLOCKED -> "blocked";
            case PARTIAL -> "undo_partial";
            case UNDONE -> "undone";
        };
        player.sendSystemMessage(Component.translatable("message.anvilcraft.building_rod." + message), true);
    }

    static Result restore(ServerPlayer player) {
        BuildingRodUndo undo = HISTORY.get(player);
        if (undo == null || undo.level != player.level()) {
            return Result.NOTHING;
        }
        if (!undo.restored && !undo.region.canRestore(player)) {
            return Result.BLOCKED;
        }
        BuildingMaterials recovery = new BuildingMaterials(player, ItemStack.EMPTY, false);
        List<Receipt> accepted = new ArrayList<>();
        for (Receipt receipt : undo.receipts) {
            if (receipt.accepted || !undo.canRemove(player, receipt.entities)) continue;
            if (recovery.reserve(receipt.returned)) accepted.add(receipt);
        }
        if (!recovery.consume()) return Result.BLOCKED;
        undo.restoring = true;
        try {
            for (Receipt receipt : accepted) {
                undo.remove(receipt.entities);
                undo.remove(receipt.drops);
                receipt.accepted = true;
            }
            if (!undo.restored) {
                undo.remove(undo.derived);
                undo.region.restore();
                undo.restored = true;
            }
            for (Entity original : undo.auxiliary.values()) {
                Entity knot = BuildingRegionSnapshot.find(undo.level, original.getUUID());
                if (knot == null || !knot.level().getEntities(knot, new AABB(knot.blockPosition()).inflate(16),
                    entity -> entity instanceof Leashable leashable && leashable.getLeashHolder() == knot).isEmpty()) {
                    continue;
                }
                knot.discard();
            }
            for (Receipt receipt : undo.receipts) {
                if (!receipt.accepted) continue;
                receipt.materials.forEach(stack -> player.getInventory().placeItemBackInInventory(stack.copy()));
                receipt.materials.clear();
                for (var payment : receipt.fluids) {
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
            }
            undo.receipts.removeIf(receipt -> receipt.accepted && receipt.fluids.stream().allMatch(payment -> payment.fluid().isEmpty()));
            if (undo.receipts.isEmpty()) HISTORY.remove(player);
        } finally {
            undo.restoring = false;
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return undo.receipts.isEmpty() ? Result.UNDONE : Result.PARTIAL;
    }
}
