package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.AmuletManager;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.SpacetimeSupercomputerBlockEntity;
import dev.dubhe.anvilcraft.block.utility.BlockDevourerBlock;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.init.ModStats;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.block.ResinBlockItem;
import dev.dubhe.anvilcraft.item.ingredients.ExpGemItem;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.component.amulet.ComradeAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.tool.DragonRodItem;
import dev.dubhe.anvilcraft.item.tool.MultitoolItem;
import dev.dubhe.anvilcraft.item.tool.MultitoolMode;
import dev.dubhe.anvilcraft.network.DragonRodDevourPacket;
import dev.dubhe.anvilcraft.util.GravityManager;
import dev.dubhe.anvilcraft.util.InfiniteFluidTankBreakProtection;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.UseItemOnBlockEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class PlayerEventListener {
    @SubscribeEvent
    public static void onJoinedServer(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            SpacetimeSupercomputerBlockEntity.cancelPendingTickSprints(serverPlayer.level().getServer());
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        InfiniteFluidTankBreakProtection.clear(event.getEntity());
    }

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
        if (item.is(ModItems.EXP_GEM)) {
            InteractionResult result = ExpGemItem.useEntity(player, target, item);
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
            || MultitoolItem.isActingAs(item, MultitoolMode.MAGNET)
            || item.is(ModItemTags.ANVIL_HAMMER)
        ) {
            return;
        }
        if (!player.isShiftKeyDown() || entities.isEmpty()) {
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
    public static void preventUseWhenRocketJump(UseItemOnBlockEvent event) {
        var player = event.getPlayer();
        if (
            event.getUsePhase() == UseItemOnBlockEvent.UsePhase.ITEM_AFTER_BLOCK
            && event.getHand() == InteractionHand.OFF_HAND
            && player.getMainHandItem().is(ModItemTags.ANVIL_HAMMER)
            && AnvilHammerItem.canRocketJump(player)
        ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void handleDragonRod(PlayerInteractEvent.LeftClickBlock event) {
        final Level level = event.getLevel();
        final BlockPos pos = event.getPos();
        final BlockState state = level.getBlockState(pos);
        final Player player = event.getEntity();
        final InteractionHand hand = event.getHand();
        final ItemStack stack = event.getItemStack();
        final Direction blockFace = event.getFace();

        if (blockFace == null) return;
        if (state.getDestroySpeed(level, pos) < 0.0F) return;
        if (!stack.has(ModComponents.DEVOUR_RANGE)) return;
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.STOP
            || event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.ABORT) {
            DragonRodItem.stopContinuousMode(player);
            return;
        }
        if (!DragonRodItem.canDevour(player, stack) || !BlockDevourerBlock.canDevour(state)) {
            event.setCanceled(true);
            return;
        }

        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START && !level.isClientSide()) {
            DragonRodItem.devourBlock((ServerLevel) level, player, hand, pos, state, blockFace);
        } else if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.CLIENT_HOLD) {
            ClientPacketDistributor.sendToServer(new DragonRodDevourPacket(hand, pos, blockFace));
        }
    }

    @SubscribeEvent
    public static void onJoinedLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            PowerGrid.MANAGER.onPlayerJoined(event.getLevel(), sp);
            GravityManager.syncTo(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerUsingTotem(LivingUseTotemEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack inHand = player.getItemInHand(event.getHandHolding());
        if (!inHand.is(ModItems.AMULET_BOX.asItem())) return;
        if (inHand.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY).totems().isEmpty()) {
            event.setCanceled(true);
        }
        AmuletManager.get(player.registryAccess()).tryRaffle(player, event.getSource());
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingIncomingDamageEvent event) {
        if (
            event.getEntity() instanceof ServerPlayer player
            && AmuletManager.get(player.registryAccess()).shouldImmune(player, event.getSource())
        ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerUse(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.has(ModComponents.AMULET)) {
            return;
        }
        IAmulet amulet = stack.get(ModComponents.AMULET);
        if (!(amulet instanceof ComradeAmulet comrade)) {
            return;
        }
        ComradeAmulet signed = comrade.sign(event.getEntity());
        if (comrade == signed) {
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        if (event.getLevel().isClientSide()) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }
        stack.set(ModComponents.AMULET, signed);
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
    }

    @SubscribeEvent
    public static void onPlayerPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (!(be instanceof IPowerComponent)) return;
        player.awardStat(ModStats.PLACE_POWER_COMPONENT);
    }
}
