package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.entity.WeaponBeamEntity;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canStartUsing(player, stack, SHOT_ENERGY)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(user instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) return;
        if (player.getCooldowns().isOnCooldown(this)) return;
        Target target = findTarget(level, player);
        BlockState rodState = level.getBlockState(target.rod());
        if (!(rodState.getBlock() instanceof LightningRodBlock)) return;
        if (!consumeEnergy(player, stack, SHOT_ENERGY, 160_000_000)) return;
        int quickCharge = stack.getEnchantmentLevel(
            level.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.QUICK_CHARGE));
        player.getCooldowns().addCooldown(this, 80 - Math.min(60, quickCharge * 5));
        strikeChain(serverLevel, player, stack, player.getEyePosition().add(player.getViewVector(1.0F).scale(0.5)), target.entity());
        level.playSound(null, player.blockPosition(), ModSoundEvents.TESLA_TOWER_STRIKE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static Target findTarget(Level level, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F).normalize();
        AABB area = new AABB(eye, eye).inflate(16.0);
        LivingEntity living = level.getEntitiesOfClass(LivingEntity.class, area, entity -> {
            if (entity == player || !entity.isAlive()) return false;
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            return to.lengthSqr() <= 256.0 && to.normalize().dot(look) >= COS_15_DEGREES;
        }).stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(player))).orElse(null);

        BlockPos rod = BlockPos.betweenClosedStream(area).filter(pos -> level.getBlockState(pos).is(Blocks.LIGHTNING_ROD))
            .filter(pos -> pos.getCenter().subtract(eye).normalize().dot(look) >= COS_15_DEGREES)
            .map(BlockPos::immutable)
            .min(Comparator.comparingDouble(pos -> pos.distToCenterSqr(eye))).orElse(null);
        if (living == null) return rod == null ? null : new Target(null, rod);
        if (rod == null || living.distanceToSqr(player) <= rod.distToCenterSqr(eye)) return new Target(living, null);
        return new Target(null, rod);
    }

    private static void strikeChain(ServerLevel level, ServerPlayer player, ItemStack weapon, Vec3 start, LivingEntity first) {
        Set<Integer> struck = new HashSet<>();
        LivingEntity target = first;
        for (int jump = 0; jump < 4 && target != null; jump++) {
            struck.add(target.getId());
            Vec3 hitPos = target.getBoundingBox().getCenter();
            level.addFreshEntity(WeaponBeamEntity.create(level, start, hitPos, WeaponBeamEntity.TESLA));
            LivingEntity origin = thunderHit(level, player, weapon, target, 40.0F - jump * 10.0F);
            start = origin.getBoundingBox().getCenter();
            target = level.getEntitiesOfClass(LivingEntity.class, origin.getBoundingBox().inflate(4.0), candidate ->
                candidate.isAlive() && !(candidate instanceof Player) && !struck.contains(candidate.getId())
            ).stream().min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(origin))).orElse(null);
        }
    }

    private static LivingEntity thunderHit(
        ServerLevel level, ServerPlayer player, ItemStack weapon, LivingEntity target, float damage
    ) {
        Vec3 oldPosition = target.position();
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) return target;
        bolt.moveTo(oldPosition);
        bolt.setCause(player);
        bolt.setDamage(0.0F);
        if (!EventHooks.onEntityStruckByLightning(target, bolt)) target.thunderHit(level, bolt);
        LivingEntity result = target;
        if (!target.isAlive() || target.isRemoved()) {
            Optional<LivingEntity> converted = level.getEntitiesOfClass(
                LivingEntity.class, new AABB(oldPosition, oldPosition).inflate(1.5), LivingEntity::isAlive
            ).stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(oldPosition)));
            result = converted.orElse(target);
        }
        DamageSource source = level.damageSources().source(DamageTypes.LIGHTNING_BOLT, bolt, player);
        if (result.hurt(source, damage)) {
            net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffectsWithItemSource(
                level, result, source, weapon);
        }
        return result;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72_000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    private record Target(LivingEntity entity, BlockPos rod) {
    }
}
