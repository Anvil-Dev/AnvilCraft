package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.IStoragePort;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import javax.annotation.Nullable;

/**
 * 仓储端口方块父类：把左右键行为重定向到 {@link IStoragePort} 的实现。
 *
 * <p>右键走原版方块交互入口，转发给 {@link IStoragePort#onRightClick}；左键没有原版方块入口
 * （挖掘在网络层被事件拦截），因此由 {@link #interceptsLeftClick} 判定是否拦截挖掘、
 * {@link #onLeftClick} 执行服务端行为，两者分别由左键事件与取出请求包调用。</p>
 *
 * <p>三个端口都没有容积以外的特殊交互，渲染形状统一为模型，故一并放在父类里。</p>
 */
public abstract class AbstractStoragePortBlock extends BaseEntityBlock implements IHammerRemovable {
    protected AbstractStoragePortBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        if (hand != InteractionHand.MAIN_HAND) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(level.getBlockEntity(pos) instanceof IStoragePort port)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        List<IStoragePort> ports = StoragePortManager.scan(level, pos).ports();
        return port.onClick(ClickAction.SECONDARY, player, hand, ports)
            ? ItemInteractionResult.sidedSuccess(level.isClientSide())
            : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * 该左键是否属于端口行为（拦截挖掘）。
     *
     * @param player 玩家
     * @param hit    左键命中点；无法取得时为 null
     * @return 是否拦截挖掘
     */
    public boolean interceptsLeftClick(Level level, BlockPos pos, Player player, @Nullable BlockHitResult hit) {
        return level.getBlockEntity(pos) instanceof IStoragePort port && port.interceptsLeftClick(player, hit);
    }

    /**
     * 执行端口的左键行为（仅服务端调用）。
     *
     * @return 是否已处理
     */
    public boolean onLeftClick(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof IStoragePort port)) {
            return false;
        }
        List<IStoragePort> ports = StoragePortManager.scan(level, pos).ports();
        return port.onClick(ClickAction.PRIMARY, player, InteractionHand.MAIN_HAND, ports);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
