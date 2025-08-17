package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class BlockEventListener {
    /**
     * 侦听左键方块事件
     *
     * @param event 左键方块事件
     */
    @SubscribeEvent
    public static void anvilHammerAttack(@NotNull PlayerInteractEvent.LeftClickBlock event) {
        InteractionHand hand = event.getHand();
        if (event.getEntity().getItemInHand(hand).getItem() instanceof AnvilHammerItem) {
            if (!AnvilHammerItem.dropAnvil(event.getEntity(), event.getLevel(), event.getPos())) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 侦听右键方块事件
     *
     * @param event 右键方块事件
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(@NotNull PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack itemStack = player.getItemInHand(hand);
        Level level = event.getLevel();
        BlockPos blockPos = event.getPos();
        BlockState targetBlockState = level.getBlockState(blockPos);
        if (
            itemStack.getItem() instanceof AnvilHammerItem
                || (itemStack.is(Tags.Items.TOOLS_WRENCH) && targetBlockState.getBlock() instanceof IHammerChangeable)
        ) {
            if (player.level().isClientSide()) return;
            if (AnvilHammerItem.ableToUseAnvilHammer(level, blockPos, player)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        } else if (
            itemStack.is(Items.IRON_BLOCK)
                && targetBlockState.is(BlockTags.ANVIL)
                && player.isShiftKeyDown()
        ) {
            onAnvilFixed(level, itemStack, blockPos, targetBlockState);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    public static void onAnvilFixed(@NotNull LevelAccessor level, ItemStack item, BlockPos pos, @NotNull BlockState state) {
        if (!state.is(Blocks.CHIPPED_ANVIL) && !state.is(Blocks.DAMAGED_ANVIL)) return;
        RandomSource random = level.getRandom();
        double chance = random.nextDouble();
        item.shrink(1);
        if (chance < 0.1) return;
        Direction facing = state.getValue(AnvilBlock.FACING);
        BlockState intact = Blocks.ANVIL.defaultBlockState();
        intact.setValue(AnvilBlock.FACING, facing);
        if (state.is(Blocks.CHIPPED_ANVIL)) {
            level.setBlock(pos, intact, 3);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            ParticleUtils.spawnParticles(level, pos, 10, 1.0, 1.0, true, ParticleTypes.HAPPY_VILLAGER);
        } else if (state.is(Blocks.DAMAGED_ANVIL)) {
            if (chance < 0.2) {
                level.setBlock(pos, intact, 3);
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                ParticleUtils.spawnParticles(level, pos, 10, 1.0, 1.0, true, ParticleTypes.HAPPY_VILLAGER);
                return;
            }
            BlockState chipped = Blocks.CHIPPED_ANVIL.defaultBlockState();
            chipped.setValue(AnvilBlock.FACING, facing);
            level.setBlock(pos, chipped, 3);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            ParticleUtils.spawnParticles(level, pos, 10, 1.0, 1.0, true, ParticleTypes.HAPPY_VILLAGER);
        }
    }
}
