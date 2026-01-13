package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.injection.entity.IFallingBlockEntityExtension;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.AccelerateManager;
import dev.dubhe.anvilcraft.util.GravityManager;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Predicate;

@Mixin(FallingBlockEntity.class)
abstract class FallingBlockEntityMixin extends Entity implements IFallingBlockEntityExtension {
    @Unique
    private static final float DAMAGE_FACTOR = 40 / 1.7444f;

    @Shadow
    public BlockState blockState;

    @Shadow
    public boolean cancelDrop;
    @Shadow
    public boolean dropItem;
    @Shadow
    private float fallDamagePerDistance;
    @Shadow
    private int fallDamageMax;
    @Unique
    private float anvilcraft$fallDistance;

    public FallingBlockEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    // 重定义下落方块的下方
    @WrapOperation(
        method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;below()Lnet/minecraft/core/BlockPos;")
    )
    private BlockPos anvilcraft$redirectBelowInTick(BlockPos instance, Operation<BlockPos> original) {
        Vec3 netGravityVector = GravityManager.getNetGravityVectorForFallingBlock(this);
        // 如果重力向下且没有显著水平分量，使用原版逻辑
        if (netGravityVector.y < -0.01 && Math.abs(netGravityVector.x) < 0.01 && Math.abs(netGravityVector.z) < 0.01) {
            return original.call(instance);
        }
        // 卡在方块里则当前坐标是下方
        if (!FallingBlock.isFree(this.level().getBlockState(instance))) {
            return instance;
        }
        // 总重力向量的方向是下落方块的下方
        return instance.relative(Direction.getNearest(netGravityVector.x, netGravityVector.y, netGravityVector.z));
    }

    /**
     * 拦截原版的 onGround() 检查，接管方块是否应该变成实体的逻辑。
     * 主逻辑 ↓
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @WrapOperation(
        method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;onGround()Z")
    )
    private boolean anvilcraft$overrideOnGround(FallingBlockEntity instance, Operation<Boolean> original) {
        Vec3 gravityVec = GravityManager.getNetGravityVectorForFallingBlock(instance);
        Direction gravityDir = Direction.getNearest(gravityVec.x, gravityVec.y, gravityVec.z);
        Level level = instance.level();
        BlockPos pos = BlockPos.containing(instance.position());
        BlockPos supportPos = pos.relative(gravityDir);
        BlockState supportState = level.getBlockState(supportPos);

        // 0. 平衡环境
        if (gravityVec.lengthSqr() < 1.0E-5) return false;

        // 1. 碰撞检测
        if (!this.anvilcraft$checkCollision(instance, gravityDir, original)) return false;
        // 没撞到就用很短的AABB扫描硬实体
        AABB checkArea = instance.getBoundingBox().expandTowards(Vec3.atLowerCornerOf(gravityDir.getNormal()).scale(0.025)).inflate(1.0E-7);
        if (!level.getEntities(instance, checkArea, e -> e.canBeCollidedWith() && !e.isSpectator()).isEmpty()) {
            // 如果有实体就判断离地距离
            if (!FallingBlock.isFree(level.getBlockState(gravityDir != Direction.DOWN ? supportPos.relative(gravityDir) : supportPos))) {
                // 重力朝下如果1格内就是地面就着陆否则碎裂，重力不朝下延伸一格
                if (gravityDir == Direction.DOWN) return true;
                else {
                    instance.dropItem = false;
                    level.setBlockAndUpdate(supportPos, instance.getBlockState());
                    AnvilEvent.OnLand event = new AnvilEvent.OnLand(this.level(), supportPos, instance, this.anvilcraft$fallDistance);
                    NeoForge.EVENT_BUS.post(event);
                    instance.discard();
                }
            } else this.anvilcraft$breakEntity(instance);
        }

        // 2. 摩擦力与滑行检查，被摩擦力抓住且速度慢 -> 着陆，速度快或没被摩擦力抓住且有路可走 -> 滑行
        float friction = supportState.isAir() ? 0.6F : supportState.getFriction(level, supportPos, instance);
        boolean isMovingSlowly = instance.getDeltaMovement().lengthSqr() < 0.04;
        boolean heldByFriction = isMovingSlowly && this.anvilcraft$isHeldByFriction(gravityVec, gravityDir, friction);
        if (!heldByFriction && this.anvilcraft$hasSlidingPath(level, pos, gravityVec, gravityDir)) return false;

        // 3. 稳定性预判
        if (!this.anvilcraft$predictStability(instance, pos)) return false;

        // 4. 落地检查
        if (!supportState.isFaceSturdy(level, supportPos, gravityDir.getOpposite())) {
            // 如果面不完整，检查碰撞箱
            VoxelShape shape = supportState.getCollisionShape(level, supportPos);
            boolean isFullHeight;
            // 根据重力方向判断检查最大值还是最小值
            if (gravityDir.getAxisDirection() == Direction.AxisDirection.NEGATIVE) isFullHeight = shape.max(gravityDir.getAxis()) == 1;
            else isFullHeight = shape.min(gravityDir.getAxis()) == 0;
            if (!isFullHeight) {
                this.anvilcraft$breakEntity(instance);
                return false;
            }
        }
        return true;
    }

    /**
     * 辅助方法：检查某方向是否发生了碰撞
     */
    @Unique
    private boolean anvilcraft$checkCollision(FallingBlockEntity entity, Direction gravityDir, Operation<Boolean> original) {
        if (gravityDir == Direction.DOWN) {
            return original.call(entity);
        } else if (gravityDir == Direction.UP) {
            return entity.verticalCollision && !original.call(entity);
        } else {
            return entity.horizontalCollision;
        }
    }

