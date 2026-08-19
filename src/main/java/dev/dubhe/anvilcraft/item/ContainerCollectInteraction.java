package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.world.inventory.ClickAction;

/**
 * 超维终端/护符盒/药盒在容器界面中共用的「收纳/取出」交互：默认使用右键（与护符盒一致），
 * 可通过客户端配置 invertContainerCollectClick 反转成左键。
 * 收纳与取出共用同一个按键：点击物品收纳、点击空位取出。
 */
public final class ContainerCollectInteraction {
    private ContainerCollectInteraction() {
    }

    /** 当前配置下用于收纳/取出的点击动作。 */
    public static ClickAction collectAction() {
        return AnvilCraft.CLIENT_CONFIG.invertContainerCollectClick ? ClickAction.PRIMARY : ClickAction.SECONDARY;
    }

    /** 判断一次点击是否为当前配置下的收纳/取出动作。 */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isCollectAction(ClickAction action) {
        return action == ContainerCollectInteraction.collectAction();
    }
}
