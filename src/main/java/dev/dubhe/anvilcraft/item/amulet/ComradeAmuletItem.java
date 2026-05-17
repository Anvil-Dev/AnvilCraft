package dev.dubhe.anvilcraft.item.amulet;

import dev.anvilcraft.lib.v2.util.InventoryUtil;
import dev.dubhe.anvilcraft.api.amulet.type.AmuletType;
import dev.dubhe.anvilcraft.init.item.ModAmuletTypes;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.SignedPlayers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

public class ComradeAmuletItem extends AmuletItem {
    public ComradeAmuletItem(Properties properties) {
        super(properties.component(ModComponents.SIGNED_PLAYERS, SignedPlayers.EMPTY));
    }

    @Override
    public Holder<AmuletType> getType() {
        return ModAmuletTypes.COMRADE;
    }

    @SuppressWarnings("unused")
    public static void inventoryTick(ServerPlayer player, ItemStack amulet, boolean isEnabled) {
        amulet.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, !ComradeAmuletItem.getSignedPlayers(amulet).isEmpty());
    }

    public static boolean shouldImmuneDamage(ServerPlayer player, DamageSource source) {
        ItemStack comrade = Optional.of(InventoryUtil.getFirstItem(player.getInventory(), ModItems.COMRADE_AMULET))
            .filter(ItemStack::isEmpty)
            .orElse(InventoryUtil.getItemInCompat(player, stack -> stack.is(ModItems.COMRADE_AMULET)));
        return Optional.ofNullable(source.getEntity())
            .map(Entity::getUUID)
            .filter(uuid -> !comrade.isEmpty() && ComradeAmuletItem.canIgnorePlayer(comrade, uuid))
            .isPresent();
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (ComradeAmuletItem.registerPlayerToAmulet(player.getItemInHand(usedHand), player)) {
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    public static boolean registerPlayerToAmulet(ItemStack amulet, Player player) {
        amulet.set(ModComponents.SIGNED_PLAYERS, ComradeAmuletItem.getSignedPlayers(amulet).sign(player));
        return true;
    }

    public static boolean canIgnorePlayer(ItemStack amulet, UUID playerUUID) {
        for (SignedPlayers.Info info : ComradeAmuletItem.getSignedPlayers(amulet).players()) {
            if (info.id().equals(playerUUID)) return true;
        }
        return false;
    }

    public static SignedPlayers getSignedPlayers(DataComponentGetter getter) {
        return getter.getOrDefault(ModComponents.SIGNED_PLAYERS, SignedPlayers.EMPTY);
    }
}
