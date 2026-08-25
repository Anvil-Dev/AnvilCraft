package dev.dubhe.anvilcraft.client.renderer.item.decoration;

import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.client.rpc.TerminalReachabilityCache;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

import java.util.UUID;
import javax.annotation.Nullable;

public class TerminalInsertionDecoration implements IItemDecorator {
    @Override
    public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int offsetX, int offsetY) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return false;
        }
        // 仅当处于容器 GUI 中、指针捏着物品、且该终端未被捏起时显示“+”
        if (!(minecraft.screen instanceof AbstractContainerScreen<?> containerScreen)) {
            return false;
        }
        ItemStack carried = minecraft.player.inventoryMenu.getCarried();
        if (carried.isEmpty() || ItemStack.isSameItemSameComponents(stack, carried)) {
            return false;
        }
        UUID targetId = TerminalInsertionDecoration.terminalTargetId(stack);
        if (targetId == null) {
            // 超维终端未绑定等不可用情况不提示
            return false;
        }
        // 仅当渲染的终端属于当前容器菜单的槽位（玩家物品栏 / 容器）时提示；
        // JEI 面板、创造模式标签栏等非槽位渲染不提示。无需悬停：界面内可见的
        // 终端都可右键放入，故界面中该 tipo 的全部槽位实例都显示“+”。
        boolean inMenuSlot = false;
        for (Slot slot : containerScreen.getMenu().slots) {
            if (slot.hasItem() && slot.getItem() == stack) {
                inMenuSlot = true;
                break;
            }
        }
        if (!inMenuSlot) {
            return false;
        }
        // 检查能否连接：本地 / 潜影终端需要当前可连接目标（32 格内板条箱 /
        // 可解析的潜影目标）；超维终端已绑定即视为可用。未知（尚未确认）与
        // 不可达都不显示，避免可达性缓存过期时“乐观显示几帧”的闪烁。
        if (!stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            TerminalReachabilityCache.ensure(targetId);
            Boolean reachable = TerminalReachabilityCache.getReachability(targetId);
            if (reachable == null || !reachable) {
                return false;
            }
        }
        // 指针捏着物品时在终端图标右上角显示“+”，提示可右键放入存储站。
        // 装饰器调用时 pose 已回到 z=0，需要抬高 z 才会绘制在物品之上。
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
        guiGraphics.drawString(font, "+", offsetX + 10, offsetY + 1, 0xFFFFFFFF, true);
        guiGraphics.pose().popPose();
        return true;
    }

    /** 任意终端的当前会话存储标识；不可用（未绑定 / 非终端）返回 null。 */
    private static @Nullable UUID terminalTargetId(ItemStack stack) {
        if (stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
            if (binding == null || binding.id().isEmpty()) {
                return null;
            }
            return binding.id().get();
        }
        if (stack.is(ModItems.LOCAL_TERMINAL)) {
            return StorageTerminalClientStub.localTerminalId();
        }
        if (stack.is(ModItems.SHULKER_TERMINAL)) {
            return StorageTerminalClientStub.shulkerTerminalId();
        }
        return null;
    }
}
