package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.TankUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Redstone;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class FluidTankBlockEntity extends BlockEntity implements IFluidResourceHandlerHolder {
    public static final int BASE_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int INFINITY_THRESHOLD = 12800 * FluidType.BUCKET_VOLUME;
    private static final int CHECK_INTERVAL = 100;

    private final SingleFluidTankHandler tank = new SingleFluidTankHandler(
        FluidTankBlockEntity.BASE_CAPACITY,
        FluidTankBlockEntity.INFINITY_THRESHOLD,
        this::onTankChanged
    );
    private int tickCounter;

    public FluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
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

    public void onFormed() {
        this.tank.setEnhanced(true);
    }

    public void onUnformed() {
        this.tank.setEnhanced(false);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity entity) {
        if (level.isClientSide() || ++entity.tickCounter % FluidTankBlockEntity.CHECK_INTERVAL != 0) return;
        boolean valid = TankUtil.isMengerStructure(level, pos, 3);
        if (entity.tank.isEnhanced() && !valid) {
            entity.onUnformed();
        } else if (!entity.tank.isEnhanced() && valid) {
            entity.onFormed();
        }
    }

    private void onTankChanged() {
        this.setChanged();
        this.updateLightLevel();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    private void updateLightLevel() {
        if (this.level == null) return;
        AuxiliaryLightManager manager = this.level.getAuxLightManager(this.getBlockPos());
        if (manager == null) return;
        FluidStack fluid = this.tank.getFluid();
        float fill = Math.min(1.0F, (float) this.tank.getFluidAmount() / this.tank.getCapacity());
        manager.setLightAt(this.getBlockPos(), (int) Math.ceil(fluid.getFluidType().getLightLevel(fluid) * fill));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.serialize(output.child("Tank"));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input.childOrEmpty("Tank"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.tank.serialize(output);
        tag.put("Tank", output.buildResult());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("Tank");
        this.tank.serializeForItem(output.child("Tank"));
    }

    public void saveToDrop(ItemStack stack, HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.saveCustomOnly(output);
        this.removeComponentsFromTag(output);
        output.discard("Tank");
        this.tank.serializeForDrop(output.child("Tank"));
        BlockItem.setBlockEntityData(stack, this.getType(), output);
        stack.applyComponents(this.collectComponents());
    }

    public static boolean isEmptyItem(ItemStack stack, HolderLookup.Provider registries) {
        if (!stack.is(ModBlocks.FLUID_TANK.asItem())) return false;
        return FluidTankBlockEntity.readItemTank(stack, registries).getFluid().isEmpty();
    }

    public static ItemStack fillItem(ItemStack stack, FluidStack fluid, HolderLookup.Provider registries) {
        if (fluid.isEmpty() || !FluidTankBlockEntity.isEmptyItem(stack, registries)) return ItemStack.EMPTY;

        SingleFluidTankHandler itemTank = new SingleFluidTankHandler(
            FluidTankBlockEntity.BASE_CAPACITY,
            FluidTankBlockEntity.INFINITY_THRESHOLD,
            () -> {}
        );
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = itemTank.insert(FluidResource.of(fluid), fluid.getAmount(), transaction);
            if (inserted != fluid.getAmount()) return ItemStack.EMPTY;
            transaction.commit();
        }

        ItemStack result = stack.copyWithCount(1);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        itemTank.serializeForItem(output.child("Tank"));
        BlockItem.setBlockEntityData(result, ModBlockEntities.FLUID_TANK.get(), output);
        return result;
    }

    private static SingleFluidTankHandler readItemTank(ItemStack stack, HolderLookup.Provider registries) {
        SingleFluidTankHandler itemTank = new SingleFluidTankHandler(
            FluidTankBlockEntity.BASE_CAPACITY,
            FluidTankBlockEntity.INFINITY_THRESHOLD,
            () -> {}
        );
        itemTank.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries, FluidTankBlockEntity.getTankData(stack)));
        return itemTank;
    }

    private static CompoundTag getTankData(ItemStack stack) {
        TypedEntityData<?> data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        return data == null ? new CompoundTag() : data.copyTagWithoutId().getCompoundOrEmpty("Tank");
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        try (Transaction transaction = Transaction.openRoot()) {
            boolean success = FluidUtil.interactWithFluidHandler(player, hand, this.getBlockPos(), this.tank, transaction);
            if (success) transaction.commit();
            return success;
        }
    }

    public int getRedstoneSignal() {
        int amount = this.tank.getFluidAmount();
        int capacity = this.tank.getCapacity();
        int strength = amount == 0 ? 0 : amount * (Redstone.SIGNAL_MAX - 1) / capacity + 1;
        return Mth.clamp(strength, Redstone.SIGNAL_MIN, Redstone.SIGNAL_MAX);
    }

    @Override
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.tank;
    }

    public boolean isInfinite() {
        return this.tank.isInfinite();
    }

    public boolean containsInfiniteFluid() {
        return this.tank.getFluidAmount() >= FluidTankBlockEntity.INFINITY_THRESHOLD;
    }
}
