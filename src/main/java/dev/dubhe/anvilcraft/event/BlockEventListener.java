package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.power.batch.BaseBatchCraftingBlock;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class BlockEventListener {
    private static final Map<UUID, BlockPos> CREATIVE_CRATE_ATTACKS = new HashMap<>();

    public static void clearCreativeCrateAttack(Player player, BlockPos pos) {
        BlockEventListener.CREATIVE_CRATE_ATTACKS.remove(player.getUUID(), pos);
    }

    /// 侦听左键方块事件
    ///
    /// @param event 左键方块事件
    @SubscribeEvent
    public static void anvilHammerAttack(PlayerInteractEvent.LeftClickBlock event) {
        InteractionHand hand = event.getHand();
        if (event.getEntity().getItemInHand(hand).getItem() instanceof AnvilHammerItem) {
            if (!AnvilHammerItem.dropAnvil(event.getEntity(), event.getLevel(), event.getPos())) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onCreativeCrateAttack(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof CreativeCrateBlockEntity crate)) return;
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.STOP
            || event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.ABORT) {
            if (event.getLevel().isClientSide()) {
                return;
            }
            BlockEventListener.clearCreativeCrateAttack(player, event.getPos());
            return;
        }
        if (!player.isCreative() && !crate.getDisplayStack().isEmpty()) {
            event.setUseBlock(TriState.FALSE);
            event.setUseItem(TriState.FALSE);
        }
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!player.isCreative()) {
            BlockPos activePos = BlockEventListener.CREATIVE_CRATE_ATTACKS.get(player.getUUID());
            if (event.getPos().equals(activePos)) {
                event.setUseBlock(TriState.FALSE);
                event.setUseItem(TriState.FALSE);
                return;
            }
        }
        if (crate.onPlayerAttack(player)) {
            if (player.isCreative()) {
                event.setCanceled(true);
            } else {
                if (!event.getLevel().isClientSide()) {
                    BlockEventListener.CREATIVE_CRATE_ATTACKS.put(player.getUUID(), event.getPos().immutable());
                }
                event.setUseBlock(TriState.FALSE);
                event.setUseItem(TriState.FALSE);
            }
        }
    }
    
    /**
     * 主手铁砧锤、副手持有方块且目标为普通方块时，取消使用物品事件，
     * 使服务端不进入铁砧锤长按，配合客户端完成副手方块放置。
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void anvilHammerUseItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!player.getMainHandItem().is(ModItemTags.ANVIL_HAMMER)) return;
        HitResult hit = player.pick(player.blockInteractionRange(), 1.0F, false);
        if (hit instanceof BlockHitResult blockHit && AnvilHammerItem.shouldPlaceOffhandBlock(player, event.getLevel(), blockHit)) {
            event.setCanceled(true);
        }
    }

    /// 侦听右键方块事件
    ///
    /// @param event 右键方块事件
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack stack = player.getItemInHand(hand);
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState targetState = level.getBlockState(pos);
        if (
            stack.getItem() instanceof AnvilHammerItem
            && targetState.getBlock() instanceof IHammerChangeable
        ) {
            if (player.level().isClientSide()) return;
            if (AnvilHammerItem.ableToUseAnvilHammer(level, pos, player)) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        } else if (
            stack.is(Items.IRON_BLOCK)
            && targetState.is(BlockTags.ANVIL)
            && player.isShiftKeyDown()
        ) {
            BlockEventListener.onAnvilFixed(level, stack, pos, targetState);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (targetState.getBlock() instanceof BaseBatchCraftingBlock target && player.isShiftKeyDown()) {
            for (Supplier<BaseBatchCraftingBlock> getter : BaseBatchCraftingBlock.getBatchCraftingBlockGetters()) {
                BaseBatchCraftingBlock block = getter.get();
                if (!stack.is(block.getToastSymbol())) continue;
                if (!level.isClientSide()) {
                    level.setBlockAndUpdate(pos, BaseBatchCraftingBlock.copy(targetState, block.defaultBlockState()));
                    Block.popResourceFromFace(level, pos, Direction.UP, target.getToastSymbol().getDefaultInstance().copyWithCount(1));
                    stack.shrink(1);
                }
                event.setCancellationResult(InteractionResult.CONSUME);
                event.setCanceled(true);
                return;
            }
        }
    }

    public static void onAnvilFixed(LevelAccessor level, ItemStack item, BlockPos pos, BlockState state) {
        if (!state.is(Blocks.CHIPPED_ANVIL) && !state.is(Blocks.DAMAGED_ANVIL)) return;
        RandomSource random = level.getRandom();
        double chance = random.nextDouble();
        item.shrink(1);
        if (chance < 0.1) return;
        Direction facing = state.getValue(AnvilBlock.FACING);
        BlockState intact = Blocks.ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, facing);
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
            BlockState chipped = Blocks.CHIPPED_ANVIL.defaultBlockState().setValue(AnvilBlock.FACING, facing);
            level.setBlock(pos, chipped, 3);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            ParticleUtils.spawnParticles(level, pos, 10, 1.0, 1.0, true, ParticleTypes.HAPPY_VILLAGER);
        }
    }
}
