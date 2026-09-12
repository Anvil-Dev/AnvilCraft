package dev.dubhe.anvilcraft.api;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import javax.annotation.Nullable;

/**
 * 仓储端口父接口：仓储端口、仓储流体端口与仓储端口整合器都实现本接口。
 *
 * <p>三者共用同一套相连关系（面相邻的端口可互相延伸，见 {@link StoragePortManager#scan}），
 * 方块层与网络包只经由本接口转发左右键，不关心具体端口类型。各端口按自身语义实现行为：
 * 左键为「取出」一类的即时动作，右键为「标记 / 塞入 / 倒流体」一类的交互。</p>
 */
public interface IStoragePort {
    /**
     * 左键行为（服务端执行）。
     *
     * @param player 玩家
     * @param ports  与本端口相连的所有其它端口
     * @return 是否已处理
     */
    default boolean onLeftClick(Player player, List<IStoragePort> ports) {
        return false;
    }

    /**
     * 右键行为。
     *
     * @param player 玩家
     * @param hand   交互手
     * @param ports  与本端口相连的所有其它端口
     * @return 是否已处理；false 时交给默认方块交互
     */
    default boolean onRightClick(Player player, InteractionHand hand, List<IStoragePort> ports) {
        return false;
    }

    /**
     * 该左键是否属于端口行为（用于拦截挖掘）。
     *
     * <p>判定允许记录状态：按住左键期间客户端会不断重触发事件，实现可借此续期「正在取出」
     * 的状态，避免缓存被取空后同一次按住变成挖掘。同一玩家的多次调用必须保持幂等语义。</p>
     *
     * @param player 玩家
     * @param hit    左键命中点；无法取得时为 null
     * @return 命中点不在端口的交互区域（例如模型外边缘的半像素框架）时返回 false
     */
    default boolean interceptsLeftClick(Player player, @Nullable BlockHitResult hit) {
        return false;
    }

    /**
     * 按住左键时客户端取出请求的节流：返回 true 表示本次不发包；仅在客户端调用。
     *
     * @param player 玩家
     */
    default boolean isLeftClickOnCooldown(Player player) {
        return false;
    }

    /**
     * 点击分发：{@link ClickAction#PRIMARY} 为左键，{@link ClickAction#SECONDARY} 为右键。
     *
     * @param action 点击类型
     * @param player 玩家
     * @param hand   交互手
     * @param ports  与本端口相连的所有其它端口
     * @return 是否已处理
     */
    default boolean onClick(ClickAction action, Player player, InteractionHand hand, List<IStoragePort> ports) {
        return switch (action) {
            case PRIMARY -> this.onLeftClick(player, ports);
            case SECONDARY -> this.onRightClick(player, hand, ports);
        };
    }
}
