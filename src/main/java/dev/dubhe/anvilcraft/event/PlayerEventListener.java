package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.item.ResinBlockItem;

import dev.dubhe.anvilcraft.item.amulet.ComradeAmuletItem;
import dev.dubhe.anvilcraft.recipe.anvil.cache.RecipeCaches;
import dev.dubhe.anvilcraft.util.AmuletUtil;
import dev.dubhe.anvilcraft.util.InventoryUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;


@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class PlayerEventListener {
    /**
     * @param event 玩家右键实体事件
     */
    @SubscribeEvent
    public static void useEntity(@NotNull PlayerInteractEvent.EntityInteract event) {
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

    @SuppressWarnings("DataFlowIssue")
    @SubscribeEvent
    public static void onPlayerUsingTotem(LivingUseTotemEvent event) {
        if (
            event.getEntity() instanceof ServerPlayer player
            && InventoryUtil.hasItem(player.getInventory(), ModItems.AMULET_BOX.asItem())
        ) {
            Inventory inventory = player.getInventory();
            AmuletUtil.startRaffle(
                player, event.getSource(),
                InventoryUtil.getFirstItem(inventory, ModItems.AMULET_BOX).get(ModComponents.TOTEM_COUNT) >= 0
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            DamageSource source = event.getSource();

            try {
                ItemStack comrade = AmuletUtil.getEffectiveAmulet(player, ModItems.COMRADE_AMULET);
                UUID causingEntityUUID = Objects.requireNonNull(source.getEntity()).getUUID();
                if (!comrade.equals(ItemStack.EMPTY) && ComradeAmuletItem.canIgnorePlayer(comrade, causingEntityUUID)) {
                    event.setCanceled(true);
                }
            } catch (NullPointerException ignored) {
            }

            if (AmuletUtil.shouldIgnoreDamage(player, source)) {
                event.setCanceled(true);
            }
        }
    }
}
