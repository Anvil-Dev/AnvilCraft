package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModDataAttachments;
import dev.dubhe.anvilcraft.init.ModItemTags;
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
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingUseTotemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
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

    @SubscribeEvent
    public static void onPlayerUsingTotem(LivingUseTotemEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getInventory().contains(ModItems.AMULET_BOX.asStack())) {
            Inventory inventory = player.getInventory();

            boolean isConsumeAmuletBox;
            if (inventory.contains(Items.TOTEM_OF_UNDYING.getDefaultInstance())) {
                inventory.removeItem(Items.TOTEM_OF_UNDYING.getDefaultInstance());
                isConsumeAmuletBox = false;
            } else {
                inventory.removeItem(ModItems.AMULET_BOX.asStack());
                isConsumeAmuletBox = true;
            }

            AmuletUtil.startRaffle(player, event.getSource(), isConsumeAmuletBox);
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            DamageSource source = event.getSource();
            DamageSources sources = player.damageSources();
            Inventory inventory = player.getInventory();

            if (
                (source.type().equals(sources.damageTypes.get(DamageTypes.FALLING_ANVIL))
                    || (source.type().equals(sources.damageTypes.get(DamageTypes.FALLING_BLOCK)) && source.getEntity() instanceof FallingGiantAnvilEntity)
                    || Optional.ofNullable(source.getWeaponItem()).filter(item -> item.is(ModItemTags.ANVIL_HAMMER)).isPresent())
                    && player.getData(ModDataAttachments.STEEL_HEAD)
            ) {
                event.getContainer().setNewDamage(0);
            }

            ItemStack comrade = InventoryUtil.getFirstItem(inventory, ModItems.COMRADE_AMULET);
            try {
                UUID causingEntityUUID = Objects.requireNonNull(source.getEntity()).getUUID();
                if (!comrade.equals(ItemStack.EMPTY) && ComradeAmuletItem.canIgnorePlayer(comrade, causingEntityUUID)) {
                    event.getContainer().setNewDamage(0);
                }
            } catch (NullPointerException ignored) {
            }

            if (
                source.type().equals(sources.damageTypes.get(DamageTypes.FALL))
                    && player.getData(ModDataAttachments.NO_FALL_DAMAGE)
            ) {
                event.getContainer().setNewDamage(0);
            }
        }
    }
}
