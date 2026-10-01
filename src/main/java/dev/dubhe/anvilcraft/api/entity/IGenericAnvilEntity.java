package dev.dubhe.anvilcraft.api.entity;

/**
 * 动态或自定义铁砧实体可声明自己满足「泛铁砧」谓词。
 * {@link dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasAnvil} 仅在谓词未指定方块类型、
 * 方块属性或 NBT 时询问此接口，因此不会误匹配专用铁砧配方。
 */
public interface IGenericAnvilEntity {
    boolean anvilcraft$isGenericAnvil();
}
