package dev.dubhe.anvilcraft.loot.modifiers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.Optional;

public class SmeltingLootModifier extends LootModifier {
    public static final MapCodec<SmeltingLootModifier> CODEC = RecordCodecBuilder.mapCodec(
        inst -> LootModifier.codecStart(inst).apply(inst, SmeltingLootModifier::new)
    );

    public SmeltingLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext ctx) {
        if (!ctx.hasParam(LootContextParams.BLOCK_STATE)) return generatedLoot;
        if (!ctx.hasParam(LootContextParams.ORIGIN)) return generatedLoot;
        ServerLevel level = ctx.getLevel();
        ItemStack tool = ctx.getParamOrNull(LootContextParams.TOOL);
        if (tool == null) return generatedLoot;
        HolderLookup<Enchantment> lookup = level.holderLookup(Registries.ENCHANTMENT);
        int lvl = tool.getEnchantmentLevel(lookup.getOrThrow(ModEnchantments.SMELTING_KEY));
        if (lvl <= 0) return generatedLoot;
        if (
            generatedLoot.size() == 1
            && generatedLoot.getFirst().is(ModItemTags.HEATABLE_BLOCKS)
            && Util.castSafely(generatedLoot.getFirst().getItem(), BlockItem.class).isPresent()
        ) {
            Optional<HeatTier> tier = HeatRecorder.getTier(
                level,
                BlockPos.containing(ctx.getParam(LootContextParams.ORIGIN)),
                Block.byItem(generatedLoot.getFirst().getItem()).defaultBlockState()
            );
            return tier.map(heatTier -> ObjectArrayList.of(
                HeatRecorder.getHeatableBlock(
                        level,
                        BlockPos.containing(ctx.getParam(LootContextParams.ORIGIN)),
                        Block.byItem(generatedLoot.getFirst().getItem()).defaultBlockState(),
                        heatTier
                    )
                    .map(block -> block.asItem().getDefaultInstance())
                    .orElse(ItemStack.EMPTY)
            )).orElseGet(ObjectArrayList::of);
        }
        ObjectArrayList<ItemStack> smeltList = new ObjectArrayList<>();
        for (ItemStack item : generatedLoot) {
            boolean needDouble = false;
            SingleRecipeInput cont = new SingleRecipeInput(item);
            RecipeHolder<SmeltingRecipe> h = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, cont, level)
                .orElse(null);
            if (h == null) {
                smeltList.add(item);
                continue;
            }
            if (item.is(Tags.Items.RAW_MATERIALS) || item.is(Tags.Items.ORES)) {
                float chance = lvl == 1 ? 0 : (lvl - 1) * 0.25f;
                if (lvl >= 5) chance = 1;
                if (ctx.getRandom().nextFloat() < chance) {
                    needDouble = true;
                }
            }
            ItemStack stack = h.value().result.copy();
            int count = item.getCount();
            if (needDouble) count *= 2;
            int maxStack = stack.getItem().getMaxStackSize(stack);
            while (count > maxStack) {
                ItemStack stack1 = stack.copy();
                stack1.setCount(maxStack);
                smeltList.add(stack1);
                count -= maxStack;
            }
            stack.setCount(count);
            smeltList.add(stack);
        }
        return smeltList;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
