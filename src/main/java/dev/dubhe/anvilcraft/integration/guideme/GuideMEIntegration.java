package dev.dubhe.anvilcraft.integration.guideme;

import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.anvilcraft.lib.integration.Integration;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItemGroups;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.guideme.data.GuideMELang;
import dev.dubhe.anvilcraft.util.DataGenUtil;
import guideme.Guide;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

@Integration("guideme")
public class GuideMEIntegration {
    public static final ResourceLocation GME_ID = AnvilCraft.of("guideme");
    @Getter
    private static Guide guideme;

    public void apply() {
        AnvilCraft.MOD_BUS.addListener(this::registerToTab);
        REGISTRATE.addDataGenerator(ProviderType.LANG, GuideMELang::init);
        guideme = Guide.builder(GME_ID).folder("ac_guidebook").build();
    }

    public static final ItemEntry<GuideMEBookItem> GUIDEME_BOOK = REGISTRATE
        .item("guideme_book", GuideMEBookItem::new)
        .lang("AnvilCraft GuideME Book")
        .model(DataGenUtil::noExtraModelOrState)
        .removeTab(ModItemGroups.ANVILCRAFT_INGREDIENTS.getKey())
        .properties(properties -> properties.stacksTo(1).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true))
        .register();

    private void registerToTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(ModItemGroups.ANVILCRAFT_TOOL.getKey())) {
            event.insertAfter(
                ModItems.STRUCTURE_TOOL.asStack(),
                GUIDEME_BOOK.asStack(),
                CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS
            );
        }
    }
}
