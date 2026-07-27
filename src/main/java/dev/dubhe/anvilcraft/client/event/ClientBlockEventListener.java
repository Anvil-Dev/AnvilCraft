package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.client.gui.screen.AnvilHammerScreen;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.HammerUsePacket;
import dev.dubhe.anvilcraft.network.InfiniteFluidTankBreakModifierPacket;
import dev.dubhe.anvilcraft.util.InfiniteFluidTankBreakProtection;
import dev.dubhe.anvilcraft.util.StateUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientBlockEventListener {
    @SubscribeEvent
    public static void syncInfiniteFluidTankBreakModifiers(PlayerInteractEvent.LeftClickBlock event) {
        if (!event.getLevel().isClientSide()) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START
            && event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) {
            return;
        }
        if (!InfiniteFluidTankBreakProtection.isProtected(event.getLevel(), event.getPos())) return;

        boolean modifiersHeld = Screen.hasControlDown() && Screen.hasShiftDown() && Screen.hasAltDown();
        PacketDistributor.sendToServer(new InfiniteFluidTankBreakModifierPacket(event.getPos(), modifiersHeld));
    }

    /**
     * 侦听右键方块事件
     *
     * @param event 右键方块事件
     */
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
        if (event.getLevel().isClientSide() && clientHandle(event, state, hand, event.getHitVec())) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (!state.is(BlockTags.CAULDRONS) && !state.is(ModBlockTags.ANVIL_HAMMER_BLACKLIST)) {
            event.setCanceled(true);
        }
    }

    private static boolean clientHandle(
        PlayerInteractEvent.RightClickBlock event,
        BlockState targetBlockState,
        InteractionHand hand,
        BlockHitResult hitVec
    ) {
        Property<?> property = AnvilHammerItem.findModifyableProperty(targetBlockState);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        if (player.isShiftKeyDown()) {
            PacketDistributor.sendToServer(new HammerUsePacket(event.getPos(), hand, hitVec));
            return true;
        }
        if (property != null) {
            if (!event.getEntity().getAbilities().mayBuild) return false;
            if (!AnvilHammerItem.ableToUseAnvilHammer(event.getLevel(), event.getPos(), event.getEntity())) return false;
            List<BlockState> possibleStates = StateUtil.findPossibleStatesForProperty(targetBlockState, property);
            if (!possibleStates.isEmpty()) {
                Minecraft.getInstance().setScreen(
                    new AnvilHammerScreen(
                        event.getPos(),
                        targetBlockState,
                        property,
                        possibleStates,
                        hand,
                        hitVec
                    )
                );
            }
            return true;
        } else {
            boolean interacted = AnvilHammerItem.interactWithBlock(
                event.getEntity(), event.getPos(), event.getLevel(), event.getEntity().getItemInHand(hand), hand, hitVec
            );
            PacketDistributor.sendToServer(new HammerUsePacket(event.getPos(), hand, hitVec));
            return interacted;
        }
    }
}
