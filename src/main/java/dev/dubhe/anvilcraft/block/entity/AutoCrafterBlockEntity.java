package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.AutoCrafterBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import lombok.Getter;
import lombok.Setter;
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

@SuppressWarnings("NullableProblems")
public class AutoCrafterBlockEntity extends BaseMachineBlockEntity implements CraftingContainer {
    @Getter
    @Setter
    private boolean record = false;
    private final AutoCrafterBlockEntity entity = this;
    @Getter
    private final NonNullList<Boolean> disabled = NonNullList.withSize(9, false);

    public AutoCrafterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.AUTO_CRAFTER, pos, blockState, 9);
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

    private static void craft(@NotNull Level level, @NotNull AutoCrafterBlockEntity entity) {
        ItemStack itemStack;
        if (entity.shouldRecord()) return;
        Optional<CraftingRecipe> optional = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, entity, level);
        NonNullList<ItemStack> nonNullList = level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, entity, level);
        if (optional.isEmpty()) return;
        itemStack = optional.get().assemble(entity, level.registryAccess());
        if (!itemStack.isItemEnabled(level.enabledFeatures())) return;
        Container result = new SimpleContainer(1);
        result.setItem(0, itemStack);
        entity.insertOrDropItem(entity.direction, level, entity.getBlockPos(), result, 0);
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
            entity.insertOrDropItem(entity.direction, level, entity.getBlockPos(), container1, i);
        }
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AutoCrafterMenu(containerId, inventory, this);
    }

    public boolean shouldRecord() {
        if (!this.isRecord()) return false;
        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack stack = this.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.getCount() > 1) continue;
            return true;
        }
        return false;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.setRecord(tag.getBoolean("record"));
        ListTag tags = tag.getList("disabled", Tag.TAG_BYTE);
        for (int i = 0; i < tags.size(); i++) {
            this.disabled.set(i, ((ByteTag) tags.get(i)).getAsByte() != 0);
        }
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
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack itemStack) {
        if (this.disabled.get(index)) return false;
        ItemStack itemStack2 = this.items.get(index);
        int j = itemStack2.getCount();
        if (j >= itemStack2.getMaxStackSize()) {
            return false;
        }
        if (itemStack2.isEmpty()) {
            return true;
        }
        return !this.smallerStackExist(j, itemStack2, index);
    }

    private boolean smallerStackExist(int i, ItemStack itemStack, int j) {
        for (int k = j + 1; k < 9; ++k) {
            ItemStack itemStack2;
            if (this.disabled.get(k) || !(itemStack2 = this.getItem(k)).isEmpty() && (itemStack2.getCount() >= i || !ItemStack.isSameItemSameTags(itemStack2, itemStack)))
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

    private void insertOrDropItem(Direction direction, Level level, @NotNull BlockPos pos, @NotNull Container container, int slot) {
        ItemStack item = container.getItem(slot);
        int count = item.getCount();
        count -= entity.insertItem(direction, level, pos, container, slot);
        container.setItem(slot, item);
        if (count <= 0) return;
        count -= entity.dropItem(direction, level, pos, container, slot);
        container.setItem(slot, item);
    }

    @SuppressWarnings("UnstableApiUsage")
    private int insertItem(Direction direction, Level level, @NotNull BlockPos pos, @NotNull Container container, int slot) {
        ItemStack item = container.getItem(slot);
        Container outContainer = HopperBlockEntity.getContainerAt(level, pos.relative(direction));
        if (null == outContainer) {
            Storage<ItemVariant> target = ItemStorage.SIDED.find(level, pos.relative(direction), direction.getOpposite());
            if (target == null) {
                return 0;
            } else {
                return (int) StorageUtil.move(
                        InventoryStorage.of(container, null).getSlot(slot),
                        target,
                        iv -> true,
                        item.getCount(),
                        null
                );
            }
        } else {
            ItemStack itemStack2 = HopperBlockEntity.addItem(entity, outContainer, item, entity.direction.getOpposite());
            return item.getCount() - itemStack2.getCount();
        }
    }

    private int dropItem(Direction direction, Level level, @NotNull BlockPos pos, @NotNull Container container, int slot) {
        ItemStack item = container.getItem(slot);
        BlockPos out = pos.relative(direction);
        Vec3 vec3 = out.getCenter().relative(direction, -0.375);
        Vec3 move = out.getCenter().subtract(vec3);
        ItemEntity entity = new ItemEntity(level, vec3.x, vec3.y, vec3.z, item, move.x, move.y, move.z);
        return level.addFreshEntity(entity) ? item.getCount() : 0;
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
}
