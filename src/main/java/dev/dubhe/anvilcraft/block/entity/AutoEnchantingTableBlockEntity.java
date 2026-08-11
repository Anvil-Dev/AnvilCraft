package dev.dubhe.anvilcraft.block.entity;

import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.workstation.AutoEnchantingTableBlock;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
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
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class AutoEnchantingTableBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, IPowerConsumer {
    @Getter
    @Setter
    @Nullable
    private PowerGrid grid;
    /// 索引0：物品输入 1：物品输出 2：引物
    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    @Getter
    private final Set<Integer> selectedEnchantmentSet = new IntArraySet();
    @Getter
    @Setter
    private ItemStack lastPrologueItem = ItemStack.EMPTY;
    @Getter
    private Runnable prologueSlotUpdateListener = () -> {};
    @Getter
    @Setter
    private Consumer<Container> slotChangedListener = (_) -> {};
    @Setter
    private boolean isOpenMenu = false;

    private int enchantmentSeed = 0;
    private final RandomSource random = RandomSource.create();

    @Getter
    private final FluidStacksResourceHandler fluidHandler = new FluidStacksResourceHandler(1, 32_000) {
        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.is(ModFluids.EXP_FLUID);
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            AutoEnchantingTableBlockEntity.this.onChange();
        }
    };

    private int cooldown = 0;
    // region 控制书本的渲染
    public int time;
    public float rot;
    public float oldRot;
    public float targetRot;
    public float open;
    public float oldOpen;
    public float flip;
    public float oldFlip;
    public float flipT;
    public float flipA;
    // endregion

    public AutoEnchantingTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public ItemStack getDisplayInputItem() {
        return this.items.getFirst();
    }

    public ItemStack getDisplayOutputItem() {
        return this.items.get(1);
    }

    public void bookAnimationTick(
        final Level level,
        final BlockPos worldPosition,
        final BlockState state
    ) {
        this.oldOpen = this.open;
        this.oldRot = this.rot;
        this.targetRot += 0.02F;

        if (!this.getItem(0).isEmpty()) {
            this.open += 0.1f;
            if (this.open < 0.5f || this.random.nextInt(40) == 0) {
                float old = this.flipT;

                do {
                    this.flipT = this.flipT + (this.random.nextInt(4) - this.random.nextInt(4));
                } while (old == this.flipT);
            }
        } else {
            this.open -= 0.1f;
        }

        while (this.rot >= (float) Math.PI) {
            this.rot -= (float) (Math.PI * 2);
        }

        while (this.rot < (float) -Math.PI) {
            this.rot += (float) (Math.PI * 2);
        }

        while (this.targetRot >= (float) Math.PI) {
            this.targetRot -= (float) (Math.PI * 2);
        }

        while (this.targetRot < (float) -Math.PI) {
            this.targetRot += (float) (Math.PI * 2);
        }

        float rotDir = this.targetRot - this.rot;

        while (rotDir >= (float) Math.PI) {
            rotDir -= (float) (Math.PI * 2);
        }

        while (rotDir < (float) -Math.PI) {
            rotDir += (float) (Math.PI * 2);
        }

        this.rot += rotDir * 0.4F;
        this.open = Mth.clamp(this.open, 0, 1f);
        this.time++;
        this.oldFlip = this.flip;
        float diff = (this.flipT - this.flip) * 0.4f;
        diff = Mth.clamp(diff, -0.2f, 0.2f);
        this.flipA = this.flipA + (diff - this.flipA) * 0.9f;
        this.flip = this.flip + this.flipA;
    }

    public void serverTick(
        final Level level,
        final BlockPos pos,
        final BlockState state
    ) {
        if (this.grid == null) {
            level.setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(AutoEnchantingTableBlock.OVERLOAD, true));
            return;
        }
        if (this.grid.isWorking()) {
            if (this.getBlockState().getValue(AutoEnchantingTableBlock.OVERLOAD)) {
                level.setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(AutoEnchantingTableBlock.OVERLOAD, false));
            }
        } else {
            if (!this.getBlockState().getValue(AutoEnchantingTableBlock.OVERLOAD)) {
                level.setBlockAndUpdate(this.getBlockPos(), this.getBlockState().setValue(AutoEnchantingTableBlock.OVERLOAD, true));
            }
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
        } else if (this.cooldown == 0) {
            this.cooldown = 80;
            if (!this.getItem(1).isEmpty() || this.getItem(0).isEmpty()) {
                return;
            }
            ItemStack enchantItem = this.getItem(0).copyWithCount(1);
            if (!enchantItem.isEnchantable()) {
                this.setItem(0, ItemStack.EMPTY);
                this.setItem(1, enchantItem);
                return;
            }
            ItemStack prologueItem = this.getItem(2);
            if (prologueItem.isEmpty()) {
                this.lastPrologueItem = ItemStack.EMPTY;
                // 无引物模式
                int exp = Mth.clamp(this.getBookShelf(level, pos) * 400, 0, 6000);
                int[][] costAndEnchant = this.getCostAndEnchant(enchantItem);
                if (costAndEnchant.length == 0) {
                    return;
                }
                int index = level.getRandom().nextInt(0, 3);
                if (exp <= 0) {
                    exp = (7 + 2 * index) * 20;
                }
                if (this.fluidHandler.getAmountAsInt(0) < exp) {
                    return;
                }
                int[] enchant = costAndEnchant[index];
                List<EnchantmentInstance> enchantmentList = this.getEnchantmentList(level.registryAccess(), enchantItem, index, enchant[0]);
                if (enchantItem.is(Items.BOOK)) {
                    enchantItem = enchantItem.transmuteCopy(Items.ENCHANTED_BOOK, 1);
                }
                if (!enchantmentList.isEmpty()) {
                    for (EnchantmentInstance enchantmentInstance : enchantmentList) {
                        enchantItem.enchant(enchantmentInstance.enchantment(), enchantmentInstance.level());
                    }
                }
                this.setItem(0, ItemStack.EMPTY);
                this.setItem(1, enchantItem);
                try (Transaction ts = Transaction.openRoot()) {
                    this.fluidHandler.extract(FluidResource.of(ModFluids.EXP_FLUID), exp, ts);
                    ts.commit();
                }
                this.enchantmentSeed = level.getRandom().nextInt();
                level.playSound(
                    null,
                    this.getBlockPos(),
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.BLOCKS,
                    1.0F,
                    level.getRandom().nextFloat() * 0.1F + 0.9F
                );
            } else {
                if (this.isOpenMenu) {
                    return;
                }
                if (this.selectedEnchantmentSet.isEmpty()) {
                    return;
                }
                IdMap<Holder<Enchantment>> idMap = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
                int totalLevel = 0;
                for (int id : this.getSelectedEnchantmentSet()) {
                    Holder<Enchantment> enchantmentHolder = idMap.byId(id);
                    if (enchantmentHolder != null) {
                        totalLevel += enchantmentHolder.value().getMaxLevel();
                    }
                }
                if (totalLevel > this.getBookShelf(level, pos)
                    || totalLevel * 400 > this.fluidHandler.getAmountAsInt(0)) {
                    return;
                }
                ItemStack enchantedBook = Items.ENCHANTED_BOOK.getDefaultInstance();
                for (int id : this.selectedEnchantmentSet) {
                    Holder<Enchantment> enchantmentHolder = idMap.byId(id);
                    if (enchantmentHolder == null) {
                        continue;
                    }
                    boolean compatible = true;
                    for (Object2IntMap.Entry<Holder<Enchantment>> existing
                        : EnchantmentHelper.getEnchantmentsForCrafting(enchantedBook).entrySet()) {
                        if (!Enchantment.areCompatible(enchantmentHolder, existing.getKey())) {
                            compatible = false;
                            break;
                        }
                    }
                    if (!compatible) {
                        continue;
                    }
                    enchantedBook.enchant(enchantmentHolder, enchantmentHolder.value().getMaxLevel());
                }
                if (enchantItem.is(Items.BOOK)) {
                    this.setItem(0, ItemStack.EMPTY);
                    this.setItem(1, enchantedBook);
                } else {
                    AutoEnchantingTableBlockEntity.applyEnchantment(enchantItem, enchantedBook);
                    this.setItem(0, ItemStack.EMPTY);
                    this.setItem(1, enchantItem);
                }
                try (Transaction ts = Transaction.openRoot()) {
                    this.fluidHandler.extract(FluidResource.of(ModFluids.EXP_FLUID), totalLevel * 400, ts);
                    ts.commit();
                }
                level.playSound(
                    null,
                    this.getBlockPos(),
                    SoundEvents.ENCHANTMENT_TABLE_USE,
                    SoundSource.BLOCKS,
                    1.0F,
                    level.getRandom().nextFloat() * 0.1F + 0.9F
                );
            }
        }
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        try (Transaction transaction = Transaction.openRoot()) {
            boolean success = FluidUtil.interactWithFluidHandler(player, hand, this.getBlockPos(), this.getFluidHandler(), transaction);
            if (success) transaction.commit();
            return success;
        }
    }

    public static void applyEnchantment(ItemStack item, ItemStack enchantedBook) {
        ItemEnchantments enchantmentsOnRight = EnchantmentHelper.getEnchantmentsForCrafting(enchantedBook);
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantmentsOnRight.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            if (!item.supportsEnchantment(holder)) {
                continue;
            }
            boolean compatible = true;
            for (Object2IntMap.Entry<Holder<Enchantment>> existing : EnchantmentHelper.getEnchantmentsForCrafting(item).entrySet()) {
                if (!Enchantment.areCompatible(holder, existing.getKey())) {
                    compatible = false;
                    break;
                }
            }
            if (!compatible) {
                continue;
            }
            item.enchant(holder, entry.getIntValue());
        }
    }

    private int getBookShelf(Level level, BlockPos pos) {
        float bookcases = 0;
        for (BlockPos offset : AutoEnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                bookcases += level.getBlockState(pos.offset(offset)).getEnchantPowerBonus(level, pos.offset(offset));
            }
        }
        return (int) bookcases;
    }

    private int[][] getCostAndEnchant(ItemStack itemStack) {
        int[] costs = new int[3];
        int[] enchantClue = new int[]{-1, -1, -1};
        int[] levelClue = new int[]{-1, -1, -1};
        if (itemStack.isEmpty() || !itemStack.isEnchantable()) {
            return new int[][]{};
        }
        if (this.level != null) {
            final IdMap<Holder<Enchantment>> idMap = this.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();
            float bookcases = this.getBookShelf(this.level, this.getBlockPos());
            if (this.enchantmentSeed == 0) {
                this.enchantmentSeed = this.level.getRandom().nextInt();
            }
            this.random.setSeed(this.enchantmentSeed);
            for (int ixx = 0; ixx < 3; ixx++) {
                costs[ixx] = EnchantmentHelper.getEnchantmentCost(this.random, ixx, (int) bookcases, itemStack);
                if (costs[ixx] < ixx + 1) {
                    costs[ixx] = 0;
                }
            }

            for (int ix = 0; ix < 3; ix++) {
                if (costs[ix] > 0) {
                    List<EnchantmentInstance> list = this.getEnchantmentList(this.level.registryAccess(), itemStack, ix, costs[ix]);
                    if (!list.isEmpty()) {
                        EnchantmentInstance enchant = list.get(this.random.nextInt(list.size()));
                        enchantClue[ix] = idMap.getId(enchant.enchantment());
                        levelClue[ix] = enchant.level();
                    }
                }
            }
            return new int[][]{
                new int[] { costs[0], enchantClue[0], levelClue[0] },
                new int[] { costs[1], enchantClue[1], levelClue[1] },
                new int[] { costs[2], enchantClue[2], levelClue[2] }
            };
        }
        return new int[][]{};
    }

    private List<EnchantmentInstance> getEnchantmentList(
        final RegistryAccess access,
        final ItemStack itemStack,
        final int slot,
        final int enchantmentCost
    ) {
        this.random.setSeed(this.enchantmentSeed + slot);
        Optional<HolderSet.Named<Enchantment>> tag = access.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (tag.isEmpty()) {
            return List.of();
        }

        List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(this.random, itemStack, enchantmentCost, tag.get().stream());
        if (itemStack.is(Items.BOOK) && list.size() > 1) {
            list.remove(this.random.nextInt(list.size()));
        }

        return list;
    }

    public void registerUpdateListener(final Runnable prologueSlotUpdateListener) {
        this.prologueSlotUpdateListener = prologueSlotUpdateListener;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        Level level = this.level;
        if (level != null) {
            for (ItemStack stack : this.items) {
                if (stack.isEmpty()) {
                    continue;
                }
                Block.popResource(level, pos, stack);
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        this.slotChangedListener.accept(this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        this.fluidHandler.serialize(output);
        output.putInt("cooldown", this.cooldown);
        output.store("LastPrologueItem", ItemStack.OPTIONAL_CODEC, this.lastPrologueItem);
        ValueOutput.TypedOutputList<Integer> selectedEnchantments = output.list("selectedEnchantments", Codec.INT);
        for (Integer id : this.selectedEnchantmentSet) {
            selectedEnchantments.add(id);
        }
        output.putBoolean("openMenu", this.isOpenMenu);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Collections.fill(this.items, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.fluidHandler.deserialize(input);
        input.getInt("cooldown").ifPresent((cooldown) -> this.cooldown = cooldown);
        input.read("LastPrologueItem", ItemStack.OPTIONAL_CODEC).ifPresent(this::setLastPrologueItem);
        this.selectedEnchantmentSet.clear();
        for (Integer id : input.listOrEmpty("selectedEnchantments", Codec.INT)) {
            this.selectedEnchantmentSet.add(id);
        }
        this.isOpenMenu = input.getBooleanOr("openMenu", false);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(new ProblemReporter.Collector(this.problemPath()), registries);
        this.saveAdditional(output);
        return output.buildResult();
    }

    public void onChange() {
        this.setChanged();
        if (this.level != null) {
            if (this.level instanceof ServerLevel serverLevel) {
                Packet<ClientGamePacketListener> packet = this.getUpdatePacket();
                if (packet != null) {
                    serverLevel.getServer().getPlayerList().broadcast(
                        null,
                        this.getBlockPos().getX() + 0.5,
                        this.getBlockPos().getY() + 0.5,
                        this.getBlockPos().getZ() + 0.5,
                        64,
                        serverLevel.dimension(),
                        packet
                    );
                }
            }
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 2);
        }
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.anvilcraft.auto_enchanting_table");
    }

    @Override
    public NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public void setItem(int slot, ItemStack itemStack) {
        this.items.set(slot, itemStack.copyWithCount(1));
        this.onChange();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack itemStack) {
        return 1;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AutoEnchantingTableMenu(ModMenuTypes.AUTO_ENCHANTING_TABLE.get(), containerId, inventory, this);
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return new int[] { 0, 1 };
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack itemStack, @Nullable Direction direction) {
        return slot == 0 && this.items.getFirst().isEmpty();
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack, Direction direction) {
        return slot == 1 && !this.items.get(1).isEmpty();
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
        return this.items.get(2).isEmpty() ? 16 : 64;
    }
}
