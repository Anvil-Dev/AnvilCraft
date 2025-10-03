package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.item.property.component.Providence;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.Unbreakable;

import java.util.Objects;

public class TranscendenceResonatorItem extends ResonatorItem {
    public static final Multiphase DEFAULT_MULTIPHASE = Multiphase.make(
        Component.translatable("item.anvilcraft.transcendence_resonator")
    );

    public TranscendenceResonatorItem(Properties properties) {
        super(
            ModTiers.TRANSCENDIUM,
            properties
                .attributes(ResonatorItem.createAttributes(ModTiers.TRANSCENDIUM, 17, -3f))
                .component(ModComponents.MULTIPHASE, DEFAULT_MULTIPHASE.copy())
                .component(DataComponents.ITEM_NAME, Objects.requireNonNull(DEFAULT_MULTIPHASE.peekFirst().getItemName()))
                .component(ModComponents.ETERNAL, Eternal.INSTANCE)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                .component(ModComponents.PROVIDENCE, Providence.INSTANCE)
                .component(ModComponents.MERCILESS, Merciless.DEFAULT)
        );
    }

    @Override
    protected double getBaseAttackDamage() {
        return 17;
    }
}
