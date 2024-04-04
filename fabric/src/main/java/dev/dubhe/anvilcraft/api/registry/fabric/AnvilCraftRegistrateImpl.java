package dev.dubhe.anvilcraft.api.registry.fabric;

import com.tterrag.registrate.builders.NoConfigBuilder;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.dubhe.anvilcraft.api.registry.AnvilCraftRegistrate;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class AnvilCraftRegistrateImpl extends AnvilCraftRegistrate {

    public AnvilCraftRegistrateImpl(String modId) {
        super(modId);
    }

    public static AnvilCraftRegistrate create(String modId) {
        return new AnvilCraftRegistrateImpl(modId);
    }
    @Override
    public void registerRegistrate() {
        register();
    }

    @Override
    protected <P> NoConfigBuilder<CreativeModeTab, CreativeModeTab, P> createCreativeModeTab(P parent, String name, Consumer<CreativeModeTab.Builder> config) {
        var tab = ResourceKey.create(Registries.CREATIVE_MODE_TAB, new ResourceLocation(getModid(), name));
        return this.generic(parent, name, Registries.CREATIVE_MODE_TAB, () -> {
            var builder = FabricItemGroup.builder()
                    .icon(() -> getAll(Registries.ITEM).stream().findFirst().map(ItemEntry::cast).map(ItemEntry::asStack).orElse(new ItemStack(Items.AIR)))
                    .title(this.addLang("itemGroup", tab.location(), RegistrateLangProvider.toEnglishName(name)));
            config.accept(builder);
            return builder.build();
        });
    }
}
