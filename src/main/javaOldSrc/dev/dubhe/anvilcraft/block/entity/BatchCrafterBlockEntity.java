package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IHasDisplayItem;
import dev.dubhe.anvilcraft.api.depository.FilteredItemDepository;
import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import dev.dubhe.anvilcraft.api.depository.ItemDepositoryHelper;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.BatchCrafterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.BatchCrafterMenu;
import dev.dubhe.anvilcraft.network.UpdateDisplayItemPacket;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Getter
@SuppressWarnings("NullableProblems")
public class BatchCrafterBlockEntity
    extends BaseMachineBlockEntity
    implements IFilterBlockEntity, IPowerConsumer, IDiskCloneable, IHasDisplayItem {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private static final RandomSource RANDOM = RandomSource.create();

    @Getter
    @Setter
    private PowerGrid grid;
    @Getter
    private final int inputPower = 1;
    private final Deque<AutoCrafterCache> cache = new ArrayDeque<>();
    private final FilteredItemDepository depository = new FilteredItemDepository.Pollable(9) {
        @Override
        public void onContentsChanged(int slot) {
            if (level != null) {
                RecipeManager recipeManager = level.getRecipeManager();
                Optional<RecipeHolder<CraftingRecipe>> recipe = recipeManager
                    .getRecipeFor(
                        RecipeType.CRAFTING,
                        dummyCraftingContainer.asCraftInput(),
                        level
                    );
                displayItemStack = recipe
                    .map(craftingRecipe -> craftingRecipe.value().getResultItem(level.registryAccess()))
                    .orElse(ItemStack.EMPTY);
                if (!level.isClientSide) {
                    PacketDistributor.sendToAllPlayers(new UpdateDisplayItemPacket(displayItemStack, getPos()));
                }
            }
            setChanged();
        }
    };
    @Getter
    private ItemStack displayItemStack = null;
    private boolean poweredBefore = false;
    private long lastTimeCrafted = 0;
    @Getter
    private final int id;

    public BatchCrafterBlockEntity(
        BlockEntityType<? extends BlockEntity> type,
        BlockPos pos,
        BlockState blockState
    ) {
        super(type, pos, blockState);
        id = COUNTER.incrementAndGet();
    }

    /**
     * @param level 世界
     * @param pos   位置
     */
    public void tick(@NotNull Level level, BlockPos pos) {
        this.flushState(level, pos);
        BlockState state = level.getBlockState(pos);
        level.updateNeighbourForOutputSignal(pos, state.getBlock());
        boolean powered = state.getValue(BatchCrafterBlock.POWERED);
        if (powered && !poweredBefore && !level.isClientSide) {
            craft(level);
        }
        poweredBefore = powered;
    }

    private boolean canCraft() {
        if (grid == null || !grid.isWork()) return false;
        if (!depository.isFilterEnabled()) return true;
        for (int i = 0; i < depository.getSlots(); i++) {
            if (depository.getStack(i).isEmpty() && !depository.getFilter(i).isEmpty()) return false;
        }
        return true;
    }

    @SuppressWarnings("UnreachableCode")
    private void craft(@NotNull Level level) {
        if (craftingContainer.isEmpty()) return;
        if (!canCraft()) return;
        System.out.println("lastTimeCrafted = " + lastTimeCrafted);
        if (lastTimeCrafted > (level.getGameTime() + AnvilCraft.config.batchCrafterCooldown))
            return;
        lastTimeCrafted = level.getGameTime();
        ItemStack result;
        Optional<AutoCrafterCache> cacheOptional = cache
            .stream()
            .filter(recipe -> recipe.test(craftingContainer))
            .findFirst();
        Optional<RecipeHolder<CraftingRecipe>> optional;
        NonNullList<ItemStack> craftRemaining;
        if (cacheOptional.isPresent()) {
            AutoCrafterCache crafterCache = cacheOptional.get();
            optional = crafterCache.getRecipe();
            craftRemaining = crafterCache.getRemaining();
        } else {
            optional = level.getRecipeManager().getRecipeFor(
                RecipeType.CRAFTING,
                craftingContainer.asCraftInput(),
                level
            );
            craftRemaining = level.getRecipeManager()
                .getRemainingItemsFor(
                    RecipeType.CRAFTING,
                    craftingContainer.asCraftInput(),
                    level
                );
            AutoCrafterCache cache = new AutoCrafterCache(craftingContainer, optional, craftRemaining);
            this.cache.push(cache);
            while (this.cache.size() >= 10) {
                this.cache.pop();
            }
        }
        if (optional.isEmpty()) return;
        result = optional.get().value().assemble(craftingContainer.asCraftInput(), level.registryAccess());
        displayItemStack = result.copy();
        if (!level.isClientSide) {
            PacketDistributor.sendToAllPlayers((new UpdateDisplayItemPacket(displayItemStack, getPos())));
        }
        if (!result.isItemEnabled(level.enabledFeatures())) return;
        int times;
        Optional<ItemStack> minStack = depository.getStacks().stream().filter((s -> !s.isEmpty())).min((s1, s2) -> {
            int a = s1.getCount();
            int b = s2.getCount();
            return Integer.compare(a, b);
        });
        if (minStack.isPresent()) {
            times = minStack.get().getCount();
        } else {
            return;
        }
        result.setCount(result.getCount() * times);
        craftRemaining.forEach(stack -> stack.setCount(stack.getCount() * times));
        IItemDepository itemDepository = ItemDepositoryHelper.getItemDepository(
            level, getBlockPos().relative(getDirection()), getDirection().getOpposite()
        );
        if (itemDepository != null) {
            // 尝试向容器插入物品
            ItemStack remained = ItemDepositoryHelper.insertItem(itemDepository, result, true);
            if (!remained.isEmpty()) return;
            remained = ItemDepositoryHelper.insertItem(itemDepository, result, false);
            spawnItemEntity(remained);
            for (ItemStack stack : craftRemaining) {
                remained = ItemDepositoryHelper.insertItem(itemDepository, stack, false);
                spawnItemEntity(remained);
            }
        } else {
            // 尝试向世界喷出物品
            Vec3 center = getBlockPos().relative(getDirection()).getCenter();
            AABB aabb = new AABB(center.add(-0.125, -0.125, -0.125), center.add(0.125, 0.125, 0.125));
            if (!getLevel().noCollision(aabb)) return;

            spawnItemEntity(result);
            for (ItemStack stack : craftRemaining) {
                spawnItemEntity(stack);
            }
        }
        for (int i = 0; i < depository.getSlots(); i++) {
            depository.extract(i, times, false);
        }
        level.updateNeighborsAt(getBlockPos(), ModBlocks.BATCH_CRAFTER.get());
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return this.saveWithoutMetadata(provider);
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        depository.deserializeNbt(provider, tag.getCompound("Inventory"));
        if (tag.contains("ResultItemStack")) {
            displayItemStack = ItemStack.parse(provider, tag.getCompound("ResultItemStack")).orElse(ItemStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Inventory", this.depository.serializeNbt(provider));
        if (displayItemStack == null) displayItemStack = ItemStack.EMPTY;
        CompoundTag item = new CompoundTag();
        this.displayItemStack.save(provider, item);
        tag.put("ResultItemStack", item);
    }

    @Override
    public Direction getDirection() {
        if (this.level == null) return Direction.UP;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (state.is(ModBlocks.BATCH_CRAFTER.get())) return state.getValue(BatchCrafterBlock.FACING);
        return Direction.UP;
    }

    @Override
    public void setDirection(Direction direction) {
        BlockPos pos = this.getBlockPos();
        Level level = this.getLevel();
        if (null == level) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.BATCH_CRAFTER.get())) return;
        level.setBlockAndUpdate(pos, state.setValue(BatchCrafterBlock.FACING, direction));
    }

    /**
     * 获取红石信号强度
     *
     * @return 红石信号强度
     */
    public int getRedstoneSignal() {
        int strength = 0;
        for (int index = 0; index < depository.getSlots(); index++) {
            ItemStack itemStack = depository.getStack(index);
            // 槽位为未设置过滤的已禁用槽位
            if (depository.isSlotDisabled(index) && depository.getFilter(index).isEmpty()) {
                strength++;
                continue;
            }
            // 槽位上没有物品
            if (itemStack.isEmpty()) {
                continue;
            }
            strength++;
        }
        return strength;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new BatchCrafterMenu(ModMenuTypes.BATCH_CRAFTER.get(), i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.batch_crafter");
    }

    @Override
    public FilteredItemDepository getFilteredItemDepository() {
        return this.depository;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public void storeDiskData(CompoundTag tag) {
        tag.put("Filtering", depository.serializeFiltering());
    }

    @Override
    public void applyDiskData(CompoundTag data) {
        depository.deserializeFiltering(data.getCompound("Filtering"));
    }

    @Override
    public void updateDisplayItem(ItemStack stack) {
        this.displayItemStack = stack;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class AutoCrafterCache implements Predicate<Container> {
        private final Container container;
        @Getter
        private final Optional<RecipeHolder<CraftingRecipe>> recipe;
        @Getter
        private final NonNullList<ItemStack> remaining;

        /**
         * 合成器缓存
         *
         * @param container 容器
         * @param recipe    配方
         * @param remaining 返还物品
         */
        public AutoCrafterCache(
            @NotNull Container container, Optional<RecipeHolder<CraftingRecipe>> recipe, NonNullList<ItemStack> remaining
        ) {
            this.container = new SimpleContainer(container.getContainerSize());
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack item = container.getItem(i).copy();
                item.setCount(1);
                this.container.setItem(i, item);
            }
            this.recipe = recipe;
            this.remaining = remaining;
        }

        @Override
        public boolean test(@NotNull Container container) {
            if (container.getContainerSize() != this.container.getContainerSize()) return false;
            for (int i = 0; i < this.container.getContainerSize(); i++) {
                if (!ItemStack.isSameItemSameComponents(container.getItem(i), this.container.getItem(i))) return false;
            }
            return true;
        }
    }

    private void spawnItemEntity0(ItemStack stack) {
        Vec3 center = getBlockPos().relative(getDirection()).getCenter();
        Vector3f step = getDirection().step();
        Level level = this.getLevel();
        if (level == null) return;
        ItemEntity itemEntity = new ItemEntity(
            level, center.x, center.y, center.z,
            stack,
            0.25 * step.x, 0.25 * step.y, 0.25 * step.z
        );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    private void spawnItemEntity(@NotNull ItemStack stack) {
        int maxStackSize = stack.getMaxStackSize();
        int stackSize = stack.getCount();
        for (; stackSize > maxStackSize; stackSize -= maxStackSize) {
            spawnItemEntity0(stack.copyWithCount(maxStackSize));
        }
        if (stackSize != 0) {
            spawnItemEntity0(stack.copyWithCount(stackSize));
        }
    }

    @Override
    public Level getCurrentLevel() {
        return this.getLevel();
    }

    @Getter
    private final CraftingContainer craftingContainer = new CraftingContainer() {
        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }

        @Override
        public List<ItemStack> getItems() {
            return depository.getStacks();
        }

        @Override
        public int getContainerSize() {
            return depository.getSlots();
        }

        @Override
        public boolean isEmpty() {
            return depository.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return depository.getStack(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = depository.extract(slot, amount, false, true);
            BatchCrafterBlockEntity.this.setChanged();
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            return depository.clearItem(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            depository.setStack(slot, stack);
        }

        @Override
        public void setChanged() {
            BatchCrafterBlockEntity.this.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < this.getContainerSize(); i++) {
                depository.clearItem(i);
            }
        }

        @Override
        public void fillStackedContents(StackedContents contents) {
            for (int i = 0; i < this.getContainerSize(); i++) {
                ItemStack itemStack = this.getItem(i);
                contents.accountSimpleStack(itemStack);
            }
        }
    };

    private final CraftingContainer dummyCraftingContainer = new CraftingContainer() {
        @Override
        public int getWidth() {
            return 3;
        }

        @Override
        public int getHeight() {
            return 3;
        }

        @Override
        public @NotNull List<ItemStack> getItems() {
            int size = this.getContainerSize();
            List<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);
            for (int i = 0; i < size; i++) {
                list.set(i, this.getItem(i));
            }
            return list;
        }

        @Override
        public int getContainerSize() {
            return depository.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack item : this.getItems()) {
                if (!item.isEmpty()) return false;
            }
            return true;
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            ItemStack stack = depository.getStack(slot);
            if (stack.isEmpty()) stack = depository.getFilter(slot);
            return stack;
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            return this.getItem(slot);
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            return this.getItem(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
        }

        @Override
        public void setChanged() {
            BatchCrafterBlockEntity.this.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
        }

        @Override
        public void fillStackedContents(StackedContents contents) {
            for (int i = 0; i < this.getContainerSize(); i++) {
                ItemStack itemStack = this.getItem(i);
                contents.accountSimpleStack(itemStack);
            }
        }
    };
}
