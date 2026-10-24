package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessStep;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockProcessingRecipe;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

public enum WipBlockProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendServerData(CompoundTag tag, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof WipBlockEntity blockEntity) {
            Identifier recipeId = blockEntity.getRecipeId();
            if (recipeId != null) {
                tag.putString("recipe", recipeId.toString());
                if (accessor.getLevel() instanceof ServerLevel level) {
                    RecipeHolder<?> holder = level.getServer().getRecipeManager().recipeMap()
                        .byKey(ResourceKey.create(Registries.RECIPE, recipeId));
                    if (holder != null && holder.value() instanceof ProceduralProcessRecipe recipe) {
                        appendNextStep(tag, recipe, blockEntity.getStepCount());
                    }
                }
            }
            tag.putInt("stepCount", blockEntity.getStepCount());
        }
    }

    private static void appendNextStep(CompoundTag tag, ProceduralProcessRecipe recipe, int stepCount) {
        int stepSize = recipe.steps().size();
        if (stepCount < 0 || stepSize == 0 || stepCount / stepSize >= recipe.loop()) return;
        int stepIndex = stepCount % stepSize;
        ProceduralProcessStep step = stepIndex == 0 && stepCount > 0
            ? recipe.multiLoopFirstStep().orElse(recipe.steps().getFirst())
            : recipe.steps().get(stepIndex);
        Recipe<?> content = step.getContent();
        Identifier typeId = BuiltInRegistries.RECIPE_TYPE.getKey(content.getType());
        if (content instanceof BlockProcessingRecipe processing && processing.getInputBlocks().size() > 1) {
            HolderSet<Block> workBlocks = processing.getInputBlocks().get(1).getBlocks();
            if (workBlocks.contains(ModBlocks.CORRUPTED_BEACON.get().builtInRegistryHolder())) {
                typeId = AnvilCraft.of("time_warp");
            } else if (workBlocks.contains(ModBlocks.HEATER.get().builtInRegistryHolder())
                || workBlocks.contains(ModBlocks.BURNING_HEATER.get().builtInRegistryHolder())) {
                typeId = AnvilCraft.of("super_heating");
            } else if (workBlocks.contains(ModBlocks.NEUTRON_IRRADIATOR.get().builtInRegistryHolder())) {
                typeId = AnvilCraft.of("neutron_irradiation");
            }
        }
        if (typeId != null) {
            tag.putString("nextStep", typeId.toString());
        }
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("wip_block");
    }
}
