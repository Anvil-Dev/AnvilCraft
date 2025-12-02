package dev.dubhe.anvilcraft.item;

import com.google.common.collect.Lists;
import dev.dubhe.anvilcraft.client.renderer.item.SpectralSlingshotRenderer;
import dev.dubhe.anvilcraft.entity.SpectralProjectileEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;

public class SpectralSlingshotItem extends ProjectileWeaponItem {
    /**
     * Set to {@code true} when the crossbow is 20% charged.
     */
    private boolean startSoundPlayed = false;
    /**
     * Set to {@code true} when the crossbow is 50% charged.
     */
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
        return itemStack -> true;
    }

    // 第一人称的手持动画、装填弹药的额外渲染等特殊代码在SpectralSlingshotRenderer等类中
    @Override
    @OnlyIn(Dist.CLIENT)
    @SuppressWarnings({"removal"})
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(SpectralSlingshotRenderer.SpectralSlingshotExtensions.of(SpectralSlingshotRenderer.getInstance()));
    }

    /**
     * 检查物品是否可以被装载（用于判断箭矢或具有攻击伤害的物品）
     *
     * @param stack 要检查的物品堆堆
     * @return 如果物品是箭矢或具有正攻击伤害属性则返回true，否则返回false
     */
    public static boolean checkLoadable(ItemStack stack) {
        // 装载箭矢的设定取消了：if (stack.is(ItemTags.ARROWS)) return true;
        ItemAttributeModifiers modifiers = stack.getAttributeModifiers();
        double dmg = 0;
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)) {
                if (entry.modifier().operation().equals(AttributeModifier.Operation.ADD_VALUE)) {
                    dmg += entry.modifier().amount();
                }
            }
        }
        return (dmg > 0);
    }

    /**
     * 获取幻灵弹弓的弹药，即从另一只手获取物品
     *
     * @param player 玩家实体
     * @return 弹药的物品堆，注意，这个东西返回的是一个引用！不是copy的值
     */
    private static ItemStack getSlingShotAmmo(Player player) {
        ItemStack stack = player.getMainHandItem();
        ItemStack stack2 = player.getOffhandItem();
        if (stack.is(ModItems.SPECTRAL_SLINGSHOT.asItem()) && checkLoadable(stack2)) return stack2;
        if (stack2.is(ModItems.SPECTRAL_SLINGSHOT.asItem()) && checkLoadable(stack)) return stack;
        return ItemStack.EMPTY;
    }

    public static boolean canTakeOutAmmo(ItemStack stack) {
        return stack.getOrDefault(ModComponents.CAN_TAKE_OUT_AMMO, true);
    }

    public static void setCanTakeOutAmmo(ItemStack stack, boolean can) {
        stack.set(ModComponents.CAN_TAKE_OUT_AMMO, can);
    }

    // 以下的代码大量从原版（neoforge融合后的）的弩物品的代码复制过来的

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        ChargedProjectiles chargedprojectiles = itemstack.get(DataComponents.CHARGED_PROJECTILES);
        if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
            if (!player.isCrouching() && !player.getCooldowns().isOnCooldown(this)) {
                this.performShooting(level, player, hand, itemstack, getShootingPower(chargedprojectiles), 1.0F, null);
                int quickCharge = itemstack.getEnchantmentLevel(
                    player.level()
                        .holderLookup(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.QUICK_CHARGE)
                );
                int cdBuff = Math.min(quickCharge * 5, 20); // 5tick = 0.25s, 20tick = 1s
                player.getCooldowns().addCooldown(this, 40 - cdBuff);
            } else {
                // 这个部分是卸载和替换弹药
                ItemStack stack = chargedprojectiles.getItems().getFirst();
                if (canTakeOutAmmo(itemstack)) player.addItem(stack); // 如果能拿出来，那么拿出来
                itemstack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
                // 装载走正常的使用流程
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
                player.startUsingItem(hand);
            }
            return InteractionResultHolder.consume(itemstack);
        } else if (!SpectralSlingshotItem.getSlingShotAmmo(player).isEmpty()) {
            // 这里改了条件，因为获取装填的弹药的方式与传统弓弩不同
            this.startSoundPlayed = false;
            this.midLoadSoundPlayed = false;
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(itemstack);
        } else {
            return InteractionResultHolder.fail(itemstack);
        }
    }

    @SuppressWarnings("unused")
    private static float getShootingPower(ChargedProjectiles projectile) {
        // 原版的弩是projectile.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;，这里直接固定用箭矢的速度
        return 1.6f;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
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
                crossbowStack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(list));
                // 有无限的话填进去的是拿不出来的物品
                crossbowStack.set(ModComponents.CAN_TAKE_OUT_AMMO, notHasInfinity);
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
            SoundEvents.CROSSBOW_SHOOT,
            shooter.getSoundSource(),
            1.0F,
            f
        );
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
        SpectralProjectileEntity projectile = SpectralProjectileEntity.of(level, shooter, ammo, weapon);
        projectile.setSoundEvent(SoundEvents.CROSSBOW_HIT);
        if (isCrit) {
            projectile.setCritArrow(true);
        }
        return projectile;
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
            // 这一行是原版的弩被neoforge切入的钩子，是说弩的射击可以被事件取消。不过，它并不是弩。
            // if (
            //     shooter instanceof Player player
            //     && net.neoforged.neoforge.event.EventHooks.onArrowLoose(weapon, shooter.level(), player, 1, true) < 0
            // ) return;
            // 因为不会消耗装填物，所以原版的weapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);改成了如下
            ChargedProjectiles chargedprojectiles = weapon.get(DataComponents.CHARGED_PROJECTILES);
            if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
                // 这里的替换是因为爆掉的时候要返还弹药
                // 注意这里写的是isCrit = false，不会暴击
                this.spectralShoot(serverlevel, shooter, hand, weapon, chargedprojectiles.getItems(), velocity, inaccuracy, false, target);
                // 触发器和进度相关的删掉了——因为它并不是弩。
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

    /**
     * Called as the item is being used by an entity.
     */
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int count) {
        // 这个应该只用来播放音效了，所以应该不用改
        if (!level.isClientSide) {
            CrossbowItem.ChargingSounds sounds = this.getChargingSounds(stack);
            float f = (float) (stack.getUseDuration(livingEntity) - count) / (float) getChargeDuration(stack, livingEntity);
            if (f < 0.2F) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
            }

            if (f >= 0.2F && !this.startSoundPlayed) {
                this.startSoundPlayed = true;
                sounds.start().ifPresent(
                    p -> level.playSound(
                        null,
                        livingEntity.getX(),
                        livingEntity.getY(),
                        livingEntity.getZ(),
                        p.value(),
                        SoundSource.PLAYERS,
                        0.5F,
                        1.0F
                    )
                );
            }

            if (f >= 0.5F && !this.midLoadSoundPlayed) {
                this.midLoadSoundPlayed = true;
                sounds.mid().ifPresent(
                    soundEventHolder -> level.playSound(
                        null,
                        livingEntity.getX(),
                        livingEntity.getY(),
                        livingEntity.getZ(),
                        soundEventHolder.value(),
                        SoundSource.PLAYERS,
                        0.5F,
                        1.0F
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

    /**
     * Returns the action that specifies what animation to play when the item is being used.
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
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
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ChargedProjectiles chargedprojectiles = stack.get(DataComponents.CHARGED_PROJECTILES);
        if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
            ItemStack itemstack = chargedprojectiles.getItems().getFirst();
            tooltipComponents.add(
                Component.translatable("item.minecraft.crossbow.projectile")
                    .append(CommonComponents.SPACE)
                    .append(itemstack.getDisplayName())
            );
            if (tooltipFlag.isAdvanced() && itemstack.is(Items.FIREWORK_ROCKET)) {
                List<Component> list = Lists.newArrayList();
                Items.FIREWORK_ROCKET.appendHoverText(itemstack, context, list, tooltipFlag);
                if (!list.isEmpty()) {
                    list.replaceAll(sibling -> Component.literal("  ").append(sibling).withStyle(ChatFormatting.GRAY));
                    tooltipComponents.addAll(list);
                }
            }
        }
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
            Level var5 = shooter.level();
            int var10000;
            if (var5 instanceof ServerLevel serverlevel) {
                // 这是原版的东西，是能让多重射击正常生效的东西
                var10000 = EnchantmentHelper.processProjectileCount(serverlevel, weapon, shooter, 1);
            } else {
                var10000 = 1;
            }

            int i = var10000;
            List<ItemStack> list = new ArrayList<>(i);
            ItemStack itemStack1 = ammo.copy();

            for (int j = 0; j < i; ++j) {
                ItemStack itemstack = SpectralSlingshotItem.useSpectralAmmo(weapon, j == 0 ? ammo : itemStack1, shooter, j > 0);
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

    // 从shoot()改的
    protected void spectralShoot(
        ServerLevel level,
        LivingEntity shooter,
        InteractionHand hand,
        ItemStack weapon,
        List<ItemStack> projectileItems,
        float velocity,
        float inaccuracy,
        boolean isCrit,
        @Nullable LivingEntity target
    ) {
        float f = EnchantmentHelper.processProjectileSpread(level, weapon, shooter, 0.0F);
        float f1 = projectileItems.size() == 1 ? 0.0F : 2.0F * f / (float) (projectileItems.size() - 1);
        float f2 = (float) ((projectileItems.size() - 1) % 2) * f1 / 2.0F;
        float f3 = 1.0F;

        for (int i = 0; i < projectileItems.size(); ++i) {
            ItemStack itemstack = projectileItems.get(i);
            if (!itemstack.isEmpty()) {
                float f4 = f2 + f3 * (float) ((i + 1) / 2) * f1;
                f3 = -f3;
                Projectile projectile = this.createProjectile(level, shooter, weapon, itemstack, isCrit);
                this.shootProjectile(shooter, projectile, i, velocity, inaccuracy, f4, target);
                level.addFreshEntity(projectile);
                // 插入的代码，预存一下里面的东西
                ChargedProjectiles chargedprojectiles = weapon.get(DataComponents.CHARGED_PROJECTILES);
                boolean canTakeOut = canTakeOutAmmo(weapon);
                ItemStack itemStack1 = ItemStack.EMPTY;
                if (canTakeOut && chargedprojectiles != null) itemStack1 = chargedprojectiles.getItems().getFirst().copy();
                // 原版的hurtAndBreak()
                weapon.hurtAndBreak(this.getDurabilityUse(itemstack), shooter, LivingEntity.getSlotForHand(hand));
                // 如果武器破坏，且里面有东西，那么吐出来
                if (weapon.isEmpty() && !itemStack1.isEmpty()) {
                    if (shooter instanceof Player player) {
                        player.addItem(itemStack1);
                    } else {
                        new ItemEntity(level, shooter.getX(), shooter.getY(), shooter.getZ(), itemStack1);
                    }
                }
                if (weapon.isEmpty()) {
                    break;
                }
            }
        }

    }
}
