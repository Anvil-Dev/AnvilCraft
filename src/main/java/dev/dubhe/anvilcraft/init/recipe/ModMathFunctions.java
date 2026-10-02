package dev.dubhe.anvilcraft.init.recipe;

import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.anvilcraft.lib.v2.math.expression.function.CustomFunction;
import dev.anvilcraft.lib.v2.math.expression.function.IFunction;
import dev.anvilcraft.lib.v2.math.init.LibBuiltInFunctions;
import dev.anvilcraft.lib.v2.math.init.LibRegistries;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;

import java.util.List;

/**
 * 本模组注册进 {@code anvillib:function} 数据包注册表的数学函数。
 *
 * <p>条目写在 {@code data/anvilcraft/anvillib/function/<名字>.json}，由 {@code runData} 生成。</p>
 *
 * <p><b>命名空间必须用 {@code anvilcraft}</b>：{@code runData} 只写出 modId 命名空间下的条目
 * （NeoForge 的 {@code RegistriesDatapackGenerator} 按命名空间过滤），注册到 {@code anvillib} 下
 * 会静默不落盘。表达式里因此要写全名 {@code anvilcraft:double(x)}；flat 文本按路径优先查注册表，
 * 全名引用能正常解析。</p>
 */
public class ModMathFunctions {
    /**
     * 把传入的维修材料消耗翻倍，作为「通用维修材料」未指定消耗时的默认值。
     *
     * <p>形参名与调用点绑定的具名传入值 {@code $(cost)} 同名，函数体既能写 {@code $(cost)*2}，
     * 也能写成 flat 文本 {@code "x*2"}——第 0 个传入值就是它。</p>
     */
    public static final ResourceKey<IFunction> DOUBLE = ResourceKey.create(
        LibRegistries.FUNCTION_KEY,
        AnvilCraft.of("double")
    );

    public static void bootstrap(BootstrapContext<IFunction> ctx) {
        ctx.register(ModMathFunctions.DOUBLE, ModMathFunctions.doubleFunction());
    }

    /**
     * 翻倍函数的定义。
     *
     * <p>函数体里的 {@code $(a)} 在求值期从调用点的具名传入值里取，所以构造调用表达式时
     * 传 {@link IExpression#ref(String) ref(a)} 即可——该引用是按位绑下形参后再按名字取的，
     * 位置和名字都能对上。形参个数仍按位校验，实参不能省。</p>
     */
    public static CustomFunction doubleFunction() {
        return CustomFunction.of(
            List.of("a"),
            LibBuiltInFunctions.MULTIPLY.call(IExpression.ref("a"), IExpression.of(2))
        );
    }
}
