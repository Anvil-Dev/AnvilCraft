package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.CreativeCrateAttackPacket;
import dev.dubhe.anvilcraft.network.HammerUsePacket;
import dev.dubhe.anvilcraft.network.InfiniteFluidTankBreakModifierPacket;
import dev.dubhe.anvilcraft.util.InfiniteFluidTankBreakProtection;
import dev.dubhe.anvilcraft.util.StateUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ClientBlockEventListener {
    private static BlockPos creativeCrateAttackPos;
    private static boolean attackWasDown;

    @SubscribeEvent
    public static void onCreativeCrateAttack(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(ModBlocks.CREATIVE_CRATE.get())) return;
        ClientBlockEventListener.creativeCrateAttackPos = event.getPos().immutable();
        ClientBlockEventListener.attackWasDown = true;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        boolean attackDown = client.options.keyAttack.isDown();
        if (ClientBlockEventListener.attackWasDown && !attackDown && ClientBlockEventListener.creativeCrateAttackPos != null) {
            ClientPacketDistributor.sendToServer(new CreativeCrateAttackPacket(ClientBlockEventListener.creativeCrateAttackPos));
            ClientBlockEventListener.creativeCrateAttackPos = null;
        }
        ClientBlockEventListener.attackWasDown = attackDown;
        if (client.level == null) {
            ClientBlockEventListener.creativeCrateAttackPos = null;
        }
    }

    @SubscribeEvent
    public static void syncInfiniteFluidTankBreakModifiers(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
            && event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) {
            return;
        }
        if (!InfiniteFluidTankBreakProtection.isProtected(event.getLevel(), event.getPos())) return;

        Minecraft client = Minecraft.getInstance();
        boolean modifiersHeld = client.hasControlDown() && client.hasShiftDown() && client.hasAltDown();
        ClientPacketDistributor.sendToServer(new InfiniteFluidTankBreakModifierPacket(event.getPos(), modifiersHeld));
    }

    /// 侦听右键方块事件
    ///
    /// @param event 右键方块事件
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void anvilHammerUse(PlayerInteractEvent.RightClickBlock event) {
        InteractionHand hand = event.getHand();
        BlockState state = event.getLevel().getBlockState(event.getPos());
        Player entity = event.getEntity();
        if (!entity.getItemInHand(hand).is(ModItemTags.ANVIL_HAMMER)) {
            return;
        }
        if (entity.isShiftKeyDown() && !state.is(ModBlockTags.HAMMER_REMOVABLE) && !(state.getBlock() instanceof IHammerRemovable)) {
            return;
        }
        if (event.getLevel().isClientSide() && ClientBlockEventListener.clientHandle(event, state, hand, event.getHitVec())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (!state.is(BlockTags.CAULDRONS) && !state.is(ModBlockTags.ANVIL_HAMMER_BLACKLIST)
                   && !AnvilHammerItem.shouldPlaceOffhandBlock(entity, event.getLevel(), event.getHitVec())) {
            event.setCanceled(true);
        }
    }

    private static boolean clientHandle(
        PlayerInteractEvent.RightClickBlock event,
        BlockState targetBlockState,
        InteractionHand hand,
        BlockHitResult hitVec
    ) {
        Level level = event.getLevel();
        if (event.getEntity().isShiftKeyDown()) {
            ClientPacketDistributor.sendToServer(new HammerUsePacket(event.getPos(), hand, hitVec));
            return true;
        }
        Property<?> property = AnvilHammerItem.findModifyableProperty(targetBlockState);
        return WheelLifecycleEventListener.openHammerWheel(
            level.getGameTime(),
            level,
            event.getPos(),
            hand,
            property,
            () -> StateUtil.findPossibleStatesForProperty(targetBlockState, property),
            hitVec
        );
    }
    
    /**
     * 主手铁砧锤、副手持有方块且目标为普通方块时，取消使用物品事件，
     * 使客户端跳过铁砧锤的长按使用，继续处理副手并放出副手方块。
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
}
