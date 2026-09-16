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
 * @param universalCost  通用维修材料的消耗数量表达式，见 {@link #DEFAULT_UNIVERSAL_COST}。
 *                       求值时的传入值见 {@link #VAR_NAME}
 */
public record RepairMaterialFrostMaterialPredicate(
    int cost,
    boolean allowUniversal,
    IExpression universalCost
) implements IFrostMaterialPredicate {
    /**
     * 求解 {@link #universalCost} 时绑给数据包函数的具名传入值，也就是表达式里能写的 {@code $(cost)}。
     *
     * <p>第 0 个传入值是同一个数，所以 {@code "x*2"} 与 {@code "$(cost)*2"} 等价。</p>
     *
     * <p>它与 {@link ModMathFunctions#doubleFunction()} 声明的形参名不必相同：{@code CustomFunction}
     * 先按<b>位置</b>把实参绑到自己的形参上，函数体里的具名引用取的正是那个位置上的实参，
     * 所以这里的具名引用只要能按名字在调用点的传入值里找到即可。</p>
     */
    public static final String VAR_NAME = "cost";
    /**
     * {@code universal_cost} 缺省时使用的表达式：调用 {@link ModMathFunctions#DOUBLE} 把维修材料消耗翻倍。
     *
     * <p>它是 {@code universal_cost} 字段在 {@code MapCodec} 里的 {@code optionalFieldOf} 默认值，
     * 而 {@code runData} 生成的形变配方传的正是本值，所以那 15 个配方 JSON 里不带
     * {@code universal_cost}，运行时按本字段取值——改 {@code data/anvilcraft/anvillib/function/double.json}
     * 就能改掉所有缺省情形的消耗，不必碰配方。</p>
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

    /**
     * 不允许使用通用维修材料。{@code universalCost} 用默认值占位——{@code test} 在
     * {@code allowUniversal} 为 {@code false} 时直接返回，该字段不参与求值，但流编解码器要求它非空。
     */
    public static RepairMaterialFrostMaterialPredicate disallowUniversal(int cost) {
        return new RepairMaterialFrostMaterialPredicate(cost, false, DEFAULT_UNIVERSAL_COST);
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
