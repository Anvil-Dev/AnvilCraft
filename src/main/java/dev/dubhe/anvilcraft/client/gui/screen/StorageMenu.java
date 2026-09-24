package dev.dubhe.anvilcraft.client.gui.screen;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.IntUnaryOperator;
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

    private StorageMenu(AbstractContainerMenu inventoryMenu, BlockPos sourcePos) {
        super(null, 0);
        this.inventoryMenu = inventoryMenu;
        this.sourcePos = sourcePos;
        // 复用原菜单的槽位映射，确保盔甲和附加口袋的同步索引保持一致。
        for (int index = 0; index < inventoryMenu.slots.size(); index++) {
            Slot source = inventoryMenu.getSlot(index);
            int slotX = HIDDEN_X;
            int slotY = HIDDEN_Y;
            if (index >= 9 && index < 36) {
                slotX = PLAYER_INVENTORY_X + 18 * ((index - 9) % 9);
                slotY = PLAYER_INVENTORY_Y + 18 * ((index - 9) / 9);
            } else if (index >= 36 && index < 45) {
                slotX = PLAYER_INVENTORY_X + 18 * (index - 36);
                slotY = PLAYER_INVENTORY_Y + HOTBAR_Y;
            }
            this.addSlot(new Slot(source.container, source.getSlotIndex(), slotX, slotY));
        }
    }

    /** 创建包裹玩家 {@code inventoryMenu} 的仓储菜单。 */
    public static StorageMenu create(Player player, BlockPos sourcePos) {
        return new StorageMenu(player.inventoryMenu, sourcePos);
    }

    public void updateSlotPositions(IntUnaryOperator transformX) {
        for (int index = 9; index < 45; index++) {
            this.getSlot(index).x = transformX.applyAsInt(PLAYER_INVENTORY_X + 18 * ((index - 9) % 9));
        }
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
     * 仓储界面自行处理点击，不通过此菜单执行原版点击逻辑。
     * 此处仅阻止本地模拟；扩展模组的发包入口由 StorageScreen.slotClicked 拦截。
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
