package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.CrabTrapBlock;
import dev.dubhe.anvilcraft.block.entity.CrabTrapBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class HitCrabTrapBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        ServerLevel level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        double fallDistance,
        AnvilEvent.OnLand event
    ) {
        if (!hitBlockState.hasBlockEntity()) return false;
        CrabTrapBlockEntity blockEntity = (CrabTrapBlockEntity) level.getBlockEntity(hitBlockPos);
        Direction face = hitBlockState.getValue(CrabTrapBlock.FACING);
        Vec3 dropPos = hitBlockPos.above().relative(face).getCenter().relative(face.getOpposite(), 0.5);
        if (blockEntity == null) return false;
        ResourceHandler<ItemResource> depository = blockEntity.getItemHandler();
        for (int i = 0; i < depository.size(); i++) {
            ItemResource resource = depository.getResource(i);
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = depository.extract(i, resource, Integer.MAX_VALUE, transaction);
                if (extracted == 0) continue;
                ItemEntity itemEntity = new ItemEntity(level, dropPos.x, dropPos.y - 0.4, dropPos.z, resource.toStack(extracted), 0, 0, 0);
                itemEntity.setDefaultPickUpDelay();
                level.addFreshEntity(itemEntity);
                transaction.commit();
            }
        }
        return false;
    }
}
