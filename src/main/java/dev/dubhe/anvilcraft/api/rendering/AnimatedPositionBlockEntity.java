package dev.dubhe.anvilcraft.api.rendering;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
/**
 * 支持平滑位移渲染的 BlockEntity 基类。
 *
 *  <p>
 * 通过 {@link #setTargetOffset(float, float, float)} 设置目标位移，
 * 渲染时会用 partialTick 在上一帧位置和目标位置之间做线性插值，
 * 从而实现不改变 BlockState 的平滑视觉移动。
 * </p>
 *
 * <pre>{@code
 * // 在子类 tick() 中调用：
 * setTargetOffset(targetX, targetY, targetZ);
 * }</pre>
 */

public abstract class AnimatedPositionBlockEntity extends BlockEntity implements IAnimatedPosition {

    // ---- 当前帧位置（tick() 结束时写入） ----
    @Getter
    private float offsetX = 0.0f;
    @Getter
    private float offsetY = 0.0f;
    @Getter
    private float offsetZ = 0.0f;

    // ---- 上一帧位置（tick() 开始时保存） ----
    private float prevOffsetX = 0.0f;
    private float prevOffsetY = 0.0f;
    private float prevOffsetZ = 0.0f;

    // ---- 目标位置（可由逻辑线程随时修改） ----
    private float targetOffsetX = 0.0f;
    private float targetOffsetY = 0.0f;
    private float targetOffsetZ = 0.0f;

    // ---- 缓动速度：每 tick 向目标靠近的比例 (0~1) ----
    // 1  = 瞬间到位（无缓动）, 0.1 = 缓慢跟随
    private float lerpSpeed = 0.15f;

    public AnimatedPositionBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // ======================================================================
    //  公共 API
    // ======================================================================

    /**
     * 设置缓动速度。
     *
     * @param speed 每 tick 向目标靠近的比例，范围 (0, 1]。
     *              推荐值：0.1~0.3 为平滑缓动，1.0 为瞬间到位。
     */
    public void setLerpSpeed(float speed) {
        this.lerpSpeed = Mth.clamp(speed, 0.01f, 1.0f);
    }

    /**
     * 直接设定目标位置（不带缓动，下一 tick 开始追）。
     */
    public void setTargetOffset(float x, float y, float z) {
        this.targetOffsetX = x;
        this.targetOffsetY = y;
        this.targetOffsetZ = z;
    }

    /**
     * 用偏移量增加目标位置。
     */
    public void addTargetOffset(float dx, float dy, float dz) {
        this.targetOffsetX += dx;
        this.targetOffsetY += dy;
        this.targetOffsetZ += dz;
    }

    /**
     * 获取渲染时使用的插值位置（供 Renderer 调用）。
     *
     * @param partialTick 帧间部分 tick，范围 [0, 1)
     * @return 插值后的 X 偏移
     */
    public float getRenderOffsetX(float partialTick) {
        return Mth.lerp(partialTick, prevOffsetX, offsetX);
    }

    /**
     * 获取渲染时使用的插值位置（供 Renderer 调用）。
     *
     * @param partialTick 帧间部分 tick，范围 [0, 1)
     * @return 插值后的 Y 偏移
     */
    public float getRenderOffsetY(float partialTick) {
        return Mth.lerp(partialTick, prevOffsetY, offsetY);
    }

    /**
     * 获取渲染时使用的插值位置（供 Renderer 调用）。
     *
     * @param partialTick 帧间部分 tick，范围 [0, 1)
     * @return 插值后的 Z 偏移
     */
    public float getRenderOffsetZ(float partialTick) {
        return Mth.lerp(partialTick, prevOffsetZ, offsetZ);
    }

    // ======================================================================
    //  Tick 逻辑
    // ======================================================================

