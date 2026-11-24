package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.entity.RailgunAnvilEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.network.WeaponChargeProgressPacket;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class AnvilRailgunItem extends EnergyWeaponItem {

    private static final float MIN_FIRE_CHARGE_PROGRESS = 0.2F;
    /** 满蓄力所需 tick，快速装填在此基础上每级再乘 0.9。 */
    private static final int BASE_CHARGE_TICKS = 25;
    private static final double QUICK_CHARGE_MULTIPLIER = 0.9;

    public AnvilRailgunItem(Properties properties) {
        super(properties);
    }

    @Override
    protected int minimumEnergy() {
        return AnvilCraft.CONFIG.equipment.anvilRailgunMinShotEnergy;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (entity instanceof Player player) {
            AnvilRailgunItem.returnMagazine(player, stack);
        }
    }

    private static void returnMagazine(Player player, ItemStack weapon) {
        if (!weapon.has(ModComponents.RAILGUN_AMMO) && !weapon.has(DataComponents.CHARGED_PROJECTILES)
            && !weapon.has(ModComponents.RAILGUN_INFINITE_AMMO_MASK)) {
            return;
        }
        ChargedProjectiles magazine = weapon.remove(ModComponents.RAILGUN_AMMO);
        ChargedProjectiles legacy = weapon.remove(DataComponents.CHARGED_PROJECTILES);
        Integer infiniteAmmoMask = weapon.remove(ModComponents.RAILGUN_INFINITE_AMMO_MASK);
        // 旧弹匣无来源标记时，原有射击逻辑将所有弹药视为不可回收。
        if (infiniteAmmoMask == null) {
            return;
        }
        if (magazine == null || magazine.isEmpty()) {
            magazine = legacy;
        }
        if (magazine == null) {
            return;
        }
        List<ItemStack> ammo = magazine.itemCopies();
        for (int slot = 0; slot < Math.min(ammo.size(), Integer.SIZE); slot++) {
            ItemStack round = ammo.get(slot);
            if ((infiniteAmmoMask & (1 << slot)) == 0 && !round.has(DataComponents.INTANGIBLE_PROJECTILE)) {
                player.getInventory().placeItemBackInInventory(round);
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack weapon = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            AnvilRailgunItem.returnMagazine(player, weapon);
        }
        if (!this.canStartUsing(player, weapon, this.minimumEnergy())) return InteractionResult.FAIL;
        if (AnvilRailgunItem.findAmmo(player).isEmpty()) return InteractionResult.FAIL;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack weapon, int remaining) {
        if (!(user instanceof Player usingPlayer) || !this.canContinueUsing(usingPlayer, weapon)) return;
        if (!(user instanceof ServerPlayer player)) return;
        int elapsed = this.getUseDuration(weapon, user) - remaining;
        WeaponChargeProgressPacket.sync(player, weapon, elapsed, AnvilRailgunItem.fullChargeTicks(level, weapon), false);
    }

    @Override
    public boolean releaseUsing(ItemStack weapon, Level level, LivingEntity user, int remaining) {
        if (!(user instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) return false;
        int elapsed = this.getUseDuration(weapon, user) - remaining;
        float progress = AnvilRailgunItem.chargeProgress(level, weapon, elapsed, 0.0F);
        if (progress >= AnvilRailgunItem.MIN_FIRE_CHARGE_PROGRESS) this.fire(serverLevel, player, weapon, progress);
        return false;
    }

    /**
     * 与弓一致：先看双手，再从物品栏里找铁砧。
     */
    private static ItemStack findAmmo(Player player) {
        ItemStack main = player.getMainHandItem();
        if (AnvilRailgunItem.isValidAnvil(main)) return main;
        ItemStack off = player.getOffhandItem();
        if (AnvilRailgunItem.isValidAnvil(off)) return off;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (AnvilRailgunItem.isValidAnvil(stack)) return stack;
        }
        return ItemStack.EMPTY;
    }

    private void fire(ServerLevel level, ServerPlayer player, ItemStack weapon, float progress) {
        ItemStack ammo = AnvilRailgunItem.findAmmo(player);
        if (ammo.isEmpty()) return;
        int energy = Math.round(progress * 20_000_000.0F);
        if (!this.consumeEnergy(player, weapon, energy)) return;

        boolean infinity = AnvilRailgunItem.enchantmentLevel(level, weapon, Enchantments.INFINITY) > 0
                           && ammo.is(Items.ANVIL);
        Block block = ((BlockItem) ammo.getItem()).getBlock();
        if (!infinity && !player.hasInfiniteMaterials()) ammo.shrink(1);

        int projectileCount = AnvilRailgunItem.enchantmentLevel(level, weapon, Enchantments.MULTISHOT) > 0 ? 3 : 1;
        int piercing = AnvilRailgunItem.enchantmentLevel(level, weapon, Enchantments.PIERCING);
        int knockback = AnvilRailgunItem.enchantmentLevel(level, weapon, Enchantments.PUNCH);
        boolean loyalty = AnvilRailgunItem.enchantmentLevel(level, weapon, Enchantments.LOYALTY) > 0;
        int power = AnvilRailgunItem.enchantmentLevel(level, weapon, Enchantments.POWER);
        double speed = Math.sqrt(progress) * 8.0 * Math.pow(1.1, Math.min(15, power));
        for (int i = 0; i < projectileCount; i++) {
            boolean center = i == 0;
            boolean ghost = !center || infinity;
            boolean returns = loyalty && center && !ghost;
            RailgunAnvilEntity projectile = RailgunAnvilEntity.create(
                level,
                player,
                block.defaultBlockState(),
                weapon,
                ghost,
                returns,
                piercing,
                knockback
            );
            float angle = projectileCount == 1 || center ? 0.0F : i == 1 ? -10.0F : 10.0F;
            Vec3 direction = Vec3.directionFromRotation(player.getXRot(), player.getYRot() + angle);
            projectile.setDeltaMovement(direction.scale(speed));
            projectile.setLoyalty(returns);
            level.addFreshEntity(projectile);
        }
        level.playSound(
            null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.9F, 0.75F);
        level.playSound(
            null, player.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.16F, 1.7F);
    }

    public static int fullChargeTicks(Level level, ItemStack weapon) {
        int quickCharge = Math.max(0, AnvilRailgunItem.enchantmentLevel(level, weapon, Enchantments.QUICK_CHARGE));
        double multiplier = Math.pow(AnvilRailgunItem.QUICK_CHARGE_MULTIPLIER, quickCharge);
        return Math.max(1, (int) Math.round(AnvilRailgunItem.BASE_CHARGE_TICKS * multiplier));
    }

    public static float chargeProgress(Level level, ItemStack weapon, int elapsedTicks, float partialTick) {
        return Math.min(1.0F, (elapsedTicks + partialTick) / AnvilRailgunItem.fullChargeTicks(level, weapon));
    }

    private static int enchantmentLevel(
        Level level,
        ItemStack stack,
        ResourceKey<Enchantment> key
    ) {
        Holder<Enchantment> enchantment = level.holderLookup(Registries.ENCHANTMENT).getOrThrow(key);
        return stack.getEnchantmentLevel(enchantment);
    }

    private static boolean isValidAnvil(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        return blockItem.getBlock().defaultBlockState().is(BlockTags.ANVIL)
               && blockItem.getBlock() != ModBlocks.SPECTRAL_ANVIL.get();
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72_000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }
}
