package dev.dubhe.anvilcraft.item.template;

import lombok.Getter;
import net.minecraft.world.item.Item;

@Getter
public class MultipleToOneTemplateItem extends Item {
    private final int size;

    public MultipleToOneTemplateItem(Properties properties, int size) {
        super(properties);
        this.size = size;
    }
}
