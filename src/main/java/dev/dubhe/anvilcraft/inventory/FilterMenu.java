package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.inventory.component.FilterOnlySlot;
import dev.dubhe.anvilcraft.inventory.component.FilterSlot;
import dev.dubhe.anvilcraft.inventory.container.FilterContainer;
import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FilterMenu extends AbstractContainerMenu {
    // 功劳归于：: diesieben07 | https://github.com/diesieben07/SevenCommons
    // 必须为 GUI 使用的每个插槽分配一个插槽编号.
    // 对于这个容器，我们可以看到过滤槽以及玩家库存插槽和快捷栏.
    // 每次我们向容器添加 Slot 时，它都会自动增加 slotIndex，这意味着
    //  0 - 8 = 快捷栏插槽（将映射到 InventoryPlayer 插槽编号 0 - 8）
    //  9 - 35 = 玩家物品栏（映射到 InventoryPlayer 插槽编号 9 - 35）
    //  36 - 44 = 过滤槽，映射到我们的 TileEntity 插槽编号 0 - 8）
    private static final int HOTBAR_SLOT_COUNT = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT = PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT = HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_SLOT_INDEX = 0;
    private static final int FILTER_FIRST_SLOT_INDEX = VANILLA_SLOT_INDEX + VANILLA_SLOT_COUNT;
    // THIS YOU HAVE TO DEFINE!
    private static final int FILTER_SLOT_COUNT = 18; // must be the number of slots you have!
    private final FilterContainer container;

    public FilterMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FilterContainer container) {
        super(menuType, containerId);
        this.addPlayerHotbar(inventory);
        this.addPlayerInventory(inventory);
        this.container = container;


        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 6; ++l) {
                this.addSlot(new FilterSlot(container, l + i * 6, 62 + l * 18, 17 + i * 18));
            }
        }
    }

    public FilterMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf buf) {
        this(menuType, containerId, inventory, new FilterContainer(inventory, buf));
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

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (
            clickType == ClickType.SWAP
                && slotId >= FILTER_FIRST_SLOT_INDEX
                && slotId < FILTER_FIRST_SLOT_INDEX + FILTER_SLOT_COUNT
                && this.getSlot(slotId) instanceof FilterOnlySlot filterSlot
                && (button >= 0 && button < HOTBAR_SLOT_COUNT || button == Inventory.SLOT_OFFHAND)
        ) {
            filterSlot.set(player.getInventory().getItem(button).copy());
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void setSynchronizer(ContainerSynchronizer synchronizer) {
        super.setSynchronizer(synchronizer);
    }

    @OnlyIn(Dist.CLIENT)
    public void sync() {
        this.container.sync();
    }
}
