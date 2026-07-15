package dev.dubhe.anvilcraft.recipe.anvil.outcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.LargeCauldronBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeOutcomeTypes;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public record RoyalPreferenceOutcome(ChanceItemStack result) implements IRecipeOutcome<RoyalPreferenceOutcome> {

    /**
     * Context data key：标记当前配方是否为产出皇家钢的配方
     */
    public static final InWorldRecipeData<Boolean> IS_ROYAL_STEEL_RECIPE =
        InWorldRecipeData.of(AnvilCraft.of("is_royal_steel_recipe"), false);

    /**
     * 配方输入范围（与 SuperHeatingRecipe.itemInputRange 一致）
     */
    private static final Vec3 INPUT_RANGE = new Vec3(0.75, 0.75, 0.75);

    /**
     * 配方输入偏移（与 SuperHeatingRecipe.itemInputOffset 一致）
     */
    private static final Vec3 INPUT_OFFSET = new Vec3(0.0, -0.375, 0.0);

    /**
     * 配方输出偏移（与 SuperHeatingRecipe.itemOutputOffset 一致）
     */
    private static final Vec3 OUTPUT_OFFSET = new Vec3(0.0, -0.75, 0.0);

    @Override
    public IRecipeOutcome.Type<RoyalPreferenceOutcome> getType() {
        return ModRecipeOutcomeTypes.ROYAL_PREFERENCE.get();
    }

    @Override
    public void accept(InWorldRecipeContext context) {
        ServerLevel level = context.getLevel();
        Vec3 pos = context.getPos();

        // 仅皇家钢配方才检查偏好
        if (!context.get(IS_ROYAL_STEEL_RECIPE)) return;

        BlockPos belowPos = BlockPos.containing(pos.x, pos.y - 1, pos.z);

        if (level.getBlockEntity(belowPos) instanceof LargeCauldronBlockEntity cauldron
            && cauldron.hasInputMatching(stack -> RoyalPreference.isRoyalPreferred(level, stack))) {
            int count = context.getInt(result.count());
            ItemStack bonus = result.stack().copyWithCount(count);
            ItemStack remaining = cauldron.insertRecipeOutput(bonus);
            if (!remaining.isEmpty()) AnvilUtil.dropItems(List.of(remaining), level, pos.add(OUTPUT_OFFSET));
            return;
        }

        // 优先检查鱼缸容器（容器合成场景）
        IItemHandler handler = level.getCapability(
            Capabilities.ItemHandler.BLOCK, belowPos, null);
        if (handler != null) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (!stack.isEmpty() && RoyalPreference.isRoyalPreferred(level, stack)) {
                    // 鱼缸场景：双倍产物放入容器输出槽
                    int count = context.getInt(result.count());
                    ItemStack bonus = result.stack().copyWithCount(count);
                    ItemStack remaining = insertIntoHandler(handler, bonus);
                    if (!remaining.isEmpty()) {
                        // 容器满了则掉落到输出位置
                        AnvilUtil.dropItems(List.of(remaining), level, pos.add(OUTPUT_OFFSET));
                    }
                    return;
                }
            }
        }

        // 炼药锅场景：扫描掉落物
        Vec3 inputCenter = pos.add(INPUT_OFFSET);
        AABB inputBox = new AABB(inputCenter, inputCenter).inflate(
            INPUT_RANGE.x, INPUT_RANGE.y, INPUT_RANGE.z);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, inputBox)) {
            if (RoyalPreference.isRoyalPreferred(level, itemEntity.getItem())) {
                // 炼药锅场景：双倍产物掉落到与其他产物一致的高度
                int count = context.getInt(result.count());
                ItemStack stackToDrop = result.stack().copyWithCount(count);
                AnvilUtil.dropItems(List.of(stackToDrop), level, pos.add(OUTPUT_OFFSET));
                return;
            }
        }
    }

    /**
     * 尝试将物品插入容器，返回未能插入的部分
     */
    private static ItemStack insertIntoHandler(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack;
        // 优先插入输出槽（0-7），然后尝试所有槽
        for (int slot = 0; slot < 8 && slot < handler.getSlots(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
            if (remaining.isEmpty()) return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
            if (remaining.isEmpty()) return ItemStack.EMPTY;
        }
        return remaining;
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

        public static void initRoyalPreference(ServerLevel level) {
            Supplier<Random> randomFactory = () -> new Random(level.getSeed());
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
