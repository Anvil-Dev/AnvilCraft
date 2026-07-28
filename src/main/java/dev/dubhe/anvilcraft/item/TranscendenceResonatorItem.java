package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.item.property.component.Providence;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.Unbreakable;

public class TranscendenceResonatorItem extends ResonatorItem {
    public static final Component NAME = Component.translatable("item.anvilcraft.transcendence_resonator");
    private static final int RESONANCE_MINING_TICKS = 10;

    public TranscendenceResonatorItem(Properties properties) {
        super(
            ModTiers.TRANSCENDIUM,
            properties.fireResistant()
                .attributes(ResonatorItem.createAttributes(ModTiers.TRANSCENDIUM, 17, -3f))
                .component(ModComponents.MULTIPHASE, Multiphase.create())
                .component(DataComponents.ITEM_NAME, Multiphase.firstPhaseName(NAME))
                .component(ModComponents.ETERNAL, Eternal.INSTANCE)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                .component(ModComponents.PROVIDENCE, Providence.INSTANCE)
                .component(ModComponents.FEROCIOUS, Ferocious.DEFAULT)
        );
    }

    @Override
    protected double getBaseAttackDamage() {
        return 17;
    }

    @Override
    protected int resonanceMiningTicks() {
        return RESONANCE_MINING_TICKS;
    }

    @Override
    protected int resonanceMiningDurabilityCost() {
        return 0;
    }
}
