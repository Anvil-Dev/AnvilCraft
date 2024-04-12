package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.api.depository.ItemDepositorySlot;
import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.component.ReadOnlySlot;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@Getter
public class AutoCrafterMenu extends BaseMachineMenu implements IFilterMenu, ContainerListener {
    public final AutoCrafterBlockEntity blockEntity;
    private final Slot resultSlot;
    private final Level level;

    public AutoCrafterMenu(
        @Nullable MenuType<?> menuType, int containerId, Inventory inventory, @NotNull FriendlyByteBuf extraData
    ) {
        this(menuType, containerId, inventory, inventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    /**
     * 合成器菜单
     *
     * @param menuType    菜单类型
     * @param containerId 容器id
     * @param inventory   背包
     * @param blockEntity 方块实体
     */
    public AutoCrafterMenu(MenuType<?> menuType, int containerId, Inventory inventory, BlockEntity blockEntity) {
        super(menuType, containerId, blockEntity);
        AutoCrafterMenu.checkContainerSize(inventory, 9);

        this.blockEntity = (AutoCrafterBlockEntity) blockEntity;
        this.level = inventory.player.level();

        this.addPlayerInventory(inventory);
        this.addPlayerHotbar(inventory);

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                this.addSlot(
                    new ItemDepositorySlot(this.blockEntity.getDepository(), i * 3 + j, 26 + j * 18, 18 + i * 18)
                );
            }
        }

        this.addSlot(resultSlot = new ReadOnlySlot(new SimpleContainer(1), 0, 8 + 7 * 18, 18 + 2 * 18));
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
    private static final int TE_INVENTORY_SLOT_COUNT = 9;  // must be the number of slots you have!

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        //noinspection ConstantValue
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;  //EMPTY_ITEM
        ItemStack sourceStack = sourceSlot.getItem();
        final ItemStack copyOfSourceStack = sourceStack.copy();
        // Check if the slot clicked is one of the vanilla container slots
        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            // This is a vanilla container slot so merge the stack into the tile inventory
            if (!moveItemStackTo(sourceStack, TE_INVENTORY_FIRST_SLOT_INDEX, TE_INVENTORY_FIRST_SLOT_INDEX
                + TE_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;  // EMPTY_ITEM
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            // This is a TE slot so merge the stack into the players inventory
            if (!moveItemStackTo(
                sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false
            )) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Invalid slotIndex:" + index);
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

    @Override
    public boolean stillValid(@NotNull Player player) {
        return stillValid(
            ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.AUTO_CRAFTER.get()
        );
    }

    @Override
    public IFilterBlockEntity getFilterBlockEntity() {
        return this.blockEntity;
    }

    @Override
    public void setItem(int slotId, int stateId, @NotNull ItemStack stack) {
        super.setItem(slotId, stateId, stack);
        this.onChanged();
    }

    private void onChanged() {
        //if (!level.isClientSide) return;
        RecipeManager recipeManager = level.getRecipeManager();
        Optional<CraftingRecipe> recipe = recipeManager
            .getRecipeFor(RecipeType.CRAFTING, blockEntity.getCraftingContainer(), level);
        if (recipe.isPresent()) {
            ItemStack resultItem = recipe.get().getResultItem(level.registryAccess());
            this.resultSlot.set(resultItem);
        } else {
            this.resultSlot.set(ItemStack.EMPTY);
        }
    }

    @Override
    public void slotChanged(
        @NotNull AbstractContainerMenu containerToSend, int dataSlotIndex, @NotNull ItemStack stack
    ) {
        onChanged();
    }

    @Override
    public void dataChanged(@NotNull AbstractContainerMenu containerMenu, int dataSlotIndex, int value) {

    }
}
