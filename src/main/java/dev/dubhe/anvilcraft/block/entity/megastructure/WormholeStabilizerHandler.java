package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.api.world.load.LoadChuckData;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CfaInterfaceScanner;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.saved.WormholeInterfaceStates;
import dev.dubhe.anvilcraft.saved.WormholeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

public class WormholeStabilizerHandler extends BaseMegastructureHandler {

    @Nullable
    private UUID bodyUuid = null;
    private boolean registered = false;
    private boolean legacyLogistics = false;
    private final Map<Cube323PartHalf, BlockPos> portals = new EnumMap<>(Cube323PartHalf.class);
    private final Map<WormholeChunkLoadKey, LoadChuckData> loadedChunks = new HashMap<>();
    private final Map<String, List<FluidStack>> lastFluidSnapshot = new HashMap<>();

    private record WormholeChunkLoadKey(ResourceLocation dimension, BlockPos pos) {
    }

    @Override
    public String name() {
        return "wormhole_stabilizer";
    }

    @Nullable
    public UUID getBodyUuid() {
        return bodyUuid;
    }

    public Map<Cube323PartHalf, BlockPos> getPortals() {
        return Collections.unmodifiableMap(portals);
    }

    @Override
    public int getInputPower(CelestialForgingAnvilBlockEntity be) {
        return super.getInputPower(be);
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !name().equals(option.megastructure())) return;
        if (!(be.getCelestialBodyData() instanceof StarData star) || star.bodyClass() != CelestialBodyClass.BLACK_HOLE) return;

        /// 从天体获取 UUID；若为空则回退到已存储的 UUID
        UUID uuid = star.bodyUuid();
        if (uuid == null) uuid = this.bodyUuid;
        if (uuid == null) return;

        if (!be.isAmplifierPresent()) {
            if (registered) {
                WormholeNetwork.get().unregister(be.getLevel(), be.getBlockPos());
                registered = false;
                clearLocalInterfaces(be);
                cleanupWormholeChunkLoading(be.getLevel());
                be.setChanged();
                be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
            syncWormholeLogistics(be);
            return;
        }

        if (registered && !uuid.equals(bodyUuid)) {
            WormholeNetwork.get().unregister(be.getLevel(), be.getBlockPos());
            registered = false;
            clearLocalInterfaces(be);
        }
        if (!registered) {
            this.bodyUuid = uuid;
            WormholeNetwork.get().register(uuid, be.getLevel(), be.getBlockPos());
            registered = WormholeNetwork.get().isRegistered(uuid, be.getLevel().dimension(), be.getBlockPos());
            if (!registered) return;
            if (!portals.isEmpty()) {
                WormholeNetwork.get().setPortalSides(be.getLevel().dimension(), be.getBlockPos(), portals.keySet());
            }
        }

        manageWormholeChunkLoading(be);
        syncWormholeLogistics(be);
        syncWormholeFluids(be);
        syncWormholeLasers(be);
    }

    @Override
    public void onBuild(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        if (be.getCelestialBodyData() instanceof StarData star && star.bodyClass()
                                                                  == CelestialBodyClass.BLACK_HOLE && be.isAmplifierPresent()) {
            UUID uuid = star.bodyUuid();
            if (uuid == null) return;
            if (bodyUuid != null && !uuid.equals(bodyUuid)) {
                WormholeNetwork.get().unregister(be.getLevel(), be.getBlockPos());
                registered = false;
                clearLocalInterfaces(be);
            }
            this.bodyUuid = uuid;
            WormholeNetwork.get().register(uuid, be.getLevel(), be.getBlockPos());
            registered = WormholeNetwork.get().isRegistered(uuid, be.getLevel().dimension(), be.getBlockPos());
            if (!registered) return;
            syncWormholeLogistics(be);
            if (!portals.isEmpty()) {
                WormholeNetwork.get().setPortalSides(be.getLevel().dimension(), be.getBlockPos(), portals.keySet());
            }
        }
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        syncWormholeLogistics(be);
        if (registered && be.getLevel() != null && !be.getLevel().isClientSide()) {
            WormholeNetwork.get().unregister(be.getLevel(), be.getBlockPos());
            registered = false;
        }
        clearLocalInterfaces(be);
        bodyUuid = null;
        legacyLogistics = false;
        portals.clear();
        cleanupWormholeChunkLoading(be.getLevel());
    }

