package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.client.gui.screen.AnvilHammerScreen;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.HammerUsePacket;
import dev.dubhe.anvilcraft.util.StateUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
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
        ItemStack itemStack = event.getEntity().getItemInHand(hand);
        BlockState targetBlockState = event.getLevel().getBlockState(event.getPos());
        if (itemStack.getItem() instanceof AnvilHammerItem
            || (itemStack.is(Tags.Items.TOOLS_WRENCH) && targetBlockState.getBlock() instanceof IHammerChangeable)
        ) {
            if (AnvilHammerItem.ableToUseAnvilHammer(event.getLevel(), event.getPos(), event.getEntity())) {
                BlockState b = event.getLevel().getBlockState(event.getPos());
                if (event.getEntity().isShiftKeyDown()) {
                    if (!b.is(ModBlockTags.HAMMER_REMOVABLE) && !(b.getBlock() instanceof IHammerRemovable)) {
                        return;
                    }
                }
                if (event.getLevel().isClientSide() && clientHandle(event, targetBlockState, hand)) {
                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            }
        }
    }


    private static boolean clientHandle(PlayerInteractEvent.@NotNull RightClickBlock event, BlockState targetBlockState, InteractionHand hand) {
        Property<?> property = AnvilHammerItem.findModifyableProperty(targetBlockState);
        if (!event.getEntity().isShiftKeyDown() && property != null) {
            if (event.getEntity().getAbilities().mayBuild) {
                List<BlockState> possibleStates = StateUtil.findPossibleStatesForProperty(targetBlockState, property);
                if (possibleStates.isEmpty()) {
                    if (event.getEntity().isShiftKeyDown()) {
                        PacketDistributor.sendToServer(new HammerUsePacket(event.getPos(), hand));
                        return true;
                    }
                    return false;
                } else {
                    Minecraft.getInstance().setScreen(new AnvilHammerScreen(
                        event.getPos(),
                        targetBlockState,
                        property,
                        possibleStates
                    ));
                }
                return true;
            }
        } else {
            if (event.getEntity().isShiftKeyDown()) {
                PacketDistributor.sendToServer(new HammerUsePacket(event.getPos(), hand));
                return true;
            }
        }
        return false;
    }
}
