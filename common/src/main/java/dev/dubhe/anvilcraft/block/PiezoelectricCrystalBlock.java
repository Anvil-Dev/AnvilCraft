package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.ChargeCollectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

public class PiezoelectricCrystalBlock extends Block implements IHammerRemovable {
    public static VoxelShape SHAPE = Shapes.or(
        Block.box(0, 14, 0, 16, 16, 16),
        Block.box(2, 2, 2, 14, 14, 14),
        Block.box(0, 0, 0, 16, 2, 16)
    );

    public PiezoelectricCrystalBlock(Properties properties) {
        super(properties);
    }


    @Override
    public @Nonnull RenderShape getRenderShape(@Nonnull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @NotNull VoxelShape getShape(
        @NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context
    ) {
        return SHAPE;
    }

    /**
     * 被铁砧砸事件
     */
    public void onHitByAnvil(float fallDistance, BlockPos blockPos) {
        int chargeNum = 0;
        if (fallDistance <= 4) {
            chargeNum = (int) Math.pow(2, (int) fallDistance);
        } else {
            chargeNum = 8;
        }
        Collection<Entry<Float, ChargeCollectorBlockEntity>> chargeCollectorCollection = ChargeCollectorManager
            .getNearestChargeCollect(blockPos);
        Iterator<Entry<Float, ChargeCollectorBlockEntity>> iterator = chargeCollectorCollection.iterator();
        int surplus = chargeNum;
        while (iterator.hasNext()) {
            Entry<Float, ChargeCollectorBlockEntity> entry = iterator.next();
            ChargeCollectorBlockEntity chargeCollectorBlockEntity = entry.getValue();
            if (!ChargeCollectorManager.canCollect(chargeCollectorBlockEntity, blockPos)) return;
            surplus = chargeCollectorBlockEntity.incomingCharge(surplus);
            if (surplus == 0) return;
        }
    }
}