    @Override
    public void onUnload(CelestialForgingAnvilBlockEntity be) {
        if (registered && be.getLevel() != null && !be.getLevel().isClientSide()) {
            WormholeNetwork.get().unregister(be.getLevel(), be.getBlockPos());
        }
        registered = false;
        lastFluidSnapshot.clear();
        stopLocalLaserOutputs(be);
        cleanupWormholeChunkLoading(be.getLevel());
    }

    public void addPortal(Cube323PartHalf side, BlockPos portalPos, CelestialForgingAnvilBlockEntity be) {
        if (side != Cube323PartHalf.BOTTOM_N && side
                                                != Cube323PartHalf.BOTTOM_S && side
                                                                               != Cube323PartHalf.BOTTOM_E && side
                                                                                                              != Cube323PartHalf.BOTTOM_W) {
            return;
        }
        if (portals.containsKey(side)) return;
        portals.put(side, portalPos);

        if (registered && be.getLevel() != null && !be.getLevel().isClientSide()) {
            WormholeNetwork.get().setPortalSides(be.getLevel().dimension(), be.getBlockPos(), portals.keySet());
        }

        be.setChanged();
        if (be.getLevel() != null) {
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
    }

    public void removePortal(Cube323PartHalf side, CelestialForgingAnvilBlockEntity be) {
        portals.remove(side);

        if (registered && be.getLevel() != null && !be.getLevel().isClientSide()) {
            WormholeNetwork.get().setPortalSides(be.getLevel().dimension(), be.getBlockPos(), portals.keySet());
        }

        be.setChanged();
        if (be.getLevel() != null) {
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
    }

    public void syncLogisticsOnChange(BlockPos interfacePos, int changedSlot, CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || !registered || bodyUuid == null) return;
        BlockPos offset = interfacePos.subtract(be.getBlockPos());
        var local = getLogisticsInterfacesMap(be).get(offset);
        if (local == null || local.getWormholeInventory() == null) return;
        for (var remote : getConnectedLogisticsInterfaces(be, offset)) {
            if (!local.getWormholeInventory().equals(remote.getWormholeInventory())) continue;
            remote.setChanged();
            remote.setEjectCooldown(CelestialForgingAnvilLogisticsInterfaceBlockEntity.EJECT_COOLDOWN);
        }
    }

    private List<CelestialForgingAnvilLogisticsInterfaceBlockEntity> getConnectedLogisticsInterfaces(
        CelestialForgingAnvilBlockEntity be, BlockPos offset
    ) {
        List<CelestialForgingAnvilLogisticsInterfaceBlockEntity> result = new ArrayList<>();
        if (!(be.getLevel() instanceof ServerLevel level) || bodyUuid == null) return result;
        for (var entry : WormholeNetwork.get().getConnected(bodyUuid, level.dimension(), be.getBlockPos())) {
            ServerLevel target = level.getServer().getLevel(entry.dimension());
            if (target == null || !target.hasChunkAt(entry.pos())) continue;
            if (!(target.getBlockEntity(entry.pos()) instanceof CelestialForgingAnvilBlockEntity cfa)) continue;
            var inventory = getLogisticsInterfacesMap(cfa).get(offset);
            if (inventory != null) result.add(inventory);
        }
        return result;
    }

    private void manageWormholeChunkLoading(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || bodyUuid == null) return;
        if (!(be.getLevel() instanceof ServerLevel serverLevel)) return;

        /// 每个建造了虫洞稳定器的锻星砧独立强加载自身所在区块及周围一圈 8 个区块
        ///（level=1 即 3×3 区域），仅加载自身所在维度的区块。
        /// 加载不受网络中其他锻星砧的影响。
        WormholeChunkLoadKey selfKey = new WormholeChunkLoadKey(
            be.getLevel().dimension().location(), be.getBlockPos()
        );

        if (!loadedChunks.containsKey(selfKey)) {
            var data = LoadChuckData.createLoadChuckData(1, be.getBlockPos(), false, serverLevel);
            LevelLoadManager.register(be.getBlockPos(), data, serverLevel);
            loadedChunks.put(selfKey, data);
        }
    }

