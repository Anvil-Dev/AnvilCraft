package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentInfo;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorOption;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialRefactorRegistry;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialSearchHistory;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialSnapshotCodec;
import dev.dubhe.anvilcraft.block.entity.celestial.PlanetaryResourceSet;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.megastructure.ExcavatorHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.PenroseSphereHandler;
import dev.dubhe.anvilcraft.block.entity.megastructure.WormholeStabilizerHandler;
import dev.dubhe.anvilcraft.block.state.Cube323PartHalf;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.CelestialForgingAnvilMenu;
import dev.dubhe.anvilcraft.item.utility.DiskItem;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CelestialForgingAnvilBlockEntity extends BlockEntity
    implements MenuProvider, IPowerConsumer, IPowerProducer, IDiskCloneable {

    // === 巨构逻辑委托 ===
    @Getter
    private final CfaMegastructureManager megastructureManager = new CfaMegastructureManager();

    @Getter
    private float preRotation = 0;
    @Getter
    private float rotation = 0;

    @Getter
    private boolean isAmplify = false;

    /**
     * 获取锻星砧 3×2×3 结构接收到的最大红石信号强度（0-15）。
     * 遍历结构内 18 个方块并取邻居信号最大值，结果缓存
     * {@code REDSTONE_SIGNAL_CACHE_TICKS} 刻；缓存过期或主动失效后重新计算。
     */
    public int getRedstoneSignal() {
        // 客户端刚加载区块时比较器输出不一定可用，直接用同步来的缓存值
        if (this.level == null || this.level.isClientSide()) return this.cachedRedstoneSignal;
        long now = this.level.getGameTime();
        if (this.redstoneSignalCacheTick >= 0
            && now - this.redstoneSignalCacheTick < CelestialForgingAnvilBlockEntity.REDSTONE_SIGNAL_CACHE_TICKS) {
            return this.cachedRedstoneSignal;
        }
        int signal = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos partPos = this.worldPosition.offset(dx, dy, dz);
                    signal = Math.max(signal, this.level.getBestNeighborSignal(partPos));
                }
            }
        }
        this.cachedRedstoneSignal = Math.min(signal, 15);
        this.redstoneSignalCacheTick = now;
        return this.cachedRedstoneSignal;
    }

    /**
     * 使红石信号缓存立即失效，由方块的邻居更新回调调用。
     */
    public void markRedstoneSignalDirty() {
        this.redstoneSignalCacheTick = -1;
    }

    /// 红石信号变化时同步给客户端，避免界面显示陈旧值
    private void syncRedstoneSignalIfChanged() {
        if (this.level == null || this.level.isClientSide()) return;
        this.getRedstoneSignal();
        if (this.cachedRedstoneSignal != this.syncedRedstoneSignal) {
            this.syncedRedstoneSignal = this.cachedRedstoneSignal;
            this.setChanged();
            this.level.sendBlockUpdated(
                this.worldPosition,
                this.getBlockState(),
                this.getBlockState(),
                Block.UPDATE_CLIENTS
            );
        }
    }

    private int cachedRedstoneSignal = 0;
    private int syncedRedstoneSignal = 0;
    private long redstoneSignalCacheTick = -1;
    private static final int REDSTONE_SIGNAL_CACHE_TICKS = 5;

    @Getter
    @Setter
    @Nullable
    private CelestialBodyData celestialBodyData = null;

    @Getter
    @Setter
    private long bodySeed = 0;

    /**
     * 天体匹配时的质量砧子数量，用于引力计算。
     */
    @Getter
    @Setter
    private int stellarMass = 0;

    /**
     * 天体匹配时的时间砧子数量，用于资源生成。
     */
    @Getter
    @Setter
    private int ageAnvilCount = 0;

    /**
     * 当前匹配天体对应的资源集合。
     */
    @Getter
    @Setter
    @Nullable
    private PlanetaryResourceSet planetaryResourceSet = null;

    /**
     * 获取当前已建巨构在重构选项中的索引；未建造时为 -1。
     */
    public int getActiveMegastructureIndex() {
        return this.megastructureManager.getActiveIndex();
    }

    /**
     * 行星开掘器是否获得有效激光输入，用于切换工作模型。
     */
    public boolean isExcavatorLaserActive() {
        ExcavatorHandler h = this.megastructureManager.findHandler(ExcavatorHandler.class);
        return h != null && h.isLaserActive();
    }

    /**
     * 彭罗斯球是否具有有效的激光输入输出组合，用于切换工作模型。
     */
    public boolean isPenroseSphereLaserActive() {
        PenroseSphereHandler h = this.megastructureManager.findHandler(PenroseSphereHandler.class);
        return h != null && h.isLaserActive();
    }

    // === 虫洞稳定器状态 ===
    /**
     * 获取建造虫洞稳定器时确定的黑洞身份标识。
     */
    @Nullable
    public UUID getWormholeParamsHash() {
        WormholeStabilizerHandler wh = this.megastructureManager.getWormholeHandler();
        return wh.getBodyUuid();
    }

    // === 神庙状态 ===
    /**
     * 三日循环中的当前位置：0、1 为赐福日，2 为惩罚日。
     */
    @Getter
    private int templeCycleDay = 0;
    /**
     * 上次刷新供奉需求时的游戏日。
     */
    private long templeLastDay = -1;
    /**
     * 当前需求的物品类型；数量固定为 1，仅用于标识并同步给客户端提示。
     */
    @Getter
    private ItemStack templeDemandItem = ItemStack.EMPTY;
    /**
     * 当前供奉需求的总数量。
     */
    @Getter
    private int templeDemandCount = 0;
    /**
     * 当前需求已累计供奉的数量；刷新需求或完成供奉时重置。
     */
    @Getter
    private int templeDemandProgress = 0;
    /**
     * 当日供奉需求是否已经满足。
     */
    @Getter
    private boolean templeDemandSatisfied = false;

    // === 恒星演化加速器逻辑委托 ===
    public int getAcceleratorStage() {
        return this.megastructureManager.getAcceleratorHandler().getStage();
    }

    public int getAcceleratorTicksRemaining() {
        return this.megastructureManager.getAcceleratorHandler().getTicksRemaining();
    }

    public int getAcceleratorTicksTotal() {
        return this.megastructureManager.getAcceleratorHandler().getTicksTotal();
    }

    public int getSupernovaFlashTicks() {
        return this.megastructureManager.getAcceleratorHandler().getSupernovaFlashTicks();
    }

    /**
     * 恒星演化加速器是否处于任一工作阶段（1-4）。
     */
    public boolean isAcceleratorActive() {
        return this.megastructureManager.getAcceleratorHandler().isActive();
    }

    public CelestialForgingAnvilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // === 电网组件接口 ===

    @Override
    public int getInputPower() {
        if (this.searchController.isSearching() && this.searchController.ticksRemaining() > 0) {
            return this.isAmplify ? 4000 : 1000;
        }
        return this.megastructureManager.getInputPower(this);
    }

    @Override
    public int getOutputPower() {
        return this.megastructureManager.getOutputPower(this);
    }

    @Override
    public boolean isInfinitePower() {
        return this.megastructureManager.isInfinitePower(this);
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public int getRange() {
        return 1;
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    public @Nullable PowerGrid getGrid() {
        return this.grid;
    }

    @Override
    public PowerComponentType getComponentType() {
        return this.megastructureManager.getComponentType(this);
    }

    @Override
    public PowerComponentInfo toPowerComponentInfo() {
        PowerComponentType type = this.getComponentType();
        return new PowerComponentInfo(
            this.getPos(),
            this.getInputPower(),
            this.getOutputPower(),
            0, 0,
            this.getRange(),
            this.getShape(),
            type
        );
    }

    @Override
    public void gridTick() {
        this.megastructureManager.gridTick(this);
    }

    @Getter
    private int bodyRotation = 0;

    // === 天体出现与消失动画，仅客户端使用且不持久化 ===
    @Getter
    private int animationTicks = 0;
    @Getter
    private boolean animationForward = true;
    @Nullable
    @Getter
    private CelestialBodyData animationPreviousBodyData = null;
    private static final int ANIMATION_DURATION_TICKS = 20; // 1 second at 20 TPS

    /**
     * 获取本帧实际用于渲染的天体数据。
     * 反向动画期间服务端已清空当前天体，因此使用缓存的旧天体继续绘制缩小过程。
     */
    @Nullable
    public CelestialBodyData getEffectiveBodyDataForRendering() {
        if (this.celestialBodyData != null) return this.celestialBodyData;
        if (this.animationTicks > 0 && !this.animationForward && this.animationPreviousBodyData != null) {
            return this.animationPreviousBodyData;
        }
        return null;
    }

    /**
     * 获取 0（完全隐藏）到 1（完全显示）的动画进度，并应用三次缓入缓出插值。
     */
    public float getAnimationProgress(float partialTick) {
        if (this.animationTicks <= 0) return this.animationForward ? 1.0f : 0.0f;
        float t = (CelestialForgingAnvilBlockEntity.ANIMATION_DURATION_TICKS - this.animationTicks + partialTick)
                  / (float) CelestialForgingAnvilBlockEntity.ANIMATION_DURATION_TICKS;
        float eased = CelestialForgingAnvilBlockEntity.easeInOutCubic(t);
        return this.animationForward ? eased : (1.0f - eased);
    }

    /**
     * 获取动画期间的旋转速度倍率，从开始时的 5 倍逐渐衰减到正常速度。
     */
    public float getAnimationRotationBoost(float partialTick) {
        float progress = this.getAnimationProgress(partialTick);
        return 1.0f + 4.0f * (1.0f - progress);
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4.0f * t * t * t : 1.0f - (float) Math.pow(-2.0f * t + 2.0f, 3) / 2.0f;
    }

    // === 超新星闪光，仅用于渲染并同步给客户端 ===
    /** 触发时记录的天体视觉中心世界 Y 坐标，避免残骸生成后闪光中心跳变。 */
    @Getter
    private double supernovaCenterY = 0;
    /** 触发时记录的天体缩放比例，使闪光大小跟随爆炸前天体。 */
    @Getter
    private float supernovaScale = 1.0f;
    /** 超新星闪光总时长，与加速器触发值一致。 */
    public static final int SUPERNOVA_FLASH_TICKS = 10;

    /**
     * 在服务端触发超新星闪光并同步客户端。
     * 必须在残骸替换天体数据前调用，才能记录爆炸恒星的中心和缩放。
     */
    public void startSupernovaFlash() {
        this.megastructureManager.getAcceleratorHandler().setSupernovaFlashTicks(
            CelestialForgingAnvilBlockEntity.SUPERNOVA_FLASH_TICKS
        );
        this.supernovaCenterY = this.getBodyCenterWorldY();
        this.supernovaScale = this.getBodyVisualScaleRatio();
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.syncToClient();
        }
    }

    /**
     * 计算当前红石信号下天体视觉中心的世界 Y 坐标，与渲染器的中心插值保持一致。
     */
    public double getBodyCenterWorldY() {
        int redstoneSignal = this.getRedstoneSignal();
        float redstoneFactor = redstoneSignal / 5.0f;
        float fullCenterY = CelestialBodyData.dynamicCenterY(this.celestialBodyData, this.isAmplify);
        float baseCenterY = this.isAmplify ? 6.5f : 4.5f;
        float centerY = baseCenterY + (fullCenterY - baseCenterY) * redstoneFactor;
        if (this.isAmplify) {
            centerY += 19.0f * (redstoneSignal / 15.0f);
        }
        return this.worldPosition.getY() + centerY;
    }

    /**
     * 计算当前红石信号下天体视觉缩放相对其原始缩放的倍率。
     * 超新星闪光和放射光束使用该倍率，确保爆发范围与触发前天体一致。
     */
    public float getBodyVisualScaleRatio() {
        if (this.celestialBodyData == null) return 1.0f;
        float rawBodyScale = this.celestialBodyData.bodyScale();
        if (rawBodyScale <= 1.0e-6f) return 1.0f;
        float redstoneFactor = this.getRedstoneSignal() / 5.0f;
        float fullBodyScale = rawBodyScale * CelestialBodyData.BODY_SCALE_FACTOR;
        float bodyScaleMultiplier = rawBodyScale + (fullBodyScale - rawBodyScale) * redstoneFactor;
        return bodyScaleMultiplier / rawBodyScale;
    }

    // === 渲染平滑，仅客户端使用且不持久化、不同步 ===
    // 使用与帧率无关的指数逼近平滑束星环缩放、天体中心、天体缩放和光束高度，
    // 避免红石变化导致画面逐刻跳变。
    @Getter
    private float smoothRingScale;
    @Getter
    private float smoothCenterY;
    @Getter
    private float smoothBodyScale;
    @Getter
    private float smoothBeamHeight;
    private boolean smoothInitialized = false;
    private long lastSmoothNanos = 0L;
    /** 指数逼近时间常数（秒），数值越小跟随越快。 */
    private static final float SMOOTH_TAU = 0.18f;

    /** 推进一帧平滑状态，并返回与帧率无关的逼近系数。 */
    private float advanceSmoothFactor() {
        long now = Util.getNanos();
        if (!this.smoothInitialized) {
            this.lastSmoothNanos = now;
            return 1.0f;
        }
        float dt = (now - this.lastSmoothNanos) / 1.0e9f;
        this.lastSmoothNanos = now;
        if (dt <= 0f) return 0f;
        if (dt > 0.25f) dt = 0.25f; // guard against jumps after lag/pause
        return 1.0f - (float) Math.exp(-dt / CelestialForgingAnvilBlockEntity.SMOOTH_TAU);
    }

    /** 使用当前目标值更新平滑后的渲染缩放和高度，由渲染器每帧调用。 */
    public void updateRenderSmoothing(float targetRingScale, float targetCenterY, float targetBodyScale, float targetBeamHeight) {
        float f = this.advanceSmoothFactor();
        if (!this.smoothInitialized) {
            this.smoothRingScale = targetRingScale;
            this.smoothCenterY = targetCenterY;
            this.smoothBodyScale = targetBodyScale;
            this.smoothBeamHeight = targetBeamHeight;
            this.smoothInitialized = true;
            return;
        }
        this.smoothRingScale += (targetRingScale - this.smoothRingScale) * f;
        this.smoothCenterY += (targetCenterY - this.smoothCenterY) * f;
        this.smoothBodyScale += (targetBodyScale - this.smoothBodyScale) * f;
        this.smoothBeamHeight += (targetBeamHeight - this.smoothBeamHeight) * f;
    }

    @Getter
    @Setter
    private boolean locked = false;

    /**
     * 增幅器多方块结构是否完整成型。
     */
    @Getter
    @Setter
    private boolean amplifierPresent = false;

    // 建材槽过滤器，在玩家选择重构选项时更新。
    @Getter
    @Setter
    private ItemStack materialFilter = new ItemStack(Items.BARRIER);
    @Getter
    @Setter
    private int materialLimit = 0;

    @Getter
    private final SimpleContainer materialContainer = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            CelestialForgingAnvilBlockEntity.this.setChanged();
        }
    };

    /**
     * 根据指定重构选项配置建材槽，由服务端处理玩家的选项变更时调用。
     */
    public void configureMaterialSlot(int optionIndex) {
        if (this.level == null || this.level.isClientSide()) return;
        if (this.celestialBodyData == null) return;
        List<CelestialRefactorOption> options = this.getClientVisibleOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) {
            this.setMaterialFilter(new ItemStack(Items.BARRIER));
            this.setMaterialLimit(0);
        } else {
            CelestialRefactorOption opt = options.get(optionIndex);
            if (opt.needsMaterial()) {
                this.setMaterialFilter(opt.material().copy());
                this.setMaterialLimit(opt.materialCount());
            } else {
                this.setMaterialFilter(new ItemStack(Items.BARRIER));
                this.setMaterialLimit(0);
            }
        }
        this.setChanged();
        this.syncToClient();
    }

    private final CfaSearchController searchController = new CfaSearchController();

    // 电网
    @Nullable
    private PowerGrid grid;

    private final CfaGravityController gravityController = new CfaGravityController();

    public void startSearch() {
        this.searchController.start(this);
    }

    public int getSearchTicksRemaining() {
        return this.searchController.ticksRemaining();
    }

    public boolean isSearching() {
        return this.searchController.isSearching();
    }

    public boolean isSearchFailed() {
        return this.searchController.hasFailed();
    }

    public boolean isPowerInsufficient() {
        return this.searchController.isPowerInsufficient();
    }

    public void serverTick() {
        this.syncRedstoneSignalIfChanged();
        this.searchController.serverTick(this);

        this.gravityController.tick(
            this.level,
            this.worldPosition,
            this.isAmplify,
            this.amplifierPresent,
            this.celestialBodyData,
            this.stellarMass,
            this.getRedstoneSignal()
        );

        // 巨构逻辑由对应处理器负责。
        this.megastructureManager.serverTick(this);

        // 服务端超新星闪光计时。
        var accel = this.megastructureManager.getAcceleratorHandler();
        if (accel.getSupernovaFlashTicks() > 0) {
            accel.setSupernovaFlashTicks(accel.getSupernovaFlashTicks() - 1);
        }
    }

    /** 强制移除当前重力源，供结构拆除和方块实体卸载时立即清理缓存。 */
    public void handleEntityContact(Entity entity) {
        this.gravityController.handleEntityContact(this.level, this.celestialBodyData, entity);
    }

    public void removeGravitySource() {
        this.gravityController.remove(this.level, this.worldPosition);
    }

    private final CelestialSearchHistory searchHistory = new CelestialSearchHistory();

    @Getter
    private final SimpleContainer anvilInventory = new SimpleContainer(5) {
        @Override
        public void setChanged() {
            super.setChanged();
            CelestialForgingAnvilBlockEntity.this.setChanged();
        }
    };

    public void tick() {
        if (this.rotation >= 360) this.rotation -= 360;
        this.preRotation = this.rotation;
        // 红石信号越大星环越大 → 转速越慢
        float rotationSpeed = 3.0f / (1.0f + this.getRedstoneSignal() * 0.4f);
        this.rotation += rotationSpeed;
        this.bodyRotation += 1;

        // 客户端天体出现/消失动画计时。
        if (this.animationTicks > 0) {
            this.animationTicks--;
            if (this.animationTicks == 0 && !this.animationForward) {
                this.animationPreviousBodyData = null;
            }
        }
        // 客户端超新星闪光倒计时。
        var accel = this.megastructureManager.getAcceleratorHandler();
        if (accel.getSupernovaFlashTicks() > 0) {
            accel.setSupernovaFlashTicks(accel.getSupernovaFlashTicks() - 1);
        }
        // 坍缩动画在加速器阶段 3 由服务端每刻同步，客户端不能自行递减以免失步；
        // 离开阶段 3 后客户端独立递减，作为同步中断时的回退。
        if (accel.getCollapseAnimTicks() > 0 && accel.getStage() != 3) {
            accel.setCollapseAnimTicks(accel.getCollapseAnimTicks() - 1);
        }
    }

    public void setAmplify(boolean amplify) {
        if (this.isAmplify != amplify) {
            this.isAmplify = amplify;
            if (this.level != null && !this.level.isClientSide()) {
                if (this.celestialBodyData instanceof StarData) {
                    if (!amplify) {
                        this.locked = true; // Lock when amplifier removed with stellar body
                    }
                }
            }
            this.setChanged();
            if (this.level != null) {
                this.syncToClient();
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null && !this.level.isClientSide() && !PowerGrid.isServerClosing) {
            this.gravityController.remove(this.level, this.worldPosition);
            // 注销虫洞并清理巨构，使连接传送门及时关闭。
            // 服务器关闭期间跳过，避免保存过程中访问持久化数据。
            this.megastructureManager.clearAllMegastructures(this);
        }
    }

    /**
     * 根据 bodySeed 生成可复现的 ±5% 偏移，仅用于界面显示年龄、半径和质量。
     *
     * @param index 0 表示年龄，1 表示半径，2 表示质量
     * @return 范围为 [-0.05, +0.05] 的偏移比例
     */
    public float getDisplayOffset(int index) {
        if (this.bodySeed == 0) return 0f;
        RandomSource rand = RandomSource.create(this.bodySeed + index * 7919L);
        return (rand.nextFloat() - 0.5f) * 0.1f;
    }

    // === 磁盘数据复制 ===

    @Override
    public void storeDiskData(ValueOutput output) {
        if (this.celestialBodyData != null) {
            output.store("celestialBody", CompoundTag.CODEC, this.celestialBodyData.toTag());
            output.putLong("bodySeed", this.bodySeed);
            output.putInt("ageAnvilCount", this.ageAnvilCount);
            output.putInt("stellarMass", this.stellarMass);
            output.putIntArray(
                "anvilCounts", new int[]{
                    this.getAnvilCount(0),
                    this.getAnvilCount(1),
                    this.getAnvilCount(2),
                    this.getAnvilCount(3)
                }
            );
            output.putBoolean("isAmplify", this.isAmplify);
            if (this.planetaryResourceSet != null) {
                output.store("planetaryResources", CompoundTag.CODEC, this.planetaryResourceSet.toTag());
            }
        }
    }

    @Override
    public void applyDiskData(ValueInput input) {
        // 磁盘数据只能通过种子槽应用，右键仅用于写入快照。
    }

    @Override
    public InteractionResult useDisk(Level level, Player player, InteractionHand hand, ItemStack itemStack, BlockHitResult hitResult) {
        if (!player.getAbilities().mayBuild) return InteractionResult.PASS;
        if (itemStack.is(ModItems.DISK.get())) {
            // 此处只允许写入，不直接应用磁盘数据。
            if (!DiskItem.hasDataStored(itemStack)) {
                // 黑洞和中子星等极端天体必须写入奇点晶体。
                if (this.celestialBodyData instanceof StarData star && star.bodyClass().isExtreme()) {
                    player.sendSystemMessage(
                        Component.translatable("message.anvilcraft.disk.extreme_body_requires_crystal")
                            .withStyle(ChatFormatting.RED)
                    );
                    return InteractionResult.FAIL;
                }
                // 将命中位置重定向到主方块，使 DiskItem.useOn 能找到控制器方块实体。
                BlockHitResult mainHit = new BlockHitResult(
                    hitResult.getLocation(),
                    hitResult.getDirection(),
                    this.getBlockPos(),
                    hitResult.isInside()
                );
                return itemStack.useOn(new UseOnContext(level, player, hand, itemStack, mainHit));
            }
        }
        return InteractionResult.PASS;
    }

    /**
     * 从磁盘或奇点晶体读取天体快照。
     */
    @Nullable
    public static CompoundTag loadSnapshotFromStack(ItemStack stack) {
        return CelestialSnapshotCodec.load(stack);
    }

    /**
     * 将天体快照写入磁盘或奇点晶体。
     */
    public static void saveSnapshotToStack(ItemStack stack, CompoundTag snapshot) {
        CelestialSnapshotCodec.save(stack, snapshot);
    }

    // === 锻星砧方块交互 ===

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            // 重新注册电网，确保锻星砧同时进入生产者和消费者集合。
            PowerGrid.addComponent(this);
            // 若虫洞稳定器仍有效，其处理器会在重新建造回调中恢复网络注册。
            WormholeStabilizerHandler wh = this.megastructureManager.getWormholeHandler();
            if (this.megastructureManager.getActiveIndex() >= 0 && this.getActiveMegastructureOption() != null
                && "wormhole_stabilizer".equals(this.getActiveMegastructureOption().megastructure())) {
                wh.onBuild(this);
            }
            this.setChanged();
            this.syncToClient();
        }
    }

    // === NBT 持久化：26.1 使用 ValueOutput / ValueInput ===

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("amplified", this.isAmplify);
        output.putLong("bodySeed", this.bodySeed);
        output.putInt("stellarMass", this.stellarMass);

        output.putBoolean("locked", this.locked);
        output.putBoolean("amplifierPresent", this.amplifierPresent);
        output.putBoolean("searching", this.isSearching());
        output.putInt("searchTicks", this.getSearchTicksRemaining());
        output.putBoolean("searchFailed", this.isSearchFailed());
        output.putBoolean("powerInsufficient", this.isPowerInsufficient());
        if (this.celestialBodyData != null) {
            output.store("celestialBody", CompoundTag.CODEC, this.celestialBodyData.toTag());
        }
        // 搜索历史
        output.store("searchHistory", CompoundTag.CODEC, this.searchHistory.toTag());
        // 砧子和种子物品栏
        output.store("anvils", CompoundTag.CODEC, CfaInventoryCodec.save(this.anvilInventory));
        // 巨构建材槽
        if (!this.materialFilter.isEmpty()) {
            output.store("materialFilter", ItemStack.OPTIONAL_CODEC, this.materialFilter);
        }
        output.putInt("materialLimit", this.materialLimit);
        output.putInt("ageAnvilCount", this.ageAnvilCount);
        if (this.planetaryResourceSet != null) {
            output.store("planetaryResources", CompoundTag.CODEC, this.planetaryResourceSet.toTag());
        }
        // 传送门由虫洞稳定器处理器经巨构管理器持久化。
        // 神庙状态
        output.putInt("templeCycleDay", this.templeCycleDay);
        output.putLong("templeLastDay", this.templeLastDay);
        if (!this.templeDemandItem.isEmpty()) {
            output.store("templeDemand", ItemStack.OPTIONAL_CODEC, this.templeDemandItem);
        }
        output.putInt("templeDemandCount", this.templeDemandCount);
        output.putInt("templeDemandProgress", this.templeDemandProgress);
        output.putBoolean("templeDemandSatisfied", this.templeDemandSatisfied);
        output.putInt("historyBrowseIndex", this.searchHistory.browseIndex());
        // 巨构专属数据交由巨构管理器保存。
        this.megastructureManager.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.isAmplify = input.getBooleanOr("amplified", false);
        this.stellarMass = input.getIntOr("stellarMass", 0);
        this.locked = input.getBooleanOr("locked", false);
        this.amplifierPresent = input.getBooleanOr("amplifierPresent", false);
        this.searchController.load(
            input.getBooleanOr("searching", false),
            input.getIntOr("searchTicks", 0),
            input.getBooleanOr("searchFailed", false),
            input.getBooleanOr("powerInsufficient", false)
        );
        this.bodySeed = input.getLongOr("bodySeed", 0);
        // 先记录旧天体，用于检测客户端动画状态变化。
        CelestialBodyData oldBodyData = this.celestialBodyData;
        this.celestialBodyData = input.read("celestialBody", CompoundTag.CODEC)
            .map(CelestialBodyData::fromTag).orElse(null);
        // 客户端在区块加载等情况下检测天体切换；恒星演化或超新星闪光期间跳过。
        boolean skipAnimLoad = this.getAcceleratorStage() >= 1 || this.getSupernovaFlashTicks() > 0;
        if (this.level != null && this.level.isClientSide() && !skipAnimLoad) {
            this.detectAnimationTransition(oldBodyData, this.celestialBodyData);
        }
        // 搜索历史
        input.read("searchHistory", CompoundTag.CODEC).ifPresent(this.searchHistory::load);
        // 物品栏
        input.read("anvils", CompoundTag.CODEC)
            .ifPresent(tag -> CfaInventoryCodec.load(tag, this.anvilInventory));
        // 建材过滤器
        this.materialFilter = input.read("materialFilter", ItemStack.OPTIONAL_CODEC)
            .orElse(new ItemStack(Items.BARRIER));
        this.materialLimit = input.getIntOr("materialLimit", 0);
        this.ageAnvilCount = input.getIntOr("ageAnvilCount", 0);
        // 天体资源
        this.planetaryResourceSet = input.read("planetaryResources", CompoundTag.CODEC)
            .map(PlanetaryResourceSet::fromTag).orElse(null);
        // 传送门由虫洞稳定器处理器经巨构管理器恢复。
        // 神庙状态
        this.templeCycleDay = input.getIntOr("templeCycleDay", 0);
        this.templeLastDay = input.getLongOr("templeLastDay", -1);
        this.templeDemandItem = input.read("templeDemand", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.templeDemandCount = input.getIntOr("templeDemandCount", 0);
        this.templeDemandProgress = input.getIntOr("templeDemandProgress", 0);
        this.templeDemandSatisfied = input.getBooleanOr("templeDemandSatisfied", false);
        // 对撞机运行态不持久化，加载后始终从空闲状态开始。
        this.searchHistory.setBrowseIndex(input.getIntOr("historyBrowseIndex", 0));
        // 运行时通过 loadAdditional 同步超新星闪光，磁盘读取不会触发。
        // 仅在传入剩余时间更大时重置，保持客户端倒计时连续。
        input.getInt("supernovaFlashTicks").ifPresent(incomingFlash -> {
            var accel = this.megastructureManager.getAcceleratorHandler();
            if (incomingFlash > accel.getSupernovaFlashTicks()) {
                accel.setSupernovaFlashTicks(incomingFlash);
            }
            this.supernovaCenterY = input.getDoubleOr("supernovaCenterY", 0);
            this.supernovaScale = input.getFloatOr("supernovaScale", 1.0f);
        });
        // 最后读取巨构数据，使处理器能够覆盖控制器中的派生状态。
        this.megastructureManager.loadAdditional(input);
        if (this.level != null && !this.level.isClientSide()) {
            this.syncToClient();
        }
    }

    /**
     * 在客户端检测天体切换并触发对应动画。
     */
    private void detectAnimationTransition(@Nullable CelestialBodyData oldBody, @Nullable CelestialBodyData newBody) {
        if (this.level == null || !this.level.isClientSide()) return;
        boolean hadBody = oldBody != null;
        boolean hasBody = newBody != null;
        if (!hadBody && hasBody) {
            // 天体出现：播放正向放大动画。
            this.animationTicks = CelestialForgingAnvilBlockEntity.ANIMATION_DURATION_TICKS;
            this.animationForward = true;
            this.animationPreviousBodyData = null;
        } else if (hadBody && !hasBody) {
            // 天体消失：播放反向缩小动画。
            this.animationTicks = CelestialForgingAnvilBlockEntity.ANIMATION_DURATION_TICKS;
            this.animationForward = false;
            this.animationPreviousBodyData = oldBody;
        } else if (hadBody && !oldBody.toTag().equals(newBody.toTag())) {
            // 天体类型变化：先缓存旧天体并播放切换动画。
            this.animationTicks = CelestialForgingAnvilBlockEntity.ANIMATION_DURATION_TICKS;
            this.animationForward = true;
            this.animationPreviousBodyData = oldBody;
        }
    }

    // === 网络同步：getUpdateTag 返回 CompoundTag，客户端经 loadAdditional 读取 ===

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("amplified", this.isAmplify);
        tag.putInt("redstoneSignal", this.getRedstoneSignal());
        this.syncedRedstoneSignal = this.cachedRedstoneSignal;
        tag.putLong("bodySeed", this.bodySeed);
        tag.putInt("stellarMass", this.stellarMass);

        tag.putBoolean("locked", this.locked);
        tag.putBoolean("amplifierPresent", this.amplifierPresent);
        tag.putBoolean("searching", this.isSearching());
        tag.putInt("searchTicks", this.getSearchTicksRemaining());
        tag.putBoolean("searchFailed", this.isSearchFailed());
        tag.putBoolean("powerInsufficient", this.isPowerInsufficient());
        if (this.celestialBodyData != null) {
            tag.put("celestialBody", this.celestialBodyData.toTag());
        }
        // 搜索历史
        tag.put("searchHistory", this.searchHistory.toTag());
        // 同步全部 5 个槽位，供客户端界面显示。
        tag.put("anvils", CfaInventoryCodec.save(this.anvilInventory));
        // 建材过滤器
        if (!this.materialFilter.isEmpty()) {
            tag.put("materialFilter", ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, this.materialFilter).getOrThrow());
        }
        tag.putInt("materialLimit", this.materialLimit);
        tag.putInt("ageAnvilCount", this.ageAnvilCount);
        if (this.planetaryResourceSet != null) {
            tag.put("planetaryResources", this.planetaryResourceSet.toTag());
        }
        // 传送门由虫洞稳定器处理器经巨构管理器同步。
        // 神庙状态
        tag.putInt("templeCycleDay", this.templeCycleDay);
        tag.putLong("templeLastDay", this.templeLastDay);
        if (!this.templeDemandItem.isEmpty()) {
            tag.put("templeDemand", ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, this.templeDemandItem).getOrThrow());
        }
        tag.putInt("templeDemandCount", this.templeDemandCount);
        tag.putInt("templeDemandProgress", this.templeDemandProgress);
        tag.putBoolean("templeDemandSatisfied", this.templeDemandSatisfied);
        // 对撞机运行态暂不写入此同步标签。
        tag.putInt("historyBrowseIndex", this.searchHistory.browseIndex());
        // 超新星闪光渲染状态
        tag.putInt("supernovaFlashTicks", this.getSupernovaFlashTicks());
        tag.putDouble("supernovaCenterY", this.supernovaCenterY);
        tag.putFloat("supernovaScale", this.supernovaScale);
        // 巨构专属同步数据
        this.megastructureManager.writeUpdateTag(tag, registries);
        return tag;
    }

    public int getAnvilCount(int slot) {
        return this.anvilInventory.getItem(slot).getCount();
    }

    public List<CelestialSearchHistory.Entry> getSearchHistory() {
        return this.searchHistory.entries();
    }

    public void clearSearchHistory() {
        this.searchHistory.clear();
    }

    public void addToSearchHistory(CelestialBodyData data, @Nullable PlanetaryResourceSet resources) {
        this.searchHistory.add(data, resources);
    }

    // === 搜索历史浏览，仅服务端修改 ===

    public boolean hasPreviousHistory() {
        return this.searchHistory.hasPrevious();
    }

    public boolean hasNextHistory() {
        return this.searchHistory.hasNext();
    }

    public void browseHistoryPrev() {
        if (this.level == null || this.level.isClientSide()) return;
        CelestialSearchHistory.Entry entry = this.searchHistory.previous(
            this.celestialBodyData, this.planetaryResourceSet
        );
        if (entry != null) this.applyHistoryEntry(entry);
    }

    public void browseHistoryNext() {
        if (this.level == null || this.level.isClientSide()) return;
        CelestialSearchHistory.Entry entry = this.searchHistory.next();
        if (entry != null) this.applyHistoryEntry(entry);
    }

    private void applyHistoryEntry(CelestialSearchHistory.Entry entry) {
        this.celestialBodyData = entry.body();
        this.planetaryResourceSet = entry.resources();
        this.setChanged();
        this.syncToClient();
    }

    public void syncToClient() {
        CfaBlockEntitySync.sendToTracking(this, this.getUpdatePacket());
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("screen.anvilcraft.celestial_forging_anvil");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        if (this.level == null) return null;
        return new CelestialForgingAnvilMenu(ModMenuTypes.CELESTIAL_FORGING_ANVIL.get(), containerId, inventory, this);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // === 巨构 ===

    /**
     * 切换天体锁定状态，由服务端处理玩家点击锁定按钮时调用。
     */
    public void toggleLocked() {
        if (this.level == null || this.level.isClientSide()) return;
        if (this.isAcceleratorActive()) {
            // 恒星演化期间禁止解锁。
            return;
        }
        this.locked = !this.locked;
        if (!this.locked) {
            // 解锁时清除巨构和加速器，恢复为普通束星环。
            this.clearMegastructure();
            this.clearAcceleratorState();
        }
        this.setChanged();
        this.syncToClient();
    }

    private void clearAcceleratorState() {
        this.megastructureManager.getAcceleratorHandler().onClear(this);
    }

    /**
     * 清除当前巨构及其状态，恢复为普通束星环。
     */
    public void clearMegastructure() {
        this.megastructureManager.clearMegastructure(this);
        // 清理仍由控制器持有的建材过滤状态。
        this.materialFilter = new ItemStack(Items.BARRIER);
        this.materialLimit = 0;
        // 重新注册电网，恢复普通消费者类型。
        PowerGrid.addComponent(this);
    }

    /**
     * 获取与客户端显示完全一致的重构选项列表。
     * 已建造普通巨构时，仅继续显示恒星演化加速器。
     */
    public List<CelestialRefactorOption> getClientVisibleOptions() {
        List<CelestialRefactorOption> options = CelestialRefactorRegistry.getOptions(
            this.celestialBodyData,
            this.isAmplify,
            this.planetaryResourceSet
        );
        if (this.megastructureManager.getActiveIndex() >= 0) {
            options = options.stream().filter(opt -> "stellar_evolution_accelerator".equals(opt.megastructure())).toList();
        }
        return options;
    }

    /**
     * 获取当前已建造的巨构选项；没有巨构时返回 {@code null}。
     */
    @Nullable
    public CelestialRefactorOption getActiveMegastructureOption() {
        return this.megastructureManager.getActiveOption(this);
    }

    /**
     * 获取锻星砧各侧已放置的传送门映射。
     */
    public Map<Cube323PartHalf, BlockPos> getPortals() {
        WormholeStabilizerHandler wh = this.megastructureManager.getWormholeHandler();
        return wh.getPortals();
    }

    /**
     * 尝试建造巨构，由服务端处理玩家点击“开始重构”时调用。
     *
     * @param optionIndex 玩家选择的重构选项索引
     */
    public void buildMegastructure(int optionIndex) {
        if (this.level == null || this.level.isClientSide()) return;
        if (this.celestialBodyData == null) return;
        List<CelestialRefactorOption> options = this.getClientVisibleOptions();
        if (optionIndex < 0 || optionIndex >= options.size()) return;

        CelestialRefactorOption option = options.get(optionIndex);

        // 先验证并扣除建材。
        if (option.needsMaterial()) {
            ItemStack contained = this.materialContainer.getItem(0);
            ItemStack required = option.material().copyWithCount(option.materialCount());
            if (!ItemStack.isSameItem(contained, required) || contained.getCount() < required.getCount()) {
                return;
            }
            contained.shrink(required.getCount());
        }

        // 实际建造逻辑交由巨构管理器。
        this.megastructureManager.buildMegastructure(optionIndex, this);

        // 重新注册电网，使巨构改变后的组件类型立即生效。
        PowerGrid.addComponent(this);
        this.setChanged();
        this.syncToClient();
    }

    // === 虫洞内容同步 ===

    /**
     * 玩家在物流接口中插入或取出物品时立即调用。
     * 虫洞稳定器会在同一刻把变更写入权威状态并推送到所有连接的锻星砧。
     */
    public void syncLogisticsOnChange(BlockPos interfacePos, int changedSlot) {
        WormholeStabilizerHandler wh = this.megastructureManager.getWormholeHandler();
        wh.syncLogisticsOnChange(interfacePos, changedSlot, this);
    }

    /**
     * 在锻星砧指定侧注册传送门。
     *
     */
    public void addPortal(Cube323PartHalf side, BlockPos portalPos) {
        WormholeStabilizerHandler wh = this.megastructureManager.getWormholeHandler();
        wh.addPortal(side, portalPos, this);
    }

    /**
     * 注销锻星砧指定侧的传送门。
     */
    public void removePortal(Cube323PartHalf side) {
        WormholeStabilizerHandler wh = this.megastructureManager.getWormholeHandler();
        wh.removePortal(side, this);
    }
}
