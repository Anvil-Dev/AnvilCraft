package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.ItemEntityEvent;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTriggers;
import dev.dubhe.anvilcraft.recipe.anvil.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.recipe.anvil.util.InWorldRecipeManager;
import dev.dubhe.anvilcraft.util.IDiscardableItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.NotNull;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class ItemEntityEventListener {
    @SubscribeEvent
    public static void onItemEntityJoinLevel(@NotNull EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ItemEntity item)) return;
        if (item.getItem().has(ModComponents.ETERNAL)) {
            item.setUnlimitedLifetime();
        }
        if (IDiscardableItemEntity.castFromItemEntity(item).anvilcraft$getDiscarded()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onItemEntityInToBlock(@NotNull ItemEntityEvent.InToBlock event) {
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        InWorldRecipeManager manager = level.getRecipeManager().anc$getInWorldRecipeManager();
        InWorldRecipeContext context = new InWorldRecipeContext(serverLevel, event.getPos(), event.getEntity());
        manager.trigger(ModRecipeTriggers.ITEM_INTO_BLOCK, context);
        context.accept();
    }
}