    /**
     * 辅助方法：计算切向力与摩擦力，判断是否能稳住
     */
    @Unique
    private boolean anvilcraft$isHeldByFriction(Vec3 gravity, Direction gravityDir, float friction) {
        double totalGravitySq = gravity.lengthSqr();
        double normalForce = Math.abs(gravity.get(gravityDir.getAxis()));
        // 切向力 = sqrt(总力^2 - 法向力^2)
        double tangentialForce = Math.sqrt(Math.max(0, totalGravitySq - normalForce * normalForce));
        double grip = 1.0 - friction;

        // 判定阈值：切向力 < 最大静摩擦力
        return tangentialForce < normalForce * grip * 2.0;
    }

    /**
     * 辅助方法：检查三个轴向上是否存在可以滑行的空位
     */
    @Unique
    private boolean anvilcraft$hasSlidingPath(Level level, BlockPos currentPos, Vec3 gravity, Direction primaryDir) {
        if (this.anvilcraft$checkAxisSlide(level, currentPos, gravity.x, Direction.EAST, Direction.WEST, primaryDir)) return true;
        if (this.anvilcraft$checkAxisSlide(level, currentPos, gravity.y, Direction.UP, Direction.DOWN, primaryDir)) return true;
        return this.anvilcraft$checkAxisSlide(level, currentPos, gravity.z, Direction.SOUTH, Direction.NORTH, primaryDir);
    }

    /**
     * 单轴滑行检查
     */
    @Unique
    private boolean anvilcraft$checkAxisSlide(
        Level level, BlockPos pos, double component, Direction posDir, Direction negDir, Direction forbiddenDir
    ) {
        if (Math.abs(component) <= 1.0E-5) return false;
        Direction targetDir = component > 0 ? posDir : negDir;
        if (targetDir == forbiddenDir) return false; // 不能向地板滑行
        return FallingBlock.isFree(level.getBlockState(pos.relative(targetDir)));
    }

    /**
     * 辅助方法：预测变成方块后的稳定性
     *
     * @return true 表示稳定（可以着陆），false 表示不稳定（应该保持实体）
     */
    @Unique
    private boolean anvilcraft$predictStability(FallingBlockEntity instance, BlockPos pos) {
        // 如果在方块里直接稳定
        if (!FallingBlock.isFree(instance.level().getBlockState(pos))) return true;

        Vec3 blockGravity = GravityManager.getNetGravityVectorForFallingBlock(
            instance.level(), Vec3.atCenterOf(pos), GravityManager.getGravityType(instance)
        );
        // 如果方块位置无重力，认为是稳定的
        if (blockGravity.lengthSqr() <= 1.0E-5) return true;

        Direction dir = Direction.getNearest(blockGravity.x, blockGravity.y, blockGravity.z);
        BlockPos targetPos = pos.relative(dir);
        BlockState targetState = instance.level().getBlockState(targetPos);

        // 主方向是空的 -> 不稳定
        if (FallingBlock.isFree(targetState)) return false;

        // 主方向有方块，检查摩擦力
        float friction = targetState.getFriction(instance.level(), targetPos, null);

        // 被摩擦力抓住则稳定，没被抓住则不稳定
        boolean heldByFriction = this.anvilcraft$isHeldByFriction(blockGravity, dir, friction);
        return heldByFriction || !this.anvilcraft$hasSlidingPath(instance.level(), pos, blockGravity, dir);
    }

    /**
     * 辅助方法：碎裂掉落逻辑
     */
    @Unique
    private void anvilcraft$breakEntity(FallingBlockEntity instance) {
        if (this.dropItem && instance.level().getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            instance.spawnAtLocation(instance.getBlockState().getBlock());
        }
        instance.discard();
    }

    @Inject(
        method = "tick", at = @At(
        value = "INVOKE",
        ordinal = 0,
        target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;level()Lnet/minecraft/world/level/Level;"
    )
    )
    private void anvilPerFallOnGround(CallbackInfo ci) {
        if (this.level().isClientSide()) return;
        if (this.onGround()) return;
        this.anvilcraft$fallDistance = this.fallDistance;
    }