    /**
     * <b>必须</b>在子类 {@code tick()} 中调用，或由子类 override 后 super.tick()。
     *
     *  <p>
     * 职责：
     * <ol>
     *   <li>保存上一帧位置（用于插值）</li>
     *   <li>将当前位置向目标位置靠近（缓动）</li>
     *   <li>标记更新以同步客户端</li>
     * </ol>
     * </p>
     */
    public void tickAnimation() {
        // 1. 记录上一帧位置
        this.prevOffsetX = this.offsetX;
        this.prevOffsetY = this.offsetY;
        this.prevOffsetZ = this.offsetZ;

        // 2. 向目标缓动
        this.offsetX += (this.targetOffsetX - this.offsetX) * lerpSpeed;
        this.offsetY += (this.targetOffsetY - this.offsetY) * lerpSpeed;
        this.offsetZ += (this.targetOffsetZ - this.offsetZ) * lerpSpeed;

        // 3. 如果距离足够近，直接 snap（避免微小的持续抖动）
        if (Math.abs(this.offsetX - this.targetOffsetX) < 1e-5f
            && Math.abs(this.offsetY - this.targetOffsetY) < 1e-5f
            && Math.abs(this.offsetZ - this.targetOffsetZ) < 1e-5f) {
            this.offsetX = this.targetOffsetX;
            this.offsetY = this.targetOffsetY;
            this.offsetZ = this.targetOffsetZ;
        }

        // 4. 标记数据变化（如果位置改变较大的话，可按需优化）
        setChanged();
    }

    // ======================================================================
    //  序列化（将位置持久化，防止重进世界后丢失）
    // ======================================================================

    private static final String TAG_OFFSET_X = "anim_offset_x";
    private static final String TAG_OFFSET_Y = "anim_offset_y";
    private static final String TAG_OFFSET_Z = "anim_offset_z";
    private static final String TAG_TARGET_X = "anim_target_x";
    private static final String TAG_TARGET_Y = "anim_target_y";
    private static final String TAG_TARGET_Z = "anim_target_z";

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat(TAG_OFFSET_X, this.offsetX);
        tag.putFloat(TAG_OFFSET_Y, this.offsetY);
        tag.putFloat(TAG_OFFSET_Z, this.offsetZ);
        tag.putFloat(TAG_TARGET_X, this.targetOffsetX);
        tag.putFloat(TAG_TARGET_Y, this.targetOffsetY);
        tag.putFloat(TAG_TARGET_Z, this.targetOffsetZ);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_OFFSET_X, Tag.TAG_FLOAT)) {
            this.offsetX = tag.getFloat(TAG_OFFSET_X);
            this.offsetY = tag.getFloat(TAG_OFFSET_Y);
            this.offsetZ = tag.getFloat(TAG_OFFSET_Z);
        }
        if (tag.contains(TAG_TARGET_X, Tag.TAG_FLOAT)) {
            this.targetOffsetX = tag.getFloat(TAG_TARGET_X);
            this.targetOffsetY = tag.getFloat(TAG_TARGET_Y);
            this.targetOffsetZ = tag.getFloat(TAG_TARGET_Z);
        }
        // 加载后保证 prev = current，防止加载瞬间跳帧
        this.prevOffsetX = this.offsetX;
        this.prevOffsetY = this.offsetY;
        this.prevOffsetZ = this.offsetZ;
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putFloat(TAG_OFFSET_X, this.offsetX);
        tag.putFloat(TAG_OFFSET_Y, this.offsetY);
        tag.putFloat(TAG_OFFSET_Z, this.offsetZ);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        if (tag.contains(TAG_OFFSET_X, Tag.TAG_FLOAT)) {
            // 直接从网络数据设置，不做插值（网络更新通常是瞬间的）
            this.offsetX = tag.getFloat(TAG_OFFSET_X);
            this.offsetY = tag.getFloat(TAG_OFFSET_Y);
            this.offsetZ = tag.getFloat(TAG_OFFSET_Z);
            this.prevOffsetX = this.offsetX;
            this.prevOffsetY = this.offsetY;
            this.prevOffsetZ = this.offsetZ;
            this.targetOffsetX = this.offsetX;
            this.targetOffsetY = this.offsetY;
            this.targetOffsetZ = this.offsetZ;
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
