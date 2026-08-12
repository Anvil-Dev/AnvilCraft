package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.AutoEnchantingTableBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import dev.dubhe.anvilcraft.util.LiquidEnchantmentUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

@Getter
public class AutoEnchantingTableBlockEntity extends BlockEntity
    implements MenuProvider, IItemHandlerHolder, IFluidHandlerHolder, IPowerConsumer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_PRIMER = 2;
    /// 默认功耗：16 kW
    public static final int DEFAULT_POWER_CONSUMPTION = 16;
    /// 内部流体容量：32 桶
    public static final int FLUID_CAPACITY = 32 * FluidType.BUCKET_VOLUME;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            AutoEnchantingTableBlockEntity.this.setChanged();
        }
    };

    private final FluidTank fluidTank = new FluidTank(
        FLUID_CAPACITY,
        stack -> stack.is(ModFluids.EXP_FLUID)
            || (stack.is(ModFluids.LIQUID_ENCHANTMENT) && LiquidEnchantmentUtil.isEnchanted(stack))
    ) {
        @Override
        protected void onContentsChanged() {
            AutoEnchantingTableBlockEntity.this.setChanged();
            if (AutoEnchantingTableBlockEntity.this.level != null) {
                AutoEnchantingTableBlockEntity.this.level.sendBlockUpdated(
                    getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    };

    @Setter
    private @Nullable PowerGrid grid;

    /**
     * 自动化（管道/漏斗）使用的物品处理器：输入只能进输入槽，输出只能抽输出槽，引物格不允许自动输入输出。
     */
    private final IItemHandler automationHandler = new IItemHandler() {
        @Override
        public int getSlots() {
            return AutoEnchantingTableBlockEntity.this.itemHandler.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return AutoEnchantingTableBlockEntity.this.itemHandler.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot != SLOT_INPUT) return stack;
            return AutoEnchantingTableBlockEntity.this.itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != SLOT_OUTPUT) return ItemStack.EMPTY;
            return AutoEnchantingTableBlockEntity.this.itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return AutoEnchantingTableBlockEntity.this.itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_INPUT && AutoEnchantingTableBlockEntity.this.itemHandler.isItemValid(slot, stack);
        }
    };

    public AutoEnchantingTableBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.AUTO_ENCHANTING_TABLE.get(), pos, blockState);
    }

    private AutoEnchantingTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static AutoEnchantingTableBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new AutoEnchantingTableBlockEntity(type, pos, blockState);
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

    public static void tick(Level level, BlockPos pos, BlockState state, AutoEnchantingTableBlockEntity blockEntity) {
        if (level.isClientSide) return;
        blockEntity.flushState(level, pos);
        if (state.getValue(AutoEnchantingTableBlock.POWERED)) return;
        if (!blockEntity.isGridWorking()) return;
        // TODO: 在此实现自动附魔逻辑（读取输入格与引物格与流体罐，将结果写入输出格）
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.itemHandler.serializeNBT(registries));
        tag.put("FluidTank", this.fluidTank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        this.fluidTank.readFromNBT(registries, tag.getCompound("FluidTank"));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("FluidTank", this.fluidTank.writeToNBT(registries, new CompoundTag()));
        return tag;
    }

    @Override
    public IFluidHandler getFluidHandler() {
        return this.fluidTank;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.auto_enchanting_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AutoEnchantingTableMenu(ModMenuTypes.AUTO_ENCHANTING_TABLE.get(), containerId, inventory, this);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(this.getBlockPos());
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
    public int getInputPower() {
        if (this.level == null) return DEFAULT_POWER_CONSUMPTION;
        return this.getBlockState().getValue(AutoEnchantingTableBlock.POWERED)
            ? 0
            : DEFAULT_POWER_CONSUMPTION;
    }
}
