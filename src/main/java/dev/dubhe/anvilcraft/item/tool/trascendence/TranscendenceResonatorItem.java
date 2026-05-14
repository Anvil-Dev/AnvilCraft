package dev.dubhe.anvilcraft.item.tool.trascendence;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.item.tool.ResonatorItem;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;

public class TranscendenceResonatorItem extends ResonatorItem {
    public static final Component NAME = Component.translatable("item.anvilcraft.transcendence_resonator");

    public TranscendenceResonatorItem(Properties properties) {
        super(
            ModToolMaterials.TRANSCENDIUM,
            17,
            -3F,
            properties.fireResistant()
                .component(ModComponents.MULTIPHASE, new MultiphaseRef())
                .component(DataComponents.ITEM_NAME, Multiphase.firstPhaseName(NAME))
                .component(ModComponents.ETERNAL, Eternal.DEFAULT)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .component(ModComponents.PROVIDENCE, Unit.INSTANCE)
                .component(ModComponents.FEROCIOUS, Ferocious.DEFAULT)
        );
    }
}
