package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.function.Consumer;

public class ModLootContextParamSets {
    public static final LootContextParamSet USE_ON_ITEM = register(
        "use_on_item",
        it -> it.required(LootContextParams.THIS_ENTITY)
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.BLOCK_STATE)
            .required(LootContextParams.TOOL)
    );

    public static final LootContextParamSet POST_BREAK_BLOCK = register(
        "post_break_block",
        it -> it.required(LootContextParams.THIS_ENTITY)
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.BLOCK_STATE)
            .required(LootContextParams.TOOL)
    );

    private static LootContextParamSet register(String registryName, Consumer<LootContextParamSet.Builder> builderConsumer) {
        LootContextParamSet.Builder builder = new LootContextParamSet.Builder();
        builderConsumer.accept(builder);
        LootContextParamSet paramSet = builder.build();
        LootContextParamSets.REGISTRY.put(AnvilCraft.of(registryName), paramSet);
        return paramSet;
    }

    public static void registerAll() {
        //intentionally empty
    }
}
