package dev.dubhe.anvilcraft.recipe.anvil.outcome;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.recipe.cache.IItemHandlerCache;
import dev.anvilcraft.lib.v2.recipe.outcome.IPlannedItemOutcome;
import dev.anvilcraft.lib.v2.recipe.outcome.IRecipeOutcome;
import dev.anvilcraft.lib.v2.recipe.outcome.PlannedItem;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeData;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.entity.IEntityCauldron;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeOutcomeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;

public record RoyalPreferenceOutcome(ChanceItemStack result) implements IPlannedItemOutcome<RoyalPreferenceOutcome> {

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
    public List<PlannedItem> plan(InWorldRecipeContext context) {
        if (!context.get(IS_ROYAL_STEEL_RECIPE) || !hasPreferredSource(context.getLevel(), context.getPos(), null, context)) {
            return List.of();
        }
        return this.planBonus(context);
    }

    /** 根据配方输入、重叠实体锅和掉落物判断加成。客户端只排除绝不可能被选为偏爱的材料。 */
    public static boolean hasPreferredSource(Level level, Vec3 pos, @Nullable IItemHandler input) {
        return hasPreferredSource(level, pos, input, null);
    }

    private static boolean hasPreferredSource(
        Level level, Vec3 pos, @Nullable IItemHandler input, @Nullable InWorldRecipeContext context
    ) {
        Predicate<ItemStack> preferred = level instanceof ServerLevel server
            ? stack -> RoyalPreference.isRoyalPreferred(server, stack)
            : stack -> (stack.is(ModItemTags.GEMS) && !stack.is(Items.EMERALD))
                || (stack.is(ModItemTags.GEM_BLOCKS) && !stack.is(Items.EMERALD_BLOCK));
        BlockPos belowPos = BlockPos.containing(pos.x, pos.y - 1, pos.z);

        BlockEntity blockEntity = level.getBlockEntity(belowPos);
        boolean hasRecipeCache = false;
        if (blockEntity instanceof IItemHandlerCache cache) {
            hasRecipeCache = true;
            if (hasRoyalPreferred(preferred, input == null ? cache.getInput() : input)) {
                return true;
            }
        }

        IItemHandlerCache entityCauldron = RoyalPreferenceOutcome.findEntityCauldron(level, belowPos, context);
        if (entityCauldron != null) {
            hasRecipeCache = true;
            if (hasRoyalPreferred(preferred, entityCauldron.getInput())) {
                return true;
            }
        }

        // 没有配方缓存的容器（如原版容器）退回到直接读能力
        if (!hasRecipeCache) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, belowPos, null);
            if (handler != null && hasRoyalPreferred(preferred, handler)) {
                return true;
            }
        }

        // 炼药锅场景：扫描掉落物
        Vec3 inputCenter = pos.add(RoyalPreferenceOutcome.INPUT_OFFSET);
        AABB inputBox = new AABB(inputCenter, inputCenter).inflate(
            RoyalPreferenceOutcome.INPUT_RANGE.x, RoyalPreferenceOutcome.INPUT_RANGE.y, RoyalPreferenceOutcome.INPUT_RANGE.z);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, inputBox)) {
            if (preferred.test(itemEntity.getItem())) {
                return true;
            }
        }
        return false;
    }

    private List<PlannedItem> planBonus(InWorldRecipeContext context) {
        ItemStack bonus = this.result.stack().copyWithCount(context.getInt(this.result.count()));
        return bonus.isEmpty() ? List.of() : List.of(new PlannedItem(bonus, context.getPos().add(OUTPUT_OFFSET)));
    }

    private static boolean hasRoyalPreferred(Predicate<ItemStack> preferred, IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty() && preferred.test(stack)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable IItemHandlerCache findEntityCauldron(Level level, BlockPos pos, @Nullable InWorldRecipeContext context) {
        Vec3 center = pos.getCenter();
        AABB lookupArea = new AABB(pos).inflate(0.0625);
        IItemHandlerCache closest = null;
        double closestDistance = Double.POSITIVE_INFINITY;
        Iterable<?> candidates = context == null
            ? level.getEntitiesOfClass(Entity.class, lookupArea, entity -> entity instanceof IEntityCauldron && entity.isAlive())
            : HasCauldron.getEntityCauldronCandidates(context, pos);
        for (Object candidate : candidates) {
            if (!(candidate instanceof Entity entity) || !(entity instanceof IItemHandlerCache cache)
                || !entity.isAlive() || !entity.getBoundingBox().intersects(lookupArea)) {
                continue;
            }
            double distance = entity.getBoundingBox().getCenter().distanceToSqr(center);
            if (distance >= closestDistance) {
                continue;
            }
            closest = cache;
            closestDistance = distance;
        }
        return closest;
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
