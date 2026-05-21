package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.attachment.AttachmentType;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public abstract class HasMobBlockEntity extends BlockEntity {
    private @Nullable SavedEntity entity = null;
    private Entity displayEntity = null;

    protected HasMobBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 设置实体
     */
    public void setEntity(@Nullable Entity entity) {
        if (entity == null) return;
        this.entity = SavedEntity.fromEntity(entity);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.entity != null) {
            output.store("entity", SavedEntity.CODEC, this.entity);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Optional<SavedEntity> entity = input.read("entity", SavedEntity.CODEC);
        if (entity.isEmpty()) return;
        this.entity = entity.get();
        if (this.level != null) {
            this.getEntity(this.level);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return this.saveWithoutMetadata(provider);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Nullable
    public Entity getOrCreateDisplayEntity(Level level) {
        if (this.displayEntity == null && this.entity != null) {
            this.getEntity(level);
        }
        return this.displayEntity;
    }

    private void getEntity(Level level) {
        Entity entity;
        if (this.entity == null) {
            entity = this.createDefaultEntity(level);
            this.entity = SavedEntity.fromEntity(entity);
        } else {
            entity = this.entity.toEntity(level);
        }
        if (entity == null) return;
        entity.setYRot(0);
        this.displayEntity = entity;
        this.displayEntity.noPhysics = true;
    }

    protected abstract @Nullable Entity createDefaultEntity(Level level);

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        SavedEntity entity = components.get(ModComponents.SAVED_ENTITY);
        if (entity == null) return;
        this.entity = entity;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModComponents.SAVED_ENTITY, this.entity);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(ValueOutput output) {
        super.removeComponentsFromTag(output);
        output.discard("entity");
    }

    @Override
    public @Nullable <T> T removeData(Supplier<AttachmentType<T>> type) {
        return super.removeData(type);
    }
}
