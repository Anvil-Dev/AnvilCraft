package dev.dubhe.anvilcraft.api.item.property;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModComponents;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
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

@Getter
@EqualsAndHashCode
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class FilterContent {
    public static final MapCodec<FilterContent> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ItemStack.OPTIONAL_CODEC
            .listOf()
            .fieldOf("list")
            .forGetter(FilterContent::getList),
        Codec.BOOL
            .fieldOf("include_components")
            .forGetter(FilterContent::isIncludeComponents),
        Codec.BOOL
            .fieldOf("black_list")
            .forGetter(FilterContent::isBlackList)
    ).apply(instance, FilterContent::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FilterContent> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
        FilterContent::getList,
        ByteBufCodecs.BOOL,
        FilterContent::isIncludeComponents,
        ByteBufCodecs.BOOL,
        FilterContent::isBlackList,
        FilterContent::new
    );
    private final NonNullList<ItemStack> list;
    @Setter
    private boolean includeComponents;
    @Setter
    private boolean blackList;

    public FilterContent(NonNullList<ItemStack> list, boolean includeComponents, boolean blackList) {
        this.list = list;
        this.includeComponents = includeComponents;
        this.blackList = blackList;
    }

    private FilterContent(List<ItemStack> list, boolean includeComponents, boolean blackList) {
        this(NonNullList.of(ItemStack.EMPTY, list.toArray(new ItemStack[0])), includeComponents, blackList);
    }

    public FilterContent() {
        this.list = NonNullList.withSize(18, ItemStack.EMPTY);
        this.includeComponents = false;
        this.blackList = false;
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
        for (ItemStack itemStack : content.getList()) {
            if (itemStack.isEmpty()) continue;
            if (FilterContent.filter(itemStack, stack, content.isIncludeComponents(), content.isBlackList())) {
                return !isBlackList;
            }
        }
        return isBlackList;
    }
}
