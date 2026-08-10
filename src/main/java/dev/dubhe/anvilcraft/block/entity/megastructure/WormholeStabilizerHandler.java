package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.api.world.load.LoadChunkData;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.saved.WormholeInterfaceStates;
import dev.dubhe.anvilcraft.saved.WormholeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 虫洞稳定器巨构处理器。
 * 负责虫洞网络注册、传送门管理、本地锻星砧区块强加载，以及通过
 * {@link WormholeInterfaceStates} 权威状态双向同步物流、流体和激光接口。
 */
public class WormholeStabilizerHandler extends BaseMegastructureHandler {

    @Nullable
    private UUID bodyUuid = null;
    private boolean registered = false;
    private boolean justReconnected = false;
    private final Map<Cube323PartHalf, BlockPos> portals = new EnumMap<>(Cube323PartHalf.class);
    private final Map<WormholeChunkLoadKey, LoadChunkData> loadedChunks = new HashMap<>();
    private final Map<String, List<FluidStack>> lastFluidSnapshot = new HashMap<>();
    private final Map<UUID, List<UnlimitedItemStack>> lastSeenItems = new HashMap<>();

    private record WormholeChunkLoadKey(Identifier dimension, BlockPos pos) {
    }

    @Override
    public String name() {
        return "wormhole_stabilizer";
    }

    @Nullable
    public UUID getBodyUuid() {
        return this.bodyUuid;
    }

    public Map<Cube323PartHalf, BlockPos> getPortals() {
        return Collections.unmodifiableMap(this.portals);
    }

    @Override
    public int getInputPower(CelestialForgingAnvilBlockEntity be) {
        return super.getInputPower(be);
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !this.name().equals(option.megastructure())) return;
        if (!(be.getCelestialBodyData() instanceof StarData star) || star.bodyClass() != CelestialBodyClass.BLACK_HOLE) return;

        // 优先使用当前天体 UUID，缺失时回退到已保存的 UUID。
        UUID uuid = star.bodyUuid();
        if (uuid == null) uuid = this.bodyUuid;
        if (uuid == null) return;

        if (!be.isAmplifierPresent()) {
            if (this.registered) {
                WormholeNetwork.get().unregister(be.getLevel(), be.getBlockPos());
                this.registered = false;
                this.clearLocalInterfaces(be);
                this.cleanupWormholeChunkLoading(be.getLevel());
                be.setChanged();
                be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
            }
            return;
        }

        if (!this.registered) {
            this.bodyUuid = uuid;
            WormholeNetwork.get().register(uuid, be.getLevel(), be.getBlockPos());
            this.registered = true;
            this.justReconnected = true;
            if (!this.portals.isEmpty()) {
                WormholeNetwork.get().setPortalSides(be.getLevel().dimension(), be.getBlockPos(), this.portals.keySet());
            }
        }

