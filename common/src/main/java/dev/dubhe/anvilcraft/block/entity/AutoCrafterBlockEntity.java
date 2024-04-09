package dev.dubhe.anvilcraft.block.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.dubhe.anvilcraft.api.depository.FilteredItemDepository;
import dev.dubhe.anvilcraft.api.depository.IItemDepository;
import dev.dubhe.anvilcraft.api.depository.ItemDepositoryHelper;
import dev.dubhe.anvilcraft.block.AutoCrafterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Getter
@SuppressWarnings("NullableProblems")
public class AutoCrafterBlockEntity extends BaseMachineBlockEntity implements IFilterBlockEntity {
    private final Deque<AutoCrafterCache> cache = new ArrayDeque<>();
    private final FilteredItemDepository depository = new FilteredItemDepository.Pollable(9);
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
            AutoCrafterBlockEntity.this.setChanged();
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
            AutoCrafterBlockEntity.this.setChanged();
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

    public AutoCrafterBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @ExpectPlatform
    public static AutoCrafterBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void onBlockEntityRegister(BlockEntityType<AutoCrafterBlockEntity> type) {
        throw new AssertionError();
    }

    public void tick(@NotNull Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getValue(AutoCrafterBlock.LIT)) craft(level);
    }

    private void craft(@NotNull Level level) {
        ItemStack itemStack;
        if (craftingContainer.isEmpty()) return;
        Optional<AutoCrafterCache> cacheOptional = cache.stream().filter(recipe -> recipe.test(craftingContainer)).findFirst();
        Optional<CraftingRecipe> optional;
        NonNullList<ItemStack> remaining;
        if (cacheOptional.isPresent()) {
            AutoCrafterCache crafterCache = cacheOptional.get();
            optional = crafterCache.getRecipe();
            remaining = crafterCache.getRemaining();
        } else {
            optional = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);
            remaining = level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, craftingContainer, level);
            AutoCrafterCache cache = new AutoCrafterCache(craftingContainer, optional, remaining);
            this.cache.push(cache);
            while (this.cache.size() >= 10) this.cache.pop();
        }
        if (optional.isEmpty()) return;
        itemStack = optional.get().assemble(craftingContainer, level.registryAccess());
        if (!itemStack.isItemEnabled(level.enabledFeatures())) return;
        Container result = new SimpleContainer(1);
        result.setItem(0, itemStack);
        Direction direction = getDirection();
        BlockPos pos = getBlockPos();
        IItemDepository itemDepository = ItemDepositoryHelper.getItemDepository(level, pos.relative(direction), direction.getOpposite());
        if (!outputItem(itemDepository, direction, level, pos, result, 0, false, false, true, false)) {
            return;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = this.getDepository().getStack(i);
            stack.shrink(1);
            this.getDepository().setStack(i, stack);
        }
        Container container1 = new SimpleContainer(remaining.size());
        for (int i = 0; i < remaining.size(); i++) container1.setItem(i, remaining.get(i));
        for (int i = 0; i < remaining.size(); i++) {
            if (container1.getItem(i).isEmpty()) continue;
            outputItem(itemDepository, getDirection(), level, getBlockPos(), container1, i, true, true, true, false);
        }
        level.updateNeighborsAt(pos, ModBlocks.AUTO_CRAFTER.get());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        depository.deserializeNBT(tag.getCompound("Inventory"));
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", this.depository.serializeNBT());
    }

    @Override
    public Direction getDirection() {
        if (this.level == null) return Direction.UP;
        BlockState state = this.level.getBlockState(this.getBlockPos());
        if (state.is(ModBlocks.AUTO_CRAFTER.get())) return state.getValue(AutoCrafterBlock.FACING);
        return Direction.UP;
    }

    @Override
    public void setDirection(Direction direction) {
        BlockPos pos = this.getBlockPos();
        Level level = this.getLevel();
        if (null == level) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.AUTO_CRAFTER.get())) return;
        level.setBlockAndUpdate(pos, state.setValue(AutoCrafterBlock.FACING, direction));
    }

    public int getRedstoneSignal() {
        int i = 0;
        for (int j = 0; j < depository.getSlots(); ++j) {
            ItemStack itemStack = depository.getStack(j);
            if (itemStack.isEmpty() && !depository.isSlotDisabled(j)) continue;
            ++i;
        }
        return i;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, @NotNull Inventory inventory, @NotNull Player player) {
        return new AutoCrafterMenu(ModMenuTypes.AUTO_CRAFTER.get(), i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.anvilcraft.auto_crafter");
    }

    @Override
    public FilteredItemDepository getFilteredItemDepository() {
        return this.depository;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static class AutoCrafterCache implements Predicate<Container> {
        private final Container container;
        @Getter
        private final Optional<CraftingRecipe> recipe;
        @Getter
        private final NonNullList<ItemStack> remaining;

        public AutoCrafterCache(@NotNull Container container, Optional<CraftingRecipe> recipe, NonNullList<ItemStack> remaining) {
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
                if (!ItemStack.isSameItemSameTags(container.getItem(i), this.container.getItem(i))) return false;
            }
            return true;
        }
    }
}
