package dev.dubhe.anvilcraft.recipe.anvil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.recipe.anvil.util.ShapelessMatcher;
import lombok.Getter;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 世界内配方类，定义了在世界中执行的配方
 * 该类实现了 Minecraft 的 Recipe 接口，用于处理在世界中而非工作台中执行的配方
 */
@Getter
public class InWorldRecipe implements Recipe<InWorldRecipeContext>, IPrioritized {
    /**
     * 配方图标物品堆
     */
    @Unmodifiable
    private final ItemStack icon;

    /**
     * 配方触发器
     */
    private final IRecipeTrigger trigger;

    /**
     * 冲突的配方谓词列表
     */
    @Unmodifiable
    private final List<IRecipePredicate<?>> conflicting;

    /**
     * 非冲突的配方谓词列表
     */
    @Unmodifiable
    private final List<IRecipePredicate<?>> nonConflicting;

    /**
     * 配方结果列表
     */
    @Unmodifiable
    private final List<IRecipeOutcome<?>> outcomes;

    /**
     * 配方优先级
     */
    private final int priority;

    /**
     * 是否兼容
     */
    private final boolean compatible;

    /**
     * 构造一个新的世界内配方
     *
     * @param icon           配方图标
     * @param trigger        配方触发器
     * @param conflicting    冲突的配方谓词列表
     * @param nonConflicting 非冲突的配方谓词列表
     * @param outcomes       配方结果列表
     * @param priority       配方优先级
     * @param compatible     是否兼容
     */
    public InWorldRecipe(
        @NotNull ItemStack icon,
        IRecipeTrigger trigger,
        @Unmodifiable List<IRecipePredicate<?>> conflicting,
        @Unmodifiable List<IRecipePredicate<?>> nonConflicting,
        @Unmodifiable List<IRecipeOutcome<?>> outcomes,
        int priority,
        boolean compatible
    ) {
        this.icon = icon;
        this.trigger = trigger;
        this.conflicting = conflicting;
        this.nonConflicting = nonConflicting;
        this.outcomes = outcomes;
        this.priority = priority;
        this.compatible = compatible;
    }

    /**
     * 构造一个新的世界内配方，自动计算优先级
     *
     * @param icon           配方图标
     * @param trigger        配方触发器
     * @param conflicting    冲突的配方谓词列表
     * @param nonConflicting 非冲突的配方谓词列表
     * @param outcomes       配方结果列表
     * @param compatible     是否兼容
     */
    public InWorldRecipe(
        @NotNull ItemStack icon,
        IRecipeTrigger trigger,
        @Unmodifiable List<IRecipePredicate<?>> conflicting,
        @Unmodifiable List<IRecipePredicate<?>> nonConflicting,
        @Unmodifiable List<IRecipeOutcome<?>> outcomes,
        boolean compatible
    ) {
        this.icon = icon;
        this.trigger = trigger;
        this.conflicting = conflicting;
        this.nonConflicting = nonConflicting;
        this.outcomes = outcomes;
        this.priority = InWorldRecipe.calcPriority(trigger, conflicting, nonConflicting, outcomes);
        this.compatible = compatible;
    }

    /**
     * 计算配方优先级
     *
     * @param trigger        配方触发器
     * @param conflicting    冲突的配方谓词列表
     * @param nonConflicting 非冲突的配方谓词列表
     * @param outcomes       配方结果列表
     * @return 计算出的优先级值
     */
    public static int calcPriority(
        @NotNull IRecipeTrigger trigger,
        @Unmodifiable @NotNull List<IRecipePredicate<?>> conflicting,
        @Unmodifiable @NotNull List<IRecipePredicate<?>> nonConflicting,
        @Unmodifiable @NotNull List<IRecipeOutcome<?>> outcomes
    ) {
        int priority = trigger.getPriority();
        for (IRecipePredicate<?> predicate : conflicting) {
            priority += predicate.getPriority();
        }
        for (IRecipePredicate<?> predicate : nonConflicting) {
            priority += predicate.getPriority();
        }
        for (IRecipeOutcome<?> outcome : outcomes) {
            priority += outcome.getPriority();
        }
        return priority;
    }

