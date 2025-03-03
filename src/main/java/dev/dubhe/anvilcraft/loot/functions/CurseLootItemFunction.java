package dev.dubhe.anvilcraft.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModLootItemFunctions;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CurseLootItemFunction extends LootItemConditionalFunction {

    public static final MapCodec<CurseLootItemFunction> CODEC = RecordCodecBuilder.mapCodec(
        ins -> commonFields(ins).apply(ins, CurseLootItemFunction::new)
    );

    public CurseLootItemFunction(List<LootItemCondition> predicates) {
        super(predicates);
    }

    @Override
    public LootItemFunctionType<? extends LootItemConditionalFunction> getType() {
        return ModLootItemFunctions.CURSE_LOOT.get();
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        if (stack.is(Items.GOLD_NUGGET)) return ModItems.CURSED_GOLD_NUGGET.asStack(stack.getCount());
        if (stack.is(Items.GOLD_INGOT)) return ModItems.CURSED_GOLD_INGOT.asStack(stack.getCount());
        if (stack.is(Items.GOLD_BLOCK)) return ModBlocks.CURSED_GOLD_BLOCK.asStack(stack.getCount());
        return stack;
    }
}
