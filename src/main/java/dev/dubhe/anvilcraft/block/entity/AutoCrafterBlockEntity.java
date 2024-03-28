package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.AutoCrafterBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import lombok.Getter;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Stack;

@SuppressWarnings("NullableProblems")
public class AutoCrafterBlockEntity extends BaseMachineBlockEntity implements CraftingContainer {
    @Getter
    private boolean record = false;
    private final AutoCrafterBlockEntity entity = this;
    @Getter
    private final NonNullList<Boolean> disabled = NonNullList.withSize(9, false);
    @Getter
    private final NonNullList<ItemStack> filter = NonNullList.withSize(9, ItemStack.EMPTY);
    private final Stack<CraftingRecipe> cache = new Stack<>();

    public AutoCrafterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.AUTO_CRAFTER, pos, blockState, 9);
        this.direction = blockState.getValue(AutoCrafterBlock.FACING);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("block.anvilcraft.auto_crafter");
    }

    public void setRecord(boolean record) {
        this.record = record;
        if (!this.isRecord()) {
            this.getFilter().clear();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockEntity e) {
        if (!(e instanceof AutoCrafterBlockEntity entity)) return;
        BlockState state = level.getBlockState(pos);
        if (state.getValue(AutoCrafterBlock.LIT)) AutoCrafterBlockEntity.craft(level, entity);
    }

    private boolean canCraft() {
        if (!this.record) return true;
        for (int i = 0; i < this.items.size(); i++) {
            if (this.getItem(i).isEmpty() && !this.getDisabled().get(i)) return false;
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
        if (!entity.insertOrDropItem(entity.direction, level, entity.getBlockPos(), result, 0, false)) return;
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
            entity.insertOrDropItem(entity.direction, level, entity.getBlockPos(), container1, i, true);
        }
        level.updateNeighborsAt(entity.getBlockPos(), ModBlocks.AUTO_CRAFTER);
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AutoCrafterMenu(containerId, inventory, this);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.setRecord(tag.getBoolean("record"));
        ListTag tags = tag.getList("disabled", Tag.TAG_BYTE);
        for (int i = 0; i < tags.size(); i++) {
            this.disabled.set(i, ((ByteTag) tags.get(i)).getAsByte() != 0);
        }
        CompoundTag filter = tag.getCompound("filter");
        ContainerHelper.loadAllItems(filter, this.filter);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("record", ByteTag.valueOf(this.isRecord()));
        ListTag tags = new ListTag();
        for (Boolean b : this.getDisabled()) {
            tags.add(ByteTag.valueOf(b));
        }
        tag.put("disabled", tags);
        CompoundTag filter = new CompoundTag();
        ContainerHelper.saveAllItems(filter, this.filter);
        tag.put("filter", filter);
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack itemStack) {
        if (this.disabled.get(index)) return false;
        ItemStack itemStack1 = this.items.get(index);
        ItemStack itemStack2 = this.filter.get(index);
        int j = itemStack1.getCount();
        if (j >= itemStack1.getMaxStackSize()) {
            return false;
        }
        if (itemStack1.isEmpty()) {
            return itemStack2.isEmpty() || ItemStack.isSameItemSameTags(itemStack, itemStack2);
        }
        return !this.smallerStackExist(j, itemStack1, index);
    }

    private boolean smallerStackExist(int i, ItemStack itemStack, int j) {
        for (int k = j + 1; k < 9; ++k) {
            ItemStack itemStack1;
            if (this.disabled.get(k) || !(itemStack1 = this.getItem(k)).isEmpty() && (itemStack1.getCount() >= i || !ItemStack.isSameItemSameTags(itemStack1, itemStack)))
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
        if (!state.is(ModBlocks.AUTO_CRAFTER)) return;
        level.setBlock(pos, state.setValue(AutoCrafterBlock.FACING, direction), 3);
    }

    @SuppressWarnings("UnstableApiUsage")
    private boolean insertOrDropItem(Direction direction, Level level, @NotNull BlockPos pos, @NotNull Container container, int slot, boolean drop) {
        ItemStack item = container.getItem(slot);
        BlockPos curPos = pos.relative(direction);
        boolean flag;
        if (canPlaceItem(level, curPos, item, direction)) {
            flag = entity.insertItem(direction, level, curPos, container, slot);
            container.setItem(slot, item);
            if (flag) return true;
        }
        if (!drop) {
            if (HopperBlockEntity.getContainerAt(level, curPos) != null) return false;
            if (ItemStorage.SIDED.find(level, curPos, direction.getOpposite()) != null) return false;
        }
        flag = entity.dropItem(direction, level, pos, container, slot);
        container.setItem(slot, item);
        return flag;
    }

    @SuppressWarnings("UnstableApiUsage")
    private boolean insertItem(Direction direction, Level level, @NotNull BlockPos pos, @NotNull Container container, int slot) {
        ItemStack item = container.getItem(slot);
        Container outContainer = HopperBlockEntity.getContainerAt(level, pos);
        long count = item.getCount();
        if (null != outContainer) {
            ItemStack itemStack2 = HopperBlockEntity.addItem(container, outContainer, item, direction.getOpposite());
            count = itemStack2.getCount();
            return count == 0;
        }
        Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos, direction.getOpposite());
        if (target == null) return false;
        return StorageUtil.move(
                InventoryStorage.of(container, null).getSlot(slot),
                target,
                iv -> true,
                item.getCount(),
                null
        ) == count;
    }

    @SuppressWarnings("UnstableApiUsage")
    private boolean canPlaceItem(Level level, BlockPos pos, @NotNull ItemStack stack, Direction direction) {
        Container outContainer = HopperBlockEntity.getContainerAt(level, pos);
        int count = stack.getCount();
        if (null != outContainer) {
            for (int i = 0; i < outContainer.getContainerSize(); i++) {
                if (!outContainer.canPlaceItem(i, stack)) continue;
                ItemStack item = outContainer.getItem(i);
                if (item.isEmpty()) return true;
                if (!ItemStack.isSameItemSameTags(stack, item)) continue;
                count -= item.getMaxStackSize() - item.getCount();
                if (count <= 0) return true;
            }
            return count <= 0;
        }
        Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos, direction.getOpposite());
        if (target == null) return false;
        if (!target.supportsInsertion()) return false;
        return StorageUtil.simulateInsert(target, ItemVariant.of(stack), count, null) == count;
    }

    private boolean dropItem(Direction direction, Level level, @NotNull BlockPos pos, @NotNull Container container, int slot) {
        ItemStack item = container.getItem(slot);
        BlockPos out = pos.relative(direction);
        Vec3 vec3 = out.getCenter().relative(direction, -0.25);
        Vec3 move = out.getCenter().subtract(vec3);
        if (direction != Direction.UP && direction != Direction.DOWN) vec3 = vec3.subtract(0.0, 0.25, 0.0);
        ItemEntity entity = new ItemEntity(level, vec3.x, vec3.y, vec3.z, item, move.x, move.y, move.z);
        return level.addFreshEntity(entity);
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