    /**
     * 判断配方是否匹配给定的上下文和世界
     *
     * @param context 配方上下文
     * @param level 世界
     * @return 是否匹配
     */
    @Override
    public boolean matches(@NotNull InWorldRecipeContext context, @NotNull Level level) {
        boolean nonConflicting = ShapelessMatcher.compatible(this.conflicting, context);
        if (!nonConflicting) {
            return false;
        }
        boolean flag;
        if (this.compatible) {
            flag = ShapelessMatcher.compatible(this.nonConflicting, context);
        } else {
            flag = ShapelessMatcher.incompatible(this.nonConflicting, context);
        }
        if (!flag) context.getStack().clear();
        return flag;
    }

    /**
     * 组装配方结果
     *
     * @param context 配方上下文
     * @param provider 数据提供器
     * @return 配方结果物品堆
     */
    @Override
    public @NotNull ItemStack assemble(@NotNull InWorldRecipeContext context, @NotNull HolderLookup.Provider provider) {
        List<IRecipePredicate<?>> stack = context.getStack();
        IRecipePredicate<?> predicate;
        while (!stack.isEmpty()) {
            predicate = stack.removeFirst();
            predicate.accept(context);
        }
        for (IRecipeOutcome<?> outcome : this.outcomes) {
            outcome.acceptWithChance(context);
        }
        return this.icon.copy();
    }

    /**
     * 判断配方是否可以在指定尺寸的工作台中制作
     *
     * @param i 宽度
     * @param i1 高度
     * @return 是否可以制作
     */
    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    /**
     * 获取配方结果物品堆
     *
     * @param provider 数据提供器
     * @return 配方结果物品堆
     */
    @Override
    public @NotNull ItemStack getResultItem(@NotNull HolderLookup.Provider provider) {
        return this.icon.copy();
    }

    /**
     * 获取配方序列化器
     *
     * @return 配方序列化器
     */
    @Override
    public @NotNull RecipeSerializer<? extends InWorldRecipe> getSerializer() {
        return ModRecipeTypes.IN_WORLD_RECIPE_SERIALIZER.get();
    }

    /**
     * 获取配方类型
     *
     * @return 配方类型
     */
    @Override
    public @NotNull RecipeType<? extends InWorldRecipe> getType() {
        return ModRecipeTypes.IN_WORLD_RECIPE.get();
    }

