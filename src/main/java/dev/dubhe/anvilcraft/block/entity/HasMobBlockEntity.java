package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.item.HasMobBlockItem;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class HasMobBlockEntity extends BlockEntity {
    private CompoundTag entity = null;
    private Entity displayEntity = null;

    protected HasMobBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 设置实体
     */
    public void setEntity(@Nullable Entity entity) {
        if (entity == null) return;
        if (this.entity == null) this.entity = new CompoundTag();
        entity.save(this.entity);
        this.entity.remove(Entity.UUID_TAG);
    }

    /**
     * 设置实体
     */
    public void setEntity(@Nullable CompoundTag entity) {
        if (entity == null) return;
        this.entity = entity;
        this.entity.remove(Entity.UUID_TAG);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (this.entity != null) {
            tag.put("entity", this.entity);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        if (tag.contains("entity")) {
            this.entity = tag.getCompound("entity");
            if (this.level != null) {
                this.getEntity(this.level);
            }
        }
        super.loadAdditional(tag, provider);
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
        if (this.entity == null) return;
        Optional<EntityType<?>> optional = EntityType.by(this.entity);
        if (optional.isEmpty()) return;
        EntityType<?> type = optional.get();
        Entity entity = type.create(level);
        if (entity == null) return;
        entity.load(this.entity);
        entity.setYRot(0);
        this.displayEntity = entity;
        this.displayEntity.noPhysics = true;
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        HasMobBlockItem.SavedEntity savedEntity = componentInput.get(ModComponents.SAVED_ENTITY);
        if (savedEntity == null) return;
        this.setEntity(savedEntity.getTag());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (this.displayEntity == null && this.level != null) {
            this.getEntity(this.level);
        }
        if (!(this.displayEntity instanceof Mob mob)) return;
        components.set(ModComponents.SAVED_ENTITY, HasMobBlockItem.SavedEntity.fromMob(mob));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("entity");
    }
}
