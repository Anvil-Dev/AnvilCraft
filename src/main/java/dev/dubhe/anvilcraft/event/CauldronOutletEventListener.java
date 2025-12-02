package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.entity.CauldronOutletEntity;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
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

import java.util.List;
import javax.annotation.Nullable;

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
        ) {
            return;
        }

        // 获取应该在哪个方向创建口
        Direction direction = getDirectionFromPlayerFacing(event.getFace(), player);

        // 检查方向，不能在顶部生成
        if (direction == Direction.UP) {
            return;
        }

        // 计算新口的位置，下方用专门的方法
        Vec3 newPosition;
        if (direction == Direction.DOWN) {
            newPosition = calculateMouthPositionForBottom(blockPos);
        } else {
            newPosition = calculateMouthPosition(blockPos, direction);
        }

        // 检查该位置是否已有口，有就移除并播放音效
        CauldronOutletEntity existingMouth = findExistingCauldronMouthAtPosition(level, blockPos, newPosition);

        if (existingMouth != null) {
            existingMouth.kill();
            level.playSound(null, blockPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
            return;
        }

        // 检查该炼药锅是否已有其他口并移除
        removeExistingCauldronMouth(level, blockPos);

        // 创建炼药锅口实体，播放音效
        CauldronOutletEntity cauldronMouthEntity = new CauldronOutletEntity(level, newPosition, blockPos, direction);
        level.addFreshEntity(cauldronMouthEntity);
        level.playSound(null, blockPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 1.0F);
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
            cauldronPos.getX() - 2,
            cauldronPos.getY() - 2,
            cauldronPos.getZ() - 2,
            cauldronPos.getX() + 2,
            cauldronPos.getY() + 2,
            cauldronPos.getZ() + 2
        );
        return level.getEntitiesOfClass(CauldronOutletEntity.class, searchBox, entity -> entity.getCauldronPos().equals(cauldronPos));
    }

    private static @Nullable CauldronOutletEntity findExistingCauldronMouthAtPosition(Level level, BlockPos cauldronPos, Vec3 position) {
        List<CauldronOutletEntity> existingMouths = getCauldronMouths(level, cauldronPos);
        for (CauldronOutletEntity mouth : existingMouths) {
            if (mouth.position().distanceTo(position) < 0.1) {
                return mouth;
            }
        }
        return null;
    }

    private static void removeExistingCauldronMouth(Level level, BlockPos cauldronPos) {
        List<CauldronOutletEntity> existingMouths = getCauldronMouths(level, cauldronPos);
        for (CauldronOutletEntity mouth : existingMouths) {
            mouth.kill();
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