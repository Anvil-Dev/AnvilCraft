package dev.dubhe.anvilcraft.item.tool.trascendence;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.item.tool.ResonatorItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;

public class TranscendenceResonatorItem extends ResonatorItem {
    public static final Component NAME = Component.translatable("item.anvilcraft.transcendence_resonator");
    public static final int RESONANCE_MINING_TICKS = 10;

    public TranscendenceResonatorItem(Properties properties) {
        super(
            ModToolMaterials.TRANSCENDIUM,
            17,
            -3F,
            properties.fireResistant()
                .component(ModComponents.MULTIPHASE, Multiphase.create())
                .component(ModComponents.ETERNAL, Eternal.DEFAULT)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .component(ModComponents.PROVIDENCE, Unit.INSTANCE)
                .component(ModComponents.FEROCIOUS, Ferocious.DEFAULT)
        );
    }

    @Override
    public int resonanceMiningTicks() {
        return RESONANCE_MINING_TICKS;
    }

    @Override
    protected int resonanceMiningDurabilityCost() {
        return 0;
    }
}
