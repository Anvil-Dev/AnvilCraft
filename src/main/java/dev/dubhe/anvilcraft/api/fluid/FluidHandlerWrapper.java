package dev.dubhe.anvilcraft.api.fluid;

import dev.dubhe.anvilcraft.fluid.HoneyFluid;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.Nullable;

/**
 * 流体处理器封装器。
 *
 * <p>
 * 统一处理发射器与流体容器（鱼缸、储罐等 {@link IFluidHandler} 实现）之间的流体交互，
 * 简化填充/抽取时物品转换逻辑，避免重复代码。
 * </p>
 */
@Getter
public class FluidHandlerWrapper {
    /**
     * -- GETTER --
     *  获取被包装的原始流体处理器。
     */
    private final IFluidHandler handler;

    public FluidHandlerWrapper(IFluidHandler handler) {
        this.handler = handler;
    }

    /**
     * 从世界中获取指定位置的流体处理器封装。
     *
     * @param level 世界
     * @param pos   目标位置
     * @param side  访问面，可为 null
     * @return 封装实例，若目标无流体处理器则返回 null
     */
    @Nullable
    public static FluidHandlerWrapper of(Level level, BlockPos pos, @Nullable Direction side) {
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
        return handler != null ? new FluidHandlerWrapper(handler) : null;
    }

    /**
     * 判断包装器是否持有有效的流体处理器。
     */
    public boolean isValid() {
        return true;
    }

