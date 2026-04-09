package dev.dubhe.anvilcraft.block.entity.batch;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.PollableFilteredItemStackHandler;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.BatchCutterMenu;
import dev.dubhe.anvilcraft.network.BatchCutterSelectPacket;
import dev.dubhe.anvilcraft.network.UpdateDisplayItemPacket;
import lombok.AccessLevel;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;

@Getter
public class BatchCutterBlockEntity extends BaseBatchCraftingBlockEntity {
    private static final AtomicInteger COUNTER = new AtomicInteger(0);
    private final Deque<BatchCutterCache> cache = new ArrayDeque<>();
    private int selecting = 0;

    public BatchCutterBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState, COUNTER.incrementAndGet());
    }

    @Override
    protected PollableFilteredItemStackHandler constructHandler() {
        return new PollableFilteredItemStackHandler(1) {
            @Override
            public void onContentsChanged(int slot) {
                BatchCutterBlockEntity.this.onContentsChanged();
            }
        };
    }

    @Override
    protected int getCooldownDuration() {
        return AnvilCraft.CONFIG.batchCutterCooldown;
    }

    private void onContentsChanged() {
        if (this.level == null) {
            this.setChanged();
            return;
        }
        
        RecipeManager manager = this.level.getRecipeManager();
        List<RecipeHolder<StonecutterRecipe>> recipes = manager.getRecipesFor(RecipeType.STONECUTTING, this.createDummyInput(), this.level);
        if (recipes.isEmpty()) {
            this.updateDisplayItem(ItemStack.EMPTY);
        } else if (this.selecting >= recipes.size()) {
            this.selecting = 0;
            if (!this.level.isClientSide) {
                PacketDistributor.sendToAllPlayers(new BatchCutterSelectPacket(
                    this.selecting,
                    this.getPos()
                ));
            }
            this.updateDisplayItem(recipes.get(this.selecting).value().getResultItem(this.level.registryAccess()));
        } else {
            this.updateDisplayItem(recipes.get(this.selecting).value().getResultItem(this.level.registryAccess()));
        }

        if (!this.level.isClientSide) {
            PacketDistributor.sendToAllPlayers(new UpdateDisplayItemPacket(
                this.getDisplayingStack(),
                this.getPos()
            ));
        }
        this.setChanged();
    }

    @Override
    public boolean craft(Level level) {
        if (this.handler.isEmpty()) return false;
        if (!this.canCraft()) return false;
        
        BatchCutterCache cache = this.findCache();
        List<RecipeHolder<StonecutterRecipe>> recipes = cache.getRecipes();
        if (recipes.isEmpty()) return false;

        if (this.selecting >= recipes.size()) {
            this.selecting = 0;
            if (!level.isClientSide) {
                PacketDistributor.sendToAllPlayers(new BatchCutterSelectPacket(
                    this.selecting,
                    this.getPos()
                ));
            }
        }
        ItemStack result = recipes.get(this.selecting).value().assemble(this.createInput(), level.registryAccess());
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
        List<ItemStack> craftRemaining = new ArrayList<>(cache.getRemaining().size());
        for (ItemStack stack : cache.getRemaining()) {
            craftRemaining.add(stack.copyWithCount(stack.getCount() * times));
        }
        if (this.ejectItems(result, craftRemaining, this.getDirection())) return false;
        for (int i = 0; i < this.handler.getSlots(); i++) {
            this.handler.extractItem(i, times, false);
        }
        level.updateNeighborsAt(this.getBlockPos(), ModBlocks.BATCH_CUTTER.get());
        return true;
    }

    private BatchCutterCache findCache() {
        Optional<BatchCutterCache> cacheOp = this.cache.stream()
            .filter(recipe -> recipe.test(this.handler))
            .findFirst();
        if (cacheOp.isPresent()) {
            return cacheOp.get();
        } else {
            SingleRecipeInput input = this.createInput();
            List<RecipeHolder<StonecutterRecipe>> recipes = this.level.getRecipeManager()
                .getRecipesFor(RecipeType.STONECUTTING, input, this.level);
            NonNullList<ItemStack> remainingItems = this.level.getRecipeManager()
                .getRemainingItemsFor(RecipeType.STONECUTTING, input, this.level);
            BatchCutterCache cache = new BatchCutterCache(this.handler, recipes, remainingItems);
            this.cache.push(cache);
            while (this.cache.size() >= 10) {
                this.cache.pop();
            }
            return cache;
        }
    }
    
    public SingleRecipeInput createInput() {
        return new SingleRecipeInput(this.handler.getStackInSlot(0));
    }

    public SingleRecipeInput createDummyInput() {
        ItemStack stack = this.handler.getStackInSlot(0);
        if (stack.isEmpty()) stack = this.handler.getFilter(0);
        return new SingleRecipeInput(stack);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new BatchCutterMenu(ModMenuTypes.BATCH_CUTTER.get(), i, inventory, this);
    }

    public void setSelecting(int selecting) {
        this.selecting = selecting;

        List<RecipeHolder<StonecutterRecipe>> recipes = this.findCache().getRecipes();
        if (recipes.isEmpty()) {
            this.updateDisplayItem(ItemStack.EMPTY);
        } else if (this.selecting >= recipes.size()) {
            this.selecting = 0;
            if (!this.level.isClientSide) {
                PacketDistributor.sendToAllPlayers(new BatchCutterSelectPacket(
                    this.selecting,
                    this.getPos()
                ));
            }
            this.updateDisplayItem(recipes.get(this.selecting).value().getResultItem(this.level.registryAccess()));
            return;
        } else {
            this.updateDisplayItem(recipes.get(this.selecting).value().getResultItem(this.level.registryAccess()));
        }

        if (!this.level.isClientSide) {
            PacketDistributor.sendToAllPlayers(new BatchCutterSelectPacket(this.selecting, this.getPos()));
        }
    }

    @Getter
    public static class BatchCutterCache implements Predicate<PollableFilteredItemStackHandler> {
        @Getter(AccessLevel.NONE)
        private final Container container;
        private final List<RecipeHolder<StonecutterRecipe>> recipes;
        private final NonNullList<ItemStack> remaining;

        /**
         * 合成器缓存
         *
         * @param container 容器
         * @param recipes    配方
         * @param remaining 返还物品
         */
        public BatchCutterCache(
            PollableFilteredItemStackHandler container,
            List<RecipeHolder<StonecutterRecipe>> recipes,
            NonNullList<ItemStack> remaining
        ) {
            this.container = new SimpleContainer(container.getSlots());
            for (int i = 0; i < container.getSlots(); i++) {
                ItemStack item = container.getStackInSlot(i).copy();
                item.setCount(1);
                this.container.setItem(i, item);
            }
            this.recipes = recipes;
            this.remaining = remaining;
        }

        @Override
        public boolean test(PollableFilteredItemStackHandler container) {
            if (container.getSlots() != this.container.getContainerSize()) return false;
            for (int i = 0; i < this.container.getContainerSize(); i++) {
                if (!ItemStack.isSameItemSameComponents(container.getStackInSlot(i), this.container.getItem(i))) return false;
            }
            return true;
        }
    }
}
