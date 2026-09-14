package dev.dubhe.anvilcraft.recipe.frost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.number.FlatExpressionParser;
import dev.dubhe.anvilcraft.api.number.INumberExpression;
import dev.dubhe.anvilcraft.api.number.NumberArguments;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModFrostMaterialPredicateTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 维修材料，要求材料是装备的维修材料，或是「通用维修材料」标签中的物品。
 *
 * @param cost          维修材料的消耗数量
 * @param universalCost 通用维修材料的消耗数量，未指定时默认为维修材料消耗的两倍，
 *                      求解时以维修材料消耗作为第 0 个传入值 {@code x} 和具名传入值 {@code $(cost)} 传入，
 *                      因此可以写成 flat 表达式，例如 {@code "x*2"}、{@code "$(cost)*3"}
 */
public record RepairMaterialFrostMaterialPredicate(
    int cost,
    Optional<INumberExpression> universalCost
) implements IFrostMaterialPredicate {
    /**
     * 未指定通用维修材料消耗时使用的表达式：传入的维修材料消耗翻倍。
     */
    public static final INumberExpression DEFAULT_UNIVERSAL_COST = FlatExpressionParser.parse("x*2");
    public static final MapCodec<RepairMaterialFrostMaterialPredicate> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.INT
            .fieldOf("cost")
            .forGetter(RepairMaterialFrostMaterialPredicate::cost),
        INumberExpression.CODEC
            .optionalFieldOf("universal_cost")
            .forGetter(RepairMaterialFrostMaterialPredicate::universalCost)
    ).apply(ins, RepairMaterialFrostMaterialPredicate::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RepairMaterialFrostMaterialPredicate> STREAM_CODEC
        = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        RepairMaterialFrostMaterialPredicate::cost,
        ByteBufCodecs.optional(INumberExpression.STREAM_CODEC),
        RepairMaterialFrostMaterialPredicate::universalCost,
        RepairMaterialFrostMaterialPredicate::new
    );

    public RepairMaterialFrostMaterialPredicate(int cost) {
        this(cost, Optional.empty());
    }

    /**
     * 通用维修材料的实际消耗数量，未指定时为维修材料消耗的两倍。
     */
    public int universalCostAmount() {
        NumberArguments inputs = NumberArguments.of(this.cost).with("cost", this.cost);
        return this.universalCost.orElse(DEFAULT_UNIVERSAL_COST).evaluateInt(inputs);
    }

    @Override
    public boolean test(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        if (this.matchesRepairMaterial(recipe, input)) return input.material().getCount() >= this.cost;
        if (this.matchesUniversalMaterial(input)) return input.material().getCount() >= this.universalCostAmount();
        return false;
    }

    @Override
    public int cost(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        if (this.matchesRepairMaterial(recipe, input)) return this.cost;
        return this.matchesUniversalMaterial(input) ? this.universalCostAmount() : 0;
    }

    @Override
    public Type type() {
        return ModFrostMaterialPredicateTypes.REPAIR_MATERIAL.get();
    }

    /**
     * 材料是装备的维修材料。装备槽为空时用配方中可能放入的装备判断，以便先放入材料。
     */
    private boolean matchesRepairMaterial(IFrostSmithingRecipe recipe, FrostSmithingRecipeInput input) {
        ItemStack material = input.material();
        if (material.isEmpty()) return false;
        ItemStack equipment = input.input();
        if (!equipment.isEmpty()) return isRepairMaterial(equipment, material);
        return recipe.possibleInputs().stream().anyMatch(possible -> isRepairMaterial(possible, material));
    }

    private boolean matchesUniversalMaterial(FrostSmithingRecipeInput input) {
        ItemStack material = input.material();
        return !material.isEmpty() && material.is(ModItemTags.UNIVERSAL_REPAIR_MATERIALS);
    }

    private static boolean isRepairMaterial(ItemStack equipment, ItemStack material) {
        return !equipment.isEmpty() && equipment.getItem().isValidRepairItem(equipment, material);
    }

    public static class Type implements IFrostMaterialPredicate.Type<RepairMaterialFrostMaterialPredicate> {
        @Override
        public MapCodec<RepairMaterialFrostMaterialPredicate> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RepairMaterialFrostMaterialPredicate> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
