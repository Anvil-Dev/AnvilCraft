package dev.dubhe.anvilcraft.recipe.anvil.cache.item.operation;

import dev.dubhe.anvilcraft.recipe.anvil.cache.item.ICacheElement;

import java.util.Set;

/**
 * 输入输出操作记录类
 */
public record InputOutputOperation(Set<ICacheElement> elements) {
}
