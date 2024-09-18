package dev.dubhe.anvilcraft.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.fml.ModList;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;

public class Util {
    private Util() {}

    /**
     * @return 模组是否加载
     */
    public static boolean isLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }

    public static <I> Function<I, I> unchanged() {
        return a -> a;
    }

    /**
     *
     */
    public static Function<InteractionResult, ItemInteractionResult> interactionResultConverter() {
        return it -> switch (it) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case FAIL -> ItemInteractionResult.FAIL;
        };
    }

    /**
     *
     */
    public static EquipmentSlot convertToSlot(InteractionHand hand) {
        return switch (hand) {
            case MAIN_HAND -> EquipmentSlot.MAINHAND;
            case OFF_HAND -> EquipmentSlot.OFFHAND;
        };
    }

    public static <E> Optional<List<E>> intoOptional(List<E> collection) {
        if (collection.isEmpty()) return Optional.empty();
        return Optional.of(collection);
    }

    public static @NotNull String generateUniqueRecipeSuffix() {
        return "_generated_" + generateRandomString(8, true, false);
    }

    public static @NotNull String generateRandomString(int len) {
        return generateRandomString(len, true, true);
    }

    public static @NotNull String generateRandomString(int len, boolean hasInteger, boolean hasUpperLetter) {
        String ch = "abcdefghijklmnopqrstuvwxyz" + (hasUpperLetter ? "ABCDEFGHIGKLMNOPQRSTUVWXYZ" : "")
                + (hasInteger ? "0123456789" : "");
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < len; i++) {
            Random random = new Random(System.nanoTime());
            int num = random.nextInt(ch.length() - 1);
            stringBuffer.append(ch.charAt(num));
        }
        return stringBuffer.toString();
    }
}
