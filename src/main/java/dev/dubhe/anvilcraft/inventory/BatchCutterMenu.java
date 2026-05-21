package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.api.itemhandler.SlotItemHandlerWithFilter;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.batch.BatchCutterBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.component.ReadOnlySlot;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

@Slf4j
@Getter
public class BatchCutterMenu extends BaseMachineMenu implements IFilterMenu, ContainerListener {
    public final BatchCutterBlockEntity entity;
    private final Slot resultSlot;
    private final Level level;
    private @Nullable List<RecipeHolder<StonecutterRecipe>> recipes;

    public BatchCutterMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf extraData) {
        this(menuType, containerId, inventory, Objects.requireNonNull(inventory.player.level().getBlockEntity(extraData.readBlockPos())));
    }

    /**
     * 合成器菜单
     *
     * @param menuType    菜单类型
     * @param containerId 容器id
     * @param inventory   背包
     * @param entity 方块实体
     */
    public BatchCutterMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity entity) {
        super(menuType, containerId, entity);
        BatchCutterMenu.checkContainerSize(inventory, 9);

        this.entity = (BatchCutterBlockEntity) entity;
        this.level = inventory.player.level();

        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);

        this.addSlot(new SlotItemHandlerWithFilter(this.entity.getItemHandler(), 0, 8, 43));
        this.addSlot(this.resultSlot = new ReadOnlySlot(new SimpleContainer(1), 0, 152, 43));
        this.onChanged();
        this.addSlotListener(this);
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    // 功劳归于：: diesieben07 | https://github.com/diesieben07/SevenCommons
    // 必须为 GUI 使用的每个插槽分配一个插槽编号.
    // 对于这个容器，我们可以看到瓷砖库存的插槽以及玩家库存插槽和快捷栏.
    // 每次我们向容器添加 Slot 时，它都会自动增加 slotIndex，这意味着
    //  0 - 8 = 快捷栏插槽（将映射到 InventoryPlayer 插槽编号 0 - 8）
    //  9 - 35 = 玩家物品栏（映射到 InventoryPlayer 插槽编号 9 - 35）
    //  36 - 44 = TileInventory 插槽，映射到我们的 TileEntity 插槽编号 0 - 8）
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX = VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;

    // THIS YOU HAVE TO DEFINE!
    private static final int TE_INVENTORY_SLOT_COUNT = 1; // must be the number of slots you have!

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        // noinspection ConstantValue
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY; // EMPTY_ITEM
        }
        ItemStack sourceStack = sourceSlot.getItem();
        final ItemStack copyOfSourceStack = sourceStack.copy();
        // Check if the slot clicked is one of the vanilla container slots
        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (moveItemToActiveSlot(sourceStack)) {
                return ItemStack.EMPTY; // EMPTY_ITEM
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(
                sourceStack,
                VANILLA_FIRST_SLOT_INDEX,
                VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT,
                false
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            BatchCutterMenu.log.warn("Invalid slotIndex: {}", index);
            return ItemStack.EMPTY;
        }
        // If stack size == 0 (the entire stack was moved) set slot contents to null
        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    // 移动物品到可用槽位
    private boolean moveItemToActiveSlot(ItemStack stack) {
        int count = stack.getCount();
        if (this.canPlace(stack, BatchCutterMenu.TE_INVENTORY_FIRST_SLOT_INDEX)) {
            this.moveItemStackTo(
                stack,
                BatchCutterMenu.TE_INVENTORY_FIRST_SLOT_INDEX,
                BatchCutterMenu.TE_INVENTORY_FIRST_SLOT_INDEX + 1,
                false
            );
        }
        return stack.getCount() >= count;
    }

    // 是否可以向槽位中放入物品
    private boolean canPlace(ItemStack stack, int index) {
        if (this.getSlot(index) instanceof SlotItemHandlerWithFilter depositorySlot) {
            // 如果当前槽位被禁用，返回false
            if (depositorySlot.isSlotDisabled(index - BatchCutterMenu.VANILLA_SLOT_COUNT)) {
                return false;
            }
            // 当前槽位没有禁用，并且要放入的物品就是当前槽位的过滤器要过滤的物品，返回true
            // 如果未设置保留物品过滤，即所有槽位都没有被禁用，此时过滤器不会过滤任何物品，所以当前过滤器要过滤的物品为空时也应该返回true
            ItemStack filterItem = depositorySlot.getFilterItem(index - BatchCutterMenu.VANILLA_SLOT_COUNT);
            return filterItem.isEmpty() || filterItem.is(stack.getItem());
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, entity.getBlockPos()), player, ModBlocks.BATCH_CUTTER.get());
    }

    @Override
    public IFilterBlockEntity getFilterBlockEntity() {
        return this.entity;
    }

    @Override
    public int getFilterSlotIndex(Slot slot) {
        return slot.index - 36;
    }

    @Override
    public void setItem(int slotId, int stateId, ItemStack stack) {
        super.setItem(slotId, stateId, stack);
        this.onChanged();
    }

    public void onChanged() {
        RecipeManager manager = this.level.getRecipeManager();
        this.recipes = manager.getRecipesFor(
            RecipeType.STONECUTTING,
            this.entity.createDummyInput(),
            this.level
        );
        if (this.recipes.isEmpty()) {
            this.resultSlot.set(ItemStack.EMPTY);
        } else if (this.entity.getSelecting() >= this.recipes.size()) {
            this.entity.setSelecting(0);
            ItemStack resultItem = this.recipes.get(this.entity.getSelecting()).value().getResultItem(this.level.registryAccess());
            this.resultSlot.set(resultItem);
        } else {
            ItemStack resultItem = this.recipes.get(this.entity.getSelecting()).value().getResultItem(this.level.registryAccess());
            this.resultSlot.set(resultItem);
        }
    }

    @Override
    public void slotChanged(AbstractContainerMenu containerToSend, int dataSlotIndex, ItemStack stack) {
        this.onChanged();
    }

    @Override
    public void dataChanged(AbstractContainerMenu containerMenu, int dataSlotIndex, int value) {
    }

    @Override
    public void flush() {
        this.onChanged();
    }
}
