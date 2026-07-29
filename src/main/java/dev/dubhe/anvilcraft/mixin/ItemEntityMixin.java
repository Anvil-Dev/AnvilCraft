package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.dubhe.anvilcraft.api.event.ItemEntityEvent;
import dev.dubhe.anvilcraft.api.injection.entity.IItemEntityExtension;
import dev.dubhe.anvilcraft.block.entity.ItemCollectorBlockEntity;
import dev.dubhe.anvilcraft.block.storage.MagnetBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.FireReforgingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin extends Entity implements IItemEntityExtension {
    public ItemEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow
    public abstract ItemStack getItem();

    @Shadow
    public abstract void setItem(ItemStack itemStack);

    @Unique
    public int anvilcraft$mergeCooldown = 0;

    @Unique
    public boolean anvilcraft$isAdsorbable = true;

    @Unique
    private @Nullable BlockPos anvilcraft$blockPos;

    @Inject(method = "tick", at = @At("RETURN"))
    private void tickReturn(CallbackInfo ci) {
        BlockPos blockPos = BlockPos.containing(this.position());
        if (!blockPos.equals(this.anvilcraft$blockPos)) {
            NeoForge.EVENT_BUS.post(new ItemEntityEvent.InToBlock(
                this.level(),
                (ItemEntity) (Object) this,
                blockPos,
                this.position(),
                this.getDeltaMovement()
            ));
        }
        this.anvilcraft$blockPos = blockPos;
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void voidResistant(CallbackInfo ci) {
        if (!this.getItem().is(ModItemTags.VOID_RESISTANT) && !this.getItem().has(ModComponents.ETERNAL)) return;
        if (this.getY() < this.level().getMinY() + 5) {
            double dy = (this.level().getMinY() + 4 - this.getY()) * 0.01;
            dy += this.getDeltaMovement().y * -0.1;
            this.addDeltaMovement(new Vec3(0, 0.04 + dy, 0));
        }
    }

    @Unique
    private static final Map<Block, Integer> REPAIR_EFFICIENCY = new HashMap<>();

    static {
        REPAIR_EFFICIENCY.put(Blocks.FIRE, 2);
        REPAIR_EFFICIENCY.put(Blocks.SOUL_FIRE, 5);
        REPAIR_EFFICIENCY.put(Blocks.LAVA, 10);
        REPAIR_EFFICIENCY.put(Blocks.LAVA_CAULDRON, 10);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void fireReforging(CallbackInfo ci) {
        ItemStack item = this.getItem();
        Block block = this.level().getBlockState(this.blockPosition()).getBlock();
        Integer repairAmount = REPAIR_EFFICIENCY.get(block);
        if (repairAmount == null) return;
        FireReforgingUtil.repair(item, repairAmount, this.level(), this.anvilcraft$blockPos);
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void explosionProof(
        ServerLevel level,
        DamageSource source,
        float damage,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (!this.getItem().isEmpty()
            && this.getItem().is(ModItemTags.EXPLOSION_PROOF)
            && source.is(DamageTypeTags.IS_EXPLOSION)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void eternalProof(
        ServerLevel level,
        DamageSource source,
        float damage,
        CallbackInfoReturnable<Boolean> cir
    ) {
        boolean typeMatches = source.is(DamageTypeTags.IS_EXPLOSION)
            || source.is(DamageTypeTags.IS_FIRE)
            || source.is(DamageTypes.CACTUS)
            || source.is(DamageTypes.FELL_OUT_OF_WORLD);
        if (this.getItem().has(ModComponents.ETERNAL) && typeMatches) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getBlockPosBelowThatAffectsMyMovement", at = @At("HEAD"), cancellable = true)
    private void slidingRailProgress(CallbackInfoReturnable<BlockPos> cir) {
        BlockState blockState = this.level().getBlockState(this.getOnPos(0.1F));
        if (blockState.is(ModBlockTags.SLIDING_RAILS)) {
            cir.setReturnValue(this.getOnPos(0.1F));
        }
    }

    // 以下是中子锭运动相关mixin
    @WrapOperation(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/item/ItemEntity;"
                + "move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void replaceTickWhenNeutronium(
        ItemEntity instance,
        MoverType moverType,
        Vec3 vec3,
        Operation<Void> original
    ) {
        ItemStack item = this.getItem();
        if (!item.is(ModItems.NEUTRONIUM_INGOT)) {
            original.call(instance, moverType, vec3);
        } else {
            this.anvilcraft$neutroniumMove(moverType, vec3);
        }
    }

    @Override
    public PushReaction getPistonPushReaction() {
        if (this.getItem().is(ModItems.NEUTRONIUM_INGOT)) return PushReaction.IGNORE;
        return super.getPistonPushReaction();
    }

    @SuppressWarnings({
        "unused",
        "SameParameterValue",
        "deprecation"
    })
    @Unique
    private void anvilcraft$neutroniumMove(MoverType moverType, Vec3 motion) {

        // 代替原版move方法中的collide调用
        AABB box = this.getBoundingBox().expandTowards(motion);
        int x1 = Mth.floor(box.minX - 1.0E-7) - 1;
        int x2 = Mth.floor(box.maxX + 1.0E-7) + 1;
        int y1 = Mth.floor(box.minY - 1.0E-7) - 1;
        int y2 = Mth.floor(box.maxY + 1.0E-7) + 1;
        int z1 = Mth.floor(box.minZ - 1.0E-7) - 1;
        int z2 = Mth.floor(box.maxZ + 1.0E-7) + 1;
        List<VoxelShape> shapes = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                for (int z = z1; z < z2; z++) {
                    pos.set(x, y, z);
                    BlockState blockState = this.level().getBlockState(pos);
                    // 只检测带有特定标签的方块的碰撞
                    if (blockState.is(ModBlockTags.NEUTRONIUM_CANNOT_PASS_THROUGH)) {
                        shapes.add(blockState.getCollisionShape(this.level(), pos).move(x, y, z));
                    }
                }
            }
        }
        Vec3 motion2 = Entity.collideWithShapes(motion, this.getBoundingBox(), shapes);
        if (motion2.lengthSqr() > 1.0E-7) {
            this.setPos(this.getX() + motion2.x, this.getY() + motion2.y, this.getZ() + motion2.z);
        }

        // 处理一些原版move方法中，对ItemEntity有必要的后续操作
        boolean collisionX = !Mth.equal(motion2.x, motion.x);
        boolean collisionZ = !Mth.equal(motion2.z, motion.z);
        this.horizontalCollision = collisionX || collisionZ;

        this.verticalCollision = motion2.y != motion.y;
        this.verticalCollisionBelow = this.verticalCollision && motion.y < 0.0;
        this.minorHorizontalCollision = false;
        this.setOnGroundWithMovement(this.verticalCollisionBelow, motion2);
        BlockPos blockpos = this.getOnPosLegacy();
        BlockState blockState = this.level().getBlockState(blockpos);
        if (this.horizontalCollision) {
            Vec3 vec31 = this.getDeltaMovement();
            this.setDeltaMovement(collisionX ? 0.0 : vec31.x, vec31.y, collisionZ ? 0.0 : vec31.z);
        }
        Block block = blockState.getBlock();
        if (motion2.y != motion.y) {
            block.updateEntityMovementAfterFallOn(this.level(), this);
        }
        if (this.onGround()) {
            block.stepOn(this.level(), blockpos, blockState, this);
        }
        this.applyEffectsFromBlocks();

    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;isMergable()Z"))
    public boolean preventMerge(ItemEntity instance, Operation<Boolean> original) {
        if (!original.call(instance)) return false;
        if (this.anvilcraft$mergeCooldown <= 0) return true;
        this.anvilcraft$mergeCooldown--;
        return false;
    }

    @Override
    public void anvilcraft$setMergeCooldown(int cooldown) {
        this.anvilcraft$mergeCooldown = cooldown;
    }

    @Unique
    public boolean anvilcraft$discarded = false;

    @Unique
    public boolean anvilcraft$shouldPoach = true;

    @Unique
    public void anvilcraft$poach() {
        if (!this.anvilcraft$shouldPoach) return;
        Level level = this.level();
        if (level.isClientSide()) return;
        ItemCollectorBlockEntity.poachItemEntity((ItemEntity) (Object) this);
    }

    @Unique
    private static final Map<String, Double> MATERIAL_MAP = new HashMap<>();
    @Unique
    private static final Map<String, String> SPECIAL_MAP = new HashMap<>();
    @Unique
    private static final List<String> SPECIAL_BLACKLIST = List.of("spawn_egg", "waxed");

    static {
        // 1. 定义材质关键词及其减速 (数值越小越慢)
        MATERIAL_MAP.put("iron", 0.50);
        MATERIAL_MAP.put("magnet", 0.50);
        MATERIAL_MAP.put("steel", 0.75);

        MATERIAL_MAP.put("silver", 0.25);
        MATERIAL_MAP.put("copper", 0.27);
        MATERIAL_MAP.put("gold", 0.28);
        MATERIAL_MAP.put("netherite", 0.30);
        MATERIAL_MAP.put("ember", 0.30);
        MATERIAL_MAP.put("aluminum", 0.30);
        MATERIAL_MAP.put("tungsten", 0.38);
        MATERIAL_MAP.put("zinc", 0.40);
        MATERIAL_MAP.put("brass", 0.42);
        MATERIAL_MAP.put("bronze", 0.45);
        MATERIAL_MAP.put("royal", 0.50);
        MATERIAL_MAP.put("tin", 0.55);
        MATERIAL_MAP.put("lead", 0.65);
        MATERIAL_MAP.put("uranium", 0.80);
        MATERIAL_MAP.put("titanium", 0.88);
        MATERIAL_MAP.put("frost_metal", 0.90);
        MATERIAL_MAP.put("plutonium", 0.99);
        // 在这里继续添加材料...

        // 2. 将不含关键词的物品映射到上述材质
        SPECIAL_MAP.put("lightning_rod", "copper");
        SPECIAL_MAP.put("bucket", "iron");
        SPECIAL_MAP.put("hopper", "iron");
        SPECIAL_MAP.put("shears", "iron");
        SPECIAL_MAP.put("anvil", "iron");
        SPECIAL_MAP.put("minecart", "iron");
        SPECIAL_MAP.put("tripwire_hook", "iron");
        SPECIAL_MAP.put("chain", "iron");
        SPECIAL_MAP.put("chute", "iron");
        SPECIAL_MAP.put("compass", "iron");
        // 在这里继续添加特判...
    }

    @Unique
    private @Nullable String anvilcraft$getMaterialKey(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String black : SPECIAL_BLACKLIST) {
            if (id.contains(black)) return null; // 黑名单检查
        }
        if (SPECIAL_MAP.containsKey(id)) return SPECIAL_MAP.get(id); // 别名/特判检查
        for (String key : MATERIAL_MAP.keySet()) { // 关键词匹配
            if (id.contains(key)) return key;
        }
        return null;
    }

    @Unique
    private boolean anvilcraft$isTouchingMagnet() {
        AABB box = this.getBoundingBox().inflate(0.01);
        return BlockPos.betweenClosedStream(box).anyMatch(p -> {
            BlockState s = this.level().getBlockState(p);
            return s.is(ModBlockTags.MAGNET)
                && !s.getOptionalValue(MagnetBlock.LIT).orElse(false)
                && !s.getCollisionShape(this.level(), p).isEmpty()
                && s.getCollisionShape(this.level(), p).toAabbs().stream().anyMatch(b -> b.move(p).intersects(box));
        });
    }

    @Unique
    private Vec3 anvilcraft$magnetAttraction() {
        Vec3 center = this.getBoundingBox().getCenter();
        AABB area = this.getBoundingBox().inflate(0.5);
        Object[] result = {null, Double.MAX_VALUE};
        BlockPos.betweenClosedStream(area).forEach(pos -> {
            BlockState state = this.level().getBlockState(pos);
            if (!state.is(ModBlockTags.MAGNET)
                || state.getOptionalValue(MagnetBlock.LIT).orElse(false)) {
                return;
            }
            for (AABB box : state.getCollisionShape(this.level(), pos).toAabbs()) {
                AABB wb = box.move(pos);
                Vec3 p = new Vec3(
                    Mth.clamp(center.x, wb.minX, wb.maxX),
                    Mth.clamp(center.y, wb.minY, wb.maxY),
                    Mth.clamp(center.z, wb.minZ, wb.maxZ)
                );
                double dist = center.distanceToSqr(p);
                if (dist < (double) result[1]) {
                    result[1] = dist;
                    result[0] = p;
                }
            }
        });
        return result[0] != null && (double) result[1] > 1.0E-7 ? ((Vec3) result[0]).subtract(center).normalize().scale(
            0.05) : Vec3.ZERO;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$magnetLogic(CallbackInfo ci) {
        if (this.isRemoved()) return;

        BlockPos pos = this.blockPosition();
        BlockState state = this.level().getBlockState(pos);
        ItemStack stack = this.getItem();
        String matKey = this.anvilcraft$getMaterialKey(stack);
        // 不是金属直接跳过
        if (matKey == null) return;
        // 1. 空芯磁铁块转化
        if ("iron".equals(matKey) || "magnet".equals(matKey)) {
            if (!this.level().isClientSide() && state.is(ModBlocks.HOLLOW_MAGNET_BLOCK.get())
                && stack.getItem().getDescriptionId().contains("ingot")
                && !state.getValue(MagnetBlock.LIT)) {

                Block targetBlock;
                if ("iron".equals(matKey)) targetBlock = ModBlocks.FERRITE_CORE_MAGNET_BLOCK.get(); // 铁锭 -> 铁芯磁铁块
                else targetBlock = ModBlocks.MAGNET_BLOCK.get(); // 磁铁锭 -> 磁铁块

                this.level().setBlockAndUpdate(pos, targetBlock.defaultBlockState());
                stack.shrink(1);
                if (stack.isEmpty()) {
                    this.discard();
                    ci.cancel();
                }
                return;
            }
            // 2. 吸铁石就要吸铁
            if (this.anvilcraft$isTouchingMagnet()) {
                this.setDeltaMovement(Vec3.ZERO);
                this.setNoGravity(true);
                this.setOnGround(true);
                return;
            } else {
                if (this.isNoGravity() && !stack.has(ModComponents.ETERNAL)) this.setNoGravity(false);
                if (this.anvilcraft$magnetAttraction().lengthSqr() > 0) {
                    this.addDeltaMovement(this.anvilcraft$magnetAttraction());
                }
            }
        }
        // 3. 涡流减速
        if (state.is(ModBlocks.HOLLOW_MAGNET_BLOCK.get()) && !state.getValue(MagnetBlock.LIT)) {
            Double speedFactor = MATERIAL_MAP.get(matKey);
            if (speedFactor != null) this.setDeltaMovement(this.getDeltaMovement().scale(speedFactor));
        }
    }

    @Override
    public boolean anvilcraft$getDiscarded() {
        return this.anvilcraft$discarded;
    }

    @Override
    public void anvilcraft$setShouldPoach(boolean shouldPoach) {
        this.anvilcraft$shouldPoach = shouldPoach;
    }

    @Override
    public void anvilcraft$setIsAdsorbable(boolean value) {
        this.anvilcraft$isAdsorbable = value;
    }

    @Override
    public boolean anvilcraft$isAdsorbable() {
        return this.anvilcraft$isAdsorbable;
    }
}
