package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.fluidtank.CreativeFluidHandler;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class CreativeFluidTankBlockEntity extends BlockEntity implements IFluidResourceHandlerHolder {
    private final CreativeFluidHandler fluidHandler = new CreativeFluidHandler();

    public CreativeFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.addContainer(this.level, this.getBlockPos());
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.removeContainer(this.level, this.getBlockPos());
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.fluidHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.fluidHandler.deserialize(input);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        StoredFluids fluids = components.getOrDefault(ModComponents.CREATIVE_TANK_FLUIDS, StoredFluids.EMPTY);
        if (!fluids.isEmpty()) {
            this.fluidHandler.replaceStacks(fluids.fluids());
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        StoredFluids fluids = new StoredFluids(this.fluidHandler.getStacks());
        if (!fluids.isEmpty()) {
            components.set(ModComponents.CREATIVE_TANK_FLUIDS, fluids);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CreativeFluidHandler getFluidHandler() {
        return this.fluidHandler;
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        var container = ItemAccess.forPlayerInteraction(player, hand).oneByOne().getCapability(Capabilities.Fluid.ITEM);
        if (container == null || container.size() == 0 || this.level == null) return false;
        if (this.level.isClientSide()) return true;
        var incoming = container.getResource(0);
        var stored = this.fluidHandler.getResource(0);
        final var soundFluid = incoming.isEmpty() ? stored : incoming;
        boolean taking = incoming.isEmpty() && !stored.isEmpty();
        if (taking) {
            if (player.isCreative()) {
                this.fluidHandler.replaceStacks(List.of(FluidStack.EMPTY));
            } else {
                try (Transaction transaction = Transaction.openRoot()) {
                    int space = container.getCapacityAsInt(0, stored) - container.getAmountAsInt(0);
                    if (space > 0 && container.insert(stored, space, transaction) > 0) transaction.commit();
                }
            }
        } else {
            if (!player.isCreative() && !incoming.isEmpty()) {
                int amount = container.getAmountAsInt(0);
                try (Transaction transaction = Transaction.openRoot()) {
                    if (container.extract(incoming, amount, transaction) != amount) return false;
                    transaction.commit();
                }
            }
            this.fluidHandler.replaceStacks(List.of(incoming.toStack(Integer.MAX_VALUE)));
        }
        this.setChanged();
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        if (!soundFluid.isEmpty()) {
            var sound = soundFluid.getFluidType().getSound(taking ? SoundActions.BUCKET_FILL : SoundActions.BUCKET_EMPTY);
            if (sound != null) this.level.playSound(null, this.getBlockPos(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }
}
