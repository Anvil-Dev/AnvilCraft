package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.util.PistonMoveGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(PistonBaseBlock.class)
abstract class PistonBaseBlockMixin implements IHammerChangeable {

    @WrapMethod(method = "moveBlocks")
    private boolean reserveMovingBlocks(
        Level level,
        BlockPos pistonPos,
        Direction direction,
        boolean extending,
        Operation<Boolean> original
    ) {
        if (level.isClientSide()) return original.call(level, pistonPos, direction, extending);
        try (PistonMoveGuard.Scope ignored = PistonMoveGuard.begin(level)) {
            return original.call(level, pistonPos, direction, extending);
        }
    }

    @WrapOperation(
        method = "moveBlocks",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;getToPush()Ljava/util/List;"
        )
    )
    private List<BlockPos> moveMagnetizedNodes(
        PistonStructureResolver resolver,
        Operation<List<BlockPos>> original,
        @Local(argsOnly = true) Level level
    ) {
        List<BlockPos> positions = original.call(resolver);
        if (level.isClientSide() || positions.isEmpty()) {
            return positions;
        }
        AABB area = new AABB(positions.getFirst());
        for (BlockPos pos : positions) {
            area = area.minmax(new AABB(pos));
        }
        // 先收集全部节点，避免连续方块上的节点迁移后再次被选中。
        List<MagnetizedNodeEntity> nodes = level.getEntitiesOfClass(
            MagnetizedNodeEntity.class, area.inflate(1), node -> positions.contains(node.blockPos)
        );
        for (MagnetizedNodeEntity node : nodes) {
            node.moveWithPiston(resolver.getPushDirection());
        }
        return positions;
    }

    @Override
    public boolean change(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer) {
        BlockState blockState = level.getBlockState(blockPos);
        // 活塞已伸出，不旋转
        if (blockState.getValue(PistonBaseBlock.EXTENDED)) {
            return false;
        }
        BlockState state = switch (blockState.getValue(DirectionalBlock.FACING)) {
            case UP -> blockState.setValue(DirectionalBlock.FACING, Direction.DOWN);
            case DOWN -> blockState.setValue(DirectionalBlock.FACING, Direction.NORTH);
            case NORTH -> blockState.setValue(DirectionalBlock.FACING, Direction.EAST);
            case EAST -> blockState.setValue(DirectionalBlock.FACING, Direction.SOUTH);
            case SOUTH -> blockState.setValue(DirectionalBlock.FACING, Direction.WEST);
            case WEST -> blockState.setValue(DirectionalBlock.FACING, Direction.UP);
        };
        level.setBlockAndUpdate(blockPos, state);
        return true;
    }

    @Override
    public boolean checkBlockState(BlockState blockState) {
        return !blockState.getValue(PistonBaseBlock.EXTENDED);
    }

    @Override
    public @Nullable Property<?> getChangeableProperty(BlockState blockState) {
        return PistonBaseBlock.FACING;
    }
}
