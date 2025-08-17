package dev.dubhe.anvilcraft.api.totem.handler;

import dev.dubhe.anvilcraft.api.item.property.BoxContents;
import dev.dubhe.anvilcraft.api.totem.TotemManager;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;


public class AmuletBoxHandler implements TotemHandler {
    @Override
    public boolean canExecute(DamageSource damageSource, LivingEntity entity, ItemStack totemItem) {
        return !totemItem.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY).getTotems().isEmpty();
    }

    @Override
    public boolean execute(DamageSource damageSource, LivingEntity entity, ItemStack totemItem) {
        Map<Item, TotemHandler> totemMap = TotemManager.INSTANCE.getTotemMap();
        List<ItemStack> totems = totemItem.getOrDefault(ModComponents.BOX_CONTENTS, BoxContents.EMPTY).getTotems();
        if (!totems.isEmpty()) {
            for (Item item : totemMap.keySet()) {
                if (totems.getFirst().is(item)) {
                    return totemMap.get(item).execute(damageSource, entity, totems.getFirst());
                }
            }
        }
        return false;
    }

    @SuppressWarnings("DataFlowIssue")
    @Override
    public void shrink(ItemStack totemItem) {
        BoxContents boxContents = totemItem.get(ModComponents.BOX_CONTENTS);
        BoxContents.Mutable mutable = boxContents.mutable();
        mutable.popTotem();
        totemItem.set(ModComponents.BOX_CONTENTS, mutable.immutable());
    }
}
