package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.AutoCrafterBlock;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.AutoCrafterMenu;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
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
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class AutoCrafterBlockEntity extends BaseMachineBlockEntity {
    @Getter
    @Setter
    private boolean record = false;
    private final AutoCrafterBlockEntity entity = this;
    @Getter
    private final NonNullList<Boolean> disabled = NonNullList.withSize(9, false);
    private final CraftingContainer container = new CraftingContainer() {
        @Override
        public void fillStackedContents(StackedContents contents) {
            for (int i = 0; i < this.getContainerSize(); i++) {
                ItemStack itemStack = this.getItem(i);
                contents.accountSimpleStack(itemStack);
            }
        }

        @Override
        public void clearContent() {
            entity.clearContent();
            for (int i = 0; i < this.getContainerSize(); i++) {
                entity.setItem(i, ItemStack.EMPTY);
            }
        }

        @Override
        public int getContainerSize() {
            return entity.getContainerSize() - 1;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < this.getContainerSize(); i++) {
                ItemStack itemStack = this.getItem(i);
                if (itemStack.isEmpty()) continue;
                return false;
            }
            return true;
        }

        @Override
        public @NotNull ItemStack getItem(int slot) {
            return entity.getItem(slot);
        }

        @Override
        public @NotNull ItemStack removeItem(int slot, int amount) {
            return entity.removeItem(slot, amount);
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int slot) {
            return entity.removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            entity.setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            entity.setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return entity.stillValid(player);
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
        public @NotNull List<ItemStack> getItems() {
            List<ItemStack> stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
            for (int i = 0; i < this.getContainerSize(); i++) stacks.set(i, entity.getItem(i));
            return stacks;
        }
    };

    public AutoCrafterBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.AUTO_CRAFTER, pos, blockState, 10);
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
        int slot = entity.getContainerSize() - 1;
        ItemStack result = entity.getItem(slot);
        if (result.isEmpty()) return;
        boolean bl = entity.dropItem(entity.direction, level, pos, result.copy().split(1));
        if (bl) {
            result.shrink(1);
        }
        entity.setItem(slot, result);
    }

    private static void craft(@NotNull Level level, @NotNull AutoCrafterBlockEntity entity) {
        ItemStack itemStack = entity.getResult();
        ItemStack itemStack2;
        if (entity.shouldRecord()) return;
        Optional<CraftingRecipe> optional = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, entity.container, level);
        NonNullList<ItemStack> nonNullList = level.getRecipeManager().getRemainingItemsFor(RecipeType.CRAFTING, entity.container, level);
        if (optional.isEmpty()) return;
        itemStack2 = optional.get().assemble(entity.container, level.registryAccess());
        if (!itemStack2.isItemEnabled(level.enabledFeatures())) return;
        if (!(itemStack.isEmpty() || ItemStack.isSameItemSameTags(itemStack, itemStack2))) return;
        if (itemStack.getCount() + itemStack2.getCount() > itemStack.getItem().getMaxStackSize()) return;
        itemStack2.setCount(itemStack.getCount() + itemStack2.getCount());
        itemStack = itemStack2;
        entity.setResult(itemStack);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = entity.getItem(i);
            stack.shrink(1);
            entity.setItem(i, stack);
        }
        nonNullList.forEach(stack -> entity.dropItem(entity.direction, level, entity.getBlockPos(), stack));
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new AutoCrafterMenu(containerId, inventory, this);
    }

    public boolean shouldRecord() {
        if (!this.isRecord()) return false;
        for (int i = 0; i < this.getContainerSize() - 1; i++) {
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
        if (index != this.getContainerSize() - 1 && this.disabled.get(index)) {
            return false;
        }
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

    private boolean dropItem(Direction direction, Level level, @NotNull BlockPos pos, ItemStack item) {
        Container outContainer = HopperBlockEntity.getContainerAt(level, pos.relative(direction));
        if (null == outContainer) {
            BlockPos out = pos.relative(direction);
            Vec3 vec3 = out.getCenter().relative(direction, -0.5);
            Vec3 move = out.getCenter().subtract(vec3);
            ItemEntity entity = new ItemEntity(level, vec3.x, vec3.y, vec3.z, item, move.x, move.y, move.z);
            return level.addFreshEntity(entity);
        } else {
            ItemStack itemStack2 = HopperBlockEntity.addItem(entity, outContainer, item, entity.direction.getOpposite());
            return itemStack2.isEmpty();
        }
    }
}