    public static boolean tryInteractWithBottle(
        Player player,
        InteractionHand hand,
        IFluidHandler handler,
        Level level,
        BlockPos pos
    ) {
        ItemStack stack = player.getItemInHand(hand);
        if (!FluidHandlerWrapper.isExplicitBottleInteraction(stack)) return false;
        FluidHandlerWrapper wrapper = new FluidHandlerWrapper(handler);
        if (level.isClientSide()) {
            return wrapper.fillFromItem(stack, true, null) != null
                || wrapper.drainToItem(stack, true) != null;
        }
        ItemStack result = wrapper.fillFromItem(stack, false, level.getRandom());
        if (result != null) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, result));
            player.awardStat(Stats.FILL_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
            return true;
        }

        result = wrapper.drainToItem(stack);
        if (result != null) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, result));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS);
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
            return true;
        }

        return false;
    }

    private static boolean isExplicitBottleInteraction(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE)
            || stack.is(Items.HONEY_BOTTLE)
            || FluidHandlerWrapper.isWaterPotion(stack)
            || stack.is(Items.EXPERIENCE_BOTTLE);
    }

    private static boolean isWaterPotion(ItemStack stack) {
        if (!stack.is(Items.POTION)) return false;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return false;
        if (contents.potion().isEmpty()) return false;
        return contents.potion().get() == Potions.WATER;
    }

    // region 填充：从物品向处理器注入流体

    /**
     * 将流体容器物品中的流体填充到此处理器。
     *
     *  <p>
     * 模拟并执行：从物品流体处理器中抽取 → 向此处理器填充 → 返回空容器。
     *
     * @param filledContainer 装有流体的容器物品栈
     * @return 填充成功返回空容器物品，失败返回 null
     */
    @Nullable
    public ItemStack fillFromItem(ItemStack filledContainer) {
        return fillFromItem(filledContainer, false, null);
    }

    /**
     * 将流体容器物品中的流体填充到此处理器（可选择是否模拟）。
     *
     * @param filledContainer 装有流体的容器物品栈
     * @param simulateOnly    仅模拟（true）或执行实际转移（false）
     * @return 填充成功返回空容器物品，失败返回 null
     */
    @Nullable
    public ItemStack fillFromItem(ItemStack filledContainer, boolean simulateOnly) {
        return fillFromItem(filledContainer, simulateOnly, null);
    }

    /**
     * 将流体容器物品中的流体填充到此处理器（可选择是否模拟和随机概率）。
     *
     * @param filledContainer 装有流体的容器物品栈
     * @param simulateOnly    仅模拟（true）或执行实际转移（false）
     * @param random          随机源，用于附魔之瓶的概率判定；null 则视为 100% 成功
     * @return 填充成功返回空容器物品，失败返回 null
     */
    @Nullable
    public ItemStack fillFromItem(ItemStack filledContainer, boolean simulateOnly, @Nullable RandomSource random) {
        // 尝试通过标准 IFluidHandlerItem 处理（桶、蜂蜜瓶等）
        IFluidHandlerItem itemHandler = FluidUtil.getFluidHandler(filledContainer).orElse(null);
        if (itemHandler != null) {
            // 模拟：从物品中抽取
            FluidStack drained = itemHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty()) return null;

            // 模拟：向目标处理器中填充
            int filled = handler.fill(drained, IFluidHandler.FluidAction.SIMULATE);
            if (filled < drained.getAmount()) return null;

            // 模拟模式下到此为止
            if (simulateOnly) {
                return itemHandler.getContainer();
            }

            // 执行：从物品中抽取并填充到目标
            itemHandler.drain(drained, IFluidHandler.FluidAction.EXECUTE);
            handler.fill(drained, IFluidHandler.FluidAction.EXECUTE);

            return itemHandler.getContainer();
        }

        // 处理水瓶（PotionContents，不含有 IFluidHandlerItem）
        ItemStack result = tryFillFromPotion(filledContainer, simulateOnly);
        if (result != null) return result;

        // 处理附魔之瓶（50% 概率填充经验液体）
        return tryFillFromExpBottle(filledContainer, simulateOnly, random);
    }

    // region 特殊物品处理

    /**
     * 尝试从水瓶中填充水到处理器。
     */
    @Nullable
    private ItemStack tryFillFromPotion(ItemStack stack, boolean simulateOnly) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) return null;
        if (contents.potion().isEmpty()) return null;
        Holder<Potion> potion = contents.potion().get();
        if (potion != Potions.WATER) return null;

        FluidStack water = new FluidStack(Fluids.WATER, FluidType.BUCKET_VOLUME / 4);
        int filled = handler.fill(water, IFluidHandler.FluidAction.SIMULATE);
        if (filled < water.getAmount()) return null;

        if (!simulateOnly) {
            handler.fill(water, IFluidHandler.FluidAction.EXECUTE);
        }
        return new ItemStack(Items.GLASS_BOTTLE);
    }

    /**
     * 尝试从附魔之瓶中填充经验液体到处理器。
     *
     *  <p>始终消耗附魔之瓶并返回玻璃瓶，但仅 50% 概率实际填充经验液体。</p>
     */
    @Nullable
    private ItemStack tryFillFromExpBottle(ItemStack stack, boolean simulateOnly, @Nullable RandomSource random) {
        if (!stack.is(Items.EXPERIENCE_BOTTLE)) return null;

        FluidStack expFluid = new FluidStack(ModFluids.EXP_FLUID, FluidType.BUCKET_VOLUME / 4);

        // 模拟：检查是否有足够空间
        int filled = handler.fill(expFluid, IFluidHandler.FluidAction.SIMULATE);
        if (filled < expFluid.getAmount()) return null;

        // 执行：50% 概率实际填充
        if (!simulateOnly && (random == null || random.nextBoolean())) {
            handler.fill(expFluid, IFluidHandler.FluidAction.EXECUTE);
        }

        // 始终返回玻璃瓶（即使概率失败）
        return new ItemStack(Items.GLASS_BOTTLE);
    }

    // endregion

    // region 抽取：从处理器向空容器抽取流体

    /**
     * 从此处理器抽取流体到空容器物品。
     *
     *  <p>
     * 根据空容器类型自动决定抽取量（桶 1000mB，瓶 250mB）。
     *
     * @param emptyContainer 空容器物品（桶、玻璃瓶等）
     * @return 装满流体的容器物品，失败返回 null
     */
    @Nullable
    public ItemStack drainToItem(ItemStack emptyContainer) {
        return drainToItem(emptyContainer, false);
    }

    @Nullable
    public ItemStack drainToItem(ItemStack emptyContainer, boolean simulateOnly) {
        if (!isSupportedEmptyContainer(emptyContainer)) return null;
        int amount = emptyContainer.is(Items.GLASS_BOTTLE)
            ? FluidType.BUCKET_VOLUME / 4
            : FluidType.BUCKET_VOLUME;
        return drainToItem(emptyContainer, amount, simulateOnly);
    }

    /**
     * 从此处理器抽取指定量流体到空容器物品。
     *
     * @param emptyContainer 空容器物品
     * @param amount         要抽取的流体量（mB）
     * @param simulateOnly   仅模拟
     * @return 装满流体的容器物品，失败返回 null
     */
    @Nullable
    public ItemStack drainToItem(ItemStack emptyContainer, int amount, boolean simulateOnly) {
        if (!isSupportedEmptyContainer(emptyContainer)) return null;

        // 模拟：从目标处理器抽取
        FluidStack drained = handler.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        if (drained.getAmount() < amount) return null;

        // 确定结果物品
        ItemStack result = determineFilledContainer(drained, emptyContainer);
        if (result.isEmpty()) return null;

        // 模拟模式下到此为止
        if (simulateOnly) return result;

        // 执行抽取
        handler.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        return result;
    }

    /**
     * 根据抽取到的流体和空容器类型确定装满后的容器物品。
     */
    private static ItemStack determineFilledContainer(FluidStack drained, ItemStack emptyContainer) {
        boolean isBottle = emptyContainer.is(Items.GLASS_BOTTLE);

        if (isBottle) {
            // 蜂蜜瓶
            if (drained.getFluid() instanceof HoneyFluid) {
                return new ItemStack(Items.HONEY_BOTTLE);
            }
            // 水瓶（需要 PotionContents）
            if (drained.getFluid() == Fluids.WATER) {
                return PotionContents.createItemStack(Items.POTION, Potions.WATER);
            }
            // 经验流体 → 附魔之瓶
            if (drained.getFluid() == ModFluids.EXP_FLUID.get()) {
                return new ItemStack(Items.EXPERIENCE_BOTTLE);
            }
            return ItemStack.EMPTY;
        }

        // 通用：检查流体是否有对应的桶装形式
        Item bucketItem = drained.getFluid().getBucket();
        if (bucketItem != Items.AIR) {
            return new ItemStack(bucketItem);
        }

        return ItemStack.EMPTY;
    }

    private static boolean isSupportedEmptyContainer(ItemStack stack) {
        return stack.is(Items.BUCKET) || stack.is(Items.GLASS_BOTTLE);
    }

    // endregion

    // region 便捷查询

    /**
     * 获取处理器中的当前流体。
     */
    public FluidStack getFluid() {
        return handler.getFluidInTank(0);
    }

    /**
     * 获取处理器容量。
     */
    public int getCapacity() {
        return handler.getTankCapacity(0);
    }

    /**
     * 处理器是否为空。
     */
    public boolean isEmpty() {
        return handler.getFluidInTank(0).isEmpty();
    }

    /**
     * 处理器是否已满。
     */
    public boolean isFull() {
        FluidStack fluid = handler.getFluidInTank(0);
        return fluid.getAmount() >= handler.getTankCapacity(0);
    }

    // endregion
}