        this.manageWormholeChunkLoading(be);
        this.syncWormholeLogistics(be);
        this.syncWormholeFluids(be);
        this.syncWormholeLasers(be);
    }

    @Override
    public void onBuild(CelestialForgingAnvilBlockEntity be) {
        if (be.getCelestialBodyData() instanceof StarData star && star.bodyClass()
                                                                  == CelestialBodyClass.BLACK_HOLE && be.isAmplifierPresent()) {
            UUID uuid = star.bodyUuid();
            if (uuid == null) return;
            this.bodyUuid = uuid;
            WormholeNetwork.get().register(uuid, be.getLevel(), be.getBlockPos());
            this.registered = true;
            if (!this.portals.isEmpty()) {
                WormholeNetwork.get().setPortalSides(be.getLevel().dimension(), be.getBlockPos(), this.portals.keySet());
            }
        }
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        if (this.registered && be.getLevel() != null && !be.getLevel().isClientSide()) {
            WormholeNetwork.get().unregister(be.getLevel(), be.getBlockPos());
            this.registered = false;
        }
        this.bodyUuid = null;
        this.portals.clear();
        this.cleanupWormholeChunkLoading(be.getLevel());
    }

    public boolean addPortal(Cube323PartHalf side, BlockPos portalPos, CelestialForgingAnvilBlockEntity be) {
        if (side != Cube323PartHalf.BOTTOM_N && side
                                                != Cube323PartHalf.BOTTOM_S && side
                                                                               != Cube323PartHalf.BOTTOM_E && side
                                                                                                              != Cube323PartHalf.BOTTOM_W) {
            return false;
        }
        if (this.portals.containsKey(side)) return false;
        this.portals.put(side, portalPos);

        if (this.registered && be.getLevel() != null && !be.getLevel().isClientSide()) {
            WormholeNetwork.get().setPortalSides(be.getLevel().dimension(), be.getBlockPos(), this.portals.keySet());
        }

        be.setChanged();
        if (be.getLevel() != null) {
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
        return true;
    }

    public void removePortal(Cube323PartHalf side, CelestialForgingAnvilBlockEntity be) {
        this.portals.remove(side);

        if (this.registered && be.getLevel() != null && !be.getLevel().isClientSide()) {
            WormholeNetwork.get().setPortalSides(be.getLevel().dimension(), be.getBlockPos(), this.portals.keySet());
        }

        be.setChanged();
        if (be.getLevel() != null) {
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
    }

    /**
     * 玩家操作物流接口时立即更新权威状态，并在同一刻推送到所有连接的锻星砧。
     */
    public void syncLogisticsOnChange(BlockPos interfacePos, int changedSlot, CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || !this.registered || this.bodyUuid == null) return;

        Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> localMap = getLogisticsInterfacesMap(be);
        CelestialForgingAnvilLogisticsInterfaceBlockEntity localBe = localMap.values()
            .stream()
            .filter(le -> le.getBlockPos().equals(interfacePos))
            .findFirst()
            .orElse(null);
        if (localBe == null) return;

        BlockPos relOffset = new BlockPos(interfacePos.getX() - be.getBlockPos().getX(), 0, interfacePos.getZ() - be.getBlockPos().getZ());
        ResourceHandler<ItemResource> localHandler = localBe.getItemHandler();
        int slots = localHandler.size();
        UUID uuid = WormholeInterfaceStates.logisticsUuid(this.bodyUuid, relOffset.getX(), relOffset.getZ());
        WormholeInterfaceStates states = WormholeInterfaceStates.get();
        List<UnlimitedItemStack> canonical = states.getOrCreateItemState(uuid, slots);

        ItemStack localStack = getStackFromHandler(localHandler, changedSlot);
        ItemStack canonStack = canonical.get(changedSlot).toStack();
        if (!ItemStack.matches(localStack, canonStack) || localStack.getCount() != canonStack.getCount()) {
            canonical.set(changedSlot, new UnlimitedItemStack(localStack));
            states.setDirty();
        }

        WormholeNetwork network = WormholeNetwork.get();
        List<WormholeNetwork.Entry> connected = network.getConnected(this.bodyUuid, be.getLevel().dimension(), be.getBlockPos());
        for (WormholeNetwork.Entry entry : connected) {
            ServerLevel targetLevel = be.getLevel().getServer().getLevel(entry.dimension());
            if (targetLevel == null) continue;
            BlockEntity targetBe = targetLevel.getBlockEntity(entry.pos());
            if (!(targetBe instanceof CelestialForgingAnvilBlockEntity targetCfa)) continue;
            Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> remoteMap =
                this.getLogisticsInterfacesMap(targetCfa);
            CelestialForgingAnvilLogisticsInterfaceBlockEntity remoteBe = remoteMap.get(relOffset);
            if (remoteBe == null || remoteBe == localBe) continue;
            ResourceHandler<ItemResource> remoteHandler = remoteBe.getItemHandler();
            remoteBe.setSyncing(true);
            try {
                setHandlerSlot(remoteHandler, changedSlot, canonStack.copy());
                remoteBe.setEjectCooldown(CelestialForgingAnvilLogisticsInterfaceBlockEntity.EJECT_COOLDOWN);
            } finally {
                remoteBe.setSyncing(false);
            }
        }
    }

    // ==================== 区块强加载 ====================

    private void manageWormholeChunkLoading(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || this.bodyUuid == null) return;
        if (!(be.getLevel() instanceof ServerLevel serverLevel)) return;

        WormholeChunkLoadKey selfKey = new WormholeChunkLoadKey(
            serverLevel.dimension().identifier(), be.getBlockPos()
        );
        if (!this.loadedChunks.containsKey(selfKey)) {
            LoadChunkData data = LoadChunkData.createSimpleLoadChunkData(1, be.getBlockPos(), serverLevel);
            LevelLoadManager.register(be.getBlockPos(), data, serverLevel);
            this.loadedChunks.put(selfKey, data);
        }
    }

    private void cleanupWormholeChunkLoading(net.minecraft.world.level.Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            this.loadedChunks.clear();
            return;
        }
        for (WormholeChunkLoadKey key : this.loadedChunks.keySet()) {
            ServerLevel targetLevel = key.dimension().equals(serverLevel.dimension().identifier())
                ? serverLevel
                : serverLevel.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, key.dimension()));
            if (targetLevel != null) {
                LevelLoadManager.unregister(key.pos(), targetLevel);
            }
        }
        this.loadedChunks.clear();
    }

    // ==================== 物流同步 ====================

    /**
     * 双向同步物流接口。每个槽位保存上次快照以判断变更来源：
     * 重连时优先采纳断连期间的本地变更；正常运行时单侧变更覆盖另一侧；
     * 两侧同时变更时以权威状态为准。
     */
    private void syncWormholeLogistics(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || !this.registered || this.bodyUuid == null) return;
        Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> localMap = getLogisticsInterfacesMap(be);
        if (localMap.isEmpty()) return;

        boolean isReconnect = this.justReconnected;
        this.justReconnected = false;

        WormholeInterfaceStates states = WormholeInterfaceStates.get();
        for (var localEntry : localMap.entrySet()) {
            BlockPos relOffset = localEntry.getKey();
            CelestialForgingAnvilLogisticsInterfaceBlockEntity localBe = localEntry.getValue();
            ResourceHandler<ItemResource> localHandler = localBe.getItemHandler();
            int slots = localHandler.size();

            UUID uuid = WormholeInterfaceStates.logisticsUuid(this.bodyUuid, relOffset.getX(), relOffset.getZ());
            List<UnlimitedItemStack> canonical = states.getOrCreateItemState(uuid, slots);
            List<UnlimitedItemStack> lastSeen = this.lastSeenItems.computeIfAbsent(uuid, k -> {
                List<UnlimitedItemStack> init = new ArrayList<>(slots);
                for (int i = 0; i < slots; i++) init.add(canonical.get(i).copy());
                return init;
            });
            while (lastSeen.size() < slots) lastSeen.add(UnlimitedItemStack.EMPTY);

            for (int slot = 0; slot < slots; slot++) {
                ItemStack localStack = getStackFromHandler(localHandler, slot);
                ItemStack canonStack = canonical.get(slot).toStack();

                boolean localVsCanonMismatch = !ItemStack.matches(localStack, canonStack)
                    || localStack.getCount() != canonStack.getCount();

                if (!localVsCanonMismatch) {
                    // 已同步，仅刷新上次快照。
                    lastSeen.set(slot, new UnlimitedItemStack(localStack));
                    continue;
                }

                // 两侧均为不同的非空物品时不自动解决，等待玩家明确操作某一接口。
                if (!localStack.isEmpty() && !canonStack.isEmpty()) {
                    lastSeen.set(slot, new UnlimitedItemStack(localStack));
                    continue;
                }

                if (isReconnect) {
                    // 重连时，只有本地有物品则采纳到权威状态；只有权威侧有物品则推送到本地。
                    if (!localStack.isEmpty()) {
                        canonical.set(slot, new UnlimitedItemStack(localStack));
                        states.setDirty();
                    } else {
                        localBe.setSyncing(true);
                        try {
                            setHandlerSlot(localHandler, slot, canonStack.copy());
                        } finally {
                            localBe.setSyncing(false);
                        }
                    }
                    lastSeen.set(slot, new UnlimitedItemStack(getStackFromHandler(localHandler, slot)));
                    continue;
                }

                // 正常运行时与上次快照比较变更来源。
                ItemStack lastStack = lastSeen.get(slot).toStack();
                boolean localChanged = !ItemStack.matches(localStack, lastStack)
                                    || localStack.getCount() != lastStack.getCount();
                boolean canonChanged = !ItemStack.matches(canonStack, lastStack)
                                     || canonStack.getCount() != lastStack.getCount();

                if (!localChanged && !canonChanged) continue;

                if (localChanged && !canonChanged) {
                    // 仅本地变化：推送到权威状态。
                    canonical.set(slot, new UnlimitedItemStack(localStack));
                    states.setDirty();
                } else {
                    // 权威状态变化或两侧同时变化：以权威状态为准。
                    if (!ItemStack.matches(localStack, canonStack) || localStack.getCount() != canonStack.getCount()) {
                        localBe.setSyncing(true);
                        try {
                            setHandlerSlot(localHandler, slot, canonStack.copy());
                        } finally {
                            localBe.setSyncing(false);
                        }
                    }
                }

                lastSeen.set(slot, new UnlimitedItemStack(getStackFromHandler(localHandler, slot)));
            }
        }
    }

    // ==================== 流体同步 ====================

    private void syncWormholeFluids(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || !this.registered || this.bodyUuid == null) return;
        Map<BlockPos, CelestialForgingAnvilFluidInterfaceBlockEntity> localMap = getFluidInterfacesMap(be);
        if (localMap.isEmpty()) return;

        WormholeInterfaceStates states = WormholeInterfaceStates.get();
        for (var localEntry : localMap.entrySet()) {
            BlockPos relOffset = localEntry.getKey();
            CelestialForgingAnvilFluidInterfaceBlockEntity localBe = localEntry.getValue();
            ResourceHandler<FluidResource> localHandler = localBe.getInternalFluidHandler();
            int tanks = localHandler.size();

            UUID uuid = WormholeInterfaceStates.fluidUuid(this.bodyUuid, relOffset.getX(), relOffset.getZ());
            String snapKey = uuid + ":" + relOffset.getX() + "," + relOffset.getZ();
            List<FluidStack> canonical = states.getOrCreateFluidState(uuid, tanks);
            List<FluidStack> lastLocal = this.lastFluidSnapshot.computeIfAbsent(snapKey, k -> new ArrayList<>(tanks));
            while (lastLocal.size() < tanks) lastLocal.add(FluidStack.EMPTY);

            for (int tank = 0; tank < tanks; tank++) {
                FluidStack localStack = getFluidFromTank(localHandler, tank);
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

    // ==================== 激光同步 ====================

    private void syncWormholeLasers(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || !this.registered || this.bodyUuid == null) return;
        Map<BlockPos, CelestialForgingAnvilLaserInterfaceBlockEntity> localMap = getLaserInterfacesMap(be);
        if (localMap.isEmpty()) return;

        WormholeNetwork network = WormholeNetwork.get();
        List<WormholeNetwork.Entry> connected = network.getConnected(this.bodyUuid, be.getLevel().dimension(), be.getBlockPos());

        for (var localEntry : localMap.entrySet()) {
            BlockPos relOffset = localEntry.getKey();
            CelestialForgingAnvilLaserInterfaceBlockEntity localBe = localEntry.getValue();

            int totalNormal = 0;
            int totalGamma = 0;
            int activeCount = localBe.isActive() ? 1 : 0;
            if (!localBe.isActive() && localBe.getReceivedLaserLevel() > 0) {
                if (localBe.isReceivedGamma()) {
                    totalGamma += localBe.getReceivedLaserLevel();
                } else {
                    totalNormal += localBe.getReceivedLaserLevel();
                }
            }

            for (WormholeNetwork.Entry entry : connected) {
                ServerLevel targetLevel = be.getLevel().getServer().getLevel(entry.dimension());
                if (targetLevel == null) continue;
                BlockEntity targetBe = targetLevel.getBlockEntity(entry.pos());
                if (!(targetBe instanceof CelestialForgingAnvilBlockEntity targetCfa)) continue;

                Map<BlockPos, CelestialForgingAnvilLaserInterfaceBlockEntity> remoteMap =
                    this.getLaserInterfacesMap(targetCfa);
                CelestialForgingAnvilLaserInterfaceBlockEntity remoteBe = remoteMap.get(relOffset);
                if (remoteBe == null) continue;

                if (remoteBe.isActive()) {
                    activeCount++;
                } else if (remoteBe.getReceivedLaserLevel() > 0) {
                    if (remoteBe.isReceivedGamma()) {
                        totalGamma += remoteBe.getReceivedLaserLevel();
                    } else {
                        totalNormal += remoteBe.getReceivedLaserLevel();
                    }
                }
            }

            if (localBe.isActive()) {
                int eachNormal = activeCount > 0 ? totalNormal / activeCount : 0;
                int eachGamma = activeCount > 0 ? totalGamma / activeCount : 0;
                localBe.setWormholeLaserOutput(eachGamma > 0 ? eachGamma : eachNormal, eachGamma > 0);
            } else {
                localBe.setWormholeLaserOutput(0, false);
            }
        }
    }

    // ==================== 清理本地接口 ====================

    /**
     * 增幅器移除时清空本地物流和流体接口。原有内容仍冻结在权威状态中；
     * 断连期间新放入的物品由 {@link #syncWormholeLogistics} 的重连冲突逻辑处理。
     */
    private void clearLocalInterfaces(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;

        Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> logisticsMap = getLogisticsInterfacesMap(be);
        for (var entry : logisticsMap.entrySet()) {
            CelestialForgingAnvilLogisticsInterfaceBlockEntity localBe = entry.getValue();
            ResourceHandler<ItemResource> handler = localBe.getItemHandler();
            int slots = handler.size();
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stack = getStackFromHandler(handler, slot);
                if (!stack.isEmpty()) {
                    extractFromHandler(handler, slot, stack.getCount());
                }
            }
        }

        Map<BlockPos, CelestialForgingAnvilFluidInterfaceBlockEntity> fluidMap = getFluidInterfacesMap(be);
        for (var entry : fluidMap.entrySet()) {
            CelestialForgingAnvilFluidInterfaceBlockEntity localBe = entry.getValue();
            ResourceHandler<FluidResource> handler = localBe.getFluidHandler();
            int tanks = handler.size();
            for (int tank = 0; tank < tanks; tank++) {
                FluidStack stack = getFluidFromTank(handler, tank);
                if (!stack.isEmpty()) {
                    try (Transaction tx = Transaction.openRoot()) {
                        handler.extract(tank, FluidResource.of(stack), stack.getAmount(), tx);
                        tx.commit();
                    }
                }
            }
        }
    }

    // ==================== 接口槽位辅助方法 ====================

    private static void setHandlerSlot(ResourceHandler<ItemResource> handler, int slot, ItemStack stack) {
        ItemStack existing = getStackFromHandlerStatic(handler, slot);
        if (!existing.isEmpty()) {
            try (Transaction tx = Transaction.openRoot()) {
                handler.extract(slot, ItemResource.of(existing), existing.getCount(), tx);
                tx.commit();
            }
        }
        if (!stack.isEmpty()) {
            try (Transaction tx = Transaction.openRoot()) {
                handler.insert(slot, ItemResource.of(stack), stack.getCount(), tx);
                tx.commit();
            }
        }
    }

    private static void setTankContents(ResourceHandler<FluidResource> handler, int tank, FluidStack stack) {
        FluidStack existing = getFluidFromTankStatic(handler, tank);
        if (!existing.isEmpty()) {
            try (Transaction tx = Transaction.openRoot()) {
                handler.extract(tank, FluidResource.of(existing), existing.getAmount(), tx);
                tx.commit();
            }
        }
        if (!stack.isEmpty()) {
            try (Transaction tx = Transaction.openRoot()) {
                handler.insert(tank, FluidResource.of(stack), stack.getAmount(), tx);
                tx.commit();
            }
        }
    }

    // 静态辅助方法使用的资源读取变体。
    private static ItemStack getStackFromHandlerStatic(ResourceHandler<ItemResource> handler, int slot) {
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        return resource.toStack(handler.getAmountAsInt(slot));
    }

    private static FluidStack getFluidFromTankStatic(ResourceHandler<FluidResource> tank, int slot) {
        FluidResource resource = tank.getResource(slot);
        if (resource.isEmpty()) return FluidStack.EMPTY;
        return resource.toStack((int) tank.getAmountAsLong(slot));
    }

    // ==================== 持久化 ====================

    @Override
    public void saveAdditional(ValueOutput output) {
        if (this.bodyUuid != null) {
            output.store("wormholeBodyUuid", UUIDUtil.CODEC, this.bodyUuid);
        }
        if (!this.portals.isEmpty()) {
            ValueOutput.ValueOutputList portalList = output.childrenList("portals");
            for (Map.Entry<Cube323PartHalf, BlockPos> entry : this.portals.entrySet()) {
                ValueOutput child = portalList.addChild();
                child.putString("side", entry.getKey().getSerializedName());
                child.putInt("px", entry.getValue().getX());
                child.putInt("py", entry.getValue().getY());
                child.putInt("pz", entry.getValue().getZ());
            }
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        this.bodyUuid = input.read("wormholeBodyUuid", UUIDUtil.CODEC).orElse(null);
        this.registered = false;
        this.portals.clear();
        input.childrenList("portals").ifPresent(list -> {
            for (ValueInput child : list) {
                String sideName = child.getStringOr("side", "");
                if (sideName.isEmpty()) continue;
                Cube323PartHalf side = Cube323PartHalf.valueOf(sideName.toUpperCase());
                BlockPos pos = new BlockPos(
                    child.getIntOr("px", 0),
                    child.getIntOr("py", 0),
                    child.getIntOr("pz", 0));
                this.portals.put(side, pos);
            }
        });
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (this.bodyUuid != null) {
            tag.store("wormholeBodyUuid", UUIDUtil.CODEC, this.bodyUuid);
        }
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.bodyUuid = tag.read("wormholeBodyUuid", UUIDUtil.CODEC).orElse(null);
    }
}
