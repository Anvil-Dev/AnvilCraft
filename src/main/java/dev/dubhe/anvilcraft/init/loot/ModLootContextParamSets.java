package dev.dubhe.anvilcraft.init.loot;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.function.Consumer;

public class ModLootContextParamSets {
    public static final ContextKeySet USE_ON_ITEM = ModLootContextParamSets.register(
        "use_on_item",
        it -> it.required(LootContextParams.THIS_ENTITY)
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.BLOCK_STATE)
            .required(LootContextParams.TOOL)
            .required(LootContextParams.ENCHANTMENT_LEVEL)
    );

    public static final ContextKeySet POST_BREAK_BLOCK = ModLootContextParamSets.register(
        "post_break_block",
        it -> it.required(LootContextParams.THIS_ENTITY)
            .required(LootContextParams.ORIGIN)
            .required(LootContextParams.BLOCK_STATE)
            .required(LootContextParams.TOOL)
    );

    private static ContextKeySet register(String registryName, Consumer<ContextKeySet.Builder> builderConsumer) {
        ContextKeySet.Builder builder = new ContextKeySet.Builder();
        builderConsumer.accept(builder);
        ContextKeySet paramSet = builder.build();
        LootContextParamSets.REGISTRY.put(AnvilCraft.of(registryName), paramSet);
        return paramSet;
    }

    public static void registerAll() {
        // intentionally empty
    }
}
