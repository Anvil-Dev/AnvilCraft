package dev.dubhe.anvilcraft.event.giantanvil.shock;

import dev.dubhe.anvilcraft.api.giantanvil.ShockAnvilBehavior;
import dev.dubhe.anvilcraft.api.giantanvil.ShockDropBehavior;
import dev.dubhe.anvilcraft.util.BlockMiningEffect;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public abstract class DestroyMode {
    public static final DestroyMode NORMAL = createForEffect(BlockMiningEffect.NORMAL);
    public static final DestroyMode SILK_TOUCH = createForEffect(BlockMiningEffect.SILK_TOUCH);
    public static final DestroyMode AUTO_SMELTING = createForEffect(BlockMiningEffect.SMELTING);
    public static final DestroyMode FORTUNE = createForEffect(BlockMiningEffect.FORTUNE_5);
    public static final DestroyMode DISINTEGRATION = createForEffect(BlockMiningEffect.DISINTEGRATION);

    private static DestroyMode createForEffect(BlockMiningEffect effect) {
        return new DestroyMode() {
            @Override
            public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx) {
                return applyEffect(state, pos, ctx, effect, null);
            }

            @Override
            public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx, ItemStack baseTool) {
                return applyEffect(state, pos, ctx, effect, baseTool);
            }
        };
    }

    public static DestroyMode fromEffect(BlockMiningEffect effect) {
        if (effect.equals(BlockMiningEffect.SILK_TOUCH)) return SILK_TOUCH;
        if (effect.equals(BlockMiningEffect.DISINTEGRATION)) return DISINTEGRATION;
        if (effect.equals(BlockMiningEffect.SMELTING)) return AUTO_SMELTING;
        if (effect.equals(BlockMiningEffect.FORTUNE_5)) return FORTUNE;
        return NORMAL;
    }

    /** 根据边框铁砧行为创建破坏模式，并保留其自定义掉落处理器。 */
    public static DestroyMode fromAnvilBehavior(ShockAnvilBehavior behavior) {
        return fromEffect(behavior.miningEffect()).withDropBehavior(behavior.dropBehavior());
    }

    private final ShockDropBehavior dropBehavior;

    protected DestroyMode() {
        this(ShockDropBehavior.DEFAULT);
    }

    private DestroyMode(ShockDropBehavior dropBehavior) {
        this.dropBehavior = dropBehavior;
    }

    private DestroyMode withDropBehavior(ShockDropBehavior behavior) {
        DestroyMode source = this;
        return new DestroyMode(behavior) {
            @Override
            public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx) {
                return source.apply(state, pos, ctx);
            }

            @Override
            public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx, ItemStack baseTool) {
                return source.apply(state, pos, ctx, baseTool);
            }
        };
    }

    /** 使用当前铁砧行为生成一个或多个掉落物实体。 */
    public final void dropItems(List<ItemStack> itemStacks, BlockPos pos, ShockContext context) {
        for (ItemStack itemStack : itemStacks) {
            if (itemStack.isEmpty()) continue;
            dropBehavior.drop(context, pos, itemStack);
        }
    }

    private static List<ItemStack> applyEffect(
        BlockState state, BlockPos pos, ShockContext ctx, BlockMiningEffect effect, ItemStack baseTool
    ) {
        if (!(ctx.level() instanceof ServerLevel serverLevel)) return List.of();
        if (effect.isDisintegration()) {
            BreakBlockUtil.dropExperience(serverLevel, pos, state, effect);
        }
        return BreakBlockUtil.drop(serverLevel, pos, state, effect, baseTool);
    }

    public abstract List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx);

    public List<ItemStack> apply(BlockState state, BlockPos pos, ShockContext ctx, ItemStack tool) {
        return apply(state, pos, ctx);
    }
}
