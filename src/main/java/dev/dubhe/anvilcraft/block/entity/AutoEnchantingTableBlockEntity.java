package dev.dubhe.anvilcraft.block.entity;

import com.mojang.serialization.Codec;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.AutoEnchantingTableBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import dev.dubhe.anvilcraft.inventory.RoyalAnvilMenu;
import dev.dubhe.anvilcraft.util.LiquidEnchantmentUtil;
import dev.dubhe.anvilcraft.util.anvil.AnvilMenuResult;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Getter
public class AutoEnchantingTableBlockEntity extends BlockEntity
    implements MenuProvider, IItemResourceHandlerHolder, IFluidResourceHandlerHolder, IPowerConsumer {
    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_PRIMER = 2;
    /// 随机模式功耗：16 kW
    public static final int DEFAULT_POWER_CONSUMPTION = 16;
    /// 引物模式功耗：64 kW
    public static final int PRIMER_POWER_CONSUMPTION = 64;
    /// 定向模式功耗：64 kW
    public static final int LIQUID_POWER_CONSUMPTION = 64;
    /// 内部流体容量：32 桶
    public static final int FLUID_CAPACITY = 32 * FluidType.BUCKET_VOLUME;
    /// 每个书架消耗的经验流体（mB）
    public static final int EXP_COST_PER_SHELF = 400;
    /// 液态魔咒模式下物品每多一条已有附魔增加的功耗
    public static final int LIQUID_POWER_PER_ENCHANTMENT = 64;

    private final ItemStacksResourceHandler itemHandler = new ItemStacksResourceHandler(3) {
        private boolean movingToOutput;

        @Override
        public int getCapacity(int slot, ItemResource resource) {
            return 1;
        }

        @Override
        public boolean isValid(int slot, ItemResource resource) {
            // 引物槽只允许有效引物，避免自动化/快速放入把无关物品卡在引物槽
            if (slot == SLOT_PRIMER) {
                return AutoEnchantingTableBlockEntity.this.isAllowedPrimer(resource.toStack());
            }
            return super.isValid(slot, resource);
        }

        @Override
        public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            return this.movingToOutput ? 0 : super.insert(slot, resource, amount, transaction);
        }

        @Override
        public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            return this.movingToOutput ? 0 : super.extract(slot, resource, amount, transaction);
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack previousContents) {
            // 反序列化（chunk 加载 / 客户端更新包）期间不执行流转逻辑，否则会在 WorkMode 读取前
            // 误将液态魔咒模式下输入槽的已附魔物品透传到输出槽，并过早发送块更新。
            if (AutoEnchantingTableBlockEntity.this.loading) return;
            AutoEnchantingTableBlockEntity.this.setChanged();
            // 仅服务端执行物品流转逻辑；客户端在反序列化时也会触发本回调，
            // 若客户端也执行透传会错误地把输入物品移出，导致客户端显示与服务端不一致。
            if (
                AutoEnchantingTableBlockEntity.this.level != null
                && !AutoEnchantingTableBlockEntity.this.level.isClientSide()
            ) {
                if (
                    !this.movingToOutput
                    && slot == SLOT_INPUT
                    && !AutoEnchantingTableBlockEntity.this.getItem(SLOT_INPUT).isEmpty()
                ) {
                    if (
                        EnchantmentHelper.hasAnyEnchantments(AutoEnchantingTableBlockEntity.this.getItem(SLOT_INPUT))
                        && AutoEnchantingTableBlockEntity.this.getItem(SLOT_OUTPUT).isEmpty()
                        && AutoEnchantingTableBlockEntity.this.workMode != WorkMode.LIQUID_ENCHANTMENT
                    ) {
                        this.movingToOutput = true;
                        this.set(SLOT_OUTPUT, this.getResource(SLOT_INPUT), this.getAmountAsInt(SLOT_INPUT));
                        this.set(SLOT_INPUT, ItemResource.EMPTY, 0);
                        this.movingToOutput = false;
                    } else {
                        // 重置4秒冷却
                        AutoEnchantingTableBlockEntity.this.cooldownTicks = AnvilCraft.CONFIG.autoEnchantingTableInterval;
                    }
                }
                // 取出引物后记忆消失
                if (
                    slot == SLOT_PRIMER
                    && AutoEnchantingTableBlockEntity.this.getItem(SLOT_PRIMER).isEmpty()
                    && !AutoEnchantingTableBlockEntity.this.selectedEnchantments.isEmpty()
                ) {
                    AutoEnchantingTableBlockEntity.this.selectedEnchantments.clear();
                }
                // 放入引物后立即按新的限制 clamp 液态魔咒附魔等级（取出引物不 clamp，避免误操作丢失等级）
                if (
                    slot == SLOT_PRIMER
                    && !AutoEnchantingTableBlockEntity.this.getItem(SLOT_PRIMER).isEmpty()
                    && AutoEnchantingTableBlockEntity.this.liquidEnchantmentLevel > 0
                ) {
                    AutoEnchantingTableBlockEntity.this.setLiquidLevel(
                        AutoEnchantingTableBlockEntity.this.liquidEnchantmentLevel);
                }
            }
            AutoEnchantingTableBlockEntity.this.syncToClient();
        }
    };

    private final FluidStacksResourceHandler fluidTank = new FluidStacksResourceHandler(1, FLUID_CAPACITY) {
        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.is(ModFluids.EXP_FLUID.get())
                || resource.is(ModFluids.LIQUID_ENCHANTMENT.get()) && LiquidEnchantmentUtil.isEnchanted(resource.toStack(1));
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            AutoEnchantingTableBlockEntity.this.setChanged();
            if (!AutoEnchantingTableBlockEntity.this.loading) AutoEnchantingTableBlockEntity.this.syncToClient();
        }
    };

    @Setter
    private @Nullable PowerGrid grid;

    private final List<Holder<Enchantment>> selectedEnchantments = new ArrayList<>();
    private int shelfLevel = 0;
    private int cooldownTicks = 0;
    private int openMenuCount = 0;
    /// 反序列化期间抑制 onContentsChanged 的物品流转副作用
    private boolean loading;
    private WorkMode workMode = WorkMode.ENCHANTING;
    /// 液态魔咒模式下玩家选择的附魔等级（0 表示未选择）
    private int liquidEnchantmentLevel = 0;
    @Setter
    private float bookHeight;
    @Setter
    private float bookOpen;

    /**
     * 自动化（管道/漏斗）使用的物品处理器：输入只能进输入槽，输出只能抽输出槽，引物格不允许自动输入输出。
     */
    private final ResourceHandler<ItemResource> automationHandler = new DelegatingResourceHandler<>(this.itemHandler) {
        @Override
        public boolean isValid(int slot, ItemResource resource) {
            return slot == SLOT_INPUT && super.isValid(slot, resource);
        }

        @Override
        public int insert(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            return slot == SLOT_INPUT ? super.insert(slot, resource, amount, transaction) : 0;
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            return this.insert(SLOT_INPUT, resource, amount, transaction);
        }

        @Override
        public int extract(int slot, ItemResource resource, int amount, TransactionContext transaction) {
            return slot == SLOT_OUTPUT ? super.extract(slot, resource, amount, transaction) : 0;
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            return this.extract(SLOT_OUTPUT, resource, amount, transaction);
        }
    };

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) net.minecraft.world.Containers.dropContents(this.level, pos, this.itemHandler.copyToList());
    }

    public ItemStack getItem(int slot) {
        return this.itemHandler.getResource(slot).toStack(this.itemHandler.getAmountAsInt(slot));
    }

    public FluidStack getFluid() {
        return this.fluidTank.getResource(0).toStack(this.fluidTank.getAmountAsInt(0));
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

    public static void tick(Level level, BlockPos pos, BlockState state, AutoEnchantingTableBlockEntity be) {
        if (level.isClientSide()) return;
        be.flushState(level, pos);
        if (state.getValue(AutoEnchantingTableBlock.POWERED)) {
            be.resetCooldownWhenIdle();
            return;
        }
        if (!be.isGridWorking()) {
            be.resetCooldownWhenIdle();
            return;
        }

        // 1. 先刷新工作模式：模式有变化则重设 4 秒冷却
        WorkMode newMode = be.refreshWorkMode();
        if (newMode != be.workMode) {
            be.workMode = newMode;
            be.cooldownTicks = AnvilCraft.CONFIG.autoEnchantingTableInterval;
            be.setChanged();
        }

        be.refreshShelfLevel(level, pos);

        if (be.getItem(AutoEnchantingTableBlockEntity.SLOT_INPUT).isEmpty()) {
            boolean isMax = be.cooldownTicks == AnvilCraft.CONFIG.autoEnchantingTableInterval;
            be.cooldownTicks = AnvilCraft.CONFIG.autoEnchantingTableInterval;
            if (!isMax) {
                be.setChanged();
                be.syncToClient();
            }
            return;
        }

        // 引物/液态魔咒模式下打开 GUI 时暂停附魔，关闭 GUI 后继续
        if (
            (be.workMode == WorkMode.PRIMER || be.workMode == WorkMode.LIQUID_ENCHANTMENT)
            && be.hasOpenMenu()
        ) {
            return;
        }

        // 当前不具备执行一次附魔的条件时保持冷却满值，让客户端进度条停止动画，避免机器空转
        if (!be.canPerformWork()) {
            be.resetCooldownWhenIdle();
            return;
        }

        // 2. 冷却逻辑
        if (be.cooldownTicks > 0) {
            be.cooldownTicks--;
            // 冷却期间每 4 tick 同步一次，降低块实体更新包频率
            if (be.cooldownTicks % 4 == 0) {
                be.syncToClient();
            }
            return;
        }

        // 3. 冷却完毕，按工作模式执行操作
        be.cooldownTicks = AnvilCraft.CONFIG.autoEnchantingTableInterval;
        switch (be.workMode) {
            case ENCHANTING -> be.tryEnchantRandomly(level, pos);
            case PRIMER -> be.tryEnchantWithPrimer(level, pos);
            case LIQUID_ENCHANTMENT -> be.tryLiquidEnchantment(level, pos);
            default -> {}
        }
    }

    /**
     * 设置不工作时将冷却处于满值，让客户端动画回到初始位置。
     */
    private void resetCooldownWhenIdle() {
        if (this.cooldownTicks != AnvilCraft.CONFIG.autoEnchantingTableInterval) {
            this.cooldownTicks = AnvilCraft.CONFIG.autoEnchantingTableInterval;
            this.setChanged();
            this.syncToClient();
        }
    }

    /**
     * 判断当前是否具备执行一次附魔的条件（输入、输出、经验/液态魔咒、书架与选择等级）。
     * 不具备时进度条应保持静止，避免机器因缺少流体等条件而空转。
     */
    private boolean canPerformWork() {
        ItemStack input = this.getItem(SLOT_INPUT);
        if (input.isEmpty()) return false;
        if (!this.getItem(SLOT_OUTPUT).isEmpty()) return false;
        return switch (this.workMode) {
            case ENCHANTING -> {
                int shelfLevel = Math.min(this.shelfLevel, AnvilCraft.CONFIG.autoEnchantingTableMaxBookshelf);
                int cost = Math.min(shelfLevel * EXP_COST_PER_SHELF, FLUID_CAPACITY);
                if (cost <= 0) yield false;
                FluidStack fluid = this.getFluid();
                yield fluid.is(ModFluids.EXP_FLUID) && fluid.getAmount() >= cost;
            }
            case PRIMER -> {
                int totalLevel = this.getSelectedTotalLevel();
                if (this.selectedEnchantments.isEmpty() || totalLevel > this.shelfLevel) yield false;
                int cost = totalLevel * EXP_COST_PER_SHELF;
                if (cost <= 0 || cost > FLUID_CAPACITY) yield false;
                FluidStack fluid = this.getFluid();
                yield fluid.is(ModFluids.EXP_FLUID) && fluid.getAmount() >= cost;
            }
            case LIQUID_ENCHANTMENT -> {
                FluidStack fluid = this.getFluid();
                if (fluid.isEmpty() || !fluid.is(ModFluids.LIQUID_ENCHANTMENT)) yield false;
                Optional<Holder<Enchantment>> enchantment = LiquidEnchantmentUtil.getEnchantment(fluid);
                if (enchantment.isEmpty()) yield false;
                int maxLevel = this.computeLiquidMaxLevel();
                // 与物品不兼容时也会原样输出，视为一次有效工作
                if (maxLevel <= 0) yield true;
                int level = this.liquidEnchantmentLevel;
                if (level <= 0) yield false;
                int cost = 1 << (level - 1);
                yield fluid.getAmount() >= cost;
            }
        };
    }

    public boolean isAllowedPrimer(ItemStack stack) {
        return stack.is(ModItemTags.AUTO_ENCHANTING_TABLE_PRIMERS);
    }

    public void onMenuOpen() {
        this.openMenuCount++;
    }

    public void onMenuClose() {
        if (this.openMenuCount > 0) {
            this.openMenuCount--;
        }
    }

    public boolean hasOpenMenu() {
        return this.openMenuCount > 0;
    }

    private WorkMode refreshWorkMode() {
        // 罐内为非空液态魔咒时即为液态魔咒模式
        if (this.isLiquidEnchantmentMode()) {
            return WorkMode.LIQUID_ENCHANTMENT;
        }
        if (this.workMode == WorkMode.LIQUID_ENCHANTMENT) {
            // 液态魔咒耗尽/更换时清空等级选择
            this.liquidEnchantmentLevel = 0;
            this.syncToClient();
        }
        ItemStack primer = this.getItem(SLOT_PRIMER);
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

    /**
     * 罐内流体是否为（附有魔咒的）液态魔咒。
     */
    public boolean isLiquidEnchantmentMode() {
        FluidStack fluid = this.getFluid();
        return !fluid.isEmpty()
            && fluid.is(ModFluids.LIQUID_ENCHANTMENT)
            && LiquidEnchantmentUtil.isEnchanted(fluid);
    }

    private void refreshShelfLevel(Level level, BlockPos pos) {
        int levelValue = 0;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            BlockPos bookshelfPos = pos.offset(offset);
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                levelValue += (int) level.getBlockState(bookshelfPos).getEnchantPowerBonus(level, bookshelfPos);
            }
        }
        levelValue = Math.min(
            levelValue,
            this.workMode == WorkMode.ENCHANTING ? AnvilCraft.CONFIG.autoEnchantingTableMaxBookshelf : Integer.MAX_VALUE
        );
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

    /**
     * 设置液态魔咒模式下选择的附魔等级（0 表示取消选择），会限制在当前引物允许的最大等级内。
     */
    public void setLiquidLevel(int level) {
        int maxLevel = this.computeLiquidMaxLevel();
        int clamped = Math.clamp(level, 0, maxLevel);
        if (clamped != this.liquidEnchantmentLevel) {
            this.liquidEnchantmentLevel = clamped;
            this.setChanged();
            this.syncToClient();
        }
    }

    /**
     * 根据当前罐内液态魔咒、引物与待附魔物品计算允许选择的最大等级。
     */
    public int computeLiquidMaxLevel() {
        FluidStack fluid = this.getFluid();
        if (fluid.isEmpty() || !fluid.is(ModFluids.LIQUID_ENCHANTMENT)) return 0;
        return LiquidEnchantmentUtil.getEnchantment(fluid)
            .map(enchantmentHolder -> AutoEnchantingTableBlockEntity.computeLiquidMaxLevel(
                this.getItem(SLOT_INPUT),
                this.getItem(SLOT_PRIMER),
                enchantmentHolder
            ))
            .orElse(0);
    }

    /**
     * 引物决定液态魔咒附魔的限制：
     * 无引物（皇家）：与物品或已有附魔冲突时不可选择（返回 0），且不超过原版上限；
     * 余烬铁砧引物：可选择冲突，但不超过原版上限；
     * 超限铁砧引物：可选择冲突，且不超过 15 级。
     */
    public static int computeLiquidMaxLevel(ItemStack item, ItemStack primer, Holder<Enchantment> enchantment) {
        LiquidEnchantRestriction restriction = getRestriction(primer);
        if (restriction == LiquidEnchantRestriction.ROYAL && !isEnchantmentCompatible(item, enchantment)) {
            return 0;
        }
        int vanillaMax = enchantment.value().getMaxLevel();
        return restriction == LiquidEnchantRestriction.OVERLIMIT
            ? AnvilCraft.CONFIG.liquidEnchantmentMaxLevel
            : Math.min(AnvilCraft.CONFIG.liquidEnchantmentMaxLevel, vanillaMax);
    }

    private static LiquidEnchantRestriction getRestriction(ItemStack primer) {
        if (!primer.isEmpty() && primer.is(ModBlocks.EMBER_ANVIL.get().asItem())) {
            return LiquidEnchantRestriction.EMBER;
        }
        if (!primer.isEmpty() && primer.is(ModBlocks.TRANSCENDENCE_ANVIL.get().asItem())) {
            return LiquidEnchantRestriction.OVERLIMIT;
        }
        return LiquidEnchantRestriction.ROYAL;
    }

    private static boolean isEnchantmentCompatible(ItemStack item, Holder<Enchantment> enchantment) {
        if (item.isEmpty()) return true;
        if (!item.supportsEnchantment(enchantment)) return false;
        for (Holder<Enchantment> other : EnchantmentHelper.getEnchantmentsForCrafting(item).keySet()) {
            if (!other.equals(enchantment) && !Enchantment.areCompatible(enchantment, other)) {
                return false;
            }
        }
        return true;
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

    /**
     * 输入物品已有附魔时直接透传到输出栏（不消耗经验流体），返回是否已透传。
     */
    private boolean tryPassThroughEnchantedInput() {
        ItemStack input = this.getItem(SLOT_INPUT);
        if (input.isEmpty()) return false;
        if (!this.getItem(SLOT_OUTPUT).isEmpty()) return false;
        if (!EnchantmentHelper.hasAnyEnchantments(input)) return false;
        this.finishEnchant(input.copy(), 0);
        return true;
    }

    private void tryEnchantRandomly(Level level, BlockPos pos) {
        if (this.tryPassThroughEnchantedInput()) return;
        ItemStack input = this.getItem(SLOT_INPUT);
        if (input.isEmpty()) return;
        // 输出槽被占用时不做
        if (!this.getItem(SLOT_OUTPUT).isEmpty()) return;

        int shelfLevel = Math.min(this.shelfLevel, AnvilCraft.CONFIG.autoEnchantingTableMaxBookshelf);
        int cost = Math.min(shelfLevel * EXP_COST_PER_SHELF, FLUID_CAPACITY);
        if (cost <= 0) return;

        // 经验流体不足则本次附魔取消，等待下一轮
        FluidStack fluid = this.getFluid();
        if (!fluid.is(ModFluids.EXP_FLUID) || fluid.getAmount() < cost) return;

        // 与原版附魔台相同的随机附魔
        var enchantmentTag = level.registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .get(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (enchantmentTag.isEmpty()) return;
        List<EnchantmentInstance> enchants = EnchantmentHelper.selectEnchantment(
            level.getRandom(), input, shelfLevel, enchantmentTag.get().stream());
        if (enchants.isEmpty()) return;

        ItemStack result = input.is(Items.BOOK) ? Items.ENCHANTED_BOOK.getDefaultInstance() : input.copy();
        for (EnchantmentInstance instance : enchants) {
            result.enchant(instance.enchantment(), instance.level());
        }

        this.finishEnchant(result, cost);
        level.playSound(
            null,
            pos,
            ModSoundEvents.AUTO_ENCHANTING_TABLE_USE.get(),
            SoundSource.BLOCKS,
            1.0F,
            level.getRandom().nextFloat() * 0.1F + 0.9F
        );
    }

    private void tryEnchantWithPrimer(Level level, BlockPos pos) {
        if (this.tryPassThroughEnchantedInput()) return;
        ItemStack input = this.getItem(SLOT_INPUT);
        if (input.isEmpty()) return;
        if (!this.getItem(SLOT_OUTPUT).isEmpty()) return;
        if (this.selectedEnchantments.isEmpty()) return;

        // 只有书架足够时才成功附魔（附魔总等级不能超过书架数量）
        int totalLevel = this.getSelectedTotalLevel();
        if (totalLevel > this.shelfLevel) return;

        int cost = totalLevel * EXP_COST_PER_SHELF;
        if (cost <= 0 || cost > FLUID_CAPACITY) return;

        FluidStack fluid = this.getFluid();
        if (!fluid.is(ModFluids.EXP_FLUID) || fluid.getAmount() < cost) return;

        ItemStack result = AutoEnchantingTableBlockEntity.computePrimerEnchantResult(input, this.selectedEnchantments);
        if (result.isEmpty()) return;

        this.finishEnchant(result, cost);
        level.playSound(
            null,
            pos,
            ModSoundEvents.AUTO_ENCHANTING_TABLE_USE.get(),
            SoundSource.BLOCKS,
            1.0F,
            level.getRandom().nextFloat() * 0.1F + 0.9F
        );
    }

    /**
     * 计算引物附魔的产出物品。
     * 书本（书/附魔书）：忽略冲突与不兼容，所有选中附魔都附上。
     * 武器工具等：先生成包含所有选中附魔的虚拟附魔书，再模拟皇家铁砧将其打到物品上，
     * 与已有附魔冲突或不兼容的附魔消失。
     */
    public static ItemStack computePrimerEnchantResult(ItemStack input, List<Holder<Enchantment>> selected) {
        if (input.isEmpty() || selected.isEmpty()) return ItemStack.EMPTY;
        boolean isBook = input.is(Items.BOOK);
        boolean isEnchantedBook = input.is(Items.ENCHANTED_BOOK);
        if (isBook || isEnchantedBook) {
            ItemStack result = isBook ? Items.ENCHANTED_BOOK.getDefaultInstance() : input.copy();
            for (Holder<Enchantment> holder : selected) {
                result.enchant(holder, holder.value().getMaxLevel());
            }
            return result;
        }
        ItemStack virtualBook = Items.ENCHANTED_BOOK.getDefaultInstance();
        for (Holder<Enchantment> holder : selected) {
            virtualBook.enchant(holder, holder.value().getMaxLevel());
        }
        AnvilMenuResult anvilResult = RoyalAnvilMenu.RESULT.get();
        anvilResult.createResult(null, input.copy(), virtualBook, input.getHoverName().getString());
        return anvilResult.result;
    }

    private void tryLiquidEnchantment(Level level, BlockPos pos) {
        FluidStack fluid = this.getFluid();
        if (fluid.isEmpty() || !fluid.is(ModFluids.LIQUID_ENCHANTMENT)) return;
        Optional<Holder<Enchantment>> enchantment = LiquidEnchantmentUtil.getEnchantment(fluid);
        if (enchantment.isEmpty()) return;
        final Holder<Enchantment> holder = enchantment.get();

        // 待附魔物品在输出槽（自动由输入槽移入）
        ItemStack item = this.getItem(SLOT_INPUT);
        if (item.isEmpty()) return;

        int maxLevel = this.computeLiquidMaxLevel();
        if (maxLevel <= 0) {
            // 皇家模式下与物品不兼容：无法附魔，直接将物品原样输出，避免卡在输入槽
            this.finishEnchant(item.copy(), 0);
            return;
        }
        int enchantLevel = this.liquidEnchantmentLevel;
        if (enchantLevel > maxLevel) {
            // 等级超出当前引物限制时降级并同步
            this.liquidEnchantmentLevel = maxLevel;
            this.setChanged();
            this.syncToClient();
            enchantLevel = maxLevel;
        }
        if (enchantLevel <= 0) return;

        // 物品已有所选附魔且不低于所选等级时视为已完成
        int existingLevel = EnchantmentHelper.getEnchantmentsForCrafting(item).getLevel(holder);
        if (existingLevel >= enchantLevel) {
            this.finishEnchant(item.copy(), 0);
            return;
        }

        // 消耗液态魔咒：2^(等级-1) mB，15 级最多 16384 mB
        int cost = 1 << (enchantLevel - 1);
        if (fluid.getAmount() < cost) return;

        ItemStack result = item.copy();
        if (result.is(Items.BOOK)) {
            result = Items.ENCHANTED_BOOK.getDefaultInstance();
        }
        // 直接附魔，无视物品兼容性与已有附魔冲突（类似超限铁砧）
        result.enchant(holder, enchantLevel);

        this.finishEnchant(result, cost);
        level.playSound(
            null,
            pos,
            ModSoundEvents.AUTO_ENCHANTING_TABLE_USE.get(),
            SoundSource.BLOCKS,
            1.0F,
            level.getRandom().nextFloat() * 0.1F + 0.9F
        );
    }

    private void finishEnchant(ItemStack result, int cost) {
        if (cost > 0) {
            try (var transaction = Transaction.openRoot()) {
                if (this.fluidTank.extract(0, this.fluidTank.getResource(0), cost, transaction) != cost) return;
                transaction.commit();
            }
        }
        this.itemHandler.set(SLOT_OUTPUT, ItemResource.of(result), result.getCount());
        this.itemHandler.set(SLOT_INPUT, ItemResource.EMPTY, 0);
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(
                this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.itemHandler.serialize(output.child("Inventory"));
        this.fluidTank.serialize(output.child("FluidTank"));
        output.store("WorkMode", WorkMode.CODEC, this.workMode);
        output.putInt("LiquidEnchantmentLevel", this.liquidEnchantmentLevel);
        output.store("SelectedEnchantments", Identifier.CODEC.listOf(), this.selectedEnchantments.stream()
            .flatMap(holder -> holder.unwrapKey().stream()).map(ResourceKey::identifier).toList());
        output.putInt("CooldownTicks", this.cooldownTicks);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.loading = true;
        try {
            var inventory = input.childOrEmpty("Inventory");
            if (inventory.read("stacks", ItemStack.OPTIONAL_CODEC.listOf()).isPresent()) {
                this.itemHandler.deserialize(inventory);
            } else {
                for (int slot = 0; slot < this.itemHandler.size(); slot++) this.itemHandler.set(slot, ItemResource.EMPTY, 0);
                for (var saved : inventory.listOrEmpty("Items", net.minecraft.world.ItemStackWithSlot.CODEC)) {
                    if (saved.isValidInContainer(this.itemHandler.size())) {
                        this.itemHandler.set(saved.slot(), ItemResource.of(saved.stack()), saved.stack().getCount());
                    }
                }
            }
            var tank = input.childOrEmpty("FluidTank");
            if (tank.read("stacks", FluidStack.OPTIONAL_CODEC.listOf()).isPresent()) {
                this.fluidTank.deserialize(tank);
            } else {
                var fluid = input.read("FluidTank", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
                this.fluidTank.set(0, FluidResource.of(fluid), fluid.getAmount());
            }
            this.shelfLevel = input.getIntOr("ShelfLevel", this.shelfLevel);
            this.workMode = input.read("WorkMode", WorkMode.CODEC).orElse(this.workMode);
            this.liquidEnchantmentLevel = input.getIntOr("LiquidEnchantmentLevel", this.liquidEnchantmentLevel);
            this.selectedEnchantments.clear();
            var registry = input.lookup().lookupOrThrow(Registries.ENCHANTMENT);
            for (Identifier id : input.read("SelectedEnchantments", Identifier.CODEC.listOf()).orElse(List.of())) {
                registry.get(ResourceKey.create(Registries.ENCHANTMENT, id)).ifPresent(this.selectedEnchantments::add);
            }
            this.cooldownTicks = input.getIntOr("CooldownTicks", this.cooldownTicks);
        } finally {
            this.loading = false;
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.saveAdditional(output);
        output.putInt("ShelfLevel", this.shelfLevel);
        return output.buildResult();
    }

    @Override
    public ResourceHandler<FluidResource> getFluidHandler() {
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
        if (this.isLiquidEnchantmentMode()) {
            // 液态魔咒模式：功耗随物品已有附魔条数线性增加
            int enchantmentCount = 0;
            ItemStack item = this.getItem(SLOT_INPUT);
            if (!item.isEmpty()) {
                enchantmentCount = item.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).size()
                    + item.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).size();
            }
            return LIQUID_POWER_CONSUMPTION + LIQUID_POWER_PER_ENCHANTMENT * enchantmentCount;
        }
        return this.isAllowedPrimer(this.getItem(SLOT_PRIMER))
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

    /// 液态魔咒模式下引物决定的附魔限制
    public enum LiquidEnchantRestriction {
        /// 皇家：与物品/已有附魔冲突不可选择，不超过原版上限
        ROYAL,
        /// 余烬：可选择冲突，不超过原版上限
        EMBER,
        /// 超限：可选择冲突，不超过 15 级
        OVERLIMIT,
    }
}
