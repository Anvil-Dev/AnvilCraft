package dev.dubhe.anvilcraft.recipe.anvil.util;

import dev.anvilcraft.lib.v2.recipe.cache.IItemHandlerCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheElement;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.dubhe.anvilcraft.block.entity.FishTankBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.util.HeaterUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

public final class RecipeOutputTracker {
    private static final String POSITION = "anvilcraft_recipe_output_cauldron";
    private static final String DIMENSION = "anvilcraft_recipe_output_dimension";
    private static final Predicate<ICacheElement> ALL_SOURCES = element -> true;

    private RecipeOutputTracker() {
    }

    public static void mark(ItemEntity entity) {
        BlockPos pos = entity.blockPosition();
        if (!entity.level().getBlockState(pos).is(BlockTags.CAULDRONS)) return;
        CompoundTag data = entity.getPersistentData();
        data.putLong(POSITION, pos.asLong());
        data.putString(DIMENSION, entity.level().dimension().location().toString());
    }

    public static @Nullable BlockPos getOrigin(ItemEntity entity) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(POSITION)) return null;
        BlockPos pos = BlockPos.of(data.getLong(POSITION));
        if (entity.blockPosition().equals(pos)
            && entity.level().dimension().location().toString().equals(data.getString(DIMENSION))
            && entity.level().getBlockState(pos).is(BlockTags.CAULDRONS)) return pos;
        data.remove(POSITION);
        data.remove(DIMENSION);
        return null;
    }

    public static boolean isOutputOf(ItemEntity entity, BlockPos pos) {
        return pos.equals(getOrigin(entity));
    }

    public static boolean canMerge(ItemEntity first, ItemEntity second) {
        return Objects.equals(getOrigin(first), getOrigin(second));
    }

    public static Predicate<ICacheElement> compressionSources(InWorldRecipeContext context) {
        BlockPos cauldronPos = BlockPos.containing(context.getPos()).below();
        Level level = context.getLevel();
        BlockState below = level.getBlockState(cauldronPos.below());
        if (!RecipeOutputTracker.requiresRecipeOutputForCompression(below)) return ALL_SOURCES;
        return element -> isRecipeOutput(element, level, cauldronPos);
    }

    /**
     * 锅下工作方块生效时，压缩配方只接受尚未取出的加工产物。
     */
    public static boolean requiresRecipeOutputForCompression(BlockState below) {
        return below.is(ModBlockTags.UNDER_CAULDRON) && HeaterUtil.isActiveHeatSource(below);
    }

    private static boolean isRecipeOutput(ICacheElement element, Level level, BlockPos cauldronPos) {
        Object source = element.getSource();
        if (source instanceof ItemEntity entity) return isOutputOf(entity, cauldronPos);
        BlockEntity blockEntity = level.getBlockEntity(cauldronPos);
        if (blockEntity instanceof FishTankBlockEntity tank) return source == tank.getOutputHandler();
        LargeCauldronBlockEntity large = LargeCauldronBlockEntity.getMain(level, cauldronPos, level.getBlockState(cauldronPos));
        if (large != null) {
            return source == large.getOutputHandler()
                || source == large.getInput() && large.getOutput().getSlots() == 0;
        }
        return blockEntity instanceof IItemHandlerCache cache
            && source == cache.getOutput() && source != cache.getInput();
    }
}
