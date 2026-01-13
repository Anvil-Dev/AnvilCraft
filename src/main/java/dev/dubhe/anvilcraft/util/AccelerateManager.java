package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.AccelerationRingBlock;
import dev.dubhe.anvilcraft.block.entity.AccelerationRingBlockEntity;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class AccelerateManager {

    public static void handleAcceleration(Entity entity) {
        if (!canBeAccelerated(entity)) return;
        Level level = entity.level();
        for (BlockPos pos : AccelerationRingBlockEntity.getAllBlocks(level)) {
            AABB aabb = AccelerationRingBlockEntity.getAABB(pos);
            if (aabb == null) continue;
            if (aabb.contains(entity.position())) {
                BlockState state = level.getBlockState(pos);
                if (isActiveAccelerationRing(state)) {
                    applyAcceleration(entity, pos, state.getValue(AccelerationRingBlock.FACING));
                }
            }
        }
    }

    private static boolean isActiveAccelerationRing(BlockState state) {
        return state.hasProperty(AccelerationRingBlock.HALF)
               && state.getValue(AccelerationRingBlock.HALF) == DirectionCube3x3PartHalf.MID_CENTER
               && state.getValue(AccelerationRingBlock.SWITCH) == IPowerComponent.Switch.ON
               && !state.getValue(AccelerationRingBlock.OVERLOAD);
    }

    public static boolean canBeAccelerated(Entity entity) {
        return entity instanceof FallingBlockEntity fallingBlockEntity
               && fallingBlockEntity.getBlockState().is(BlockTags.ANVIL)
               && !fallingBlockEntity.getBlockState().is(ModBlockTags.NON_MAGNETIC)
               || entity instanceof Projectile
               || (entity instanceof Player player && isPlayerCanBeAccelerated(player));
    }

    static boolean isPlayerCanBeAccelerated(Player player) {
        Iterable<ItemStack> armorSlots = player.getArmorSlots();
        boolean hasHammer = false;
        int count = 0;
        for (ItemStack stack : armorSlots) {
            if (stack.getItem() instanceof AnvilHammerItem) {
                hasHammer = true;
            }
            if (stack.getItem() instanceof ArmorItem) {
                count++;
            }
        }
        return count >= 2 && hasHammer;
    }

    private static void applyAcceleration(Entity entity, BlockPos ringPos, Direction direction) {
        double speed = Math.abs(entity.getDeltaMovement().get(direction.getAxis()));
        if (speed > 25565 || (entity instanceof Player && speed > 20)) {
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, entity.getGravity(), 0));
            return;
        }
        Vec3 fixMovement = ringPos
            .getCenter()
            .subtract(
                entity instanceof FallingBlockEntity || entity instanceof Player
                ? entity.position().add(0, 0.5, 0)
                : entity.position()
            );
        Vec3 deltaMovement = entity.getDeltaMovement();
        fixMovement = switch (direction.getAxis()) {
            case X -> fixMovement.multiply(0, 1, 1);
            case Y -> fixMovement.multiply(1, 0, 1);
            case Z -> fixMovement.multiply(1, 1, 0);
        };
        deltaMovement = switch (direction.getAxis()) {
            case X -> deltaMovement.multiply(1, 0, 0);
            case Y -> deltaMovement.multiply(0, 1, 0);
            case Z -> deltaMovement.multiply(0, 0, 1);
        };
        fixMovement = fixMovement.multiply(0.2, 0.2, 0.2);
        if (Math.abs(entity.getDeltaMovement().get(direction.getAxis())) <= 5) {
            deltaMovement = deltaMovement.add(fixMovement);
        } else {
            entity.setPos(entity.position().add(fixMovement.multiply(5, 5, 5)));
        }
        deltaMovement = deltaMovement.scale(1.0204081632653061)
            .add(new Vec3(0.1f, 0.1f, 0.1f).multiply(Vec3.atLowerCornerOf(direction.getNormal())));
        entity.setDeltaMovement(deltaMovement);
        entity.setDeltaMovement(entity.getDeltaMovement().add(0, entity.getGravity(), 0));
    }
}
