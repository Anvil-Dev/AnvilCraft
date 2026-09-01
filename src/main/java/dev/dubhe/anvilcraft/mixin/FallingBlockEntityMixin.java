package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.injection.entity.IFallingBlockEntityExtension;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.AccelerateManager;
import dev.dubhe.anvilcraft.util.AirResistanceManager;
import dev.dubhe.anvilcraft.util.GravityManager;
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
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;

@Mixin(FallingBlockEntity.class)
abstract class FallingBlockEntityMixin extends Entity implements IFallingBlockEntityExtension {
    @Unique
    private static final float DAMAGE_FACTOR = 40 / 1.7444f;
    @Unique
    private static final EntityTypeTest<Entity, Entity> ANVILCRAFT_ENTITY_TEST = EntityTypeTest.forClass(Entity.class);

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
    @Unique
    private float anvilcraft$directionalFallDistance;
    @Unique
    private Vec3 anvilcraft$positionBeforeTick;
    @Unique
    private @Nullable Vec3 anvilcraft$cachedGravityPosition;
    @Unique
    private @Nullable Vec3 anvilcraft$cachedNetGravity;
    @Unique
    private final List<Entity> anvilcraft$entityCollisionResults = new ArrayList<>(1);

    public FallingBlockEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;blockPosition()Lnet/minecraft/core/BlockPos;",
            ordinal = 0
        )
    )
    private BlockPos anvilcraft$useGravityFaceAsLandingPosition(
        FallingBlockEntity instance,
        Operation<BlockPos> original
    ) {
        Vec3 gravity = this.anvilcraft$getNetGravityVector(instance);
        Direction direction = Direction.getNearest(gravity.x, gravity.y, gravity.z);
        if (direction == Direction.DOWN) return original.call(instance);
        return anvilcraft$getGravityFaceBlockPos(instance, direction);
    }

    // 重定义下落方块的下方
    @WrapOperation(
        method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;below()Lnet/minecraft/core/BlockPos;")
    )
    private BlockPos anvilcraft$redirectBelowInTick(BlockPos instance, Operation<BlockPos> original) {
        Vec3 netGravityVector = this.anvilcraft$getNetGravityVector(this);
        Direction gravityDirection = Direction.getNearest(netGravityVector.x, netGravityVector.y, netGravityVector.z);
        if (gravityDirection == Direction.DOWN) return original.call(instance);

        // 卡在方块里则当前坐标是下方
        if (!FallingBlock.isFree(this.level().getBlockState(instance))) {
            return instance;
        }
        return instance.relative(gravityDirection);
    }

    /**
     * 拦截原版的 onGround() 检查，接管实体是否应该变成方块的逻辑。
     * 主逻辑 ↓
     */
    @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
    @WrapOperation(
        method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;onGround()Z")
    )
    private boolean anvilcraft$overrideOnGround(FallingBlockEntity instance, Operation<Boolean> original) {
        Vec3 gravityVec = this.anvilcraft$getNetGravityVector(instance);

        if (
            this.anvilcraft$isDeflected()
            || AccelerateManager.isControlledByRing(instance)
        ) {
            return false;
        }

        Direction gravityDir = Direction.getNearest(gravityVec.x, gravityVec.y, gravityVec.z);
        boolean entityCollision = this.anvilcraft$hasEntityCollision(instance, gravityDir);
        if (gravityDir == Direction.DOWN && !entityCollision) return original.call(instance);

        if (gravityDir != Direction.DOWN && anvilcraft$positionBeforeTick != null) {
            anvilcraft$directionalFallDistance += (float) position().distanceTo(anvilcraft$positionBeforeTick);
            anvilcraft$fallDistance = Math.max(anvilcraft$fallDistance, anvilcraft$directionalFallDistance);
        }

        Level level = instance.level();
        BlockPos pos = this.anvilcraft$getGravityFaceBlockPos(instance, gravityDir);
        BlockPos supportPos = pos.relative(gravityDir);
        BlockState supportState = level.getBlockState(supportPos);

        // 1. 碰撞检测
        if (entityCollision) {
            if (!FallingBlock.isFree(supportState)) return true;
            this.anvilcraft$breakEntity(instance);
            return false;
        }
        if (!this.anvilcraft$checkBlockCollision(instance, gravityDir, original)) return false;
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

    @ModifyArgs(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/context/DirectionalPlaceContext;<init>("
                     + "Lnet/minecraft/world/level/Level;"
                     + "Lnet/minecraft/core/BlockPos;"
                     + "Lnet/minecraft/core/Direction;"
                     + "Lnet/minecraft/world/item/ItemStack;"
                     + "Lnet/minecraft/core/Direction;)V"
        )
    )
    private void anvilcraft$useGravityPlacementDirection(Args args) {
        Vec3 gravity = this.anvilcraft$getNetGravityVector(this);
        Direction direction = Direction.getNearest(gravity.x, gravity.y, gravity.z);
        if (direction == Direction.DOWN) return;
        args.set(2, direction);
        args.set(4, direction.getOpposite());
    }

    @ModifyArgs(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;")
    )
    private void anvilcraft$reflectVelocityAlongGravityAxis(Args args) {
        Vec3 gravity = this.anvilcraft$getNetGravityVector(this);
        Direction direction = Direction.getNearest(gravity.x, gravity.y, gravity.z);
        if (direction == Direction.DOWN || direction == Direction.UP) return;
        if (direction.getAxis() == Direction.Axis.X) {
            args.set(0, -0.5);
            args.set(1, 0.7);
        } else {
            args.set(1, 0.7);
            args.set(2, -0.5);
        }
    }

    /**
     * 辅助方法：检查某方向是否发生了碰撞
     */
    @Unique
    private boolean anvilcraft$checkBlockCollision(
        FallingBlockEntity entity,
        Direction gravityDir,
        Operation<Boolean> original
    ) {
        if (gravityDir == Direction.DOWN) {
            return original.call(entity);
        }
        Vec3 normal = Vec3.atLowerCornerOf(gravityDir.getNormal()).scale(0.001);
        return entity.level().getBlockCollisions(entity, entity.getBoundingBox().move(normal)).iterator().hasNext();
    }

    @Unique
    private boolean anvilcraft$hasEntityCollision(FallingBlockEntity entity, Direction gravityDir) {
        AABB box = entity.getBoundingBox();
        double depth = 0.05;
        double inset = 1.0E-5;
        AABB contactArea = switch (gravityDir) {
            case DOWN -> new AABB(
                box.minX + inset, box.minY - depth, box.minZ + inset,
                box.maxX - inset, box.minY + inset, box.maxZ - inset
            );
            case UP -> new AABB(
                box.minX + inset, box.maxY - inset, box.minZ + inset,
                box.maxX - inset, box.maxY + depth, box.maxZ - inset
            );
            case WEST -> new AABB(
                box.minX - depth, box.minY + inset, box.minZ + inset,
                box.minX + inset, box.maxY - inset, box.maxZ - inset
            );
            case EAST -> new AABB(
                box.maxX - inset, box.minY + inset, box.minZ + inset,
                box.maxX + depth, box.maxY - inset, box.maxZ - inset
            );
            case NORTH -> new AABB(
                box.minX + inset, box.minY + inset, box.minZ - depth,
                box.maxX - inset, box.maxY - inset, box.minZ + inset
            );
            case SOUTH -> new AABB(
                box.minX + inset, box.minY + inset, box.maxZ - inset,
                box.maxX - inset, box.maxY - inset, box.maxZ + depth
            );
        };
        anvilcraft$entityCollisionResults.clear();
        entity.level().getEntities(
            ANVILCRAFT_ENTITY_TEST,
            contactArea,
            other -> other != entity
                && other.canBeCollidedWith()
                && !other.isSpectator()
                && entity.canCollideWith(other),
            anvilcraft$entityCollisionResults,
            1
        );
        boolean collision = !anvilcraft$entityCollisionResults.isEmpty();
        anvilcraft$entityCollisionResults.clear();
        return collision;
    }

    @Unique
    private BlockPos anvilcraft$getGravityFaceBlockPos(FallingBlockEntity entity, Direction gravityDir) {
        AABB box = entity.getBoundingBox();
        Vec3 center = box.getCenter();
        Vec3 faceCenter = switch (gravityDir.getAxis()) {
            case X -> new Vec3(
                gravityDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.maxX : box.minX,
                center.y,
                center.z
            );
            case Y -> new Vec3(
                center.x,
                gravityDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.maxY : box.minY,
                center.z
            );
            case Z -> new Vec3(
                center.x,
                center.y,
                gravityDir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.maxZ : box.minZ
            );
        };
        Vec3 inward = Vec3.atLowerCornerOf(gravityDir.getNormal()).scale(-1.0E-4);
        return BlockPos.containing(faceCenter.add(inward));
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
        Vec3 movement = this.getDeltaMovement();
        if (movement.x * movement.x + movement.z * movement.z < 0.75 * 0.75 && movement.y < 2.5) {
            return;
        }
        if (!this.blockState.is(BlockTags.ANVIL)) return;
        boolean deflected = this.anvilcraft$isDeflected();
        Vec3 traceMovement = deflected ? this.anvilcraft$getFixedDeltaMovement() : movement;
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
            this.level(),
            this,
            this.position()
                .subtract(0, 0.5, 0)
                .subtract(traceMovement),
            this.position().subtract(0, 0.5, 0),
            this.getBoundingBox().expandTowards(traceMovement.scale(-1.0)).inflate(1.0),
            Entity::isAttackable
        );
        if (hitResult == null) return;
        if (hitResult.getType() != EntityHitResult.Type.ENTITY) return;
        float hurtAmount = (float) (Math.sqrt(movement.lengthSqr()) * DAMAGE_FACTOR);
        hitResult.getEntity().hurt(damageSources().anvil(this), hurtAmount);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void anvilcraft$applyFallingBlockHorizontalGravity(CallbackInfo ci) {
        if (this.anvilcraft$discardLevitationPowderAboveBuildHeight()) return;
        if (this.isNoGravity()) return;
        Vec3 gravityVector = this.anvilcraft$getNetGravityVector(this);
        if (gravityVector.x == 0 && gravityVector.z == 0) return;
        if (AccelerateManager.isControlledByRing(this)) return;
        this.setDeltaMovement(this.getDeltaMovement().add(gravityVector.x, 0, gravityVector.z));
    }

    @Inject(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/item/FallingBlockEntity;"
                     + "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void anvilcraft$discardLevitationPowderAfterMovement(CallbackInfo ci) {
        if (this.anvilcraft$discardLevitationPowderAboveBuildHeight()) {
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$handleAcceleration(CallbackInfo ci) {
        anvilcraft$cachedGravityPosition = null;
        anvilcraft$cachedNetGravity = null;
        anvilcraft$positionBeforeTick = position();
        if (this.anvilcraft$discardLevitationPowderAboveBuildHeight()) {
            ci.cancel();
            return;
        }
        AccelerateManager.handleAcceleration(this);
    }

    @Unique
    private Vec3 anvilcraft$getNetGravityVector(Entity entity) {
        Vec3 currentPosition = entity.position();
        Vec3 cachedPosition = anvilcraft$cachedGravityPosition;
        Vec3 cachedGravity = anvilcraft$cachedNetGravity;
        if (cachedPosition != null
            && cachedGravity != null
            && cachedPosition.x == currentPosition.x
            && cachedPosition.y == currentPosition.y
            && cachedPosition.z == currentPosition.z) {
            return cachedGravity;
        }
        Vec3 gravity = GravityManager.getNetGravityVectorForFallingBlock(entity);
        anvilcraft$cachedGravityPosition = currentPosition;
        anvilcraft$cachedNetGravity = gravity;
        return gravity;
    }

    @Unique
    private boolean anvilcraft$discardLevitationPowderAboveBuildHeight() {
        if (!this.blockState.is(ModBlocks.LEVITATION_POWDER_BLOCK.get())
            || this.blockPosition().getY() < this.level().getMaxBuildHeight()) {
            return false;
        }
        this.discard();
        return true;
    }

    @ModifyConstant(method = "tick", constant = @Constant(doubleValue = 0.98))
    private double anvilcraft$scaleAirDrag(double vanillaDrag) {
        return AirResistanceManager.drag(this.level(), vanillaDrag);
    }
}
