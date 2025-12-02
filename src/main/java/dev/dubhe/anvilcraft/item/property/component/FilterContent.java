package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
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

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

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

    /**
     * 过滤物品堆栈是否匹配指定条件
     *
     * @param filterStack       过滤器物品堆栈，用于定义过滤条件
     * @param stack             待检查的物品堆栈
     * @param includeComponents 是否考虑组件信息进行匹配
     * @return 如果物品堆栈匹配过滤条件则返回true，否则返回false
     */
    public static boolean filter(ItemStack filterStack, ItemStack stack, boolean includeComponents) {
        // 如果过滤器为空，则所有物品都匹配
        if (filterStack.isEmpty()) return true;

        // 检查过滤器是否包含自定义过滤组件
        if (!filterStack.has(ModComponents.FILTER_CONTENT)) {
            // 处理命名牌作为过滤器的特殊情况，支持标签过滤
            if (filterStack.is(Items.NAME_TAG) && filterStack.has(DataComponents.CUSTOM_NAME)) {
                Component name = filterStack.getOrDefault(DataComponents.CUSTOM_NAME, Component.empty());
                String string = name.getString();
                // 匹配以#开头的标签格式
                Pattern pattern = Pattern.compile("^#(([a-z0-9._-]*:[a-z0-9/._-]*)|[a-z0-9/._-]*)$");
                if (pattern.matcher(string).matches()) {
                    TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(string.substring(1)));
                    if (stack.is(tag)) return true;
                }
            }

            // 根据是否包含组件进行物品匹配
            if (!includeComponents && ItemStack.isSameItem(filterStack, stack)) {
                return true;
            } else {
                return includeComponents && ItemStack.isSameItemSameComponents(filterStack, stack);
            }
        }

        // 处理自定义过滤组件逻辑
        FilterContent content = filterStack.get(ModComponents.FILTER_CONTENT);
        if (content == null) return false;

        boolean contentIsBlackList = content.blackList();
        // 遍历过滤列表中的每个物品进行匹配检查
        for (ItemStack itemStack : content.list()) {
            if (itemStack.isEmpty()) continue;
            if (FilterContent.filter(itemStack, stack, content.includeComponents())) {
                // 如果是白名单模式，找到匹配项则返回true；如果是黑名单模式，找到匹配项则返回false
                return !contentIsBlackList;
            }
        }

        // 如果是黑名单模式且未找到匹配项则返回true，否则返回false
        return contentIsBlackList;
    }

    public static ItemStack getFirstItem(ItemStack filter) {
        if (filter.isEmpty()) return ItemStack.EMPTY;
        if (!filter.is(ModItems.FILTER)) return filter;
        if (!filter.has(ModComponents.FILTER_CONTENT)) return filter;
        FilterContent content = filter.get(ModComponents.FILTER_CONTENT);
        ItemStack result = null;
        for (ItemStack stack : content.list) {
            result = stack;
            break;
        }
        return result == null ? ItemStack.EMPTY : result;
    }
}
