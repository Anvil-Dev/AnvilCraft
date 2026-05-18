package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.util.StateUtil;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientBlockEventListener {
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
        Level level = event.getLevel();
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
}
