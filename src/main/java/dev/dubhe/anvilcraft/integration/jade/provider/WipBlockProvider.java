package dev.dubhe.anvilcraft.integration.jade.provider;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.WipBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
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
            }
            tag.putInt("stepCount", blockEntity.getStepCount());
        }
    }

    @Override
    public Identifier getUid() {
        return AnvilCraft.of("wip_block");
    }
}
