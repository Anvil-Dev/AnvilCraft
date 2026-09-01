package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.block.batch.BaseBatchCraftingBlock;
import dev.dubhe.anvilcraft.block.entity.CreativeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.StoragePortBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.StoragePortTakeOutPacket;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Supplier;
import javax.annotation.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class BlockEventListener {
    /**
     * 侦听左键方块事件
     *
     * @param event 左键方块事件
     */
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
    public static void clickCreativeCrateEvent(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CreativeCrateBlockEntity creativeCrateBlockEntity)) {
            return;
        }
        ItemStack stackInSlot = creativeCrateBlockEntity.getItemStackHandler().getStackInSlot(0);
        if (player.isCreative()) {
            if (!stackInSlot.isEmpty()) {
                if (!player.addItem(stackInSlot.copyWithCount(1))) {
                    Block.popResource(level, BlockPos.containing(player.position()), stackInSlot.copyWithCount(1));
                }
                creativeCrateBlockEntity.getItemStackHandler().setStackInSlot(0, ItemStack.EMPTY);
                if (level.isClientSide()) {
                    event.setCanceled(true);
                }
                event.setCanceled(true);
            }
        } else {
            if (!stackInSlot.isEmpty()) {
                if (player.isShiftKeyDown()) {
                    if (!player.addItem(stackInSlot.copyWithCount(stackInSlot.getMaxStackSize()))) {
                        Block.popResource(
                            level,
                            BlockPos.containing(player.position()),
                            stackInSlot.copyWithCount(stackInSlot.getMaxStackSize())
                        );
                    }
                } else {
                    if (!player.addItem(stackInSlot.copyWithCount(1))) {
                        Block.popResource(level, BlockPos.containing(player.position()), stackInSlot.copyWithCount(1));
                    }
                }
                if (level.isClientSide()) {
                    event.setCanceled(true);
                }
                event.setCanceled(true);
            }
        }
    }

    /**
     * 左键仓储端口取出物品：左键取 1 个，shift 左键取一组；手持铁砧锤时不触发。
     * 点击外边缘半像素（1/32）框架时走正常挖掘；缓存为空时也不取消事件，让玩家可以挖掘方块。
     *
     * <p>取出全部在服务端执行：客户端只取消事件（阻止挖掘）并发送取出请求包，
     * 不在本地改动任何物品，避免幻影物品与快速点击刷物品。按住左键时（取消事件后客户端
     * 不会进入挖掘状态）{@code START} 事件会每 tick 重触发，用节流限制发包频率。</p>
     */
    @SubscribeEvent
    public static void clickStoragePortEvent(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        if (!(level.getBlockEntity(pos) instanceof StoragePortBlockEntity port)) {
            return;
        }
        if (player.getMainHandItem().getItem() instanceof AnvilHammerItem) {
            return;
        }
        // 外边缘半像素框架：敲掉逻辑，不取出
        BlockHitResult aim = BlockEventListener.aimHit(player);
        if (aim != null && aim.getBlockPos().equals(pos) && StoragePortBlockEntity.isEdgeHit(aim)) {
            return;
        }
        // 缓存空：不拦截，允许挖掘
        if (port.isBufferEmpty()) {
            return;
        }
        // 取消事件，阻止挖掘（客户端与服务端都会触发本事件）
        event.setCanceled(true);
        if (!level.isClientSide()) {
            return;
        }
        // 客户端只发送取出请求，实际取出由服务端在收到请求包后执行
        PlayerInteractEvent.LeftClickBlock.Action action = event.getAction();
        if (action != PlayerInteractEvent.LeftClickBlock.Action.START
            && action != PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) {
            return;
        }
        if (port.onTakeOutHoldCooldown(player)) {
            return;
        }
        PacketDistributor.sendToServer(new StoragePortTakeOutPacket(pos, player.isShiftKeyDown()));
    }

    /**
     * 沿玩家视线做射线检测，得到精确的瞄准位置（左键事件本身不提供命中点）。
     */
    @Nullable
    private static BlockHitResult aimHit(Player player) {
        HitResult pick = player.pick(player.blockInteractionRange(), 1.0F, false);
        return pick instanceof BlockHitResult hitResult ? hitResult : null;
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

    @SubscribeEvent
    public static void useMagnetBlockWithOffhandItem(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (event.getHand() != InteractionHand.MAIN_HAND
            || !player.isShiftKeyDown()
            || !player.getMainHandItem().isEmpty()
            || !event.getLevel().getBlockState(event.getPos()).is(ModBlocks.MAGNET_BLOCK)) {
            return;
        }
        event.setUseBlock(TriState.TRUE);
    }

    /**
     * 侦听右键方块事件
     *
     * @param event 右键方块事件
     */
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
            onAnvilFixed(level, stack, pos, targetState);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (targetState.getBlock() instanceof BaseBatchCraftingBlock target && player.isShiftKeyDown()) {
            for (Supplier<BaseBatchCraftingBlock> getter : BaseBatchCraftingBlock.getBatchCraftingBlockGetters()) {
                BaseBatchCraftingBlock block = getter.get();
                if (!stack.is(block.getToastSymbol())) continue;
                if (!level.isClientSide) {
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
