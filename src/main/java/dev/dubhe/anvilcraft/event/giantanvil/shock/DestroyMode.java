package dev.dubhe.anvilcraft.event.giantanvil.shock;

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
