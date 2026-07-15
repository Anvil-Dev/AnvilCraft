package dev.dubhe.anvilcraft.item.tool;

import dev.dubhe.anvilcraft.entity.SpectralProjectileEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.CanTakeOutAmmo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class SpectralSlingshotItem extends ProjectileWeaponItem {
    /// Set to `true` when the crossbow is 20% charged.
    private boolean startSoundPlayed = false;
    /// Set to `true` when the crossbow is 50% charged.
    private boolean midLoadSoundPlayed = false;

    // 证明自己，比起弹弓，更像弩（指这里的音效从弩抄的）
    private static final CrossbowItem.ChargingSounds DEFAULT_SOUNDS = new CrossbowItem.ChargingSounds(
        Optional.of(SoundEvents.CROSSBOW_LOADING_START),
        Optional.of(SoundEvents.CROSSBOW_LOADING_MIDDLE),
        Optional.of(SoundEvents.CROSSBOW_LOADING_END)
    );

    public SpectralSlingshotItem(Properties properties) {
        super(properties);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return _ -> true;
    }

    /// 检查物品是否可以被装载（用于判断箭矢或具有攻击伤害的物品）
    ///
    /// @param weapon 将要装载的武器物品堆
    /// @param stack 要检查的物品堆
    /// @return 如果物品是箭矢或具有正攻击伤害属性则返回true，否则返回false
    public boolean checkLoadable(ItemStack weapon, ItemStack stack) {
        // 装载箭矢的设定取消了：if (stack.is(ItemTags.ARROWS)) return true;
        if (this.unableToUse(weapon)) return false;
        ItemAttributeModifiers modifiers = stack.getAttributeModifiers();
        double dmg = 0;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
                if (entry.modifier().operation().equals(AttributeModifier.Operation.ADD_VALUE)) {
                    dmg += entry.modifier().amount();
                }
            }
        }
        return dmg > 0;
    }

    /// 获取幻灵弹弓的弹药，即从另一只手获取物品
    ///
    /// @param player 玩家实体
    /// @return 弹药的物品堆
    /// @apiNote 对此方法返回的物品堆的修改会影响原物品
    private static ItemStack getSlingShotAmmo(Player player) {
        ItemStack stack = player.getMainHandItem();
        ItemStack stack2 = player.getOffhandItem();
        if (stack.getItem() instanceof SpectralSlingshotItem item && item.checkLoadable(stack, stack2)) return stack2;
        if (stack2.getItem() instanceof SpectralSlingshotItem item && item.checkLoadable(stack2, stack)) return stack;
        return ItemStack.EMPTY;
    }

    public static boolean canTakeOutAmmo(ItemStack stack) {
        return stack.getOrDefault(ModComponents.CAN_TAKE_OUT_AMMO, CanTakeOutAmmo.CAN).value();
    }

    public static void setCanTakeOutAmmo(ItemStack stack, boolean can) {
        stack.set(ModComponents.CAN_TAKE_OUT_AMMO, new CanTakeOutAmmo(can));
    }

    public boolean unableToUse(ItemStack stack) {
        return false;
    }

    // 以下的代码大量从原版（neoforge融合后的）的弩物品的代码复制过来的

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack inHand = player.getItemInHand(hand);
        ChargedProjectiles projectiles = inHand.get(DataComponents.CHARGED_PROJECTILES);
        if (projectiles != null && !projectiles.isEmpty()) {
            if (!player.isCrouching() && !player.getCooldowns().isOnCooldown(inHand)) {
                if (this.unableToUse(inHand)) return InteractionResult.FAIL;
                this.performShooting(level, player, hand, inHand, SpectralSlingshotItem.getShootingPower(), 1.0F, null);
                int quickCharge = inHand.getEnchantmentLevel(
                    player.level()
                        .holderLookup(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.QUICK_CHARGE)
                );
                int cdBuff = Math.min(quickCharge * 5, 20); // 5tick = 0.25s, 20tick = 1s
                player.getCooldowns().addCooldown(inHand, 40 - cdBuff);
            } else {
                // 这个部分是卸载和替换弹药
                ItemStack stack = projectiles.itemCopies().getFirst();
                if (SpectralSlingshotItem.canTakeOutAmmo(inHand)) player.addItem(stack); // 如果能拿出来，那么拿出来
                inHand.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
                // 装载走正常的使用流程
                if (!SpectralSlingshotItem.getSlingShotAmmo(player).isEmpty()) {
                    this.startSoundPlayed = false;
                    this.midLoadSoundPlayed = false;
                    player.startUsingItem(hand);
                }
            }
            return InteractionResult.CONSUME;
        } else if (!SpectralSlingshotItem.getSlingShotAmmo(player).isEmpty()) {
            // 这里改了条件，因为获取装填的弹药的方式与传统弓弩不同
            this.startSoundPlayed = false;
            this.midLoadSoundPlayed = false;
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.FAIL;
        }
    }

    private static float getShootingPower() {
        // 原版的弩是projectile.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;，这里直接固定用箭矢的速度
        return 3.15F;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        int i = this.getUseDuration(stack, entityLiving) - timeLeft;
        float f = getPowerForTime(i, stack, entityLiving);
        if (f >= 1.0F && !isCharged(stack) && tryLoadProjectiles(entityLiving, stack)) {
            CrossbowItem.ChargingSounds sounds = this.getChargingSounds(stack);
            sounds.end().ifPresent(
                sound -> level.playSound(
                    null,
                    entityLiving.getX(),
                    entityLiving.getY(),
                    entityLiving.getZ(),
                    sound.value(),
                    entityLiving.getSoundSource(),
                    1.0F,
                    1.0F / (level.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F
                )
            );
        }
        int timeHeld = this.getUseDuration(stack, entityLiving) - timeLeft;
        return getPowerForTime(timeHeld, stack, entityLiving) >= 1.0F && isCharged(stack);
    }

    private static boolean tryLoadProjectiles(LivingEntity shooter, ItemStack crossbowStack) {
        if (shooter instanceof Player player) {
            // 因为是从副手获取装填的弹药，所以稍微改改
            ItemStack ammo = SpectralSlingshotItem.getSlingShotAmmo(player);
            if (ammo.isEmpty()) return false;
            // 底下这两行是获取无限附魔
            int infinity = crossbowStack
                .getEnchantmentLevel(
                    player.level()
                        .holderLookup(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.INFINITY)
                );
            boolean notHasInfinity = !(infinity > 0);
            // draw被换了个写法
            List<ItemStack> list = SpectralSlingshotItem.spectralDraw(crossbowStack, ammo, shooter);
            if (!list.isEmpty()) {
                crossbowStack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.ofNonEmpty(list));
                // 有无限的话填进去的是拿不出来的物品
                crossbowStack.set(ModComponents.CAN_TAKE_OUT_AMMO, new CanTakeOutAmmo(notHasInfinity));
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public static boolean isCharged(ItemStack crossbowStack) {
        ChargedProjectiles chargedprojectiles = crossbowStack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        return !chargedprojectiles.isEmpty();
    }

    @Override
    protected void shootProjectile(
        LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy, float angle, @Nullable LivingEntity target
    ) {
        Vector3f vector3f;
        if (target != null) {
            double d0 = target.getX() - shooter.getX();
            double d1 = target.getZ() - shooter.getZ();
            double d2 = Math.sqrt(d0 * d0 + d1 * d1);
            double d3 = target.getY(0.3333333333333333) - projectile.getY() + d2 * 0.2F;
            vector3f = getProjectileShotVector(shooter, new Vec3(d0, d3, d1), angle);
        } else {
            Vec3 vec3 = shooter.getUpVector(1.0F);
            Quaternionf quaternionf = new Quaternionf().setAngleAxis((angle * (float) (Math.PI / 180.0)), vec3.x, vec3.y, vec3.z);
            Vec3 vec31 = shooter.getViewVector(1.0F);
            vector3f = vec31.toVector3f().rotate(quaternionf);
        }

        projectile.shoot(vector3f.x(), vector3f.y(), vector3f.z(), velocity, inaccuracy);
        float f = getShotPitch(shooter.getRandom(), index);
        shooter.level().playSound(
            null,
            shooter.getX(),
            shooter.getY(),
            shooter.getZ(),
            this.getShootSound(),
            shooter.getSoundSource(),
            this.getShootVolume(),
            f * this.getShootPitch()
        );
    }

    protected SoundEvent getShootSound() {
        return SoundEvents.CROSSBOW_SHOOT;
    }

    protected float getShootVolume() {
        return 1.0F;
    }

    protected float getShootPitch() {
        return 1.0F;
    }

    private static Vector3f getProjectileShotVector(LivingEntity shooter, Vec3 distance, float angle) {
        Vector3f vector3f = distance.toVector3f().normalize();
        Vector3f vector3f1 = new Vector3f(vector3f).cross(new Vector3f(0.0F, 1.0F, 0.0F));
        if ((double) vector3f1.lengthSquared() <= 1.0E-7) {
            Vec3 vec3 = shooter.getUpVector(1.0F);
            vector3f1 = new Vector3f(vector3f).cross(vec3.toVector3f());
        }

        Vector3f vector3f2 = new Vector3f(vector3f).rotateAxis((float) (Math.PI / 2), vector3f1.x, vector3f1.y, vector3f1.z);
        return new Vector3f(vector3f).rotateAxis(angle * (float) (Math.PI / 180.0), vector3f2.x, vector3f2.y, vector3f2.z);
    }

    @Override
    protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack ammo, boolean isCrit) {
        // 这里完全另外写了，毕竟固定射出来一个特殊射弹
        SpectralProjectileEntity projectile = SpectralProjectileEntity.of(level, shooter, ammo, weapon, this.getDamageAmplification());
        projectile.setSoundEvent(SoundEvents.CROSSBOW_HIT);
        if (isCrit) {
            projectile.setCritArrow(true);
        }
        return projectile;
    }

    protected double getDamageAmplification() {
        return 0.5;
    }

    public void performShooting(
        Level level,
        LivingEntity shooter,
        InteractionHand hand,
        ItemStack weapon,
        float velocity,
        float inaccuracy,
        @Nullable LivingEntity target
    ) {
        if (level instanceof ServerLevel serverlevel) {
            // 因为不会消耗装填物，所以原版的weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);改成了如下
            ChargedProjectiles chargedProjectiles = weapon.get(DataComponents.CHARGED_PROJECTILES);
            if (chargedProjectiles != null && !chargedProjectiles.isEmpty()) {
                // 注意这里写的是isCrit = false，不会暴击
                this.shoot(serverlevel, shooter, hand, weapon, chargedProjectiles.itemCopies(), velocity, inaccuracy, false, target);
                if (shooter instanceof ServerPlayer serverplayer) {
                    // 触发器删掉了——因为它并不是弩。
                    serverplayer.awardStat(Stats.ITEM_USED.get(weapon.getItem()));
                }
            }
        }
    }

    private static float getShotPitch(RandomSource random, int index) {
        return index == 0 ? 1.0F : getRandomShotPitch((index & 1) == 1, random);
    }

    private static float getRandomShotPitch(boolean isHighPitched, RandomSource random) {
        float f = isHighPitched ? 0.63F : 0.43F;
        return 1.0F / (random.nextFloat() * 0.5F + 1.8F) + f;
    }

    /// Called as the item is being used by an entity.
    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        // 这个应该只用来播放音效了，所以应该不用改
        if (!level.isClientSide()) {
            CrossbowItem.ChargingSounds sounds = this.getChargingSounds(stack);
            float tickPercent = (float) (stack.getUseDuration(entity) - ticksRemaining) / getChargeDuration(stack, entity);
            if (tickPercent < 0.2F) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
            }

            if (tickPercent >= 0.2F && !this.startSoundPlayed) {
                this.startSoundPlayed = true;
                sounds.start().ifPresent(sound -> level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    sound.value(),
                    SoundSource.PLAYERS,
                    0.5F,
                    1.0F
                ));
            }

            if (tickPercent >= 0.5F && !this.midLoadSoundPlayed) {
                this.midLoadSoundPlayed = true;
                sounds.mid().ifPresent(sound -> level.playSound(
                    null,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    sound.value(),
                    SoundSource.PLAYERS,
                    0.5F,
                    1.0F
                ));
            }

            if (tickPercent >= 1.0F && !isCharged(stack) && tryLoadProjectiles(entity, stack)) {
                sounds.end().ifPresent(
                    sound -> level.playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        sound.value(),
                        entity.getSoundSource(),
                        1.0F,
                        1.0F / (level.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F
                    )
                );
            }
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return getChargeDuration(stack, entity) + 3;
    }

    public static int getChargeDuration(ItemStack stack, LivingEntity shooter) {
        float f = EnchantmentHelper.modifyCrossbowChargingTime(stack, shooter, 1.25F);
        return Mth.floor(f * 20.0F);
    }

    /// Returns the action that specifies what animation to play when the item is being used.
    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    CrossbowItem.ChargingSounds getChargingSounds(ItemStack stack) {
        return EnchantmentHelper.pickHighestLevel(stack, EnchantmentEffectComponents.CROSSBOW_CHARGING_SOUNDS).orElse(DEFAULT_SOUNDS);
    }

    private static float getPowerForTime(int timeLeft, ItemStack stack, LivingEntity shooter) {
        float f = (float) timeLeft / (float) getChargeDuration(stack, shooter);
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    public boolean useOnRelease(ItemStack stack) {
        return stack.is(this);
    }

    @Override
    public int getDefaultProjectileRange() {
        return 8;
    }

    // 这个是从原版的draw()改的
    protected static List<ItemStack> spectralDraw(ItemStack weapon, ItemStack ammo, LivingEntity shooter) {
        if (ammo.isEmpty()) {
            return List.of();
        } else {
            int i = shooter.level() instanceof ServerLevel serverlevel
                    ? EnchantmentHelper.processProjectileCount(serverlevel, weapon, shooter, 1)
                    : 1;
            List<ItemStack> list = new ArrayList<>(i);
            ItemStack stack1 = ammo.copy();

            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = SpectralSlingshotItem.useSpectralAmmo(weapon, j == 0 ? ammo : stack1, shooter, j > 0);
                if (!itemstack.isEmpty()) {
                    list.add(itemstack);
                }
            }

            return list;
        }
    }

    // 这个是从原版的useAmmo()改的
    protected static ItemStack useSpectralAmmo(ItemStack weapon, ItemStack ammo, LivingEntity shooter, boolean intangible) {
        int ammoCountToUse;
        Level level = shooter.level();
        if (!intangible && level instanceof ServerLevel serverlevel) {
            Item ammoItem = ammo.getItem();
            if (shooter.hasInfiniteMaterials()
                || (ammoItem instanceof ArrowItem ai && ai.isInfinite(ammo, weapon, shooter))
            ) {
                ammoCountToUse = 0;
            } else {
                ammoCountToUse = EnchantmentHelper.processAmmoUse(serverlevel, weapon, ammo, 1);
            }
        } else {
            ammoCountToUse = 0;
        }

        // 特殊处理：如果有无限附魔，那么不消耗（插入的代码）
        int infinity = weapon.getEnchantmentLevel(shooter.level().holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.INFINITY));
        if (infinity > 0) ammoCountToUse = 0;

        int i = ammoCountToUse;
        if (i > ammo.getCount()) {
            return ItemStack.EMPTY;
        } else {
            ItemStack itemstack;
            if (i == 0) {
                itemstack = ammo.copyWithCount(1);
                itemstack.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            } else {
                itemstack = ammo.split(i);
                if (ammo.isEmpty() && shooter instanceof Player player) {
                    player.getInventory().removeItem(ammo);
                }
            }
            return itemstack;
        }
    }

    @Override
    protected void shoot(
        ServerLevel level,
        LivingEntity shooter,
        InteractionHand hand,
        ItemStack weapon,
        List<ItemStack> projectiles,
        float power,
        float uncertainty,
        boolean isCrit,
        @Nullable LivingEntity targetOverride
    ) {
        float f = EnchantmentHelper.processProjectileSpread(level, weapon, shooter, 0.0F);
        float f1 = projectiles.size() == 1 ? 0.0F : 2.0F * f / (float) (projectiles.size() - 1);
        float f2 = (float) ((projectiles.size() - 1) % 2) * f1 / 2.0F;
        float f3 = 1.0F;

        for (int i = 0; i < projectiles.size(); ++i) {
            ItemStack stack = projectiles.get(i);
            if (!stack.isEmpty()) {
                float f4 = f2 + f3 * (float) ((i + 1) / 2) * f1;
                f3 = -f3;
                Projectile projectile = this.createProjectile(level, shooter, weapon, stack, isCrit);
                this.shootProjectile(shooter, projectile, i, power, uncertainty, f4, targetOverride);
                level.addFreshEntity(projectile);
                // 插入的代码，预存一下里面的东西
                ChargedProjectiles chargedProjectiles = weapon.get(DataComponents.CHARGED_PROJECTILES);
                boolean canTakeOut = canTakeOutAmmo(weapon);
                ItemStack stack1 = ItemStack.EMPTY;
                if (canTakeOut && chargedProjectiles != null) stack1 = chargedProjectiles.itemCopies().getFirst().copy();
                // 原版的hurtAndBreak()
                weapon.hurtAndBreak(this.getDurabilityUse(stack), shooter, hand.asEquipmentSlot());
                // 如果武器破坏，且里面有东西，那么吐出来
                if (weapon.isEmpty() && !stack1.isEmpty()) {
                    if (shooter instanceof Player player) {
                        player.addItem(stack1);
                    } else {
                        new ItemEntity(level, shooter.getX(), shooter.getY(), shooter.getZ(), stack1);
                    }
                }
                if (weapon.isEmpty()) {
                    break;
                }
            }
        }
    }
}
