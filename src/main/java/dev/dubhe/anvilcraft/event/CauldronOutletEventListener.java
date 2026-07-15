package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.LargeCauldronBlock;
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
        if (!blockState.is(BlockTags.CAULDRONS)
            || !(itemStack.getItem() instanceof AnvilHammerItem)
            || blockState.getBlock() instanceof LargeCauldronBlock) {
            return;
        }

        Direction direction = directionFromClickedFace(event.getFace(), player);
        if (direction == Direction.UP) return;
        Vec3 position = direction == Direction.DOWN
            ? bottomOutletPosition(blockPos)
            : sideOutletPosition(blockPos, direction);
        toggleOutlet(level, blockPos, position, direction);
    }

    public static void toggleOutlet(Level level, BlockPos cauldronPos, Vec3 position, Direction direction) {
        if (level.isClientSide()) return;
        CauldronOutletEntity existingOutlet = findExistingOutlet(level, cauldronPos, position);
        if (existingOutlet != null) {
            existingOutlet.kill();
            playOutletSound(level, cauldronPos);
            return;
        }

        removeExistingOutlets(level, cauldronPos);
        level.addFreshEntity(new CauldronOutletEntity(level, position, cauldronPos, direction));
        playOutletSound(level, cauldronPos);
    }

    private static void playOutletSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
    }

    private static Direction directionFromClickedFace(Direction clickedFace, Player player) {
        return clickedFace == Direction.UP ? player.getDirection() : clickedFace;
    }

    private static List<CauldronOutletEntity> getOutlets(Level level, BlockPos cauldronPos) {
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
            entity -> entity.getCauldronPos().equals(cauldronPos)
        );
    }

    private static @Nullable CauldronOutletEntity findExistingOutlet(
        Level level,
        BlockPos cauldronPos,
        Vec3 position
    ) {
        for (CauldronOutletEntity outlet : getOutlets(level, cauldronPos)) {
            if (outlet.position().distanceTo(position) < 0.1) return outlet;
        }
        return null;
    }

    private static void removeExistingOutlets(Level level, BlockPos cauldronPos) {
        for (CauldronOutletEntity outlet : getOutlets(level, cauldronPos)) {
            outlet.kill();
        }
    }

    private static Vec3 sideOutletPosition(BlockPos cauldronPos, Direction direction) {
        double x = cauldronPos.getX() + 0.5 + direction.getStepX() * 0.5;
        double y = cauldronPos.getY() + 0.375 + direction.getStepY() * 0.5;
        double z = cauldronPos.getZ() + 0.5 + direction.getStepZ() * 0.5;
        return new Vec3(x, y, z);
    }

    private static Vec3 bottomOutletPosition(BlockPos cauldronPos) {
        return new Vec3(cauldronPos.getX() + 0.5, cauldronPos.getY() + 0.05, cauldronPos.getZ() + 0.5);
    }
}
