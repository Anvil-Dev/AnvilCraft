package dev.dubhe.anvilcraft.event;

import dev.anvilcraft.lib.v2.recipe.InWorldRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.BlockCache;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeEvent;
import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeManagerEvent;
import dev.anvilcraft.lib.v2.recipe.event.ItemCacheEvent;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CrushingTableBlockEntity;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockSmearRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.MeshRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.VanillaRecipesWrap;
import dev.dubhe.anvilcraft.recipe.generate.MeshRecipeGeneratingCache;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class InWorldRecipeEventListener {
    @SubscribeEvent
    public static void inWorldRecipe(InWorldRecipeManagerEvent.Init event) {
        RecipeManager manager = event.getRecipeManager();
        List<RecipeHolder<InWorldRecipe>> init = VanillaRecipesWrap.init(
            manager.anvillib$getRegistries(),
            manager.getRecipes()
        );
        new MeshRecipeGeneratingCache(manager.anvillib$getRegistries())
            .buildRecipes()
            .ifPresent(recipeHolders -> {
                for (RecipeHolder<MeshRecipe> holder : recipeHolders) {
                    init.add(new RecipeHolder<>(holder.id(), holder.value()));
                }
            });
        manager.anvillib$addRecipes(init);
    }

    @SubscribeEvent
    public static void inWorldRecipe(InWorldRecipeEvent event) {
        RecipeType<? extends InWorldRecipe> recipeType = event.getRecipeType();
        ResourceLocation id = event.getId();
        InWorldRecipeContext context = event.getContext();
        ServerLevel level = context.getLevel();
        BlockPos pos = BlockPos.containing(context.getPos());
        if (recipeType == ModRecipeTypes.ITEM_CRUSH_TYPE.get()) {
            if (level.getBlockEntity(pos.below()) instanceof CrushingTableBlockEntity table) {
                table.onRecipeExecuted(20);
            }
        }
        TriggerUtil.inWorldRecipe(level, pos, ResourceLocation.parse(recipeType.toString()), id);
    }

    /**
     * 砧库的方块涂抹结果不会继承输入方块的方块状态，
     * 这里把被处理方块的属性（如去皮前的原木朝向）复制给结果方块。
     *
     * @param event 世界内配方事件
     */
    @SubscribeEvent
    public static void inheritSmearBlockState(InWorldRecipeEvent event) {
        if (event.getRecipeType() != ModRecipeTypes.BLOCK_SMEAR_TYPE.get()) return;
        if (!(event.getRecipe() instanceof BlockSmearRecipe recipe)) return;
        List<BlockStatePredicate> inputs = recipe.getInputBlocks();
        if (inputs.isEmpty()) return;
        InWorldRecipeContext context = event.getContext();
        BlockCache cache = context.computeIfAbsent(BlockCache.BLOCK_CACHE);
        Vec3 inputOffset = recipe.getProperty().getBlockInputOffset().subtract(0, inputs.size() - 1, 0);
        BlockPos inputPos = BlockPos.containing(context.getPos().add(inputOffset));
        BlockPos outputPos = BlockPos.containing(context.getPos().add(recipe.getProperty().getBlockOutputOffset()));
        BlockState input = context.getLevel().getBlockState(inputPos);
        BlockState output = cache.getBlockState(outputPos);
        cache.setBlock(outputPos, inheritProperties(input, output));
    }

    private static BlockState inheritProperties(BlockState source, BlockState target) {
        BlockState result = target;
        for (Map.Entry<Property<?>, Comparable<?>> entry : source.getValues().entrySet()) {
            Property<?> property = entry.getKey();
            Comparable<?> value = entry.getValue();
            if (result.hasProperty(property) && property.getValueClass().isInstance(value)) {
                result = inheritProperty(result, property, value);
            }
        }
        return result;
    }

    private static <T extends Comparable<T>> BlockState inheritProperty(
        BlockState target,
        Property<T> property,
        Comparable<?> value
    ) {
        return target.setValue(property, property.getValueClass().cast(value));
    }

    @SubscribeEvent
    public static void spawnItemEntity(ItemCacheEvent.SpawnItemEntity event) {
        ItemEntity entity = event.getEntity();
        entity.anvilcraft$setIsAdsorbable(false);
        BlockPos pos = entity.blockPosition();
        LargeCauldronBlockEntity cauldron = LargeCauldronBlockEntity.getMain(
            entity.level(),
            pos,
            entity.level().getBlockState(pos)
        );
        if (cauldron != null) {
            ItemStack remaining = cauldron.insertRecipeOutput(entity.getItem());
            if (remaining.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(remaining);
            }
            return;
        }
        entity.level().getBlockEntity(entity.blockPosition(), ModBlockEntities.FISH_TANK.get())
            .ifPresent(be -> {
                ItemStack remaining = be.insertRecipeOutput(entity.getItem());
                if (remaining.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(remaining);
                }
            });
    }
}
