package dev.dubhe.anvilcraft.saved.storage.category;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.init.storage.ModCategoryTypes;
import dev.dubhe.anvilcraft.mixin.accessor.CreativeModeTabAccessor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public record CreativeModeTabCategory(ItemStackTemplate icon, Component name, ResourceKey<CreativeModeTab> key) implements ICategory {
    // 配方管理器随世界和数据包重载替换，弱键使旧内容缓存可以释放。
    private static final Map<RecipeManager, Map<ResourceKey<CreativeModeTab>, Set<ItemStack>>> CONTENTS = new WeakHashMap<>();

    public CreativeModeTabCategory(ItemLike icon, Identifier suffix, ResourceKey<CreativeModeTab> key) {
        this(new ItemStackTemplate(icon.asItem()), ICategory.constructName(suffix), key);
    }

    @Override
    public boolean test(UnlimitedItemStack stack) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.getOrThrow(this.key).value();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return tab.contains(stack.getStack());
        }
        return CreativeModeTabCategory.contents(server, this.key, tab).contains(stack.getStack());
    }

    private static synchronized Set<ItemStack> contents(
        MinecraftServer server, ResourceKey<CreativeModeTab> key, CreativeModeTab tab
    ) {
        Map<ResourceKey<CreativeModeTab>, Set<ItemStack>> tabs = CreativeModeTabCategory.CONTENTS.computeIfAbsent(
            server.getRecipeManager(), ignored -> new HashMap<>()
        );
        return tabs.computeIfAbsent(key, ignored -> {
            Set<ItemStack> contents = ItemStackLinkedSet.createTypeAndComponentsSet();
            CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(
                server.getWorldData().enabledFeatures(), false, server.registryAccess()
            );
            // 单独生成服务端分类内容，避免覆盖集成服务器客户端共用的创造栏缓存。
            EventHooks.onCreativeModeTabBuildContents(
                tab,
                ((CreativeModeTabAccessor) tab).anvilcraft$getDisplayItemsGenerator(),
                parameters,
                (stack, visibility) -> {
                    if (visibility != CreativeModeTab.TabVisibility.PARENT_TAB_ONLY
                        && stack.getItem().isEnabled(parameters.enabledFeatures())) {
                        contents.add(stack);
                    }
                }
            );
            return contents;
        });
    }

    @Override
    public Type getType() {
        return ModCategoryTypes.CREATIVE_MODE_TAB.get();
    }

    public static class Type implements ICategory.Type<CreativeModeTabCategory> {
        public static final MapCodec<CreativeModeTabCategory> CODEC = CodecUtil.mapCodec(
            ItemStackTemplate.CODEC
                .fieldOf("icon")
                .forGetter(CreativeModeTabCategory::icon),
            ICategory.NAME_CODEC
                .fieldOf("name")
                .forGetter(CreativeModeTabCategory::name),
            ResourceKey.codec(Registries.CREATIVE_MODE_TAB)
                .fieldOf("key")
                .forGetter(CreativeModeTabCategory::key),
            CreativeModeTabCategory::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, CreativeModeTabCategory> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC,
            CreativeModeTabCategory::icon,
            ComponentSerialization.STREAM_CODEC,
            CreativeModeTabCategory::name,
            ResourceKey.streamCodec(Registries.CREATIVE_MODE_TAB),
            CreativeModeTabCategory::key,
            CreativeModeTabCategory::new
        );

        @Override
        public MapCodec<CreativeModeTabCategory> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CreativeModeTabCategory> streamCodec() {
            return Type.STREAM_CODEC;
        }
    }
}
