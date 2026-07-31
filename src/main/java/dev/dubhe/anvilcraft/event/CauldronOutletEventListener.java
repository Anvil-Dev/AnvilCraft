package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
import dev.dubhe.anvilcraft.entity.CauldronOutletEntity;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jspecify.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class CauldronOutletEventListener {

    @SubscribeEvent
    public static void onPlayerUseAnvilHammerOnCauldron(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack itemStack = player.getItemInHand(event.getHand());
        Level level = event.getLevel();
        BlockPos blockPos = event.getPos();
        BlockState blockState = level.getBlockState(blockPos);

        // 检查是否是炼药锅且手持铁砧锤
        if (
            !blockState.is(BlockTags.CAULDRONS)
            || !(itemStack.getItem() instanceof AnvilHammerItem)
            || blockState.getBlock() instanceof LargeCauldronBlock
        ) {
            return;
        }

        // 获取应该在哪个方向创建口
        Direction direction = CauldronOutletEventListener.getDirectionFromPlayerFacing(event.getFace(), player);

        // 检查方向，不能在顶部生成
        if (direction == Direction.UP) {
            return;
        }

        // 计算新口的位置，下方用专门的方法
        Vec3 newPosition;
        if (direction == Direction.DOWN) {
            newPosition = CauldronOutletEventListener.calculateMouthPositionForBottom(blockPos);
        } else {
            newPosition = CauldronOutletEventListener.calculateMouthPosition(blockPos, direction);
        }
        if (level.isClientSide()) return;

        // 检查该位置是否已有口，有就移除并播放音效
        CauldronOutletEntity existingMouth = CauldronOutletEventListener.findExistingCauldronMouthAtPosition(level, blockPos, newPosition);

        if (existingMouth != null) {
            existingMouth.discard();
            CauldronOutletEventListener.playOutletSound(level, blockPos);
            return;
        }

        // 检查该炼药锅是否已有其他口并移除
        CauldronOutletEventListener.removeExistingCauldronMouth(level, blockPos);
        CauldronOutletEventListener.removeOpposingOutlet(level, blockPos, direction);

        // 创建炼药锅口实体，播放音效
        CauldronOutletEntity cauldronMouthEntity = new CauldronOutletEntity(level, newPosition, blockPos, direction);
        level.addFreshEntity(cauldronMouthEntity);
        CauldronOutletEventListener.playOutletSound(level, blockPos);
    }

    private static void playOutletSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private static Direction getDirectionFromPlayerFacing(Direction clickedFace, Player player) {
        // 如果点击的是上表面，则根据玩家朝向确定方向
        if (clickedFace == Direction.UP) {
            return player.getDirection();
        }
        return clickedFace;
    }

    private static List<CauldronOutletEntity> getCauldronMouths(Level level, BlockPos cauldronPos) {
        AABB searchBox = new AABB(
            cauldronPos.getX() - 4,
            cauldronPos.getY() - 4,
            cauldronPos.getZ() - 4,
            cauldronPos.getX() + 4,
            cauldronPos.getY() + 4,
            cauldronPos.getZ() + 4
        );
        return level.getEntitiesOfClass(
            CauldronOutletEntity.class,
            searchBox,
            entity -> !entity.isRemoved() && entity.getCauldronPos().equals(cauldronPos)
        );
    }

    private static @Nullable CauldronOutletEntity findExistingCauldronMouthAtPosition(Level level, BlockPos cauldronPos, Vec3 position) {
        List<CauldronOutletEntity> existingMouths = CauldronOutletEventListener.getCauldronMouths(level, cauldronPos);
        for (CauldronOutletEntity mouth : existingMouths) {
            if (mouth.position().distanceTo(position) < 0.1) {
                return mouth;
            }
        }
        return null;
    }

    private static void removeExistingCauldronMouth(Level level, BlockPos cauldronPos) {
        List<CauldronOutletEntity> existingMouths = CauldronOutletEventListener.getCauldronMouths(level, cauldronPos);
        for (CauldronOutletEntity mouth : existingMouths) {
            mouth.discard();
        }
    }

    private static void removeOpposingOutlet(Level level, BlockPos cauldronPos, Direction direction) {
        BlockPos targetPos = cauldronPos.relative(direction);
        for (CauldronOutletEntity outlet : CauldronOutletEventListener.getCauldronMouths(level, targetPos)) {
            if (outlet.getAttachedDirection() == direction.getOpposite()) outlet.discard();
        }
    }

    private static Vec3 calculateMouthPosition(BlockPos cauldronPos, Direction direction) {
        double x = cauldronPos.getX() + 0.5 + (direction.getStepX() * 0.5);
        double y = cauldronPos.getY() + 0.375 + (direction.getStepY() * 0.5);
        double z = cauldronPos.getZ() + 0.5 + (direction.getStepZ() * 0.5);
        return new Vec3(x, y, z);
    }

    private static Vec3 calculateMouthPositionForBottom(BlockPos cauldronPos) {
        return new Vec3(cauldronPos.getX() + 0.5, cauldronPos.getY() + 0.05, cauldronPos.getZ() + 0.5);
    }
}
