package dev.dubhe.anvilcraft.recipe.frost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.math.expression.Arguments;
import dev.anvilcraft.lib.v2.math.expression.FunctionExpression;
import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModFrostMaterialPredicateTypes;
import dev.dubhe.anvilcraft.init.recipe.ModMathFunctions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 维修材料，要求材料是装备的维修材料，或是「通用维修材料」标签中的物品。
 *
 * @param cost           维修材料的消耗数量
 * @param allowUniversal 是否允许使用通用维修材料
 * @param universalCost  通用维修材料的消耗数量，未指定时默认为维修材料消耗的两倍，
 *                       求解时以维修材料消耗作为第 0 个传入值 {@code x} 和具名传入值 {@code $(cost)} 传入，
 *                       因此可以写成 flat 表达式，例如 {@code "x*2"}、{@code "$(cost)*3"}
 */
public record RepairMaterialFrostMaterialPredicate(
    int cost,
    boolean allowUniversal,
    IExpression universalCost
) implements IFrostMaterialPredicate {
    /**
     * {@link DEFAULT_UNIVERSAL_COST} 的形参名。
     */
    public static final String VAR_NAME = "cost";
    /**
     * 未指定通用维修材料消耗时使用的表达式：调用 {@link ModMathFunctions#DOUBLE} 把传入的维修材料消耗翻倍。
     *
     * <p>这里内联的是<b>一次对数据包函数的调用</b>，函数定义本身由 {@code runData} 生成在
     * {@code anvillib:function/double}，改函数定义就能改掉所有默认消耗，不必碰配方。</p>
     *
     * <p>只在手写配方省略了 {@code universal_cost} 时兜底：{@code runData} 生成的配方一律显式带上
     * （写成对注册表条目的引用 {@code "anvilcraft:double($(cost))"}）。</p>
     */
    public static final IExpression DEFAULT_UNIVERSAL_COST = FunctionExpression.of(
        ModMathFunctions.doubleFunction(),
        IExpression.ref(VAR_NAME)
    );

    public static final MapCodec<RepairMaterialFrostMaterialPredicate> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        Codec.INT
            .fieldOf("cost")
            .forGetter(RepairMaterialFrostMaterialPredicate::cost),
        Codec.BOOL
            .optionalFieldOf("allow_universal", true)
            .forGetter(RepairMaterialFrostMaterialPredicate::allowUniversal),
        IExpression.CODEC
            .optionalFieldOf("universal_cost", DEFAULT_UNIVERSAL_COST)
            .forGetter(RepairMaterialFrostMaterialPredicate::universalCost)
    ).apply(ins, RepairMaterialFrostMaterialPredicate::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, RepairMaterialFrostMaterialPredicate> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT,
        RepairMaterialFrostMaterialPredicate::cost,
        ByteBufCodecs.BOOL,
        RepairMaterialFrostMaterialPredicate::allowUniversal,
        IExpression.STREAM_CODEC,
        RepairMaterialFrostMaterialPredicate::universalCost,
        RepairMaterialFrostMaterialPredicate::new
    );

    public static RepairMaterialFrostMaterialPredicate disallowUniversal(int cost) {
        return new RepairMaterialFrostMaterialPredicate(cost, false, DEFAULT_UNIVERSAL_COST); // 占位
    }

    public static RepairMaterialFrostMaterialPredicate allowUniversal(int cost, IExpression expression) {
        return new RepairMaterialFrostMaterialPredicate(cost, true, expression);
    }

    public static RepairMaterialFrostMaterialPredicate allowUniversal(int cost) {
        return RepairMaterialFrostMaterialPredicate.allowUniversal(cost, DEFAULT_UNIVERSAL_COST);
    }

    /**
     * 通用维修材料的实际消耗数量，未指定时为维修材料消耗的两倍。
     */
    public int universalCostAmount() {
        // 函数体里的 $(cost) 在求值期从传入值里按名字取，所以这里要把 cost 同时按下标和名字绑上
        Arguments inputs = Arguments.of(this.cost).withAll(
            List.of(VAR_NAME),
            List.of(new Arguments.Value.Single(this.cost))
        );
        return this.universalCost.evaluateInt(inputs);
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
        if (!this.allowUniversal()) return false;
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
