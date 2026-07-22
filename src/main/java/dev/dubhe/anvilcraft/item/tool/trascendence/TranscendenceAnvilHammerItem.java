package dev.dubhe.anvilcraft.item.tool.trascendence;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.Block;

public class TranscendenceAnvilHammerItem extends AnvilHammerItem {
    public static final Component NAME = Component.translatable("item.anvilcraft.transcendence_anvil_hammer");

    /// 初始化铁砧锤
    ///
    /// @param properties 物品属性
    public TranscendenceAnvilHammerItem(Properties properties) {
        super(
            properties.fireResistant()
                .component(ModComponents.MULTIPHASE, Multiphase.create())
                .component(DataComponents.ITEM_NAME, Multiphase.firstPhaseName(NAME))
                .component(ModComponents.ETERNAL, Eternal.DEFAULT)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .component(ModComponents.PROVIDENCE, Unit.INSTANCE));
    }

    @Override
    protected float getAttackDamageModifierAmount() {
        return 13;
    }

    @Override
    public Block getAnvil() {
        return ModBlocks.TRANSCENDENCE_ANVIL.get();
    }

    @Override
    protected float calculateFallDamageBonus(float fallDistance) {
        return fallDistance * 2;
    }
}
