package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
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
import net.neoforged.neoforge.items.IItemHandler;

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
    private boolean justReconnected = false;
    private final Map<Cube323PartHalf, BlockPos> portals = new EnumMap<>(Cube323PartHalf.class);
    private final Map<WormholeChunkLoadKey, LoadChuckData> loadedChunks = new HashMap<>();
    private final Map<String, List<FluidStack>> lastFluidSnapshot = new HashMap<>();
    private final Map<UUID, List<UnlimitedItemStack>> lastSeenItems = new HashMap<>();

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
            return;
        }

        if (!registered) {
            this.bodyUuid = uuid;
            WormholeNetwork.get().register(uuid, be.getLevel(), be.getBlockPos());
            registered = true;
            justReconnected = true;
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
        if (be.getCelestialBodyData() instanceof StarData star && star.bodyClass()
                                                                  == CelestialBodyClass.BLACK_HOLE && be.isAmplifierPresent()) {
            UUID uuid = star.bodyUuid();
            if (uuid == null) return;
            this.bodyUuid = uuid;
            WormholeNetwork.get().register(uuid, be.getLevel(), be.getBlockPos());
            registered = true;
            if (!portals.isEmpty()) {
                WormholeNetwork.get().setPortalSides(be.getLevel().dimension(), be.getBlockPos(), portals.keySet());
            }
        }
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        if (registered && be.getLevel() != null && !be.getLevel().isClientSide()) {
            WormholeNetwork.get().unregister(be.getLevel(), be.getBlockPos());
            registered = false;
        }
        // The local interfaces are only a mirror of the shared canonical state.
        // Clear this disconnected mirror so it cannot duplicate the contents
        // that remain available through the other connected wormholes.
        clearLocalInterfaces(be);
        bodyUuid = null;
        portals.clear();
        cleanupWormholeChunkLoading(be.getLevel());
    }

    @Override
    public void onUnload(CelestialForgingAnvilBlockEntity be) {
        if (registered && be.getLevel() != null && !be.getLevel().isClientSide()) {
            WormholeNetwork.get().unregister(be.getLevel(), be.getBlockPos());
        }
        registered = false;
        justReconnected = false;
        lastSeenItems.clear();
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

        Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> localMap = getLogisticsInterfacesMap(be);
        CelestialForgingAnvilLogisticsInterfaceBlockEntity localBe = localMap.values()
            .stream()
            .filter(le -> le.getBlockPos().equals(interfacePos))
            .findFirst()
            .orElse(null);
        if (localBe == null) return;

        BlockPos relOffset = new BlockPos(interfacePos.getX() - be.getBlockPos().getX(), 0, interfacePos.getZ() - be.getBlockPos().getZ());
        IItemHandler localHandler = localBe.getItemHandler();
        int slots = localHandler.getSlots();
        UUID uuid = WormholeInterfaceStates.logisticsUuid(bodyUuid, relOffset.getX(), relOffset.getZ());
        WormholeInterfaceStates states = WormholeInterfaceStates.get();
        List<UnlimitedItemStack> canonical = states.getOrCreateItemState(uuid, slots);

        ItemStack localStack = localHandler.getStackInSlot(changedSlot);
        ItemStack canonStack = canonical.get(changedSlot).toStack();
        if (!ItemStack.matches(localStack, canonStack) || localStack.getCount() != canonStack.getCount()) {
            canonical.set(changedSlot, new UnlimitedItemStack(localStack));
            states.setDirty();
            // Push the value just accepted into canonical state, not the stale snapshot.
            canonStack = localStack.copy();
        }

        WormholeNetwork network = WormholeNetwork.get();
        List<WormholeNetwork.Entry> connected = network.getConnected(bodyUuid, be.getLevel().dimension(), be.getBlockPos());
        for (WormholeNetwork.Entry entry : connected) {
            ServerLevel targetLevel = be.getLevel().getServer().getLevel(entry.dimension());
            if (targetLevel == null) continue;
            BlockEntity targetBe = targetLevel.getBlockEntity(entry.pos());
            if (!(targetBe instanceof CelestialForgingAnvilBlockEntity targetCfa)) continue;
            Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> remoteMap = CfaInterfaceScanner.getInterfacesMap(
                    CelestialForgingAnvilLogisticsInterfaceBlockEntity.class, targetCfa.getLevel(), targetCfa.getBlockPos()
                );
            CelestialForgingAnvilLogisticsInterfaceBlockEntity remoteBe = remoteMap.get(relOffset);
            if (remoteBe == null || remoteBe == localBe) continue;
            IItemHandler remoteHandler = remoteBe.getItemHandler();
            remoteBe.setSyncing(true);
            try {
                setHandlerSlot(remoteHandler, changedSlot, canonStack.copy());
                remoteBe.setEjectCooldown(CelestialForgingAnvilLogisticsInterfaceBlockEntity.EJECT_COOLDOWN);
            } finally {
                remoteBe.setSyncing(false);
            }
        }
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

    /**
     * 物流接口的双向同步。
     *
     * <p>使用每个槽位的"上次见到"快照来判断是谁变更了：
     * <ul>
     *   <li>重连时（{@link #justReconnected}）：本地变更始终生效 —
     *       断连期间放入的物品会采纳到 canonical</li>
     *   <li>正常运行时：本地变更 & canonical 未变 → 更新 canonical</li>
     *   <li>正常运行时：canonical 变更 & 本地未变 → 更新本地</li>
     *   <li>两侧都变更 → canonical 为权威来源（由 syncLogisticsOnChange 携带意图设置）</li>
     * </ul>
     */
    private void syncWormholeLogistics(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || !registered || bodyUuid == null) return;
        Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> localMap = getLogisticsInterfacesMap(be);
        if (localMap.isEmpty()) return;

        boolean isReconnect = justReconnected;
        justReconnected = false;

        WormholeInterfaceStates states = WormholeInterfaceStates.get();
        for (var localEntry : localMap.entrySet()) {
            BlockPos relOffset = localEntry.getKey();
            CelestialForgingAnvilLogisticsInterfaceBlockEntity localBe = localEntry.getValue();
            IItemHandler localHandler = localBe.getItemHandler();
            int slots = localHandler.getSlots();

            UUID uuid = WormholeInterfaceStates.logisticsUuid(bodyUuid, relOffset.getX(), relOffset.getZ());
            List<UnlimitedItemStack> canonical = states.getOrCreateItemState(uuid, slots);
            List<UnlimitedItemStack> lastSeen = lastSeenItems.computeIfAbsent(uuid, k -> {
                List<UnlimitedItemStack> init = new ArrayList<>(slots);
                /// 重连时，从 canonical 初始化 lastSeen，使仅真正的本地变更
                ///（断连期间放入的物品）显示为变更。
                /// 正常运行时，同样从 canonical 初始化。
                for (int i = 0; i < slots; i++) init.add(canonical.get(i).copy());
                return init;
            });
            while (lastSeen.size() < slots) lastSeen.add(UnlimitedItemStack.EMPTY);

            for (int slot = 0; slot < slots; slot++) {
                ItemStack localStack = localHandler.getStackInSlot(slot);
                ItemStack canonStack = canonical.get(slot).toStack();

                boolean localVsCanonMismatch = !ItemStack.matches(localStack, canonStack)
                    || localStack.getCount() != canonStack.getCount();

                if (!localVsCanonMismatch) {
                    /// 已同步 — 更新 lastSeen 并继续
                    lastSeen.set(slot, new UnlimitedItemStack(localStack));
                    continue;
                }

                /// 冲突：两侧有不同的非空物品。
                /// 不自动解决 — 保持各自原位，直到玩家操作某个接口
                ///（syncLogisticsOnChange 触发同步）。
                if (!localStack.isEmpty() && !canonStack.isEmpty()) {
                    lastSeen.set(slot, new UnlimitedItemStack(localStack));
                    continue;
                }

                if (isReconnect) {
                    /// 重连时：本地在断连期间被修改。
                    /// 若仅本地有物品 → 采纳到 canonical。
                    /// 若仅 canonical 有物品 → 推送 canonical 到本地。
                    if (!localStack.isEmpty()) {
                        canonical.set(slot, new UnlimitedItemStack(localStack));
                        states.setDirty();
                    } else {
                        setHandlerSlot(localHandler, slot, canonStack.copy());
                    }
                    lastSeen.set(slot, new UnlimitedItemStack(localHandler.getStackInSlot(slot)));
                    continue;
                }

                /// 正常运行：与 lastSeen 比较
                ItemStack lastStack = lastSeen.get(slot).toStack();
                boolean localChanged = !ItemStack.matches(localStack, lastStack)
                                    || localStack.getCount() != lastStack.getCount();
                boolean canonChanged = !ItemStack.matches(canonStack, lastStack)
                                     || canonStack.getCount() != lastStack.getCount();

                if (!localChanged && !canonChanged) continue;

                if (localChanged && !canonChanged) {
                    /// 本地变更而 canonical 未变 → 推送本地到 canonical
                    canonical.set(slot, new UnlimitedItemStack(localStack));
                    states.setDirty();
                } else {
                    /// canonical 变更（或两侧都变更）→ canonical 为权威来源
                    if (!ItemStack.matches(localStack, canonStack) || localStack.getCount() != canonStack.getCount()) {
                        setHandlerSlot(localHandler, slot, canonStack.copy());
                    }
                }

                lastSeen.set(slot, new UnlimitedItemStack(localHandler.getStackInSlot(slot)));
            }
        }
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

    /**
     * 当本地虫洞节点断开时，清除本地物流、流体和激光接口镜像。
     * 已有的物品和流体不会写回或删除共享 canonical，仍可从其它虫洞接口访问。
     * 激光输出置零，使输出端激光立即停止发射。
     */
    private void clearLocalInterfaces(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;

        Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> logisticsMap = getLogisticsInterfacesMap(be);
        for (var entry : logisticsMap.entrySet()) {
            CelestialForgingAnvilLogisticsInterfaceBlockEntity localBe = entry.getValue();
            IItemHandler handler = localBe.getItemHandler();
            int slots = handler.getSlots();
            for (int slot = 0; slot < slots; slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    handler.extractItem(slot, stack.getCount(), false);
                }
            }
        }

        Map<BlockPos, CelestialForgingAnvilFluidInterfaceBlockEntity> fluidMap = getFluidInterfacesMap(be);
        for (var entry : fluidMap.entrySet()) {
            CelestialForgingAnvilFluidInterfaceBlockEntity localBe = entry.getValue();
            IFluidHandler handler = localBe.getFluidHandler();
            int tanks = handler.getTanks();
            for (int tank = 0; tank < tanks; tank++) {
                FluidStack stack = handler.getFluidInTank(tank);
                if (!stack.isEmpty()) {
                    handler.drain(stack, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }

        stopLocalLaserOutputs(be);
    }

    private void stopLocalLaserOutputs(CelestialForgingAnvilBlockEntity be) {
        Map<BlockPos, CelestialForgingAnvilLaserInterfaceBlockEntity> laserMap = getLaserInterfacesMap(be);
        for (var entry : laserMap.entrySet()) {
            CelestialForgingAnvilLaserInterfaceBlockEntity localBe = entry.getValue();
            localBe.setWormholeLaserOutput(0, false);
        }
    }

    private static void setHandlerSlot(IItemHandler handler, int slot, ItemStack stack) {
        ItemStack existing = handler.getStackInSlot(slot);
        if (!existing.isEmpty()) handler.extractItem(slot, existing.getCount(), false);
        if (!stack.isEmpty()) handler.insertItem(slot, stack, false);
    }

    private static void setTankContents(IFluidHandler handler, int tank, FluidStack stack) {
        FluidStack existing = handler.getFluidInTank(tank);
        if (!existing.isEmpty()) handler.drain(existing, IFluidHandler.FluidAction.EXECUTE);
        if (!stack.isEmpty()) handler.fill(stack.copy(), IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
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
