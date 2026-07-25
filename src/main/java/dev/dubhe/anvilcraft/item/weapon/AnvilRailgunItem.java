package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.entity.RailgunAnvilEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class AnvilRailgunItem extends EnergyWeaponItem {
    private static final int MAX_AMMO = 16;
    private static final int MIN_SHOT_ENERGY = 2_000_000;
    private static final float MIN_FIRE_CHARGE_PROGRESS = 0.2F;

    public AnvilRailgunItem(Properties properties) {
        super(properties.component(ModComponents.RAILGUN_AMMO, ChargedProjectiles.EMPTY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack weapon = player.getItemInHand(hand);
        if (!this.canStartUsing(player, weapon, MIN_SHOT_ENERGY)) return InteractionResult.FAIL;
        if (ammo(weapon).isEmpty()
            && !isValidAnvil(otherHand(player, hand))
            && findNormalAnvil(player) < 0
        ) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack weapon, int remaining) {
        if (!(user instanceof ServerPlayer player) || isLoading(player, weapon, player.getUsedItemHand())) return;
        int elapsed = this.getUseDuration(weapon, user) - remaining;
        int fullTicks = fullChargeTicks(level, weapon);
        if (elapsed > 0 && elapsed % fullTicks == 0) {
            this.fire((ServerLevel) level, player, weapon, 1.0F);
            if (ammo(weapon).isEmpty()) player.releaseUsingItem();
        }
    }

    @Override
    public boolean releaseUsing(ItemStack weapon, Level level, LivingEntity user, int remaining) {
        if (!(user instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) return false;
        int elapsed = this.getUseDuration(weapon, user) - remaining;
        InteractionHand hand = player.getUsedItemHand();
        if (isLoading(player, weapon, hand)) {
            if (elapsed >= loadTicks(level, weapon)) load(player, weapon, hand);
            return false;
        }
        float progress = chargeProgress(level, weapon, elapsed, 0.0F);
        if (progress >= MIN_FIRE_CHARGE_PROGRESS) this.fire(serverLevel, player, weapon, progress);
        return false;
    }

    public static boolean isLoading(Player player, ItemStack weapon, InteractionHand hand) {
        List<ItemStack> ammo = ammo(weapon);
        if (ammo.size() >= MAX_AMMO) return false;
        ItemStack supplied = otherHand(player, hand);
        if (ammo.isEmpty()) return isValidAnvil(supplied) || findNormalAnvil(player) >= 0;
        return isValidAnvil(supplied) && ItemStack.isSameItemSameComponents(ammo.getFirst(), supplied);
    }

    private static void load(ServerPlayer player, ItemStack weapon, InteractionHand hand) {
        List<ItemStack> loaded = new ArrayList<>(ammo(weapon));
        ItemStack source = otherHand(player, hand);
        int inventorySlot = -1;
        if (!isValidAnvil(source)) {
            inventorySlot = findNormalAnvil(player);
            if (inventorySlot < 0) return;
            source = player.getInventory().getItem(inventorySlot);
        }
        boolean infinity = enchantmentLevel(player.level(), weapon, Enchantments.INFINITY) > 0
                           && source.is(Items.ANVIL);
        int amount = infinity
            ? MAX_AMMO - loaded.size()
            : Math.min(source.getCount(), MAX_AMMO - loaded.size());
        int infiniteAmmoMask = infiniteAmmoMask(weapon, loaded.size());
        for (int i = 0; i < amount; i++) loaded.add(source.copyWithCount(1));
        if (infinity) infiniteAmmoMask |= ammoMask(amount) << (loaded.size() - amount);
        if (!infinity && !player.hasInfiniteMaterials()) source.shrink(amount);
        if (inventorySlot >= 0 && source.isEmpty()) player.getInventory().removeItem(inventorySlot, 1);
        setAmmo(weapon, loaded, infiniteAmmoMask);
        player.level().playSound(
            null,
            player.blockPosition(),
            SoundEvents.CROSSBOW_LOADING_END.value(),
            SoundSource.PLAYERS,
            1.0F,
            1.0F
        );
    }

    private void fire(ServerLevel level, ServerPlayer player, ItemStack weapon, float progress) {
        List<ItemStack> loaded = new ArrayList<>(ammo(weapon));
        if (loaded.isEmpty()) return;
        int energy = Math.round(progress * 20_000_000.0F);
        if (!this.consumeEnergy(player, weapon, energy, 160_000_000)) return;

        ItemStack projectileStack = loaded.getFirst();
        boolean infinity = enchantmentLevel(level, weapon, Enchantments.INFINITY) > 0
                           && projectileStack.is(Items.ANVIL);
        int infiniteAmmoMask = infiniteAmmoMask(weapon, loaded.size());
        boolean loadedByInfinity = (infiniteAmmoMask & 1) != 0;
        if (!infinity) {
            loaded.removeFirst();
            infiniteAmmoMask >>>= 1;
        }
        setAmmo(weapon, loaded, infiniteAmmoMask);

        int projectileCount = enchantmentLevel(level, weapon, Enchantments.MULTISHOT) > 0 ? 3 : 1;
        int piercing = enchantmentLevel(level, weapon, Enchantments.PIERCING);
        int knockback = enchantmentLevel(level, weapon, Enchantments.PUNCH);
        boolean loyalty = enchantmentLevel(level, weapon, Enchantments.LOYALTY) > 0;
        int power = enchantmentLevel(level, weapon, Enchantments.POWER);
        double speed = Math.sqrt(progress) * 8.0 * (1.0 + Math.min(10, power) * 0.1);
        Block block = ((BlockItem) projectileStack.getItem()).getBlock();
        for (int i = 0; i < projectileCount; i++) {
            boolean center = i == 0;
            boolean ghost = !center || infinity || loadedByInfinity;
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

    private static int loadTicks(Level level, ItemStack weapon) {
        return Math.max(5, 25 - enchantmentLevel(level, weapon, Enchantments.QUICK_CHARGE) * 5);
    }

    public static int fullChargeTicks(Level level, ItemStack weapon) {
        return (int) Math.ceil(100.0 / chargePercentPerTick(level, weapon));
    }

    public static float chargeProgress(Level level, ItemStack weapon, int elapsedTicks, float partialTick) {
        int fullTicks = fullChargeTicks(level, weapon);
        return Math.min(
            1.0F,
            ((elapsedTicks % fullTicks) + partialTick) * chargePercentPerTick(level, weapon) / 100.0F
        );
    }

    private static float chargePercentPerTick(Level level, ItemStack weapon) {
        int quickCharge = enchantmentLevel(level, weapon, Enchantments.QUICK_CHARGE);
        return Math.min(4.0F, 1.0F + quickCharge * 0.2F);
    }

    private static int enchantmentLevel(
        Level level,
        ItemStack stack,
        net.minecraft.resources.ResourceKey<Enchantment> key
    ) {
        Holder<Enchantment> enchantment = level.holderLookup(Registries.ENCHANTMENT).getOrThrow(key);
        return stack.getEnchantmentLevel(enchantment);
    }

    private static ItemStack otherHand(Player player, InteractionHand hand) {
        return player.getItemInHand(
            hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }

    private static int findNormalAnvil(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.ANVIL)) return i;
        }
        return -1;
    }

    private static boolean isValidAnvil(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        return blockItem.getBlock().defaultBlockState().is(BlockTags.ANVIL)
               && blockItem.getBlock() != ModBlocks.SPECTRAL_ANVIL.get();
    }

    private static List<ItemStack> ammo(ItemStack weapon) {
        ChargedProjectiles stored = weapon.getOrDefault(ModComponents.RAILGUN_AMMO, ChargedProjectiles.EMPTY);
        if (!stored.isEmpty()) return stored.itemCopies();
        return weapon.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY).itemCopies();
    }

    private static void setAmmo(ItemStack weapon, List<ItemStack> ammo, int infiniteAmmoMask) {
        weapon.set(ModComponents.RAILGUN_AMMO, ChargedProjectiles.ofNonEmpty(ammo));
        weapon.set(ModComponents.RAILGUN_INFINITE_AMMO_MASK, infiniteAmmoMask & ammoMask(ammo.size()));
        weapon.remove(DataComponents.CHARGED_PROJECTILES);
    }

    private static int infiniteAmmoMask(ItemStack weapon, int ammoSize) {
        int validMask = ammoMask(ammoSize);
        Integer stored = weapon.get(ModComponents.RAILGUN_INFINITE_AMMO_MASK);
        if (stored == null) {
            // 升级前装填的弹药视作无限弹，避免凭空生成的铁砧被回收
            return validMask;
        }
        return stored & validMask;
    }

    private static int ammoMask(int ammoSize) {
        return (1 << Math.min(ammoSize, MAX_AMMO)) - 1;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72_000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(
        ItemStack stack,
        Item.TooltipContext context,
        TooltipDisplay display,
        Consumer<Component> tooltip,
        TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        List<ItemStack> loaded = ammo(stack);
        if (!loaded.isEmpty()) {
            tooltip.accept(Component.translatable("item.minecraft.crossbow.projectile")
                .append(CommonComponents.SPACE)
                .append(loaded.getFirst().getDisplayName())
                .append(Component.literal(" x" + loaded.size()).withStyle(ChatFormatting.GRAY)));
        }
    }
}
