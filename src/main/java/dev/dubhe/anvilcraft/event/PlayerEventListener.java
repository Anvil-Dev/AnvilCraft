package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.item.ResinBlockItem;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.DragonRodItem;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.network.DragonRodDevourPacket;
import dev.dubhe.anvilcraft.recipe.anvil.cache.RecipeCaches;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class PlayerEventListener {
    /**
     * @param event 玩家右键实体事件
     */
    @SubscribeEvent
    public static void useEntity(PlayerInteractEvent.EntityInteract event) {
        InteractionHand hand = event.getHand();
        Player player = event.getEntity();
        ItemStack item = player.getItemInHand(hand);
        Entity target = event.getTarget();
        if (item.is(ModBlocks.RESIN_BLOCK.asItem())) {
            InteractionResult result = ResinBlockItem.useEntity(player, target, item);
            if (result != InteractionResult.PASS) {
                event.setCancellationResult(result);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void playerRightClickMagnetizedNode(PlayerInteractEvent.RightClickBlock event) {
        InteractionHand hand = event.getHand();
        if (hand != InteractionHand.MAIN_HAND) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        ItemStack item = player.getItemInHand(hand);
        List<MagnetizedNodeEntity> entities = level.getEntitiesOfClass(
            MagnetizedNodeEntity.class,
            new AABB(pos).expandTowards(0.0, 0.0625, 0.0)
        );
        if (
            item.is(ModItems.MAGNET)
            || (item.is(ModItems.MULTITOOL_ITEM) && MultitoolItem.getMode(item) == MultitoolItem.MAGNET_MODE)
            || item.is(Tags.Items.BUCKETS)
        ) {
            return;
        }
        if (player.isShiftKeyDown() || entities.isEmpty()) {
            return;
        }
        MagnetizedNodeEntity node = entities.getFirst();
        ItemStack stack = item.copy();
        stack.setCount(1);
        if (!player.isCreative()) {
            item.shrink(1);
        }
        ItemEntity itemEntity = new ItemEntity(level, node.position().x, node.position().y, node.position().z, stack);
        itemEntity.setDeltaMovement(0, 0, 0);
        itemEntity.setPickUpDelay(60);
        level.addFreshEntity(itemEntity);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void handleDragonRod(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack stack = event.getItemStack();
        Direction blockFace = event.getFace();

        if (blockFace == null) return;
        if (state.getDestroySpeed(level, pos) == 0.0F) return;
        if (!stack.has(ModComponents.DEVOUR_RANGE)) return;
        if (!DragonRodItem.canDevour(player, stack)) event.setCanceled(true);

        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START && !level.isClientSide) {
            DragonRodItem.devourBlock((ServerLevel) level, player, hand, pos, state, blockFace);
        } else if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) {
            PacketDistributor.sendToServer(new DragonRodDevourPacket(level.dimension(), hand, pos, blockFace));
        }
    }

    @SubscribeEvent
    public static void onJoinedLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PowerGrid.MANAGER.onPlayerJoined(event.getLevel(), sp);
        }
    }

    @SubscribeEvent
    public static void onJoinedServer(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            RecipeCaches.sync(serverPlayer);
        }
    }

    @SubscribeEvent
    public static void onPlayerUsingTotem(LivingUseTotemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack inHand = player.getItemInHand(event.getHandHolding());
        if (!inHand.is(ModItems.AMULET_BOX.asItem())) return;
        if (inHand.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY).totems().isEmpty()) return;
        AmuletManager.INSTANCE.startRaffle(player, event.getSource());
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
            && AmuletManager.INSTANCE.shouldIgnoreDamage(player, event.getSource())
        ) {
            event.setCanceled(true);
        }
    }
}
