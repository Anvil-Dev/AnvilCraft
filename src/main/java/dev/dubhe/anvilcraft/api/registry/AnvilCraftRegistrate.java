package dev.dubhe.anvilcraft.api.registry;

import com.tterrag.registrate.Registrate;
import com.tterrag.registrate.builders.Builder;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.builders.NoConfigBuilder;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import dev.dubhe.anvilcraft.util.IFormattingUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class AnvilCraftRegistrate extends Registrate {

    protected AnvilCraftRegistrate(String modId) {
        super(modId);
    }

    @NotNull
    public static AnvilCraftRegistrate create(@NotNull String modId) {
        return new AnvilCraftRegistrate(modId);
    }

    public void registerRegistrate(IEventBus bus) {
        registerEventListeners(bus);
    }

    protected <P> NoConfigBuilder<CreativeModeTab, CreativeModeTab, P> createCreativeModeTab(
        P parent, String name, Consumer<CreativeModeTab.Builder> config
    ) {
        var tab = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            ResourceLocation.fromNamespaceAndPath(getModid(), name)
        );
        return this.generic(parent, name, Registries.CREATIVE_MODE_TAB, () -> {
            var builder = CreativeModeTab.builder()
                .icon(
                    () -> getAll(Registries.ITEM)
                        .stream()
                        .findFirst()
                        .map(ItemEntry::cast)
                        .map(ItemEntry::asStack)
                        .orElse(new ItemStack(Items.AIR))
                )
                .title(this.addLang("itemGroup", tab.location(), RegistrateLangProvider.toEnglishName(name)));
            config.accept(builder);
            return builder.build();
        });
    }

    @Override
    public <T extends Item> @NotNull ItemBuilder<T, Registrate> item(
        @NotNull String name,
        @NotNull NonNullFunction<Item.Properties, T> factory
    ) {
        return super.item(name, factory)
            .lang(IFormattingUtil.toEnglishName(name.replaceAll("/.", "_")));
    }

    private RegistryEntry<CreativeModeTab, CreativeModeTab> currentTab;
    private static final Map<RegistryEntry<?, ?>, RegistryEntry<CreativeModeTab, CreativeModeTab>> TAB_LOOKUP = new IdentityHashMap<>();

    public void creativeModeTab(@NotNull Supplier<RegistryEntry<CreativeModeTab, CreativeModeTab>> currentTab) {
        this.currentTab = currentTab.get();
    }

    public void creativeModeTab(RegistryEntry<CreativeModeTab, CreativeModeTab> currentTab) {
        this.currentTab = currentTab;
    }

    public boolean isInCreativeTab(RegistryEntry<?, ?> entry, RegistryEntry<CreativeModeTab, CreativeModeTab> tab) {
        return TAB_LOOKUP.get(entry) == tab;
    }

    public void setCreativeTab(RegistryEntry<?, ?> entry, @Nullable RegistryEntry<CreativeModeTab, CreativeModeTab> tab) {
        TAB_LOOKUP.put(entry, tab);
    }

    @Override
    @ParametersAreNonnullByDefault
    protected <R, T extends R> @NotNull RegistryEntry<R, T> accept(
        String name,
        ResourceKey<? extends Registry<R>> type,
        Builder<R, T, ?, ?> builder,
        NonNullSupplier<? extends T> creator,
        NonNullFunction<DeferredHolder<R, T>, ? extends RegistryEntry<R, T>> entryFactory
    ) {
        RegistryEntry<R, T> entry = super.accept(name, type, builder, creator, entryFactory);

        if (this.currentTab != null) {
            TAB_LOOKUP.put(entry, this.currentTab);
        }

        return entry;
    }

    @Override
    public <P> @NotNull NoConfigBuilder<CreativeModeTab, CreativeModeTab, P> defaultCreativeTab(
        @NotNull P parent, @NotNull String name, @NotNull Consumer<CreativeModeTab.Builder> config
    ) {
        return createCreativeModeTab(parent, name, config);
    }
}
