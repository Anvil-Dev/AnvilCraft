package dev.dubhe.anvilcraft.util;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import javax.annotation.Nullable;

public final class BlockPlacementPicking {
    private static final double HIT_EPSILON = 1.0E-5;

    private BlockPlacementPicking() {
    }

    public static UseOnContext forPlacement(UseOnContext context) {
        if (!(context instanceof PlayerClick click) || !click.anvilcraft$isPlayerClick()) return context;
        Player player = context.getPlayer();
        if (player == null || player instanceof FakePlayer) return context;
        // 放置辅助可以直接指定可替换格，或在非本格坐标中编码朝向；这些不是需要修正的模型命中。
        if (context.getLevel().getBlockState(context.getClickedPos()).canBeReplaced()
            || !new AABB(context.getClickedPos()).inflate(HIT_EPSILON).contains(context.getClickLocation())) return context;
        Vec3 start = player.getEyePosition();
        // 使用本次点击携带的位置恢复射线，避免服务端依赖另一帧的视角包。
        Vec3 direction = context.getClickLocation().subtract(start);
        if (direction.lengthSqr() < 1.0E-10) direction = player.getViewVector(1);
        Vec3 end = start.add(direction.normalize().scale(player.blockInteractionRange()));
        BlockHitResult hit = context.getLevel().clip(new ClipContext(
            start, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        // 已在原形状表面的点击保留调用方给出的面和 inside 标记，包括只调整朝向的放置操作。
        if (hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(context.getClickedPos())
            && hit.getLocation().distanceToSqr(context.getClickLocation()) <= HIT_EPSILON * HIT_EPSILON) return context;
        if (!allowed(context.getLevel(), player, context.getItemInHand(), hit)) {
            hit = BlockHitResult.miss(hit.getLocation(), hit.getDirection(), hit.getBlockPos());
        }
        return new UseOnContext(context.getLevel(), player, context.getHand(), context.getItemInHand(), hit);
    }

    @Nullable
    public static InteractionResultHolder<ItemStack> tryPlaceFromAir(ItemStack stack, Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = findAirPlacementHit(stack, level, player);
        if (hit == null) return null;
        return new InteractionResultHolder<>(stack.useOn(new UseOnContext(level, player, hand, stack, hit)), stack);
    }

    @Nullable
    public static BlockHitResult findAirPlacementHit(ItemStack stack, Level level, Player player) {
        if (player instanceof FakePlayer || player.isSpectator()) return null;
        HitResult picked = player.pick(player.blockInteractionRange(), 1, false);
        if (!(picked instanceof BlockHitResult hit) || !allowed(level, player, stack, hit)) return null;
        Vec3 start = player.getEyePosition();
        Vec3 end = hit.getLocation();
        if (ProjectileUtil.getEntityHitResult(player, start, end, player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1),
            entity -> !entity.isSpectator() && entity.isPickable(), start.distanceToSqr(end)) != null) return null;
        return hit;
    }

    private static boolean allowed(Level level, Player player, ItemStack stack, BlockHitResult hit) {
        return hit.getType() == HitResult.Type.BLOCK
            && level.getWorldBorder().isWithinBounds(hit.getBlockPos())
            && player.canInteractWithBlock(hit.getBlockPos(), 0)
            && level.mayInteract(player, hit.getBlockPos())
            && (player.getAbilities().mayBuild || stack.canPlaceOnBlockInAdventureMode(new BlockInWorld(level, hit.getBlockPos(), false)));
    }

    public interface PlayerClick {
        boolean anvilcraft$isPlayerClick();

        boolean anvilcraft$hasBlockHit();
    }
}
