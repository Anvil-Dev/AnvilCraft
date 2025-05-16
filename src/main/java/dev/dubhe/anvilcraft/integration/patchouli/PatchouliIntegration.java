package dev.dubhe.anvilcraft.integration.patchouli;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.integration.Integration;
import dev.dubhe.anvilcraft.integration.patchouli.element.ItemTagVariableSerializer;
import dev.dubhe.anvilcraft.integration.patchouli.page.PageMultipleToOneSmithing;
import dev.dubhe.anvilcraft.integration.patchouli.page.PageSpotlightItemTag;
import dev.dubhe.anvilcraft.integration.patchouli.page.PageTimeWarp;
import vazkii.patchouli.api.VariableHelper;
import vazkii.patchouli.client.book.ClientBookRegistry;

@SuppressWarnings("unused")
@Integration("patchouli")
public class PatchouliIntegration {
    public void apply() {
        VariableHelper.instance().registerSerializer(new ItemTagVariableSerializer(), ItemTagVariableSerializer.getClazz());
    }

    public void applyClient() {
        ClientBookRegistry.INSTANCE.pageTypes.put(AnvilCraft.of("spotlight_item_tag"), PageSpotlightItemTag.class);
        ClientBookRegistry.INSTANCE.pageTypes.put(AnvilCraft.of("time_warp"), PageTimeWarp.class);
        ClientBookRegistry.INSTANCE.pageTypes.put(AnvilCraft.of("multiple_to_one_smithing"), PageMultipleToOneSmithing.class);
    }
}
