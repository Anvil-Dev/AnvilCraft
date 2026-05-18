package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

public class ResentfulAmberBlockEntity extends MobAmberBlockEntity {
    private ResentfulAmberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected @Nullable Entity createDefaultEntity(Level level) {
        return EntityType.ZOMBIE.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
    }

    public static ResentfulAmberBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new ResentfulAmberBlockEntity(type, pos, blockState);
    }

    // @OnlyIn(Dist.CLIENT)
    public void clientTick(ClientLevel level, BlockPos blockPos) {
        Entity displayEntity = getOrCreateDisplayEntity(level);
        if (displayEntity == null) return;
        Vec3 center = blockPos.getCenter();
        Player nearest = level.getNearestPlayer(
            center.x,
            center.y,
            center.z,
            8,
            false
        );
        if (nearest == null) return;
        displayEntity.setPos(blockPos.getCenter());
        displayEntity.lookAt(
            EntityAnchorArgument.Anchor.FEET,
            EntityAnchorArgument.Anchor.EYES.apply(nearest)
        );
    }
}