    private void cleanupWormholeChunkLoading(net.minecraft.world.level.Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            loadedChunks.clear();
            return;
        }
        for (WormholeChunkLoadKey key : loadedChunks.keySet()) {
            /// 为此 key 的维度解析正确的 ServerLevel，
            /// 确保跨维度条目也能成功注销。
            ServerLevel targetLevel = key.dimension().equals(serverLevel.dimension().location())
                ? serverLevel
                : serverLevel.getServer().getLevel(
                    ResourceKey.create(
                        net.minecraft.core.registries.Registries.DIMENSION,
                        key.dimension()
                    )
                );
            if (targetLevel != null) {
                LevelLoadManager.unregister(key.pos(), targetLevel);
            }
        }
        loadedChunks.clear();
    }

    private void syncWormholeLogistics(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        for (var inventory : getLogisticsInterfacesMap(be).values()) {
            prepareLogisticsInterface(inventory, be);
        }
    }

    public void prepareLogisticsInterface(
        CelestialForgingAnvilLogisticsInterfaceBlockEntity inventory, CelestialForgingAnvilBlockEntity be
    ) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        if (!be.isAmplifierPresent() && !inventory.isLegacyWormholeInventory()) {
            detachLogisticsInterface(inventory, be);
            return;
        }
        if (bodyUuid == null || (!registered && !legacyLogistics)) return;
        if (be.isAmplifierPresent()
            && !WormholeNetwork.get().isRegistered(bodyUuid, be.getLevel().dimension(), be.getBlockPos())) {
            inventory.setWormholeInventory(null);
            return;
        }
        BlockPos offset = inventory.getBlockPos().subtract(be.getBlockPos());
        UUID uuid = WormholeInterfaceStates.logisticsUuid(bodyUuid, offset.getX(), offset.getZ());
        if (uuid.equals(inventory.getWormholeInventory())) return;
        if (inventory.getWormholeInventory() != null) detachLogisticsInterface(inventory, be);

        ItemStackHandler local = inventory.getLocalItemHandler();
        WormholeInterfaceStates states = WormholeInterfaceStates.get();
        boolean migrateMirror = legacyLogistics && states.hasItemState(uuid);
        ItemStackHandler shared = states.getItemHandler(uuid, local.getSlots());
        if (inventory.isLegacyWormholeInventory()) {
            // Legacy mirrors have no ownership marker; never replay already withdrawn shared items.
            if (migrateMirror) {
                inventory.backupLegacyWormholeInventory();
                for (int slot = 0; slot < local.getSlots(); slot++) {
                    ItemStack canonical = shared.getStackInSlot(slot);
                    if (canonical.isEmpty() || ItemStack.isSameItemSameComponents(local.getStackInSlot(slot), canonical)) {
                        local.setStackInSlot(slot, ItemStack.EMPTY);
                    }
                }
            }
            inventory.setWormholeInventory(null);
        }

        List<ItemStack> merged = new ArrayList<>(shared.getSlots());
        for (int slot = 0; slot < shared.getSlots(); slot++) merged.add(shared.getStackInSlot(slot).copy());
        for (int slot = 0; slot < local.getSlots(); slot++) {
            if (!mergeLogisticsStack(merged, local.getStackInSlot(slot), slot, local.getSlotLimit(slot))) return;
        }
        for (int slot = 0; slot < merged.size(); slot++) shared.setStackInSlot(slot, merged.get(slot));
        for (int slot = 0; slot < local.getSlots(); slot++) local.setStackInSlot(slot, ItemStack.EMPTY);
        inventory.setWormholeInventory(uuid);
        if (!be.isAmplifierPresent()) {
            detachLogisticsInterface(inventory, be);
            return;
        }
        syncLogisticsOnChange(inventory.getBlockPos(), 0, be);
    }

    private static boolean mergeLogisticsStack(List<ItemStack> merged, ItemStack stack, int preferredSlot, int limit) {
        if (stack.isEmpty()) return true;
        for (ItemStack existing : merged) {
            if (!ItemStack.isSameItemSameComponents(existing, stack)) continue;
            int available = Math.min(limit, stack.getMaxStackSize()) - existing.getCount();
            if (stack.getCount() > available) return false;
            existing.grow(stack.getCount());
            return true;
        }
        if (merged.get(preferredSlot).isEmpty()) {
            merged.set(preferredSlot, stack.copy());
            return true;
        }
        for (int slot = 0; slot < merged.size(); slot++) {
            if (!merged.get(slot).isEmpty()) continue;
            merged.set(slot, stack.copy());
            return true;
        }
        // A full interface remains independent until its entire inventory fits; never transfer a prefix.
        return false;
    }

    private void detachLogisticsInterface(
        CelestialForgingAnvilLogisticsInterfaceBlockEntity inventory, CelestialForgingAnvilBlockEntity be
    ) {
        UUID uuid = inventory.getWormholeInventory();
        if (uuid == null) return;
        BlockPos offset = inventory.getBlockPos().subtract(be.getBlockPos());
        boolean hasPeer = getConnectedLogisticsInterfaces(be, offset).stream()
            .anyMatch(peer -> uuid.equals(peer.getWormholeInventory()));
        ItemStackHandler local = inventory.getLocalItemHandler();
        if (!hasPeer) {
            WormholeInterfaceStates states = WormholeInterfaceStates.get();
            ItemStackHandler shared = states.getItemHandler(uuid, local.getSlots());
            for (int slot = 0; slot < local.getSlots(); slot++) {
                local.setStackInSlot(slot, shared.getStackInSlot(slot).copy());
            }
            states.clearItemState(uuid);
        }
        inventory.setWormholeInventory(null);
    }

    private void syncWormholeFluids(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || !registered || bodyUuid == null) return;
        Map<BlockPos, CelestialForgingAnvilFluidInterfaceBlockEntity> localMap = getFluidInterfacesMap(be);
        if (localMap.isEmpty()) return;

        WormholeInterfaceStates states = WormholeInterfaceStates.get();
        for (var localEntry : localMap.entrySet()) {
            BlockPos relOffset = localEntry.getKey();
            CelestialForgingAnvilFluidInterfaceBlockEntity localBe = localEntry.getValue();
            IFluidHandler localHandler = localBe.getInternalFluidHandler();
            int tanks = localHandler.getTanks();

            UUID uuid = WormholeInterfaceStates.fluidUuid(bodyUuid, relOffset.getX(), relOffset.getZ());
            String snapKey = uuid + ":" + relOffset.getX() + "," + relOffset.getZ();
            List<FluidStack> canonical = states.getOrCreateFluidState(uuid, tanks);
            List<FluidStack> lastLocal = lastFluidSnapshot.computeIfAbsent(snapKey, k -> new ArrayList<>(tanks));
            while (lastLocal.size() < tanks) lastLocal.add(FluidStack.EMPTY);

            for (int tank = 0; tank < tanks; tank++) {
                FluidStack localStack = localHandler.getFluidInTank(tank);
                FluidStack canonStack = canonical.get(tank);
                FluidStack prevStack = lastLocal.isEmpty() || tank >= lastLocal.size() ? FluidStack.EMPTY : lastLocal.get(tank);

                if (FluidStack.matches(localStack, canonStack) && localStack.getAmount() == canonStack.getAmount()) {
                    while (lastLocal.size() <= tank) lastLocal.add(FluidStack.EMPTY);
                    lastLocal.set(tank, localStack.copy());
                    continue;
                }

                boolean localChanged = !FluidStack.matches(localStack, prevStack) || localStack.getAmount() != prevStack.getAmount();
                if (localChanged) {
                    canonical.set(tank, localStack.copy());
                    states.setDirty();
                } else {
                    setTankContents(localHandler, tank, canonStack);
                }

                while (lastLocal.size() <= tank) lastLocal.add(FluidStack.EMPTY);
                lastLocal.set(tank, localStack.copy());
            }
        }
    }

    private void syncWormholeLasers(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || !registered || bodyUuid == null) return;
        Map<BlockPos, CelestialForgingAnvilLaserInterfaceBlockEntity> localMap = getLaserInterfacesMap(be);
        if (localMap.isEmpty()) return;

        WormholeNetwork network = WormholeNetwork.get();
        List<WormholeNetwork.Entry> connected = network.getConnected(bodyUuid, be.getLevel().dimension(), be.getBlockPos());

        for (var localEntry : localMap.entrySet()) {
            BlockPos relOffset = localEntry.getKey();
            CelestialForgingAnvilLaserInterfaceBlockEntity localBe = localEntry.getValue();

            LaserPool pool = new LaserPool();
            /// 收集本地的激光贡献/消耗
            pool.add(localBe);

            for (WormholeNetwork.Entry entry : connected) {
                ServerLevel targetLevel = be.getLevel().getServer().getLevel(entry.dimension());
                if (targetLevel == null) continue;
                BlockEntity targetBe = targetLevel.getBlockEntity(entry.pos());
                if (!(targetBe instanceof CelestialForgingAnvilBlockEntity targetCfa)) continue;

                Map<BlockPos, CelestialForgingAnvilLaserInterfaceBlockEntity> remoteMap = CfaInterfaceScanner.getInterfacesMap(
                    CelestialForgingAnvilLaserInterfaceBlockEntity.class, targetCfa.getLevel(), targetCfa.getBlockPos()
                );
                CelestialForgingAnvilLaserInterfaceBlockEntity remoteBe = remoteMap.get(relOffset);
                if (remoteBe == null) continue;

                /// 收集远端的激光贡献/消耗
                pool.add(remoteBe);
            }

            int totalNormal = pool.totalNormal;
            int totalGamma = pool.totalGamma;
            int activeCount = pool.activeCount;

            if (localBe.isActive()) {
                int eachNormal = activeCount > 0 ? totalNormal / activeCount : 0;
                int eachGamma = activeCount > 0 ? totalGamma / activeCount : 0;
                localBe.setWormholeLaserOutput(eachGamma > 0 ? eachGamma : eachNormal, eachGamma > 0);
            } else {
                localBe.setWormholeLaserOutput(0, false);
            }
        }
    }

    /// 虫洞激光等级池：统计所有接口的贡献/消耗。
    /// 关键规则：如果接口处于激活模式但正在接收激光，则它不会实际发射
    ///（serverTick 中接收优先于发射），因此应作为生产者（贡献等级）
    /// 而非消费者（增加 activeCount）。
    private static final class LaserPool {
        int totalNormal;
        int totalGamma;
        int activeCount;

        void add(CelestialForgingAnvilLaserInterfaceBlockEntity be) {
            if (be.isActive() && be.getReceivedLaserLevel() > 0) {
                /// 激活 + 正在接收 → 接收优先，不会发射 → 贡献到池
                if (be.isReceivedGamma()) {
                    totalGamma += be.getReceivedLaserLevel();
                } else {
                    totalNormal += be.getReceivedLaserLevel();
                }
            } else if (be.isActive()) {
                /// 激活 + 无接收 → 消费者
                activeCount++;
            } else if (be.getReceivedLaserLevel() > 0) {
                /// 被动 + 有接收 → 贡献到池
                if (be.isReceivedGamma()) {
                    totalGamma += be.getReceivedLaserLevel();
                } else {
                    totalNormal += be.getReceivedLaserLevel();
                }
            }
        }
    }

    /** Detaches item storage, clears fluid mirrors and stops local laser output after unregistering. */
    private void clearLocalInterfaces(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;

        boolean lastNode = this.bodyUuid != null && WormholeNetwork.get()
            .getConnected(this.bodyUuid, be.getLevel().dimension(), be.getBlockPos())
            .isEmpty();

        WormholeInterfaceStates states = WormholeInterfaceStates.get();

        for (var inventory : getLogisticsInterfacesMap(be).values()) {
            detachLogisticsInterface(inventory, be);
        }

        Map<BlockPos, CelestialForgingAnvilFluidInterfaceBlockEntity> fluidMap = getFluidInterfacesMap(be);
        for (var entry : fluidMap.entrySet()) {
            CelestialForgingAnvilFluidInterfaceBlockEntity localBe = entry.getValue();
            IFluidHandler handler = localBe.getFluidHandler();
            int tanks = handler.getTanks();
            if (lastNode) {
                // 最后节点：canonical 内容归还本地接口（见 returnCanonicalFluidsToLocal）
                UUID uuid = WormholeInterfaceStates.fluidUuid(
                    this.bodyUuid, entry.getKey().getX(), entry.getKey().getZ());
                if (returnCanonicalFluidsToLocal(states, uuid, handler, tanks)) {
                    states.clearFluidState(uuid);
                }
                continue;
            }
            for (int tank = 0; tank < tanks; tank++) {
                FluidStack stack = handler.getFluidInTank(tank);
                if (!stack.isEmpty()) {
                    handler.drain(stack, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }

        stopLocalLaserOutputs(be);
    }

    /**
     * 把 canonical 中的全部流体归还给本地流体接口。
     *
     * @return 是否全部归还成功
     */
    private static boolean returnCanonicalFluidsToLocal(
        WormholeInterfaceStates states, UUID uuid, IFluidHandler handler, int tanks
    ) {
        List<FluidStack> canonical = states.getOrCreateFluidState(uuid, tanks);
        boolean allReturned = true;
        for (int tank = 0; tank < tanks; tank++) {
            FluidStack stack = canonical.get(tank);
            if (stack.isEmpty()) continue;
            FluidStack remainder = returnToTanks(handler, stack, tank);
            if (!remainder.isEmpty()) {
                allReturned = false;
            }
        }
        return allReturned;
    }

    private void stopLocalLaserOutputs(CelestialForgingAnvilBlockEntity be) {
        Map<BlockPos, CelestialForgingAnvilLaserInterfaceBlockEntity> laserMap = getLaserInterfacesMap(be);
        for (var entry : laserMap.entrySet()) {
            CelestialForgingAnvilLaserInterfaceBlockEntity localBe = entry.getValue();
            localBe.setWormholeLaserOutput(0, false);
        }
    }

    /**
     * 把 canonical 中的一份流体归还给本地接口。
     *
     * @return 未能收纳的剩余流体；为空表示全部归还成功
     */
    private static FluidStack returnToTanks(IFluidHandler handler, FluidStack stack, int preferredTank) {
        FluidStack remaining = stack.copy();
        if (preferredTank >= 0 && preferredTank < handler.getTanks()) {
            FluidStack existing = handler.getFluidInTank(preferredTank);
            if (existing.isEmpty() || (FluidStack.isSameFluidSameComponents(existing, remaining)
                && existing.getAmount() + remaining.getAmount() <= handler.getTankCapacity(preferredTank))) {
                int accepted = handler.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
                remaining.shrink(accepted);
            }
        }
        if (!remaining.isEmpty()) {
            for (int tank = 0; tank < handler.getTanks() && !remaining.isEmpty(); tank++) {
                FluidStack existing = handler.getFluidInTank(tank);
                if (existing.isEmpty() || (FluidStack.isSameFluidSameComponents(existing, remaining)
                    && existing.getAmount() + remaining.getAmount() <= handler.getTankCapacity(tank))) {
                    int accepted = handler.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
                    remaining.shrink(accepted);
                }
            }
        }
        return remaining;
    }

    private static void setTankContents(IFluidHandler handler, int tank, FluidStack stack) {
        FluidStack existing = handler.getFluidInTank(tank);
        if (!existing.isEmpty()) handler.drain(existing, IFluidHandler.FluidAction.EXECUTE);
        if (!stack.isEmpty()) handler.fill(stack.copy(), IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("wormholeItemStorageVersion", !legacyLogistics);
        if (bodyUuid != null) {
            tag.putUUID("wormholeBodyUuid", bodyUuid);
        }
        if (!portals.isEmpty()) {
            CompoundTag portalTag = new CompoundTag();
            for (Map.Entry<Cube323PartHalf, BlockPos> entry : portals.entrySet()) {
                BlockPos p = entry.getValue();
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("x", p.getX());
                posTag.putInt("y", p.getY());
                posTag.putInt("z", p.getZ());
                portalTag.put(entry.getKey().getSerializedName(), posTag);
            }
            tag.put("portals", portalTag);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        this.bodyUuid = tag.contains("wormholeBodyUuid") ? tag.getUUID("wormholeBodyUuid") : null;
        this.legacyLogistics = bodyUuid != null && !tag.getBoolean("wormholeItemStorageVersion");
        this.registered = false;
        this.loadPortals(tag);
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        saveAdditional(tag, registries);
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.bodyUuid = tag.contains("wormholeBodyUuid") ? tag.getUUID("wormholeBodyUuid") : null;
        this.loadPortals(tag);
    }

    /** Reads both the current handler-owned state and the legacy CFA portal tag. */
    private void loadPortals(CompoundTag tag) {
        this.portals.clear();
        if (!tag.contains("portals")) return;
        CompoundTag portalTag = tag.getCompound("portals");
        for (String key : portalTag.getAllKeys()) {
            Cube323PartHalf side = null;
            for (Cube323PartHalf candidate : Cube323PartHalf.values()) {
                if (candidate.name().equalsIgnoreCase(key)) {
                    side = candidate;
                    break;
                }
            }
            if (side == null) continue;
            CompoundTag posTag = portalTag.getCompound(key);
            this.portals.put(side, new BlockPos(
                posTag.getInt("x"),
                posTag.getInt("y"),
                posTag.getInt("z")
            ));
        }
    }
}
