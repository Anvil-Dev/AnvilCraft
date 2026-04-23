package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.EntityThroughPortalEvent;
import dev.dubhe.anvilcraft.api.portal.PortalType;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.PortalConversionRecipe;
import dev.dubhe.anvilcraft.util.CompatUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.Map;
import java.util.Optional;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class PortalEventListener {
    @SubscribeEvent
    public static void onThroughPortal(EntityThroughPortalEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof FallingBlockEntity entity)) return;
        PortalEventListener.processBlockPortalConversionRecipe(level, entity, event.getType());
    }

    private static void processBlockPortalConversionRecipe(ServerLevel level, FallingBlockEntity entity, PortalType type) {
        if (entity.anvilcraft$isSpectral()) {
            entity.discard();
            return;
        }
        if (entity.blockState.is(ModBlockTags.END_PORTAL_UNABLE_CHANGE)) return;
        Map.Entry<BlockState, CompoundTag> result = CompatUtil.PORTAL_DEFAULT_CONVERSION.get(type.getPortal());
        Optional<RecipeHolder<PortalConversionRecipe>> recipeOp = level.getRecipeManager().getRecipeFor(
            ModRecipeTypes.PORTAL_CONVERSION_TYPE.get(),
            new PortalConversionRecipe.Input(type, entity),
            level
        );
        if (recipeOp.isPresent()) result = recipeOp.get().value().getResults().getResult(level);
        if (result == null) return;
        entity.blockState = result.getKey();
        CompoundTag nbt = result.getValue();
        entity.blockData = nbt == null ? null : nbt.copy();
    }
}
