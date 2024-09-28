package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.init.ModEnchantmentEffectComponents;
import dev.dubhe.anvilcraft.init.ModLootContextParamSets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class ModEnchantmentHelper {
    public static void onUseOnBlock(
        ServerLevel level,
        ItemStack stack,
        LivingEntity entity,
        EquipmentSlot slot,
        Vec3 pos,
        BlockState state
    ) {
        EnchantmentHelper.runIterationOnItem(stack, (holder, enchantmentLevel) -> {
            LootParams lootParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, entity)
                .withParameter(LootContextParams.ORIGIN, pos)
                .withParameter(LootContextParams.BLOCK_STATE, state)
                .withParameter(LootContextParams.TOOL, stack)
                .create(ModLootContextParamSets.USE_ON_ITEM);

            Enchantment.applyEffects(
                holder.value().getEffects(
                    ModEnchantmentEffectComponents.USE_ON_BLOCK),
                new LootContext.Builder(lootParams).create(Optional.empty()),
                effect -> effect.apply(
                    level,
                    enchantmentLevel,
                    new EnchantedItemInUse(
                        stack,
                        slot,
                        entity
                    ),
                    entity,
                    pos
                )
            );
        });
    }
}
