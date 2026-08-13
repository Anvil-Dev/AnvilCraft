package dev.dubhe.anvilcraft.block.entity;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.AutoEnchantingTableBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import dev.dubhe.anvilcraft.util.LiquidEnchantmentUtil;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
public class AutoEnchantingTableBlockEntity extends BlockEntity
    implements MenuProvider, IItemHandlerHolder, IFluidHandlerHolder, IPowerConsumer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_PRIMER = 2;
    /// 无引物功耗：16 kW
    public static final int DEFAULT_POWER_CONSUMPTION = 16;
    /// 有引物功耗：64 kW
    public static final int PRIMER_POWER_CONSUMPTION = 64;
    /// 内部流体容量：32 桶
    public static final int FLUID_CAPACITY = 32 * FluidType.BUCKET_VOLUME;
    /// 每次附魔尝试的间隔（tick），4 秒 = 80 tick
    public static final int ENCHANT_INTERVAL_TICKS = 80;
    /// 每个书架消耗的经验流体（mB）
    public static final int EXP_COST_PER_SHELF = 400;

    private final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        protected void onContentsChanged(int slot) {
            AutoEnchantingTableBlockEntity.this.setChanged();
            // 取出引物后记忆消失
            if (
                slot == SLOT_PRIMER
                && AutoEnchantingTableBlockEntity.this.itemHandler.getStackInSlot(SLOT_PRIMER).isEmpty()
                && !AutoEnchantingTableBlockEntity.this.selectedEnchantments.isEmpty()
            ) {
                AutoEnchantingTableBlockEntity.this.selectedEnchantments.clear();
            }
            AutoEnchantingTableBlockEntity.this.syncToClient();
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

    private final List<Holder<Enchantment>> selectedEnchantments = new ArrayList<>();
    private int shelfLevel = 0;
    private int cooldownTicks = 0;
    private WorkMode workMode = WorkMode.ENCHANTING;
    @Setter
    private float bookOpen;

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
        // 刷新书架等级（供客户端自选附魔校验与同步）
        blockEntity.refreshShelfLevel(level, pos);
        blockEntity.flushState(level, pos);
        if (state.getValue(AutoEnchantingTableBlock.POWERED)) return;
        if (!blockEntity.isGridWorking()) return;

        // 1. 先刷新工作模式：模式有变化则重设 4 秒冷却
        WorkMode newMode = blockEntity.refreshWorkMode();
        if (newMode != blockEntity.workMode) {
            blockEntity.workMode = newMode;
            blockEntity.cooldownTicks = ENCHANT_INTERVAL_TICKS;
            blockEntity.setChanged();
        }

        // 2. 冷却逻辑
        if (blockEntity.cooldownTicks > 0) {
            blockEntity.cooldownTicks--;
            return;
        }

        // 3. 冷却完毕，按工作模式执行操作
        blockEntity.cooldownTicks = ENCHANT_INTERVAL_TICKS;
        switch (blockEntity.workMode) {
            case ENCHANTING -> blockEntity.tryEnchantRandomly(level, pos);
            case PRIMER -> blockEntity.tryEnchantWithPrimer(level, pos);
            case LIQUID_ENCHANTMENT -> blockEntity.tryLiquidEnchantment(level, pos);
        }
    }

    public boolean isAllowedPrimer(ItemStack stack) {
        return stack.is(ModItemTags.AUTO_ENCHANTING_TABLE_PRIMERS);
    }

    private WorkMode refreshWorkMode() {
        ItemStack primer = this.itemHandler.getStackInSlot(SLOT_PRIMER);
        if (!primer.isEmpty() && this.isAllowedPrimer(primer)) {
            return WorkMode.PRIMER;
        }
        if (this.workMode == WorkMode.PRIMER) {
            // 引物被取出时清空记忆
            this.selectedEnchantments.clear();
            this.syncToClient();
        }
        return WorkMode.ENCHANTING;
    }

    private void refreshShelfLevel(Level level, BlockPos pos) {
        int levelValue = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            BlockPos bookshelfPos = pos.offset(offset);
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                levelValue += (int) level.getBlockState(bookshelfPos).getEnchantPowerBonus(level, bookshelfPos);
            }
        }
        if (levelValue != this.shelfLevel) {
            this.shelfLevel = levelValue;
            this.setChanged();
            this.syncToClient();
        }
    }

    public void selectEnchantment(Holder<Enchantment> enchantment) {
        if (!this.selectedEnchantments.contains(enchantment)) {
            this.selectedEnchantments.add(enchantment);
            this.setChanged();
            this.syncToClient();
        }
    }

    public void unselectEnchantment(Holder<Enchantment> enchantment) {
        if (this.selectedEnchantments.remove(enchantment)) {
            this.setChanged();
            this.syncToClient();
        }
    }

    public boolean isSelected(Holder<Enchantment> enchantment) {
        return this.selectedEnchantments.contains(enchantment);
    }

    public List<Holder<Enchantment>> getSelectedEnchantments() {
        return List.copyOf(this.selectedEnchantments);
    }

    public int getSelectedTotalLevel() {
        int total = 0;
        for (Holder<Enchantment> holder : this.selectedEnchantments) {
            total += holder.value().getMaxLevel();
        }
        return total;
    }

    public void syncToClient() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(
                this.getBlockPos(),
                this.getBlockState(),
                this.getBlockState(),
                Block.UPDATE_CLIENTS
            );
        }
    }

    private void tryEnchantRandomly(Level level, BlockPos pos) {
        ItemStack input = this.itemHandler.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return;
        // 输出槽被占用时不做
        if (!this.itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty()) return;

        int shelfLevel = this.shelfLevel;
        int cost = Math.min(shelfLevel * EXP_COST_PER_SHELF, FLUID_CAPACITY);
        if (cost <= 0) return;

        // 经验流体不足则本次附魔取消，等待下一轮
        FluidStack fluid = this.fluidTank.getFluid();
        if (!fluid.is(ModFluids.EXP_FLUID) || fluid.getAmount() < cost) return;

        // 与原版附魔台相同的随机附魔
        var enchantmentTag = level.registryAccess()
            .registryOrThrow(Registries.ENCHANTMENT)
            .getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (enchantmentTag.isEmpty()) return;
        List<EnchantmentInstance> enchants = EnchantmentHelper.selectEnchantment(
            level.random, input, shelfLevel, enchantmentTag.get().stream());
        if (enchants.isEmpty()) return;

        ItemStack result = input.copy();
        for (EnchantmentInstance instance : enchants) {
            result.enchant(instance.enchantment, instance.level);
        }

        this.finishEnchant(result, cost);
    }

    private void tryEnchantWithPrimer(Level level, BlockPos pos) {
        ItemStack input = this.itemHandler.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return;
        if (!this.itemHandler.getStackInSlot(SLOT_OUTPUT).isEmpty()) return;
        if (this.selectedEnchantments.isEmpty()) return;

        // 只有书架足够时才成功附魔（附魔总等级不能超过书架数量）
        int totalLevel = this.getSelectedTotalLevel();
        if (totalLevel > this.shelfLevel) return;

        int cost = totalLevel * EXP_COST_PER_SHELF;
        if (cost <= 0 || cost > FLUID_CAPACITY) return;

        FluidStack fluid = this.fluidTank.getFluid();
        if (!fluid.is(ModFluids.EXP_FLUID) || fluid.getAmount() < cost) return;

        ItemStack result = input.copy();
        boolean applied = false;
        for (Holder<Enchantment> holder : this.selectedEnchantments) {
            if (holder.value().canEnchant(result)) {
                result.enchant(holder, holder.value().getMaxLevel());
                applied = true;
            }
        }
        if (!applied) return;

        this.finishEnchant(result, cost);
    }

    private void tryLiquidEnchantment(Level level, BlockPos pos) {
        // TODO: 使用液态魔咒进行附魔
    }

    private void finishEnchant(ItemStack result, int cost) {
        this.fluidTank.drain(cost, IFluidHandler.FluidAction.EXECUTE);
        this.itemHandler.setStackInSlot(SLOT_OUTPUT, result);
        this.itemHandler.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(
                this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.itemHandler.serializeNBT(registries));
        tag.put("FluidTank", this.fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putString("WorkMode", this.workMode.getSerializedName());
        ListTag selected = new ListTag();
        for (Holder<Enchantment> holder : this.selectedEnchantments) {
            holder.unwrapKey().ifPresent(key -> selected.add(StringTag.valueOf(key.location().toString())));
        }
        tag.put("SelectedEnchantments", selected);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        this.fluidTank.readFromNBT(registries, tag.getCompound("FluidTank"));
        if (tag.contains("ShelfLevel")) {
            this.shelfLevel = tag.getInt("ShelfLevel");
        }
        if (tag.contains("WorkMode")) {
            for (WorkMode mode : WorkMode.values()) {
                if (mode.getSerializedName().equals(tag.getString("WorkMode"))) {
                    this.workMode = mode;
                    break;
                }
            }
        }
        this.selectedEnchantments.clear();
        if (tag.contains("SelectedEnchantments", CompoundTag.TAG_LIST)) {
            ListTag selected = tag.getList("SelectedEnchantments", CompoundTag.TAG_STRING);
            var registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
            for (int i = 0; i < selected.size(); i++) {
                ResourceLocation id = ResourceLocation.tryParse(selected.getString(i));
                if (id != null) {
                    registry.get(ResourceKey.create(Registries.ENCHANTMENT, id))
                        .ifPresent(this.selectedEnchantments::add);
                }
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("Inventory", this.itemHandler.serializeNBT(registries));
        tag.put("FluidTank", this.fluidTank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("ShelfLevel", this.shelfLevel);
        ListTag selected = new ListTag();
        for (Holder<Enchantment> holder : this.selectedEnchantments) {
            holder.unwrapKey().ifPresent(key -> selected.add(StringTag.valueOf(key.location().toString())));
        }
        tag.put("SelectedEnchantments", selected);
        return tag;
    }

    @Override
    public IFluidHandler getFluidHandler() {
        return this.fluidTank;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.anvilcraft.auto_enchanting_table.title");
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
        if (this.getBlockState().getValue(AutoEnchantingTableBlock.POWERED)) return 0;
        return this.isAllowedPrimer(this.itemHandler.getStackInSlot(SLOT_PRIMER))
            ? PRIMER_POWER_CONSUMPTION
            : DEFAULT_POWER_CONSUMPTION;
    }

    public enum WorkMode implements StringRepresentable {
        ENCHANTING,
        PRIMER,
        LIQUID_ENCHANTMENT,
        ;

        public static final Codec<WorkMode> CODEC = StringRepresentable.fromEnum(WorkMode::values);
        public static final StreamCodec<ByteBuf, WorkMode> STREAM_CODEC = StreamCodecUtil.enumStreamCodec(WorkMode.class);

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }
}
