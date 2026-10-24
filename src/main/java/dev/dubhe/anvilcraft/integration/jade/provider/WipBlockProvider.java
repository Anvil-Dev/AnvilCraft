package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jade.WipBlockRecipeInfo;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessStep;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.BlockProcessingRecipe;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum WipBlockProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor blockAccessor, IPluginConfig pluginConfig) {
        CompoundTag serverData = blockAccessor.getServerData();
        if (serverData.contains("stepCount")) {
            tooltip.add(Component.translatable(
                "tooltip.anvilcraft.wip_block.jade.step_count",
                serverData.getInt("stepCount")
            ));
        }
        if (serverData.contains("nextStep")) {
            ResourceLocation typeId = ResourceLocation.parse(serverData.getString("nextStep"));
            tooltip.add(Component.translatable(
                "tooltip.anvilcraft.wip_block.jade.next_step",
                Component.translatableWithFallback(
                    "gui." + typeId.getNamespace() + ".category." + typeId.getPath(), typeId.toString()
                )
            ));
        }
        if (serverData.contains("nextStepDetails")) {
            ComponentSerialization.CODEC.listOf().parse(
                blockAccessor.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), serverData.get("nextStepDetails")
            ).result().ifPresent(lines -> lines.forEach(tooltip::add));
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof WipBlockEntity blockEntity) {
            ResourceLocation recipeId = blockEntity.getRecipeId();
            if (recipeId != null) {
                if (blockAccessor.getLevel() instanceof ServerLevel level) {
                    RecipeHolder<?> holder = level.getRecipeManager().byKey(recipeId).orElse(null);
                    if (holder != null && holder.value() instanceof ProceduralProcessRecipe recipe) {
                        appendNextStep(compoundTag, recipe, blockEntity.getStepCount(), level);
                    }
                }
            }
            compoundTag.putInt("stepCount", blockEntity.getStepCount());
        }
    }

    private static void appendNextStep(CompoundTag tag, ProceduralProcessRecipe recipe, int stepCount, ServerLevel level) {
        int stepSize = recipe.getSteps().size();
        if (stepCount < 0 || stepSize == 0 || stepCount / stepSize >= recipe.getLoop()) return;
        int stepIndex = stepCount % stepSize;
        ProceduralProcessStep step = stepIndex == 0 && stepCount > 0
            ? recipe.getMultiLoopFirstStep().orElse(recipe.getSteps().getFirst())
            : recipe.getSteps().get(stepIndex);
        Recipe<?> content = step.getContent();
        ResourceLocation typeId = BuiltInRegistries.RECIPE_TYPE.getKey(content.getType());
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
        ComponentSerialization.CODEC.listOf().encodeStart(
            level.registryAccess().createSerializationContext(NbtOps.INSTANCE), WipBlockRecipeInfo.getDetails(content)
        ).result().ifPresent(details -> tag.put("nextStepDetails", details));
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("wip_block");
    }
}
