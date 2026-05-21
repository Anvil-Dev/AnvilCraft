package dev.dubhe.anvilcraft.recipe.anvil.predicate.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheElement;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheInput;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheInputOutputImpl;
import dev.anvilcraft.lib.v2.recipe.cache.item.operation.InputOutputOperation;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.function.IPredicateFunction;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.recipe.ModRecipePredicateTypes;
import dev.dubhe.anvilcraft.mixin.accessor.ICacheInputOutputImplAccessor;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record HasDiffItems(
    Vec3 offset,
    Vec3 range,
    ItemIngredientPredicate item,
    List<IPredicateFunction<?>> functions
) implements IRecipePredicate<HasDiffItems> {
    /// 构造一个物品原料条件谓词
    ///
    /// @param offset    偏移量
    /// @param range     范围
    public static HasDiffItems fromPredicate(ItemIngredientPredicate predicate, Vec3 offset, Vec3 range) {
        return new HasDiffItems(offset, range, predicate, List.of());
    }

    @Override
    public boolean test(InWorldRecipeContext context) {
        ICacheInput input = this.getItem(context);
        Set<Item> items = new HashSet<>();
        IntList counts = new IntArrayList();
        input.apply(stack -> items.add(stack.getItem().asItem()));
        // noinspection StatementWithEmptyBody
        if (input instanceof ICacheInputOutputImpl impl) {
            ICacheInputOutputImplAccessor accessor = Util.cast(impl);
            for (ICacheElement element : accessor.getElements()) {
                counts.add(element.getCount());
            }
        } else {
            // TODO: 找到不使用ICacheInputOutputImpl也能获取所有元素的数量列表的方法
            // input.apply(stack -> counts.add(stack.count()));
        }
        if (counts.size() != this.item.count() || counts.size() != items.size()) return false;
        for (int count : counts) {
            if (count < 1) return false;
        }
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void accept(InWorldRecipeContext context) {
        ICacheInput item1 = this.getItem(context);
        item1.apply(itemStack -> {
            for (IPredicateFunction<?> function : this.functions) {
                IPredicateFunction<ItemStack> function1 = (IPredicateFunction<ItemStack>) function;
                itemStack = function1.apply(context, itemStack);
            }
        });
        context.putAcceptor(ItemCache.ITEM_CACHE.location(), ItemCache.DEFAULT_ACCEPTOR);
    }

    @Override
    public void snapshot(InWorldRecipeContext context) {
        ICacheInput input = this.getItem(context);
        if (!(input instanceof ICacheInputOutputImpl impl)) {
            // TODO: 找到不使用ICacheInputOutputImpl也能使所有元素分别减一的方法
            // input.apply(stack -> stack.shrink(1));
            return;
        }
        ICacheInputOutputImplAccessor accessor = Util.cast(impl);
        // region ICacheInputOutputImpl#shrink
        Set<ICacheElement> elements = new HashSet<>();
        for (ICacheElement element : accessor.getElements()) {
            element.shrink(1);
            elements.add(element);
        }
        accessor.getShrinkSimulateStack().push(new InputOutputOperation(elements));
        // endregion
    }

    @Override
    public void rollback(InWorldRecipeContext context) {
        ICacheInput input = this.getItem(context);
        input.rollbackShrink();
    }

    @Override
    public void clearStack(InWorldRecipeContext context) {
        ICacheInput input = this.getItem(context);
        input.clearStack();
    }

    public ICacheInput getItem(InWorldRecipeContext context) {
        context.computeIfAbsent(ItemCache.ITEM_CACHE);
        final InWorldRecipeData<ICacheInput> cacheInput = InWorldRecipeData.of(
            AnvilLibRecipe.of("item_cache_input/%s".formatted(this.hashCode())),
            (ctx, _) -> {
                ItemCache itemCache = ctx.get(ItemCache.ITEM_CACHE);
                return itemCache.getInput(this.item.testIgnoreCount(), context.getPos().add(this.offset), this.range);
            });
        return context.computeIfAbsent(cacheInput);
    }

    @Override
    public Type getType() {
        return ModRecipePredicateTypes.HAS_DIFF_ITEMS.get();
    }

    public static class Type implements IRecipePredicate.Type<HasDiffItems> {
        public static final MapCodec<HasDiffItems> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Vec3.CODEC
                .fieldOf("offset")
                .forGetter(HasDiffItems::offset),
            Vec3.CODEC
                .fieldOf("range")
                .forGetter(HasDiffItems::range),
            ItemIngredientPredicate.CODEC
                .fieldOf("ingredient")
                .forGetter(HasDiffItems::item),
            IPredicateFunction.CODEC
                .listOf()
                .optionalFieldOf("functions", List.of())
                .forGetter(HasDiffItems::functions)
        ).apply(inst, HasDiffItems::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, HasDiffItems> STREAM_CODEC = StreamCodec.composite(
            Vec3.STREAM_CODEC,
            HasDiffItems::offset,
            Vec3.STREAM_CODEC,
            HasDiffItems::range,
            ItemIngredientPredicate.STREAM_CODEC,
            HasDiffItems::item,
            StreamCodecUtil.codec2Stream(IPredicateFunction.CODEC).apply(ByteBufCodecs.list()),
            HasDiffItems::functions,
            HasDiffItems::new
        );

        @Override
        public MapCodec<HasDiffItems> codec() {
            return Type.CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, HasDiffItems> streamCodec() {
            return Type.STREAM_CODEC;
        }

        @Override
        public boolean conflict() {
            return true;
        }
    }
}
