package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public record FilterContent(NonNullList<ItemStack> list, boolean includeComponents, boolean blackList) {
    public static final MapCodec<FilterContent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC
            .listOf()
            .fieldOf("list")
            .forGetter(FilterContent::list),
        Codec.BOOL
            .fieldOf("include_components")
            .forGetter(FilterContent::includeComponents),
        Codec.BOOL
            .fieldOf("black_list")
            .forGetter(FilterContent::blackList)
    ).apply(instance, FilterContent::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FilterContent> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
        FilterContent::list,
        ByteBufCodecs.BOOL,
        FilterContent::includeComponents,
        ByteBufCodecs.BOOL,
        FilterContent::blackList,
        FilterContent::new
    );

    private FilterContent(List<ItemStack> list, boolean includeComponents, boolean blackList) {
        this(NonNullList.of(ItemStack.EMPTY, list.toArray(new ItemStack[0])), includeComponents, blackList);
    }

    public FilterContent() {
        this(NonNullList.withSize(18, ItemStack.EMPTY), false, false);
    }

    public FilterContent setList(NonNullList<ItemStack> list) {
        return new FilterContent(list, this.includeComponents, this.blackList);
    }

    public FilterContent setIncludeComponents(boolean includeComponents) {
        return new FilterContent(this.list, includeComponents, this.blackList);
    }

    public FilterContent setBlackList(boolean blackList) {
        return new FilterContent(this.list, this.includeComponents, blackList);
    }

    public int getNestingLevel() {
        int maxLevel = 0;
        for (ItemStack stack : list) {
            if (stack.has(ModComponents.FILTER_CONTENT)) {
                FilterContent content = Objects.requireNonNull(stack.get(ModComponents.FILTER_CONTENT));
                int nestingLevel = content.getNestingLevel();
                if (nestingLevel > maxLevel) maxLevel = nestingLevel;
            }
        }
        return maxLevel + 1;
    }

    public static boolean filter(ItemStack filterStack, ItemStack stack, boolean isIncludeComponents, boolean isBlackList) {
        if (filterStack.isEmpty()) return !isBlackList;
        if (!filterStack.has(ModComponents.FILTER_CONTENT)) {
            if (filterStack.is(Items.NAME_TAG) && filterStack.has(DataComponents.CUSTOM_NAME)) {
                Component name = filterStack.getOrDefault(DataComponents.CUSTOM_NAME, Component.empty());
                String string = name.getString();
                if (string.startsWith("#")) {
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(string.substring(1)));
                    if (stack.is(tag)) return !isBlackList;
                }
            }
            boolean flag = false;
            if (!isIncludeComponents && ItemStack.isSameItem(filterStack, stack)) flag = true;
            else if (isIncludeComponents && ItemStack.isSameItemSameComponents(filterStack, stack)) flag = true;
            if (flag) return !isBlackList;
            return isBlackList;
        }
        FilterContent content = filterStack.get(ModComponents.FILTER_CONTENT);
        if (content == null) return false;
        for (ItemStack itemStack : content.list()) {
            if (itemStack.isEmpty()) continue;
            if (FilterContent.filter(itemStack, stack, content.includeComponents(), content.blackList())) {
                return !isBlackList;
            }
        }
        return isBlackList;
    }
}
