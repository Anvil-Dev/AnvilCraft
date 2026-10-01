package dev.dubhe.anvilcraft.recipe.anvil.predicate.item;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.recipe.AnvilLibRecipe;
import dev.anvilcraft.lib.v2.recipe.cache.ItemCache;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheElement;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheInput;
import dev.anvilcraft.lib.v2.recipe.cache.item.ICacheInputOutputImpl;
import dev.anvilcraft.lib.v2.recipe.cache.item.ItemResourceHandlerCacheElement;
import dev.anvilcraft.lib.v2.recipe.cache.item.operation.InputOutputOperation;
import dev.anvilcraft.lib.v2.recipe.predicate.IRecipePredicate;
import dev.anvilcraft.lib.v2.recipe.predicate.function.IPredicateFunction;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.anvilcraft.lib.v2.recipe.util.Range;
import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.block.entity.StampingPlatformBlockEntity;
import dev.dubhe.anvilcraft.init.recipe.ModRecipePredicateTypes;
import dev.dubhe.anvilcraft.mixin.accessor.ICacheInputOutputImplAccessor;
import dev.dubhe.anvilcraft.mixin.accessor.ItemResourceHandlerCacheElementAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public record HasDiffItems(
    Vec3 offset,
    Vec3 range,
    List<ItemIngredientPredicate> ingredients,
    List<IPredicateFunction<?>> functions
) implements IRecipePredicate<HasDiffItems> {
    public HasDiffItems {
        ingredients = List.copyOf(ingredients);
        functions = List.copyOf(functions);
    }

    public HasDiffItems(Vec3 offset, Vec3 range, ItemIngredientPredicate item, List<IPredicateFunction<?>> functions) {
        this(offset, range, List.of(item), functions);
    }

    public ItemIngredientPredicate item() {
        return this.ingredients.getFirst();
    }

    public static HasDiffItems fromPredicates(List<ItemIngredientPredicate> predicates, Vec3 offset, Vec3 range) {
        return new HasDiffItems(offset, range, predicates, List.of());
    }

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
        if (!(input instanceof ICacheInputOutputImpl impl)) return false;
        ICacheInputOutputImplAccessor accessor = Util.cast(impl);
        Set<ICacheElement> elements = accessor.getElements();
        if (this.ingredients.isEmpty() || this.ingredients.stream().anyMatch(ingredient -> ingredient.count() <= 0)) return false;
        long count = this.ingredients.stream().mapToLong(ItemIngredientPredicate::count).sum();
        if (elements.size() != count) return false;
        List<ICacheElement> ordered = elements.stream().sorted(Comparator.comparingInt(element ->
            element instanceof ItemResourceHandlerCacheElement
                ? ((ItemResourceHandlerCacheElementAccessor) element).getSlot() : Integer.MAX_VALUE)).toList();
        Set<Item> items = new HashSet<>();
        int index = 0;
        for (ItemIngredientPredicate ingredient : this.ingredients) {
            for (int i = 0; i < ingredient.count(); i++) {
                ICacheElement element = ordered.get(index++);
                if (element.getCount() != 1 || !element.is(ingredient.testIgnoreCount())) return false;
                element.apply(stack -> items.add(stack.getItem()));
            }
        }
        return items.size() == elements.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void accept(InWorldRecipeContext context) {
        ICacheInput input = this.getItem(context);
        ItemCache cache = context.computeIfAbsent(ItemCache.ITEM_CACHE);
        Vec3 outputPos = BlockPos.containing(context.getPos().add(this.offset)).getBottomCenter();
        input.apply(itemStack -> {
            ItemStackTemplate remainder = itemStack.getCraftingRemainder();
            if (remainder != null) {
                var remainingStack = remainder.create();
                cache.getOutput(remainingStack, outputPos).grow(remainingStack, true);
            }
            if (this.functions.isEmpty()) return;
            ItemStackTemplate consumed = ItemStackTemplate.fromNonEmptyStack(itemStack);
            for (IPredicateFunction<?> function : this.functions) {
                IPredicateFunction<ItemStackTemplate> itemFunction = (IPredicateFunction<ItemStackTemplate>) function;
                consumed = itemFunction.apply(context, consumed);
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
                Vec3 pos = ctx.getPos().add(this.offset);
                ICacheInput input = itemCache.getInput(stack -> !stack.isEmpty(), pos, this.range);
                if (!(ctx.getLevel().getBlockEntity(BlockPos.containing(pos)) instanceof StampingPlatformBlockEntity platform)
                    || !(input instanceof ICacheInputOutputImpl impl)) return input;
                ICacheInputOutputImplAccessor accessor = Util.cast(impl);
                var elements = accessor.getElements().stream().filter(element ->
                    element instanceof ItemResourceHandlerCacheElement
                        && ((ItemResourceHandlerCacheElementAccessor) element).getItemHandler() == platform.getInput()).toList();
                return new ICacheInputOutputImpl(this, itemCache, pos, Range.of(pos, this.range), elements);
            });
        return context.computeIfAbsent(cacheInput);
    }

    @Override
    public Type getType() {
        return ModRecipePredicateTypes.HAS_DIFF_ITEMS.get();
    }

    public static class Type implements IRecipePredicate.Type<HasDiffItems> {
        private static final MapCodec<List<ItemIngredientPredicate>> INGREDIENTS_CODEC = Codec.mapEither(
            ItemIngredientPredicate.CODEC.fieldOf("ingredient"),
            ItemIngredientPredicate.CODEC.listOf().fieldOf("ingredients")
        ).xmap(either -> either.map(List::of, Function.identity()), ingredients -> ingredients.size() == 1
            ? Either.left(ingredients.getFirst()) : Either.right(ingredients));
        public static final MapCodec<HasDiffItems> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            Vec3.CODEC
                .fieldOf("offset")
                .forGetter(HasDiffItems::offset),
            Vec3.CODEC
                .fieldOf("range")
                .forGetter(HasDiffItems::range),
            Type.INGREDIENTS_CODEC.forGetter(HasDiffItems::ingredients),
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
            ItemIngredientPredicate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            HasDiffItems::ingredients,
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
