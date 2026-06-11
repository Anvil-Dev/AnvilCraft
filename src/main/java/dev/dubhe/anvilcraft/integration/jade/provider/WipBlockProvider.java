package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        if (serverData.contains("recipe")) {
            String s = serverData.getString("recipe");
            ResourceLocation rl = ResourceLocation.parse(s);
            if (!rl.getPath().isEmpty()) {
                String s1 = rl.getPath();
                String [] splits = s1.split("/");
                String s2 = splits[splits.length - 1];
                tooltip.add(
                    Component.translatable(
                        "tooltip.anvilcraft.wip_block.jade.recipe",
                        s2
                    )
                );
            }
        }
        if (serverData.contains("stepCount")) {
            tooltip.add(Component.translatable(
                "tooltip.anvilcraft.wip_block.jade.step_count",
                serverData.getInt("stepCount")
            ));
        }
    }

    @Override
    public void appendServerData(CompoundTag compoundTag, BlockAccessor blockAccessor) {
        if (blockAccessor.getBlockEntity() instanceof WipBlockEntity blockEntity) {
            ResourceLocation recipeId = blockEntity.getRecipeId();
            if (recipeId != null) {
                compoundTag.putString("recipe", recipeId.toString());
            }
            compoundTag.putInt("stepCount", blockEntity.getStepCount());
        }
    }

    @Override
    public ResourceLocation getUid() {
        return AnvilCraft.of("wip_block");
    }
}
