package dev.dubhe.anvilcraft.inventory;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 玩家手动添加的配方模板，保存组件与添加顺序，并与实际物品库存隔离。 */
public record SmithingTemplateAdditions(List<ItemStack> templates) {
    public static final SmithingTemplateAdditions EMPTY = new SmithingTemplateAdditions(List.of());
    public static final Codec<SmithingTemplateAdditions> CODEC = ItemStack.CODEC.listOf()
        .xmap(SmithingTemplateAdditions::new, SmithingTemplateAdditions::templates);

    public SmithingTemplateAdditions {
        templates = templates.stream().filter(stack -> !stack.isEmpty()).map(stack -> stack.copyWithCount(1)).toList();
    }

    @Override
    public List<ItemStack> templates() {
        return this.templates.stream().map(ItemStack::copy).toList();
    }

    public SmithingTemplateAdditions add(ItemStack stack) {
        List<ItemStack> result = new ArrayList<>(this.templates);
        result.removeIf(existing -> existing.is(stack.getItem()));
        result.add(stack);
        return new SmithingTemplateAdditions(result);
    }

    public SmithingTemplateAdditions remove(ItemStack stack) {
        return new SmithingTemplateAdditions(this.templates.stream().filter(existing -> !existing.is(stack.getItem())).toList());
    }
}
