package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.entity.WeaponBeamEntity;
import dev.dubhe.anvilcraft.util.WeaponRaycastUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CorruptedBeaconActivatorItem extends EnergyWeaponItem {
    private static final int ENERGY_PER_PULSE = 200_000;

    public CorruptedBeaconActivatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canStartUsing(player, stack, ENERGY_PER_PULSE)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(user instanceof Player player) || !(level instanceof ServerLevel serverLevel)) return;
        WeaponRaycastUtil.Ray ray = WeaponRaycastUtil.ray(player, 64.0);
        Vec3 end = WeaponRaycastUtil.blockEnd(level, player, ray);
        Vec3 visualStart = WeaponRaycastUtil.visualStart(player, WeaponRaycastUtil.MUZZLE_RIGHT_OFFSET);
        int elapsed = getUseDuration(stack, user) - remaining;
        WeaponBeamEntity.showContinuous(
            level, visualStart, end, WeaponBeamEntity.CORRUPTED, 1, player);

        int quickCharge = stack.getEnchantmentLevel(
            level.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.QUICK_CHARGE));
        int period = 20 - Math.min(quickCharge, 10);
        if (elapsed % period != 0 || !consumeEnergy(player, stack, ENERGY_PER_PULSE, 160_000_000)) return;

        int power = stack.getEnchantmentLevel(
            level.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.POWER));
        float damage = 10.0F + power * 2.0F;
        for (LivingEntity target : WeaponRaycastUtil.livingEntities(level, player, ray, Integer.MAX_VALUE)) {
            if (target.hurt(player.damageSources().playerAttack(player), damage)) {
                EnchantmentHelper.doPostAttackEffectsWithItemSource(
                    serverLevel, target, player.damageSources().playerAttack(player), stack);
            }
            MobEffectInstance wither = target.getEffect(MobEffects.WITHER);
            int amplifier = wither == null ? 0 : wither.getAmplifier();
            if (wither != null && elapsed > 0 && elapsed % 40 < period) amplifier = Math.min(4, amplifier + 1);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, amplifier));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72_000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }
}
