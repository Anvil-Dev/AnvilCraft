package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fluid interface for the Celestial Forging Anvil.
 * Stores 4 fluid types, each up to 80 buckets.
 * Consumes 128kW power. Supports fluid I/O via pipes.
 */
public class CelestialForgingAnvilFluidInterfaceBlockEntity extends BlockEntity implements IPowerConsumer {
    private static final int TANK_COUNT = 4;
    private static final int CAPACITY_PER_TANK = 80_000; // 80 buckets in mB

    @Getter
    private final FluidTank[] tanks = new FluidTank[TANK_COUNT];

    @Setter
    @Nullable
    private PowerGrid grid;

    public CelestialForgingAnvilFluidInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        for (int i = 0; i < TANK_COUNT; i++) {
            final int index = i;
            tanks[i] = new FluidTank(CAPACITY_PER_TANK) {
                @Override
                public boolean isFluidValid(FluidStack stack) {
                    // Only accept fluid if this tank already has it, or if no other tank has it
                    FluidStack current = getFluid();
                    if (current.isEmpty()) {
                        for (int j = 0; j < TANK_COUNT; j++) {
                            if (j != index && tanks[j].getFluid().is(stack.getFluid())) {
                                return false;
                            }
                        }
                        return true;
                    }
                    return current.is(stack.getFluid());
                }

                @Override
                protected void onContentsChanged() {
                    CelestialForgingAnvilFluidInterfaceBlockEntity.this.setChanged();
                }
            };
        }
    }

    /**
     * Sync block entity data to all tracking clients.
     */
    public void syncToClients() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = getUpdatePacket();
            if (packet != null) {
                for (ServerPlayer player : serverLevel.getChunkSource().chunkMap
                    .getPlayers(serverLevel.getChunkAt(worldPosition).getPos(), false)) {
                    player.connection.send(packet);
                }
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            syncToClients();
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int getInputPower() {
        return 128; // 128kW
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public @Nullable PowerGrid getGrid() {
        return this.grid;
    }

    @Override
    public PowerComponentType getComponentType() {
        return IPowerConsumer.super.getComponentType();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeTanks(tag, registries);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readTanks(tag, registries);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeTanks(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        readTanks(tag, registries);
    }

    private void writeTanks(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < TANK_COUNT; i++) {
            CompoundTag tankTag = new CompoundTag();
            tanks[i].writeToNBT(registries, tankTag);
            tag.put("tank" + i, tankTag);
        }
    }

    private void readTanks(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < TANK_COUNT; i++) {
            if (tag.contains("tank" + i)) {
                tanks[i].readFromNBT(registries, tag.getCompound("tank" + i));
            }
        }
    }

    /**
     * Returns the fluid handler capability for pipe I/O.
     * Merges all 4 tanks into a single handler.
     */
    @SuppressWarnings("unused")
    public IFluidHandler getFluidHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return TANK_COUNT;
            }

            @Override
            public @NotNull FluidStack getFluidInTank(int tank) {
                return tanks[tank].getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return tanks[tank].getCapacity();
            }

            @Override
            public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
                return tanks[tank].isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) return 0;
                // Try existing tank first, then empty tanks
                for (int i = 0; i < TANK_COUNT; i++) {
                    if (tanks[i].getFluid().is(resource.getFluid())) {
                        return tanks[i].fill(resource, action);
                    }
                }
                for (int i = 0; i < TANK_COUNT; i++) {
                    if (tanks[i].getFluid().isEmpty()) {
                        return tanks[i].fill(resource, action);
                    }
                }
                return 0;
            }

            @Override
            public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) return FluidStack.EMPTY;
                for (int i = 0; i < TANK_COUNT; i++) {
                    if (tanks[i].getFluid().is(resource.getFluid())) {
                        return tanks[i].drain(resource, action);
                    }
                }
                return FluidStack.EMPTY;
            }

            @Override
            public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                for (int i = 0; i < TANK_COUNT; i++) {
                    if (!tanks[i].getFluid().isEmpty()) {
                        return tanks[i].drain(maxDrain, action);
                    }
                }
                return FluidStack.EMPTY;
            }
        };
    }
}
