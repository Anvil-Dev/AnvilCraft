package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.client.gui.screen.AnvilHammerScreen;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.HammerUsePacket;
import dev.dubhe.anvilcraft.util.StateUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientBlockEventListener {
    /**
     * 侦听右键方块事件
     *
     * @param event 右键方块事件
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void anvilHammerUse(@NotNull PlayerInteractEvent.RightClickBlock event) {
        InteractionHand hand = event.getHand();
        BlockState state = event.getLevel().getBlockState(event.getPos());
        Player entity = event.getEntity();
        if (
            !entity.getItemInHand(hand).is(ModItemTags.ANVIL_HAMMER)
                && !entity.getItemInHand(hand).is(ModItemTags.WRENCH)
        ) return;
        if (entity.isShiftKeyDown()) {
            if (!state.is(ModBlockTags.HAMMER_REMOVABLE) && !(state.getBlock() instanceof IHammerRemovable)) {
                return;
            }
        }
        if (event.getLevel().isClientSide() && clientHandle(event, state, hand)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }


    private static boolean clientHandle(PlayerInteractEvent.@NotNull RightClickBlock event, BlockState targetBlockState, InteractionHand hand) {
        Property<?> property = AnvilHammerItem.findModifyableProperty(targetBlockState);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return false;
        if (property != null) {
            if (event.getEntity().isShiftKeyDown()) {
                PacketDistributor.sendToServer(new HammerUsePacket(event.getPos(), hand));
                return false;
            }
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
                        hand
                    )
                );
            }
            return true;
        } else {
            PacketDistributor.sendToServer(new HammerUsePacket(event.getPos(), hand));
        }
        return false;
    }
}
