package dev.dubhe.anvilcraft.client.gui.screen;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 仓储界面的纯客户端容器菜单。
 *
 * <p>仅用于让 {@link StorageScreen} 满足 {@code AbstractContainerScreen} 的菜单要求：
 * 槽位与仓储界面渲染的玩家物品栏区域一一对应（供渲染 / 命中检测 / 各
 * {@code instanceof AbstractContainerScreen} 扩展读取），指针物品委托给
 * {@code inventoryMenu}——服务端 RPC 读写的是 {@code player.containerMenu} 的指针，
 * 而仓储界面没有真实服务端菜单，服务端菜单恒为 {@code inventoryMenu}。
 *
 * <p>不含任何原版容器同步：不注册 MenuType、不设置 synchronizer、不发送任何容器点击包，
 * 仓储内容与指针的同步全部由 RPC 管控。
 */
public class StorageMenu extends AbstractContainerMenu {
    /**
     * 玩家物品栏在仓储界面中的渲染偏移（与 StorageScreen 一致）。
     * 隐藏槽（合成结果 / 合成格 / 盔甲 / 副手）用负坐标，不渲染。
     */
    private static final int PLAYER_INVENTORY_X = 114;
    private static final int PLAYER_INVENTORY_Y = 140;
    private static final int HOTBAR_Y = 58;
    private static final int HIDDEN_X = -1000;
    private static final int HIDDEN_Y = -1000;
    private final AbstractContainerMenu inventoryMenu;
    /** 存储站方块位置，供 JEI 转移等场景通过菜单定位存储。 */
    @Getter
    private final BlockPos sourcePos;

    private StorageMenu(AbstractContainerMenu inventoryMenu, Inventory inventory, BlockPos sourcePos) {
        super(null, 0);
        this.inventoryMenu = inventoryMenu;
        this.sourcePos = sourcePos;
        // 槽位布局与 InventoryMenu 完全一致（46 槽），保证服务端广播
        // （containerId == 0 的槽位/内容包）按同一 index 写回正确的背包槽。
        // 0 合成结果、1~4 合成格：服务端是独立容器（不在地图背包），用空容器承接广播
        SimpleContainer hidden = new SimpleContainer(1);
        for (int i = 0; i < 5; i++) {
            this.addSlot(new Slot(hidden, 0, HIDDEN_X, HIDDEN_Y));
        }
        // 5~8 盔甲（Inventory 36~39）：与 InventoryMenu 一致
        for (int i = 0; i < 4; i++) {
            this.addSlot(new Slot(inventory, 36 + i, HIDDEN_X, HIDDEN_Y));
        }
        // 9~35 主物品栏：3 行，与 StorageScreen 渲染位置一致
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(
                    inventory,
                    9 + row * 9 + column,
                    PLAYER_INVENTORY_X + 18 * column,
                    PLAYER_INVENTORY_Y + 18 * row
                ));
            }
        }
        // 36~44 快捷栏（0~8）
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, PLAYER_INVENTORY_X + 18 * i, PLAYER_INVENTORY_Y + HOTBAR_Y));
        }
        // 45 副手：隐藏
        this.addSlot(new Slot(inventory, 40, HIDDEN_X, HIDDEN_Y));
    }

    /** 创建包裹玩家 {@code inventoryMenu} 的仓储菜单。 */
    public static StorageMenu create(Player player, BlockPos sourcePos) {
        return new StorageMenu(player.inventoryMenu, player.getInventory(), sourcePos);
    }

    @Override
    public ItemStack getCarried() {
        return this.inventoryMenu.getCarried();
    }

    @Override
    public void setCarried(ItemStack stack) {
        this.inventoryMenu.setCarried(stack);
    }

    /**
     * 仓储界面不应触发原版容器点击逻辑：所有交互（含背包槽）都走 RPC。
     * 兜底为空实现，任何路径都不会真正同步到服务端。
     */
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** 客户端关闭界面时无需把指针放回背包（纯客户端菜单，指针本来就在 inventoryMenu）。 */
    @Override
    public void removed(Player player) {
    }

    @Override
    @SuppressWarnings("RedundantMethodOverride")
    public boolean canDragTo(Slot slot) {
        return true;
    }

    @Override
    @SuppressWarnings("RedundantMethodOverride")
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return true;
    }

    @Override
    public void setSynchronizer(@Nullable ContainerSynchronizer synchronizer) {
        // 不注册同步器：仓储同步全部由 RPC 管控
    }

    @Override
    @SuppressWarnings("RedundantMethodOverride")
    public boolean clickMenuButton(Player player, int id) {
        return false;
    }
}
