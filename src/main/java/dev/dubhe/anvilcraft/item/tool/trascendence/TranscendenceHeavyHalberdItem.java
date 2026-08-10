package dev.dubhe.anvilcraft.item.tool.trascendence;

import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.entity.ThrownTranscendenceHeavyHalberdEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class TranscendenceHeavyHalberdItem extends HeavyHalberdItem {
    public static final Component NAME = Component.translatable("item.anvilcraft.transcendence_heavy_halberd");

    public TranscendenceHeavyHalberdItem(Properties properties) {
        super(
            ModToolMaterials.TRANSCENDIUM,
            17,
            -2.4F,
            properties.fireResistant()
                .component(ModComponents.MULTIPHASE, Multiphase.create())
                .component(DataComponents.ITEM_NAME, Multiphase.firstPhaseName(TranscendenceHeavyHalberdItem.NAME))
                .component(ModComponents.ETERNAL, Eternal.DEFAULT)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .component(ModComponents.PROVIDENCE, Unit.INSTANCE)
                .component(ModComponents.FEROCIOUS, Ferocious.DEFAULT)
        );
    }

    @Override
    public ThrownHeavyHalberdEntity createThrown(Level level, LivingEntity shooter, ItemStack pickupItemStack) {
        return new ThrownTranscendenceHeavyHalberdEntity(level, shooter, pickupItemStack);
    }

    @Override
    public ThrownHeavyHalberdEntity createThrown(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        return new ThrownTranscendenceHeavyHalberdEntity(level, x, y, z, pickupItemStack);
    }
}
