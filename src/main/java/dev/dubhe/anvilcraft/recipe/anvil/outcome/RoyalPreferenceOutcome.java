package dev.dubhe.anvilcraft.recipe.anvil.outcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeOutcomeTypes;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

public record RoyalPreferenceOutcome(ChanceItemStack result) implements IRecipeOutcome<RoyalPreferenceOutcome> {
    public static final InWorldRecipeData<Boolean> IS_ROYAL_STEEL_RECIPE = InWorldRecipeData.of(
        AnvilCraft.of("is_royal_steel_recipe"),
        false
    );
    private static final Vec3 INPUT_RANGE = new Vec3(0.75, 0.75, 0.75);
    private static final Vec3 INPUT_OFFSET = new Vec3(0.0, -0.375, 0.0);
    private static final Vec3 OUTPUT_OFFSET = new Vec3(0.0, -0.75, 0.0);

    @Override
    public IRecipeOutcome.Type<RoyalPreferenceOutcome> getType() {
        return ModRecipeOutcomeTypes.ROYAL_PREFERENCE.get();
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        ServerLevel level = context.getLevel();
        Vec3 pos = context.getPos();
        if (!context.get(IS_ROYAL_STEEL_RECIPE)) return;

        Vec3 inputCenter = pos.add(INPUT_OFFSET);
        AABB inputBox = new AABB(inputCenter, inputCenter).inflate(INPUT_RANGE.x, INPUT_RANGE.y, INPUT_RANGE.z);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, inputBox)) {
            if (RoyalPreference.isRoyalPreferred(level, itemEntity.getItem())) {
                int count = context.getInt(this.result.count());
                ItemStack stackToDrop = this.result.stack().create().copyWithCount(count);
                AnvilUtil.dropItems(List.of(stackToDrop), level, pos.add(OUTPUT_OFFSET));
                return;
            }
        }
    }

    @SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
    public static class RoyalPreference {
        static @Nullable Optional<Item> preferredGem;
        static @Nullable Optional<Item> preferredGemBlock;

        public static boolean isRoyalPreferred(ServerLevel level, ItemStack stack) {
            if (RoyalPreference.preferredGem == null) RoyalPreference.initRoyalPreferredGem(() -> new Random(level.getSeed()));
            if (RoyalPreference.preferredGemBlock == null) RoyalPreference.initRoyalPreferredGemBlock(() -> new Random(level.getSeed()));
            return stack.is(RoyalPreference.preferredGem.orElseThrow()) || stack.is(RoyalPreference.preferredGemBlock.orElseThrow());
        }

        public static void initRoyalPreference(long seed) {
            Supplier<Random> randomFactory = () -> new Random(seed);
            RoyalPreference.initRoyalPreferredGem(randomFactory);
            RoyalPreference.initRoyalPreferredGemBlock(randomFactory);
        }

        private static void initRoyalPreferredGem(Supplier<Random> randomFactory) {
            List<Item> gems = new ArrayList<>();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ModItemTags.GEMS)) {
                if (holder.value() != Items.EMERALD) {
                    gems.add(holder.value());
                }
            }
            if (gems.isEmpty()) return;
            Random random = randomFactory.get();
            RoyalPreference.preferredGem = Optional.of(gems.get(random.nextInt(gems.size())));
        }

        private static void initRoyalPreferredGemBlock(Supplier<Random> randomFactory) {
            List<Item> gemBlocks = new ArrayList<>();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(ModItemTags.GEM_BLOCKS)) {
                if (holder.value() != Items.EMERALD_BLOCK) {
                    gemBlocks.add(holder.value());
                }
            }
            if (gemBlocks.isEmpty()) return;
            Random random = randomFactory.get();
            RoyalPreference.preferredGemBlock = Optional.of(gemBlocks.get(random.nextInt(gemBlocks.size())));
        }
    }

    public static class Type implements IRecipeOutcome.Type<RoyalPreferenceOutcome> {
        public static final MapCodec<RoyalPreferenceOutcome> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ChanceItemStack.CODEC.fieldOf("result").forGetter(RoyalPreferenceOutcome::result)
        ).apply(instance, RoyalPreferenceOutcome::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, RoyalPreferenceOutcome> STREAM_CODEC = StreamCodec.composite(
            ChanceItemStack.STREAM_CODEC,
            RoyalPreferenceOutcome::result,
            RoyalPreferenceOutcome::new
        );

        @Override
        public MapCodec<RoyalPreferenceOutcome> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RoyalPreferenceOutcome> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
