package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.entity.WeaponBeamEntity;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TeslaGunItem extends EnergyWeaponItem {
    private static final int SHOT_ENERGY = 4_000_000;
    private static final double COS_15_DEGREES = Math.cos(Math.toRadians(15.0));

    public TeslaGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!this.canStartUsing(player, stack, TeslaGunItem.SHOT_ENERGY)) return InteractionResult.FAIL;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(user instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) return;
        if (player.getCooldowns().isOnCooldown(stack)) return;
        Target target = TeslaGunItem.findTarget(level, player);
        if (target == null) return;
        BlockPos rod = target.rod();
        if (rod != null && !(level.getBlockState(rod).getBlock() instanceof LightningRodBlock)) return;
        if (!this.consumeEnergy(player, stack, TeslaGunItem.SHOT_ENERGY, 160_000_000)) return;
        int quickCharge = stack.getEnchantmentLevel(
            level.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.QUICK_CHARGE));
        player.getCooldowns().addCooldown(stack, 80 - Math.min(60, quickCharge * 5));
        Vec3 start = player.getEyePosition().add(player.getViewVector(1.0F).scale(0.5));
        if (target.entity() != null) {
            TeslaGunItem.strikeChain(serverLevel, player, stack, start, target.entity());
        } else if (rod != null) {
            TeslaGunItem.strikeRod(serverLevel, start, rod);
        }
        level.playSound(
            null,
            player.blockPosition(),
            ModSoundEvents.TESLA_TOWER_STRIKE.get(),
            SoundSource.PLAYERS,
            1.0F,
            1.0F
        );
    }

    private static @Nullable Target findTarget(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        AABB area = new AABB(eye, eye).inflate(16.0);
        LivingEntity living = level.getEntitiesOfClass(LivingEntity.class, area, entity -> {
            if (entity == player || !entity.isAlive()) return false;
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            return to.lengthSqr() <= 256.0 && to.normalize().dot(look) >= TeslaGunItem.COS_15_DEGREES;
        }).stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player))).orElse(null);

        BlockPos rod = BlockPos.betweenClosedStream(area)
            .filter(pos -> level.getBlockState(pos).is(Blocks.LIGHTNING_ROD))
            .filter(pos -> pos.getCenter().subtract(eye).normalize().dot(look) >= TeslaGunItem.COS_15_DEGREES)
            .map(BlockPos::immutable)
            .min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(eye)))
            .orElse(null);
        if (living == null) return rod == null ? null : new Target(null, rod);
        if (rod == null || living.distanceToSqr(player) <= rod.distToCenterSqr(eye)) {
            return new Target(living, null);
        }
        return new Target(null, rod);
    }

    private static void strikeRod(ServerLevel level, Vec3 start, BlockPos rod) {
        BlockState state = level.getBlockState(rod);
        if (!(state.getBlock() instanceof LightningRodBlock lightningRod)) return;
        level.addFreshEntity(WeaponBeamEntity.create(level, start, rod.getCenter(), WeaponBeamEntity.TESLA));
        lightningRod.onLightningStrike(state, level, rod);
    }

    private static void strikeChain(
        ServerLevel level,
        ServerPlayer player,
        ItemStack weapon,
        Vec3 start,
        LivingEntity first
    ) {
        Set<Integer> struck = new HashSet<>();
        LivingEntity target = first;
        for (int jump = 0; jump < 4 && target != null; jump++) {
            struck.add(target.getId());
            Vec3 hitPos = target.getBoundingBox().getCenter();
            level.addFreshEntity(WeaponBeamEntity.create(level, start, hitPos, WeaponBeamEntity.TESLA));
            LivingEntity origin = TeslaGunItem.thunderHit(level, player, weapon, target, 40.0F - jump * 10.0F);
            start = origin.getBoundingBox().getCenter();
            target = level.getEntitiesOfClass(
                LivingEntity.class,
                origin.getBoundingBox().inflate(4.0),
                candidate -> candidate.isAlive()
                             && !(candidate instanceof Player)
                             && !struck.contains(candidate.getId())
            ).stream().min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(origin))).orElse(null);
        }
    }

    private static LivingEntity thunderHit(
        ServerLevel level,
        ServerPlayer player,
        ItemStack weapon,
        LivingEntity target,
        float damage
    ) {
        Vec3 oldPosition = target.position();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return target;
        bolt.setPos(oldPosition);
        bolt.setCause(player);
        bolt.setDamage(0.0F);
        if (!EventHooks.onEntityStruckByLightning(target, bolt)) target.thunderHit(level, bolt);
        LivingEntity result = target;
        if (!target.isAlive() || target.isRemoved()) {
            Optional<LivingEntity> converted = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(oldPosition, oldPosition).inflate(1.5),
                LivingEntity::isAlive
            ).stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(oldPosition)));
            result = converted.orElse(target);
        }
        DamageSource source = level.damageSources().source(DamageTypes.LIGHTNING_BOLT, bolt, player);
        if (result.hurtServer(level, source, damage)) {
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level, result, source, weapon);
        }
        return result;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72_000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    private record Target(@Nullable LivingEntity entity, @Nullable BlockPos rod) {
    }
}
