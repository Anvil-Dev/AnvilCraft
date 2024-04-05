package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.IFilterBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.component.FilterSlot;
import dev.dubhe.anvilcraft.inventory.component.ReadOnlySlot;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class AutoCrafterMenu extends BaseMachineMenu implements IFilterMenu {
    public static AutoCrafterMenu clientOf(MenuType<AutoCrafterMenu> type, int containerId, Inventory inventory) {
        return new Client(type, containerId, inventory);
    }

    public static AutoCrafterMenu serverOf(int containerId, @NotNull Inventory inventory, AutoCrafterBlockEntity blockEntity) {
        return new Server(containerId, inventory, blockEntity);
    }

    private final Inventory inventory;
    private final Slot resultSlot;

    public AutoCrafterMenu(int containerId, @NotNull Inventory inventory, @NotNull CraftingContainer machine) {
        this(ModMenuTypes.AUTO_CRAFTER.get(), containerId, inventory, machine);
    }

    public AutoCrafterMenu(MenuType<AutoCrafterMenu> type, int containerId, @NotNull Inventory inventory, @NotNull CraftingContainer machine) {
        super(type, containerId, machine);
        this.inventory = inventory;
        this.machine.startOpen(this.inventory.player);
        int i, j;
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 3; ++j) {
                this.addSlot(new FilterSlot(this.machine, j + i * 3, 26 + j * 18, 18 + i * 18, this));
            }
        }
        this.addSlot(resultSlot = new ReadOnlySlot(new SimpleContainer(1), 0, 8 + 7 * 18, 18 + 2 * 18));
        for (i = 0; i < 3; ++i) {
            for (j = 0; j < 9; ++j) {
                this.addSlot(new Slot(this.inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (i = 0; i < 9; ++i) {
            this.addSlot(new Slot(this.inventory, i, 8 + i * 18, 142));
        }
        this.updateResult();
    }

    public @Nullable IFilterBlockEntity getEntity() {
        return this.machine instanceof IFilterBlockEntity entity ? entity : null;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        this.machine.stopOpen(player);
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        super.slotsChanged(container);
        if (container == getMachine()) {
            this.updateResult();
        }
    }

    @Override
    public CraftingContainer getMachine() {
        return (CraftingContainer) super.getMachine();
    }

    public void updateResult() {
        if (!(this.getMachine() instanceof AutoCrafterBlockEntity entity)) return;
        Level level = getInventory().player.level();
        AutoCrafterContainer container = new AutoCrafterContainer(NonNullList.of(ItemStack.EMPTY, getMachine().getItems().stream().map(ItemStack::copy).toArray(ItemStack[]::new)));
        for (int i = 0; i < container.items.size(); i++) {
            if (!container.getItem(i).isEmpty()) continue;
            container.setItem(i, entity.getFilter().get(i));
        }
        if (container.isEmpty()) {
            getResultSlot().set(ItemStack.EMPTY);
            return;
        }
        CraftingRecipe recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, container, level).orElse(null);
        getResultSlot().set(recipe == null ? ItemStack.EMPTY : recipe.assemble(container, level.registryAccess()));
    }

    private static class Server extends AutoCrafterMenu implements IFilterMenuSever {
        public Server(int containerId, @NotNull Inventory inventory, AutoCrafterBlockEntity blockEntity) {
            super(containerId, inventory, blockEntity);
        }
        @Override
        public AutoCrafterBlockEntity getBlockEntity() {
            return super.getBlockEntity();
        }
        @Override
        public AbstractContainerMenu getMenu() {
            return this;
        }
        @Override
        public ServerPlayer getPlayer() {
            return (ServerPlayer) getInventory().player;
        }
        @Override
        public void clicked(int slotId, int button, @NotNull ClickType clickType, @NotNull Player player){
            IFilterMenuSever.super.clicked(slotId, button, clickType, player);
            super.clicked(slotId, button, clickType, player);
        }
        @Override
        public void setRecord(boolean record){
            super.setRecord(record);
            IFilterMenuSever.super.setRecord(record);
        }
    }

    private static class Client extends AutoCrafterMenu {
        public Client(MenuType<AutoCrafterMenu> type, int containerId, Inventory inventory) {
            super(type, containerId, inventory, new AutoCrafterContainer(NonNullList.withSize(9, ItemStack.EMPTY)));
        }
    }

    public AutoCrafterBlockEntity getBlockEntity() {
        return (AutoCrafterBlockEntity) getMachine();
    }
}
