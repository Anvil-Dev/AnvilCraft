package dev.dubhe.anvilcraft.loot.modifiers;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.util.Util;
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

    public static final MapCodec<SmeltingLootModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
        LootModifier.codecStart(inst).apply(inst, SmeltingLootModifier::new)
    );

    public SmeltingLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> objectArrayList, LootContext lootContext) {
        if (!lootContext.hasParam(LootContextParams.BLOCK_STATE)) return objectArrayList;
        if (!lootContext.hasParam(LootContextParams.ORIGIN)) return objectArrayList;
        ServerLevel level = lootContext.getLevel();
        ItemStack tool = lootContext.getParamOrNull(LootContextParams.TOOL);
        if (tool == null) return objectArrayList;
        HolderLookup<Enchantment> lookup = level.holderLookup(Registries.ENCHANTMENT);
        int lvl = tool.getEnchantmentLevel(lookup.getOrThrow(ModEnchantments.SMELTING_KEY));
        if (lvl <= 0) return objectArrayList;
        if (
            objectArrayList.size() == 1
            && objectArrayList.getFirst().is(ModItemTags.HEATABLE_BLOCKS)
            && Util.castSafely(objectArrayList.getFirst().getItem(), BlockItem.class).isPresent()
        ) {
            Optional<HeatTier> tier = HeatRecorder.getTier(
                level,
                BlockPos.containing(lootContext.getParam(LootContextParams.ORIGIN)),
                Block.byItem(objectArrayList.getFirst().getItem()).defaultBlockState()
            );
            return tier.map(heatTier -> ObjectArrayList.of(
                HeatRecorder.getHeatableBlock(
                        level,
                        BlockPos.containing(lootContext.getParam(LootContextParams.ORIGIN)),
                        Block.byItem(objectArrayList.getFirst().getItem()).defaultBlockState(),
                        heatTier
                    )
                    .map(block -> block.asItem().getDefaultInstance())
                    .orElse(ItemStack.EMPTY)
            )).orElseGet(ObjectArrayList::of);
        }
        ObjectArrayList<ItemStack> smeltList = new ObjectArrayList<>();
        for (ItemStack item : objectArrayList) {
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
                if (lootContext.getRandom().nextFloat() < chance) {
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
