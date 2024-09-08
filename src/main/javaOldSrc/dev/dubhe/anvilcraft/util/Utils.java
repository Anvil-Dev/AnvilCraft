package dev.dubhe.anvilcraft.util;


import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.neoforged.fml.ModList;

import java.util.function.Function;

public abstract class Utils {
    private Utils() {
    }

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
}
