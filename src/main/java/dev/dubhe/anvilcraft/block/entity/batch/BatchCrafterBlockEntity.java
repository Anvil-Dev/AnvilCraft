package dev.dubhe.anvilcraft.block.entity.batch;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.PollableFilteredItemStackHandler;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.BatchCrafterMenu;
import dev.dubhe.anvilcraft.network.UpdateDisplayItemPacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class BatchCrafterBlockEntity extends BaseBatchCraftingBlockEntity {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private final Deque<BatchCrafterCache> cache = new ArrayDeque<>();

    public BatchCrafterBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState, COUNTER.incrementAndGet());
    }

    @Override
    protected PollableFilteredItemStackHandler constructHandler() {
        return new PollableFilteredItemStackHandler(9) {
            @Override
            public void onContentsChanged(int slot) {
                BatchCrafterBlockEntity.this.onContentsChanged();
            }
        };
    }

    @Override
    protected int getCooldownDuration() {
        return AnvilCraft.CONFIG.batchCrafterCooldown;
    }

    private void onContentsChanged() {
        if (this.level == null) {
            this.setChanged();
            return;
        }
        
        RecipeManager manager = this.level.getRecipeManager();
        Optional<RecipeHolder<CraftingRecipe>> recipeOp = manager.getRecipeFor(
            RecipeType.CRAFTING,
            this.dummyCraftingContainer.asCraftInput(),
            this.level
        );
        this.updateDisplayItem(recipeOp.map(
            recipe -> recipe.value()
                .getResultItem(this.level.registryAccess())
        ).orElse(ItemStack.EMPTY));

        if (!this.level.isClientSide) {
            PacketDistributor.sendToAllPlayers(new UpdateDisplayItemPacket(
                Objects.requireNonNull(this.getDisplayingStack()),
                this.getPos()
            ));
        }
        this.setChanged();
    }

    @Override
    public boolean craft(Level level) {
        if (this.craftingContainer.isEmpty()) return false;
        if (!this.canCraft()) return false;
        ItemStack result;
        
        Optional<BatchCrafterCache> cacheOp = this.cache.stream()
            .filter(recipe -> recipe.test(craftingContainer))
            .findFirst();
        Optional<RecipeHolder<CraftingRecipe>> holderOp;
        List<ItemStack> craftRemaining;
        if (cacheOp.isPresent()) {
            BatchCrafterCache crafterCache = cacheOp.get();
            holderOp = crafterCache.getRecipe();
            craftRemaining = crafterCache.getRemaining();
        } else {
            holderOp = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, this.craftingContainer.asCraftInput(), level);
            NonNullList<ItemStack> remainingItems = level.getRecipeManager()
                .getRemainingItemsFor(RecipeType.CRAFTING, this.craftingContainer.asCraftInput(), level);
            BatchCrafterCache cache = new BatchCrafterCache(this.craftingContainer, holderOp, remainingItems);
            craftRemaining = remainingItems;
            this.cache.push(cache);
            while (this.cache.size() >= 10) {
                this.cache.pop();
            }
        }
        if (holderOp.isEmpty()) return false;
        
        result = holderOp.get().value().assemble(this.craftingContainer.asCraftInput(), level.registryAccess());
        this.displayingStack = result.copy();
        if (!level.isClientSide) PacketDistributor.sendToAllPlayers(new UpdateDisplayItemPacket(this.displayingStack, this.getPos()));
        if (!result.isItemEnabled(level.enabledFeatures())) return false;

        int times = IntStream.range(0, this.handler.getSlots())
            .mapToObj(this.handler::getStackInSlot)
            .filter((s -> !s.isEmpty()))
            .mapToInt(ItemStack::getCount)
            .min()
            .orElse(0);
        if (times < 1) return false;
        result.setCount(result.getCount() * times);
        if (!craftRemaining.isEmpty()) {
            craftRemaining = craftRemaining.stream()
                .map(stack -> stack.copyWithCount(stack.getCount() * times))
                .collect(Collectors.toList());
        }
        if (this.ejectItems(result, craftRemaining, this.getDirection())) return false;
        for (int i = 0; i < this.handler.getSlots(); i++) {
            this.handler.extractItem(i, times, false);
        }
        level.updateNeighborsAt(this.getBlockPos(), ModBlocks.BATCH_CRAFTER.get());
        return true;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new BatchCrafterMenu(ModMenuTypes.BATCH_CRAFTER.get(), i, inventory, this);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class BatchCrafterCache implements Predicate<Container> {
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
        public BatchCrafterCache(
            Container container,
            Optional<RecipeHolder<CraftingRecipe>> recipe,
            NonNullList<ItemStack> remaining
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
        public boolean test(Container container) {
            if (container.getContainerSize() != this.container.getContainerSize()) return false;
            for (int i = 0; i < this.container.getContainerSize(); i++) {
                if (!ItemStack.isSameItemSameComponents(container.getItem(i), this.container.getItem(i))) return false;
            }
            return true;
        }
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
            return BatchCrafterBlockEntity.this.handler.getStacks();
        }

        @Override
        public int getContainerSize() {
            return BatchCrafterBlockEntity.this.handler.getSlots();
        }

        @Override
        public boolean isEmpty() {
            return BatchCrafterBlockEntity.this.handler.getStacks().isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return BatchCrafterBlockEntity.this.handler.getStackInSlot(slot);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = BatchCrafterBlockEntity.this.handler.extractItem(slot, amount, false);
            BatchCrafterBlockEntity.this.setChanged();
            return stack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = BatchCrafterBlockEntity.this.handler.getStackInSlot(slot);
            BatchCrafterBlockEntity.this.handler.setStackInSlot(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            BatchCrafterBlockEntity.this.handler.setStackInSlot(slot, stack);
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
                this.removeItemNoUpdate(i);
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
        public List<ItemStack> getItems() {
            int size = this.getContainerSize();
            List<ItemStack> list = NonNullList.withSize(size, ItemStack.EMPTY);
            for (int i = 0; i < size; i++) {
                list.set(i, this.getItem(i));
            }
            return list;
        }

        @Override
        public int getContainerSize() {
            return BatchCrafterBlockEntity.this.handler.getSlots();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack item : this.getItems()) {
                if (!item.isEmpty()) return false;
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            ItemStack stack = BatchCrafterBlockEntity.this.handler.getStackInSlot(slot);
            if (stack.isEmpty()) stack = BatchCrafterBlockEntity.this.handler.getFilter(slot);
            return stack;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            return this.getItem(slot);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
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
