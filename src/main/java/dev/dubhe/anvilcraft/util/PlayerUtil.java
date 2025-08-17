package dev.dubhe.anvilcraft.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.Optional;
import java.util.function.Predicate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PlayerUtil {

    public static boolean isFakePlayer(Player player) {
        return player instanceof FakePlayer;
    }

    public static EquipmentSlot handToSlot(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }

    public static InteractionHand anotherHand(InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> InteractionHand.OFF_HAND;
            case OFF_HAND -> InteractionHand.MAIN_HAND;
        };
    }

    public static Optional<InteractionHand> getHand(ServerPlayer player, Predicate<ItemStack> filter) {
        if (filter.test(player.getMainHandItem())) {
            return Optional.of(InteractionHand.MAIN_HAND);
        } else if (filter.test(player.getOffhandItem())) {
            return Optional.of(InteractionHand.OFF_HAND);
        } else {
            return Optional.empty();
        }
    }
}
