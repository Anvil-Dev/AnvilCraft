package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 控制阀菜单。
 *
 * <p>仅含玩家物品栏（{@code (8,84)} 起 3×9，快捷栏 {@code (8,142)}）。过滤格<b>不是物品槽</b>——
 * 因部分流体无对应桶，过滤直接以 {@link net.neoforged.neoforge.fluids.FluidStack} 存于 BE，
 * 由屏幕在 {@code (80,18)} 处绘制流体贴图并处理点击 / JEI 拖入。流速由
 * {@link dev.dubhe.anvilcraft.network.ControlValveUpdatePacket} 同步。
 */
@Getter
public class ControlValveMenu extends AbstractContainerMenu {
    /** 过滤格在 GUI 中的本地坐标与尺寸（供屏幕绘制与命中判定） */
    public static final int FILTER_X = 80;
    public static final int FILTER_Y = 18;

    @Nullable
    private final ControlValveBlockEntity blockEntity;

    /** 服务端 / BE 构造 */
    public ControlValveMenu(int containerId, Inventory inventory, ControlValveBlockEntity blockEntity) {
        super(ModMenuTypes.CONTROL_VALVE.get(), containerId);
        this.blockEntity = blockEntity;
        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);
    }

    /** 客户端 / 注册构造 */
    public ControlValveMenu(@Nullable MenuType<?> menuType, int containerId, Inventory inventory, FriendlyByteBuf buf) {
        super(menuType, containerId);
        this.blockEntity = null;
        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);
    }

    private void addPlayerInventory(Inventory inv) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(inv, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
