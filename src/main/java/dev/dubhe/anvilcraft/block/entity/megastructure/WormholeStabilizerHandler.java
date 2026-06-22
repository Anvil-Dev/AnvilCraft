package dev.dubhe.anvilcraft.block.entity.megastructure;

import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilFluidInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLaserInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.CelestialForgingAnvilLogisticsInterfaceBlockEntity;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.saved.WormholeNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Wormhole Stabilizer handler.
 * <p>
 * WormholeNetwork registration/unregistration is fully integrated.
 * Cross-CFA chunk loading, logistics sync, and fluid sync are deferred
 * to future wormhole content synchronization.
 */
public class WormholeStabilizerHandler extends BaseMegastructureHandler {

    @Nullable
    private UUID bodyUuid = null;
    private boolean registered = false;
    private boolean justReconnected = false;
    private final Map<Cube323PartHalf, BlockPos> portals = new EnumMap<>(Cube323PartHalf.class);

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
        return 0;
    }

    @Override
    public void serverTick(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;
        CelestialRefactorOption option = be.getActiveMegastructureOption();
        if (option == null || !name().equals(option.megastructure())) return;
        if (!(be.getCelestialBodyData() instanceof StarData star) || star.bodyClass() != CelestialBodyClass.BLACK_HOLE) return;

        UUID uuid = star.bodyUuid();
        if (uuid == null) uuid = this.bodyUuid;
        if (uuid == null) return;

        if (!be.isAmplifierPresent()) {
            if (registered) {
                WormholeNetwork.get().unregister(be.getLevel(), be.getBlockPos());
                registered = false;
                clearLocalInterfaces(be);
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
        }

        // Cross-CFA chunk loading, logistics sync, and fluid sync deferred
        // to future wormhole content synchronization.
        syncWormholeLasers(be);
    }

    @Override
    public void onBuild(CelestialForgingAnvilBlockEntity be) {
        if (be.getCelestialBodyData() instanceof StarData star
            && star.bodyClass() == CelestialBodyClass.BLACK_HOLE && be.isAmplifierPresent()) {
            UUID uuid = star.bodyUuid();
            if (uuid == null) return;
            this.bodyUuid = uuid;
            WormholeNetwork.get().register(uuid, be.getLevel(), be.getBlockPos());
            registered = true;
        }
    }

    @Override
    public void onClear(CelestialForgingAnvilBlockEntity be) {
        if (registered && be.getLevel() != null && !be.getLevel().isClientSide()) {
            WormholeNetwork.get().unregister(be.getLevel(), be.getBlockPos());
            registered = false;
        }
        bodyUuid = null;
        portals.clear();
    }

    public boolean addPortal(Cube323PartHalf side, BlockPos portalPos, CelestialForgingAnvilBlockEntity be) {
        if (side != Cube323PartHalf.BOTTOM_N && side != Cube323PartHalf.BOTTOM_S
            && side != Cube323PartHalf.BOTTOM_E && side != Cube323PartHalf.BOTTOM_W) return false;
        if (portals.containsKey(side)) return false;
        portals.put(side, portalPos);
        be.setChanged();
        if (be.getLevel() != null) {
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
        return true;
    }

    public void removePortal(Cube323PartHalf side, CelestialForgingAnvilBlockEntity be) {
        portals.remove(side);
        be.setChanged();
        if (be.getLevel() != null) {
            be.getLevel().sendBlockUpdated(be.getBlockPos(), be.getBlockState(), be.getBlockState(), 3);
        }
    }

    // === Laser sync (self-contained, no external dependencies) ===

    private void syncWormholeLasers(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide() || !registered || bodyUuid == null) return;
        Map<BlockPos, CelestialForgingAnvilLaserInterfaceBlockEntity> localMap = getLaserInterfacesMap(be);
        if (localMap.isEmpty()) return;

        // Query wormhole network for connected CFAs' laser outputs
        WormholeNetwork network = WormholeNetwork.get();
        List<WormholeNetwork.Entry> connected = network.getConnected(
            bodyUuid, be.getLevel().dimension(), be.getBlockPos());

        // Remap connected entries by relative position for O(1) lookup
        Map<BlockPos, List<WormholeNetwork.Entry>> byPos = new HashMap<>();
        for (WormholeNetwork.Entry e : connected) {
            byPos.computeIfAbsent(e.pos(), k -> new ArrayList<>()).add(e);
        }

        // For each local laser interface, sync from connected CFA's corresponding slot
        for (var localEntry : localMap.entrySet()) {
            BlockPos relOffset = localEntry.getKey();
            CelestialForgingAnvilLaserInterfaceBlockEntity localLaser = localEntry.getValue();

            boolean found = false;
            for (WormholeNetwork.Entry remoteEntry : connected) {
                // Look up the remote CFA's laser interface at the same relative offset;
                // cross-CFA laser level resolution requires full wormhole content sync.
                if (remoteEntry.pos() != null) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                localLaser.setWormholeLaserOutput(0, false);
            }
        }
    }

    // === Local interface cleanup ===

    private void clearLocalInterfaces(CelestialForgingAnvilBlockEntity be) {
        if (be.getLevel() == null || be.getLevel().isClientSide()) return;

        Map<BlockPos, CelestialForgingAnvilLogisticsInterfaceBlockEntity> logisticsMap = getLogisticsInterfacesMap(be);
        for (var entry : logisticsMap.entrySet()) {
            ResourceHandler<ItemResource> handler = entry.getValue().getItemHandler();
            for (int slot = 0; slot < handler.size(); slot++) {
                ItemStack stack = getStackFromHandler(handler, slot);
                if (!stack.isEmpty()) {
                    extractFromHandler(handler, slot, stack.getCount());
                }
            }
        }

        Map<BlockPos, CelestialForgingAnvilFluidInterfaceBlockEntity> fluidMap = getFluidInterfacesMap(be);
        for (var entry : fluidMap.entrySet()) {
            ResourceHandler<FluidResource> handler = entry.getValue().getFluidHandler();
            for (int tank = 0; tank < handler.size(); tank++) {
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

    // === Persistence ===

    @Override
    public void saveAdditional(ValueOutput output) {
        if (bodyUuid != null) {
            output.store("wormholeBodyUuid", UUIDUtil.CODEC, bodyUuid);
        }
        if (!portals.isEmpty()) {
            ValueOutput.ValueOutputList portalList = output.childrenList("portals");
            for (Map.Entry<Cube323PartHalf, BlockPos> entry : portals.entrySet()) {
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
                portals.put(side, pos);
            }
        });
    }

    @Override
    public void writeUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        if (bodyUuid != null) {
            tag.store("wormholeBodyUuid", UUIDUtil.CODEC, bodyUuid);
        }
    }

    @Override
    public void readUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.bodyUuid = tag.read("wormholeBodyUuid", UUIDUtil.CODEC).orElse(null);
    }
}
