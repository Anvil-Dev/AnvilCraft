package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
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
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import org.jspecify.annotations.Nullable;

/**
 * Fluid interface for the Celestial Forging Anvil.
 * Stores 4 fluid types, each up to 80 buckets.
 * Consumes 128kW power. Supports fluid I/O via pipes.
 */
public class CelestialForgingAnvilFluidInterfaceBlockEntity extends BlockEntity implements IPowerConsumer {
    private static final int TANK_COUNT = 4;
    private static final int CAPACITY_PER_TANK = 80_000; // 80 buckets in mB

    @Getter
    private final FluidStacksResourceHandler tank;

    @Setter
    @Nullable
    private PowerGrid grid;

    public CelestialForgingAnvilFluidInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.tank = new FluidStacksResourceHandler(TANK_COUNT, CAPACITY_PER_TANK) {
            @Override
            public boolean isValid(int index, FluidResource resource) {
                if (resource.isEmpty()) return false;
                FluidStack currentStack = this.getStackFrom(this.getResource(index), this.getAmountAsInt(index));
                if (!currentStack.isEmpty() && currentStack.is(resource.getFluid())) return true;
                if (currentStack.isEmpty()) {
                    for (int j = 0; j < TANK_COUNT; j++) {
                        if (j != index) {
                            FluidStack otherStack = this.getStackFrom(this.getResource(j), this.getAmountAsInt(j));
                            if (!otherStack.isEmpty() && otherStack.is(resource.getFluid())) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
                return false;
            }

            protected void onContentsChanged() {
                CelestialForgingAnvilFluidInterfaceBlockEntity.this.setChanged();
            }
        };
    }

    public CelestialForgingAnvilFluidInterfaceBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE.get(), pos, blockState);
    }

    public static CelestialForgingAnvilFluidInterfaceBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState state
    ) {
        return new CelestialForgingAnvilFluidInterfaceBlockEntity(type, pos, state);
    }

    public void syncToClients() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = this.getUpdatePacket();
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
            this.syncToClients();
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int getInputPower() {
        return 128;
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.serialize(output.child("tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input.childOrEmpty("tank"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        TagValueOutput fluidTag = TagValueOutput.createWithContext(
            new ProblemReporter.Collector(this.problemPath()), registries);
        this.tank.serialize(fluidTag);
        tag.put("tank", fluidTag.buildResult());
        return tag;
    }

    /**
     * Returns the fluid handler capability for pipe I/O.
     */
    @SuppressWarnings("unused")
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.tank;
    }
}