    /**
     * 世界内配方序列化器
     */
    public static class Serializer implements RecipeSerializer<InWorldRecipe> {
        private static final Codec<IRecipePredicate<?>> PREDICATE_CODEC = ModRegistries.PREDICATE_TYPE_REGISTRY
            .byNameCodec()
            .dispatch(IRecipePredicate::getType, IRecipePredicate.Type::codec);
        private static final Codec<IRecipeOutcome<?>> OUTCOME_CODEC = ModRegistries.OUTCOME_TYPE_REGISTRY
            .byNameCodec()
            .dispatch(IRecipeOutcome::getType, IRecipeOutcome.Type::codec);
        private static final MapCodec<InWorldRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                ItemStack.CODEC
                    .fieldOf("icon")
                    .orElse(Items.ANVIL.getDefaultInstance())
                    .forGetter(InWorldRecipe::getIcon),
                ModRegistries.TRIGGER_REGISTRY
                    .byNameCodec()
                    .fieldOf("trigger")
                    .forGetter(InWorldRecipe::getTrigger),
                PREDICATE_CODEC
                    .listOf()
                    .fieldOf("conflicting")
                    .forGetter(InWorldRecipe::getConflicting),
                PREDICATE_CODEC
                    .listOf()
                    .fieldOf("non_conflicting")
                    .forGetter(InWorldRecipe::getNonConflicting),
                OUTCOME_CODEC
                    .listOf()
                    .fieldOf("outcomes")
                    .forGetter(InWorldRecipe::getOutcomes),
                Codec.INT
                    .fieldOf("priority")
                    .orElse(1)
                    .forGetter(InWorldRecipe::getPriority),
                Codec.BOOL
                    .fieldOf("compatible")
                    .orElse(true)
                    .forGetter(InWorldRecipe::isCompatible)
            ).apply(instance, InWorldRecipe::new)
        );

        /**
         * 获取MapCodec编解码器
         *
         * @return MapCodec编解码器
         */
        @Override
        public @NotNull MapCodec<InWorldRecipe> codec() {
            return Serializer.CODEC;
        }

        /**
         * 流编解码器
         */
        public final StreamCodec<RegistryFriendlyByteBuf, InWorldRecipe> streamCodec = StreamCodec.of(
            Serializer::encode,
            Serializer::decode
        );

        /**
         * 获取流编解码器
         *
         * @return 流编解码器
         */
        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, InWorldRecipe> streamCodec() {
            return this.streamCodec;
        }

        /**
         * 编码配方到字节缓冲区
         *
         * @param buf 字节缓冲区
         * @param recipe 要编码的配方
         * @param <P> 配方谓词类型
         * @param <O> 配方结果类型
         */
        @SuppressWarnings("unchecked")
        private static <P extends IRecipePredicate<P>, O extends IRecipeOutcome<O>> void encode(
            RegistryFriendlyByteBuf buf, @NotNull InWorldRecipe recipe
        ) {
            ItemStack.STREAM_CODEC.encode(buf, recipe.icon);
            buf.writeResourceLocation(recipe.trigger.getId());
            buf.writeVarInt(recipe.conflicting.size());
            for (IRecipePredicate<?> predicate : recipe.conflicting) {
                buf.writeResourceLocation(predicate.getType().getId());
                ((P) predicate).getType().streamCodec().encode(buf, (P) predicate);
            }
            buf.writeVarInt(recipe.nonConflicting.size());
            for (IRecipePredicate<?> predicate : recipe.nonConflicting) {
                buf.writeResourceLocation(predicate.getType().getId());
                ((P) predicate).getType().streamCodec().encode(buf, (P) predicate);
            }
            buf.writeVarInt(recipe.outcomes.size());
            for (IRecipeOutcome<?> outcome : recipe.outcomes) {
                buf.writeResourceLocation(outcome.getType().getId());
                ((O) outcome).getType().streamCodec().encode(buf, (O) outcome);
            }
            buf.writeInt(recipe.priority);
            buf.writeBoolean(recipe.compatible);
        }

        /**
         * 从字节缓冲区解码配方
         *
         * @param buf 字节缓冲区
         * @return 解码出的配方
         */
        private static @NotNull InWorldRecipe decode(RegistryFriendlyByteBuf buf) {
            ItemStack icon = ItemStack.STREAM_CODEC.decode(buf);
            IRecipeTrigger trigger = ModRegistries.TRIGGER_REGISTRY.get(buf.readResourceLocation());
            List<IRecipePredicate<?>> conflicting = decodeRecipePredicateList(buf);
            List<IRecipePredicate<?>> nonConflicting = decodeRecipePredicateList(buf);
            List<IRecipeOutcome<?>> outcomes = new ArrayList<>();
            int outcomesSize = buf.readVarInt();
            for (int i = 0; i < outcomesSize; i++) {
                ResourceLocation location = buf.readResourceLocation();
                IRecipeOutcome.Type<?> type = ModRegistries.OUTCOME_TYPE_REGISTRY.get(location);
                if (type == null) throw new IllegalArgumentException("Unknown outcome type: " + location);
                IRecipeOutcome<?> outcome = type.streamCodec().decode(buf);
                outcomes.add(outcome);
            }
            return new InWorldRecipe(
                icon,
                trigger,
                Collections.unmodifiableList(conflicting),
                Collections.unmodifiableList(nonConflicting),
                Collections.unmodifiableList(outcomes),
                buf.readInt(),
                buf.readBoolean()
            );
        }

        /**
         * 从字节缓冲区解码配方谓词列表
         *
         * @param buf 字节缓冲区
         * @return 解码出的配方谓词列表
         */
        private static @NotNull List<IRecipePredicate<?>> decodeRecipePredicateList(
            @NotNull RegistryFriendlyByteBuf buf
        ) {
            int size = buf.readVarInt();
            List<IRecipePredicate<?>> predicates = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                ResourceLocation location = buf.readResourceLocation();
                IRecipePredicate.Type<?> type = ModRegistries.PREDICATE_TYPE_REGISTRY.get(location);
                if (type == null) throw new IllegalArgumentException("Unknown predicate type: " + location);
                IRecipePredicate<?> predicate = type.streamCodec().decode(buf);
                predicates.add(predicate);
            }
            return predicates;
        }
    }
}