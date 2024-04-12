package dev.dubhe.anvilcraft.block.entity;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.dubhe.anvilcraft.AnvilCraft;
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
import net.minecraft.world.entity.item.ItemEntity;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@Getter
@SuppressWarnings("NullableProblems")
public class AutoCrafterBlockEntity extends BaseMachineBlockEntity implements IFilterBlockEntity {
    private final Deque<AutoCrafterCache> cache = new ArrayDeque<>();
    private final FilteredItemDepository depository = new FilteredItemDepository.Pollable(9) {
        @Override
        public void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int cooldown = AnvilCraft.config.autoCrafterCooldown;
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
    public static AutoCrafterBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
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

    private boolean canCraft() {
        if (cooldown > 0) return false;
        if (!depository.isFilterEnabled()) return true;
        for (int i = 0; i < depository.getSlots(); i++) {
            if (depository.getStack(i).isEmpty() && !depository.getFilter(i).isEmpty()) return false;
        }
        return true;
    }

    @SuppressWarnings("UnreachableCode")
    private void craft(@NotNull Level level) {
        if (craftingContainer.isEmpty()) return;
        if (cooldown <= 0) {
            cooldown = AnvilCraft.config.autoCrafterCooldown;
        }
        cooldown--;
        if (!canCraft()) return;
        ItemStack result;
        Optional<AutoCrafterCache> cacheOptional = cache
            .stream()
            .filter(recipe -> recipe.test(craftingContainer))
            .findFirst();
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
            while (this.cache.size() >= 10) {
                this.cache.pop();
            }
        }
        if (optional.isEmpty()) return;
        result = optional.get().assemble(craftingContainer, level.registryAccess());
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
        remaining.forEach(stack -> stack.setCount(stack.getCount() * times));
        IItemDepository itemDepository = ItemDepositoryHelper.getItemDepository(
            level, getBlockPos().relative(getDirection()), getDirection().getOpposite()
        );
        if (itemDepository != null) {
            // 尝试向容器插入物品
            ItemStack remained = ItemDepositoryHelper.insertItem(itemDepository, result, true);
            if (!remained.isEmpty()) return;
            remained = ItemDepositoryHelper.insertItem(itemDepository, result, false);
            spawnItemEntity(remained);
            for (ItemStack stack : remaining) {
                remained = ItemDepositoryHelper.insertItem(itemDepository, stack, false);
                spawnItemEntity(remained);
            }
        } else {
            // 尝试向世界喷出物品
            Vec3 center = getBlockPos().relative(getDirection()).getCenter();
            AABB aabb = new AABB(center.add(-0.125, -0.125, -0.125), center.add(0.125, 0.125, 0.125));
            if (!getLevel().noCollision(aabb)) return;

            spawnItemEntity(result);
            for (ItemStack stack : remaining) {
                spawnItemEntity(stack);
            }
        }
        for (int i = 0; i < depository.getSlots(); i++) {
            depository.extract(i, times, false);
        }
        level.updateNeighborsAt(getBlockPos(), ModBlocks.AUTO_CRAFTER.get());
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        depository.deserializeNbt(tag.getCompound("Inventory"));
        cooldown = tag.getInt("Cooldown");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", this.depository.serializeNbt());
        tag.putInt("Cooldown", cooldown);
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

    /**
     * 获取红石信号强度
     *
     * @return 红石信号强度
     */
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

        /**
         * 合成器缓存
         *
         * @param container 容器
         * @param recipe    配方
         * @param remaining 返还物品
         */
        public AutoCrafterCache(
            @NotNull Container container, Optional<CraftingRecipe> recipe, NonNullList<ItemStack> remaining
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
                if (!ItemStack.isSameItemSameTags(container.getItem(i), this.container.getItem(i))) return false;
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
}
