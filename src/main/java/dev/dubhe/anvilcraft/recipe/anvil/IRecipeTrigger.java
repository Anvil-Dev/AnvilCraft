package dev.dubhe.anvilcraft.recipe.anvil;

import dev.dubhe.anvilcraft.init.ModRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * 配方触发器接口，用于定义配方的触发条件
 * 实现该接口的类表示一种可以触发配方执行的条件
 */
public interface IRecipeTrigger extends IPrioritized {
    /**
     * 获取配方触发器的ID
     *
     * @return ID
     */
    default ResourceLocation getId() {
        return ModRegistries.TRIGGER_REGISTRY.getKey(this);
    }
}