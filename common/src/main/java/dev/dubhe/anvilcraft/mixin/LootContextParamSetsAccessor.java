package dev.dubhe.anvilcraft.mixin;

import com.google.common.collect.BiMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问器
 */
@Mixin(LootContextParamSets.class)
public interface LootContextParamSetsAccessor {
    @Accessor("REGISTRY")
    static BiMap<ResourceLocation, LootContextParamSet> getRegistry() {
        throw new UnsupportedOperationException();
    }
}
