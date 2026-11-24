package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.item.property.component.StoredItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class FoodAdvancementEventListener {
    /** 罐头营养值上限：营养值最高的食物为 10，装五个时的倍率为 3。 */
    private static final int MAX_CAN_NUTRITION = 30;

    @SubscribeEvent
    public static void onEatFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getItem();
        if (!stack.is(ModFoodItems.CANNED_FOOD)) return;
        FoodAdvancementEventListener.compressedBiscuit(player, stack);
        FoodAdvancementEventListener.bigStomach(player, stack);
    }

    /** 吃掉由五个曲奇饼压成的罐头。 */
    private static void compressedBiscuit(ServerPlayer player, ItemStack stack) {
        StoredItem contents = stack.get(ModComponents.DISPLAY_ITEM);
        if (contents == null) return;
        ItemStack food = contents.stored();
        if (food.is(Items.COOKIE) && food.getCount() == 5) {
            ModCriterionTriggers.COMPRESSED_BISCUIT.get().trigger(player);
        }
    }

    /** 吃掉饱食度达到罐头上限的罐头。 */
    private static void bigStomach(ServerPlayer player, ItemStack stack) {
        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food == null || food.nutrition() < MAX_CAN_NUTRITION) return;
        ModCriterionTriggers.BIG_STOMACH.get().trigger(player);
    }
}
