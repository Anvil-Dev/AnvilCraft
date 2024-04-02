package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.AutoCrafterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
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
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

@SuppressWarnings("NullableProblems")
public class AutoCrafterBlockEntity extends BaseMachineBlockEntity implements CraftingContainer, IFilterBlockEntity {
    @Getter
    @Setter
    private boolean record = false;
    @Getter
    private final NonNullList<Boolean> disabled = this.getNewDisabled();
    @Getter
    private final NonNullList<@Unmodifiable ItemStack> filter = this.getNewFilter();
    private final Deque<CraftingRecipe> cache = new ArrayDeque<>();
    public AutoCrafterBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState, 9);
        this.direction = blockState.getValue(AutoCrafterBlock.FACING);
    }
    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("block.anvilcraft.auto_crafter");
    }
    public static void tick(Level level, BlockPos pos, BlockEntity e) {
        if (!(e instanceof AutoCrafterBlockEntity entity)) return;
        BlockState state = level.getBlockState(pos);
        if (state.getValue(AutoCrafterBlock.LIT)) AutoCrafterBlockEntity.craft(level, entity);
    }
    private boolean canCraft() {
        if (!this.isRecord()) return true;
        for (int i = 0; i < this.items.size(); i++) {
            if (this.getItem(i).isEmpty() && !(this.getDisabled().get(i) || isRecord() && getFilter().get(i).isEmpty())) return false;
        }
        return true;
    }
    private static void craft(@NotNull Level level, @NotNull AutoCrafterBlockEntity entity) {
        ItemStack itemStack;
        if (entity.isEmpty()) return;
        if (!entity.canCraft()) return;
        Optional<CraftingRecipe> optional = entity.cache.stream().filter(recipe -> recipe.matches(entity, level)).findFirst();
        if (optional.isEmpty()) {
            optional = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, entity, level);
            if (optional.isPresent()) {
                CraftingRecipe recipe = optional.get();
                entity.cache.push(recipe);
                while (entity.cache.size() >= 10) {
                    entity.cache.pop();
                }
            }
        }
        if (optional.isEmpty()) return;
        NonNullList<ItemStack> nonNullList = level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, entity, level);
        itemStack = optional.get().assemble(entity, level.registryAccess());
        if (!itemStack.isItemEnabled(level.enabledFeatures())) return;
        Container result = new SimpleContainer(1);
        result.setItem(0, itemStack);
        if (!entity.insertOrDropItem(entity.direction, level, entity.getBlockPos(), result, 0, false, true, false)) {
            return;
        }
        for (int i = 0; i < 9; i++) {
            ItemStack stack = entity.getItem(i);
            stack.shrink(1);
            entity.setItem(i, stack);
        }
        Container container1 = new SimpleContainer(nonNullList.size());
        for (int i = 0; i < nonNullList.size(); i++) {
            container1.setItem(i, nonNullList.get(i));
        }
        for (int i = 0; i < nonNullList.size(); i++) {
            entity.insertOrDropItem(entity.direction, level, entity.getBlockPos(), container1, i, true, true, false);
        }
        level.updateNeighborsAt(entity.getBlockPos(), ModBlocks.AUTO_CRAFTER.get());
    }
    @Override
    protected @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return AutoCrafterMenu.serverOf(containerId, inventory, this);
    }
    @Override
    public void load(@NotNull CompoundTag tag) {
        super.load(tag);
        this.loadTag(tag);
    }
    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        this.saveTag(tag);
    }
    @Override
    public boolean canPlaceItem(int index, ItemStack insertingStack) {
        if (this.getDisabled().get(index)) return false;
        ItemStack storedStack = this.items.get(index);
        ItemStack filterStack = this.getFilter().get(index);
        if (isRecord() && filterStack.isEmpty()) return insertingStack.isEmpty();
        int count = storedStack.getCount();
        if (count >= storedStack.getMaxStackSize()) {
            return false;
        }
        if (storedStack.isEmpty()) {
            return filterStack.isEmpty() || ItemStack.isSameItemSameTags(insertingStack, filterStack);
        }
        return !this.smallerStackExist(count, storedStack, index);
    }
    private boolean smallerStackExist(int count, ItemStack itemStack, int index) {
        for (int index2 = index + 1; index2 < 9; ++index2) {
            ItemStack itemStack1;
            if (this.getDisabled().get(index2) || isRecord() && getFilter().get(index2).isEmpty() || !(itemStack1 = this.getItem(index2)).isEmpty() && (itemStack1.getCount() >= count || !ItemStack.isSameItemSameTags(itemStack1, itemStack)))
                continue;
            return true;
        }
        return false;
    }
    @Override
    public void setDirection(Direction direction) {
        super.setDirection(direction);
        BlockPos pos = this.getBlockPos();
        Level level = this.getLevel();
        if (null == level) return;
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.AUTO_CRAFTER.get())) return;
        level.setBlockAndUpdate(pos, state.setValue(AutoCrafterBlock.FACING, direction));
    }
    @Override
    public int getWidth() {
        return 3;
    }
    @Override
    public int getHeight() {
        return 3;
    }
    @Override
    public void fillStackedContents(StackedContents contents) {
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack itemStack = this.getItem(i);
            contents.accountSimpleStack(itemStack);
        }
    }
    public int getRedstoneSignal() {
        int i = 0;
        for (int j = 0; j < this.getContainerSize(); ++j) {
            ItemStack itemStack = this.getItem(j);
            if (itemStack.isEmpty() && !this.getDisabled().get(j)) continue;
            ++i;
        }
        return i;
    }
}
