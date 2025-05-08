package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
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
    public static void anvilHammerUse(@NotNull PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().level().isClientSide) return;
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
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
            }
        }
    }
}
