package dev.dubhe.anvilcraft.item.template.mto;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;

@Getter
public abstract class BaseMultipleToOneTemplateItem extends Item {
    private final int size;

    public BaseMultipleToOneTemplateItem(Properties properties, int size) {
        super(properties);
        this.size = size;
    }

    public abstract Component getMaterialTooltip();

    public abstract List<ResourceLocation> getEmptySlotTextures();
}
