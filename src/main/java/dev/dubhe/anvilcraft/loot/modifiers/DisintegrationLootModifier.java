package dev.dubhe.anvilcraft.loot.modifiers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.Optional;

public class DisintegrationLootModifier extends LootModifier {
    public static final MapCodec<DisintegrationLootModifier> CODEC = RecordCodecBuilder.mapCodec(
        inst -> LootModifier.codecStart(inst).apply(inst, DisintegrationLootModifier::new)
    );

    public DisintegrationLootModifier(LootItemCondition[] conditionsIn) {
        super(conditionsIn);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext ctx) {
        final ServerLevel level = ctx.getLevel();
        ItemStack tool = ctx.getParamOrNull(LootContextParams.TOOL);
        if (tool == null) {
            tool = Optional.ofNullable(ctx.getParamOrNull(LootContextParams.DIRECT_ATTACKING_ENTITY))
                .map(Entity::getWeaponItem)
                .orElse(null);
        }
        if (tool == null) {
            tool = Optional.ofNullable(ctx.getParamOrNull(LootContextParams.ATTACKING_ENTITY))
                .map(Entity::getWeaponItem)
                .orElse(null);
        }
        if (tool == null) return generatedLoot;
        HolderLookup<Enchantment> lookup = level.holderLookup(Registries.ENCHANTMENT);
        int lvl = tool.getEnchantmentLevel(lookup.getOrThrow(ModEnchantments.DISINTEGRATION_KEY));
        if (lvl <= 0) return generatedLoot;
        generatedLoot.clear();
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