    @Override
    public float anvilcraft$getFallDistance() {
        return this.anvilcraft$fallDistance;
    }

    @SuppressWarnings("UnreachableCode")
    @Inject(
        method = "tick", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/block/Fallable;"
                 + "onLand("
                 + "Lnet/minecraft/world/level/Level;"
                 + "Lnet/minecraft/core/BlockPos;"
                 + "Lnet/minecraft/world/level/block/state/BlockState;"
                 + "Lnet/minecraft/world/level/block/state/BlockState;"
                 + "Lnet/minecraft/world/entity/item/FallingBlockEntity;"
                 + ")V"
    )
    )
    private void anvilFallOnGround(CallbackInfo ci, @Local BlockPos blockPos) {
        if (this.level().isClientSide()) return;
        if (!this.blockState.is(BlockTags.ANVIL)) return;
        FallingBlockEntity entity = Util.cast(this);
        AnvilEvent.OnLand event = new AnvilEvent.OnLand(this.level(), blockPos, entity, this.anvilcraft$fallDistance);
        NeoForge.EVENT_BUS.post(event);
        if (event.isAnvilDamage()) {
            BlockState state = this.blockState.is(ModBlocks.ROYAL_ANVIL.get()) ? this.blockState : AnvilBlock.damage(this.blockState);
            if (state != null) {
                this.level().setBlockAndUpdate(blockPos, state);
            } else {
                this.level().setBlockAndUpdate(blockPos, Blocks.AIR.defaultBlockState());
                if (!this.isSilent()) this.level().levelEvent(1029, this.getOnPos(), 0);
                this.cancelDrop = true;
            }
        }
    }

    @SuppressWarnings("UnreachableCode")
    @Inject(
        method = "causeFallDamage", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;"
                 + "getEntities("
                 + "Lnet/minecraft/world/entity/Entity;"
                 + "Lnet/minecraft/world/phys/AABB;"
                 + "Ljava/util/function/Predicate;"
                 + ")Ljava/util/List;"
    )
    )
    private void anvilHurtEntity(float fallDistance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Level level = this.level();
        FallingBlockEntity fallingBlockEntity = Util.cast(this);
        Predicate<Entity> predicate = EntitySelector.NO_CREATIVE_OR_SPECTATOR.and(EntitySelector.LIVING_ENTITY_STILL_ALIVE);
        int i = Mth.ceil(this.fallDistance - 1.0F);
        float f = (float) Math.min(Mth.floor((float) i * this.fallDamagePerDistance), this.fallDamageMax);
        if (fallingBlockEntity.getBlockState().is(BlockTags.ANVIL)) {
            List<Entity> entities = level.getEntities(this, this.getBoundingBox(), predicate);
            for (Entity entity : entities) {
                NeoForge.EVENT_BUS.post(new AnvilEvent.HurtEntity(fallingBlockEntity, this.getOnPos(), level, entity, f));
            }
        }
    }

    @Inject(
        method = "tick", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V",
        ordinal = 1
    )
    )
    private void hurtEntity(CallbackInfo ci) {
        if (this.getDeltaMovement().multiply(1, 0, 1).length() < 0.75 && this.getDeltaMovement().y < 2.5) {
            return;
        }
        if (!this.blockState.is(BlockTags.ANVIL)) return;
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
            this.level(),
            this,
            this.position()
                .subtract(0, 0.5, 0)
                .subtract(this.anvilcraft$isDeflected() ? this.anvilcraft$getFixedDeltaMovement() : this.getDeltaMovement()),
            this.position().subtract(0, 0.5, 0),
            this.getBoundingBox().expandTowards((
                this.anvilcraft$isDeflected() ? this.anvilcraft$getFixedDeltaMovement() : this.getDeltaMovement()
            ).multiply(-1, -1, -1)).inflate(1.0),
            Entity::isAttackable
        );
        if (hitResult == null) return;
        if (hitResult.getType() != EntityHitResult.Type.ENTITY) return;
        float hurtAmount = (float) (this.getDeltaMovement().length() * DAMAGE_FACTOR);
        hitResult.getEntity().hurt(damageSources().anvil(this), hurtAmount);
    }

    @Inject(
        method = "tick", at = @At("TAIL")
    )
    private void anvilcraft$ApplyFallingBlockGravity(CallbackInfo ci) {
        // 如果是无重力实体则返回
        if (this.isNoGravity()) return;
        // 应用引力向量的水平分量
        Vec3 gravityVector = GravityManager.getGravityVector(this);
        this.setDeltaMovement(this.getDeltaMovement().add(gravityVector.x, 0, gravityVector.z));
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void anvilcraft$handleAcceleration(CallbackInfo ci) {
        AccelerateManager.handleAcceleration(this);
    }
}
