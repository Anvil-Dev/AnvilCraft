package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class HasMobBlockEntity extends BlockEntity {
    private CompoundTag entity = null;
    private Entity displayEntity = null;

    protected HasMobBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 设置实体
     */
    public void setEntity(Entity entity) {
        if (entity == null) return;
        if (this.entity == null) this.entity = new CompoundTag();
        entity.save(this.entity);
        this.entity.remove(Entity.UUID_TAG);
    }

    /**
     * 设置实体
     */
    public void setEntity(CompoundTag entity) {
        if (entity == null) return;
        this.entity = entity;
        this.entity.remove(Entity.UUID_TAG);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.entity != null) {
            tag.put("entity", this.entity);
        }
    }

    @Override
    public void load(@NotNull CompoundTag tag) {
        if (tag.contains("entity")) {
            this.entity = tag.getCompound("entity");
            if (this.level != null) {
                this.getEntity(this.level);
            }
        }
        super.load(tag);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * @return 实体
     */
    @Nullable
    public Entity getOrCreateDisplayEntity(Level level) {
        if (this.displayEntity == null && this.entity != null) {
            this.getEntity(level);
        }
        return this.displayEntity;
    }

    private void getEntity(Level level) {
        Optional<EntityType<?>> optional = EntityType.by(this.entity);
        if (optional.isEmpty()) return;
        EntityType<?> type = optional.get();
        Entity entity = type.create(level);
        if (entity == null) return;
        entity.load(this.entity);
        this.displayEntity = entity;
    }
}
