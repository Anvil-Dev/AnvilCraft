package dev.dubhe.anvilcraft.item.tool;

import dev.dubhe.anvilcraft.api.tooltip.providers.IItemTooltipProvider;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentTags;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class HeavyHalberdItem extends Item implements ProjectileItem, IItemTooltipProvider {
    private final ToolMaterial material;
    private final float attackDamage;

    public HeavyHalberdItem(ToolMaterial material, float attackDamage, float attackSpeed, Properties properties) {
        super(
            properties
                .attributes(HeavyHalberdItem.createAttributes(material, attackDamage, attackSpeed))
                .component(DataComponents.TOOL, HeavyHalberdItem.createToolProperties(material, true))
                .component(DataComponents.WEAPON, new Weapon(1))
                .component(ModComponents.HEAVY_HALBERD_MODE, HeavyHalberdMode.TRIDENT)
                .durability(material.durability()).repairable(material.repairItems()).enchantable(material.enchantmentValue())
                .rarity(Rarity.EPIC)
        );
        this.material = material;
        this.attackDamage = attackDamage;
    }

    public static ItemAttributeModifiers createAttributes(ToolMaterial material, float attackDamage, float attackSpeed) {
        return ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    attackDamage + material.attackDamageBonus(),
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .build();
    }

    public static double getThrownBaseDamage(ItemStack stack) {
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if ((entry.matches(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE_ID)
                || entry.matches(Attributes.ATTACK_DAMAGE, Merciless.MERCILESS_ID))
                && entry.modifier().operation().equals(AttributeModifier.Operation.ADD_VALUE)
            ) {
                return entry.modifier().amount() / 3;
            }
        }
        return 2.0;
    }

    @SuppressWarnings("deprecation")
    public static Tool createToolProperties(ToolMaterial material, boolean isBootstrap) {
        HolderGetter<Block> lookup = isBootstrap
                                     ? BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK)
                                     : BuiltInRegistries.BLOCK;
        ArrayList<Tool.Rule> rules = new ArrayList<>();
        rules.add(Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F));
        rules.add(Tool.Rule.overrideSpeed(lookup.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE));
        rules.add(Tool.Rule.overrideSpeed(lookup.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5F));
        rules.add(Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_AXE), material.speed()));
        return new Tool(rules, material.speed(), 1, false);
    }

    public static HeavyHalberdMode getMode(ItemInstance stack) {
        return stack.getOrDefault(ModComponents.HEAVY_HALBERD_MODE, HeavyHalberdMode.TRIDENT);
    }

    public static void setMode(Player player, InteractionHand hand, HeavyHalberdMode mode) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof HeavyHalberdItem)) return;
        stack.set(ModComponents.HEAVY_HALBERD_MODE, mode);
        updateModeComponents(stack, mode);
    }

    private static void updateModeComponents(ItemStack stack, HeavyHalberdMode mode) {
        if (mode == HeavyHalberdMode.SPEAR) {
            ItemStack spear = Items.NETHERITE_SPEAR.getDefaultInstance();
            copyComponentIfMissing(stack, spear, DataComponents.DAMAGE_TYPE);
            copyComponentIfMissing(stack, spear, DataComponents.KINETIC_WEAPON);
            copyComponentIfMissing(stack, spear, DataComponents.PIERCING_WEAPON);
            copyComponentIfMissing(stack, spear, DataComponents.ATTACK_RANGE);
            copyComponentIfMissing(stack, spear, DataComponents.MINIMUM_ATTACK_CHARGE);
            copyComponentIfMissing(stack, spear, DataComponents.SWING_ANIMATION);
            copyComponentIfMissing(stack, spear, DataComponents.USE_EFFECTS);
            return;
        }
        stack.remove(DataComponents.DAMAGE_TYPE);
        stack.remove(DataComponents.KINETIC_WEAPON);
        stack.remove(DataComponents.PIERCING_WEAPON);
        stack.remove(DataComponents.ATTACK_RANGE);
        stack.remove(DataComponents.MINIMUM_ATTACK_CHARGE);
        stack.remove(DataComponents.SWING_ANIMATION);
        stack.remove(DataComponents.USE_EFFECTS);
    }

    private static <T> void copyComponentIfMissing(
        ItemStack target,
        ItemStack source,
        DataComponentType<T> componentType
    ) {
        if (target.has(componentType)) return;
        T value = source.get(componentType);
        if (value != null) target.set(componentType, value);
    }

    private static boolean hasAnySpearComponent(ItemStack stack) {
        return stack.has(DataComponents.DAMAGE_TYPE)
               || stack.has(DataComponents.KINETIC_WEAPON)
               || stack.has(DataComponents.PIERCING_WEAPON)
               || stack.has(DataComponents.ATTACK_RANGE)
               || stack.has(DataComponents.MINIMUM_ATTACK_CHARGE)
               || stack.has(DataComponents.SWING_ANIMATION)
               || stack.has(DataComponents.USE_EFFECTS);
    }

    public static void checkTooDamaged(ToolMaterial material, ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof HeavyHalberdItem heavyHalberd)) return;
        if (isTooDamagedToUse(stack)) {
            if (stack.has(DataComponents.ENCHANTMENTS)) {
                ItemEnchantments enchs = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemEnchantments disabledEnchs = stack.getOrDefault(ModComponents.DISABLED_ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemEnchantments.Mutable enchsMut = new ItemEnchantments.Mutable(enchs);
                ItemEnchantments.Mutable disabledEnchsMut = new ItemEnchantments.Mutable(disabledEnchs);
                for (Holder<Enchantment> enchantment : enchs.keySet()) {
                    if (enchantment.is(ModEnchantmentTags.DISABLED_PASSED)) continue;

                    int level = enchs.getLevel(enchantment);
                    int storedLevel = disabledEnchs.getLevel(enchantment);
                    if (level == storedLevel) {
                        level++;
                    } else {
                        level = Math.max(level, storedLevel);
                    }
                    enchsMut.removeIf(holder -> holder.equals(enchantment));
                    disabledEnchsMut.set(enchantment, level);
                }
                stack.set(DataComponents.ENCHANTMENTS, enchsMut.toImmutable());
                stack.set(ModComponents.DISABLED_ENCHANTMENTS, disabledEnchsMut.toImmutable());
            }
            if (stack.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
                ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                for (ItemAttributeModifiers.Entry entry : stack.get(DataComponents.ATTRIBUTE_MODIFIERS).modifiers()) {
                    if (!entry.matches(Attributes.ATTACK_DAMAGE, BASE_ATTACK_DAMAGE_ID)) {
                        builder.add(entry.attribute(), entry.modifier(), entry.slot());
                    }
                }
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
            }
            if (stack.has(DataComponents.TOOL)) {
                stack.remove(DataComponents.TOOL);
            }
        } else {
            if (stack.has(ModComponents.DISABLED_ENCHANTMENTS)) {
                ItemEnchantments disabledEnchs = stack.getOrDefault(ModComponents.DISABLED_ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemEnchantments enchs = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemEnchantments.Mutable enchsMut = new ItemEnchantments.Mutable(enchs);
                for (Holder<Enchantment> enchantment : disabledEnchs.keySet()) {
                    enchsMut.set(enchantment, disabledEnchs.getLevel(enchantment));
                }
                stack.set(DataComponents.ENCHANTMENTS, enchsMut.toImmutable());
                stack.remove(ModComponents.DISABLED_ENCHANTMENTS);
            }
            if (stack.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
                ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS)
                    .withModifierAdded(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                            BASE_ATTACK_DAMAGE_ID,
                            heavyHalberd.attackDamage + material.attackDamageBonus(),
                            AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                    );
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
            }
            if (!stack.has(DataComponents.TOOL)) {
                stack.set(DataComponents.TOOL, createToolProperties(material, false));
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        HeavyHalberdMode mode = getMode(stack);
        if (mode == HeavyHalberdMode.SPEAR || hasAnySpearComponent(stack)) {
            updateModeComponents(stack, mode);
        }
        if (!stack.has(DataComponents.UNBREAKABLE)) HeavyHalberdItem.checkTooDamaged(this.material, stack);
    }

    @Override
    public void appendItemTooltip(
        ItemStack stack,
        TooltipContext context,
        TooltipDisplay display,
        Consumer<Component> builder,
        TooltipFlag tooltipFlag
    ) {
        builder.accept(Component.translatable(
            "tooltip.anvilcraft.heavy_halberd.desc",
            Component.keybind("key.anvilcraft.switch_tool_mode")
        ).withStyle(ChatFormatting.GRAY));
    }

    /// Returns the action that specifies what animation to play when the item is being used.
    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return switch (getMode(stack)) {
            case TRIDENT, SPEAR -> ItemUseAnimation.SPEAR;
            case SWORD -> ItemUseAnimation.BLOCK;
            case MACE -> ItemUseAnimation.NONE;
        };
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return getMode(stack) == HeavyHalberdMode.MACE ? 0 : 72000;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (getMode(stack) != HeavyHalberdMode.TRIDENT) return false;
        if (!(entityLiving instanceof Player player)) return false;
        int i = this.getUseDuration(stack, entityLiving) - timeLeft;
        if (i < 10) return false;
        float spinStrength = EnchantmentHelper.getTridentSpinAttackStrength(stack, player);
        if (spinStrength > 0.0F && !player.isInWaterOrRain()) return false;
        if (isTooDamagedToUse(stack)) return false;
        Holder<SoundEvent> soundEvent = EnchantmentHelper.pickHighestLevel(stack, EnchantmentEffectComponents.TRIDENT_SOUND)
            .orElse(SoundEvents.TRIDENT_THROW);
        if (!level.isClientSide()) {
            stack.hurtAndBreak(1, player, entityLiving.getUsedItemHand());
            if (spinStrength == 0.0F) {
                ThrownHeavyHalberdEntity thrown = this.createThrown(level, player, stack);
                thrown.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.5F, 1.0F);
                if (player.hasInfiniteMaterials()) {
                    thrown.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                }

                level.addFreshEntity(thrown);
                level.playSound(null, thrown, soundEvent.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                if (!player.hasInfiniteMaterials()) {
                    player.getInventory().removeItem(stack);
                }
            }
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        if (spinStrength <= 0.0F) return false;
        float rotY = player.getYRot();
        float rotX = player.getXRot();
        float deltaX = -Mth.sin(rotY * (float) (Math.PI / 180.0)) * Mth.cos(rotX * (float) (Math.PI / 180.0));
        float deltaY = -Mth.sin(rotX * (float) (Math.PI / 180.0));
        float deltaZ = Mth.cos(rotY * (float) (Math.PI / 180.0)) * Mth.cos(rotX * (float) (Math.PI / 180.0));
        float fixer = Mth.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        deltaX *= spinStrength / fixer;
        deltaY *= spinStrength / fixer;
        deltaZ *= spinStrength / fixer;
        player.push(deltaX, deltaY, deltaZ);
        player.startAutoSpinAttack(20, 8.0F, stack);
        if (player.onGround()) {
            float fixY = 1.1999999F;
            player.move(MoverType.SELF, new Vec3(0.0, fixY, 0.0));
        }

        level.playSound(null, player, soundEvent.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return true;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isTooDamagedToUse(stack)) {
            return InteractionResult.FAIL;
        }
        HeavyHalberdMode mode = getMode(stack);
        if (mode == HeavyHalberdMode.SPEAR) {
            return super.use(level, player, hand);
        }
        if (mode == HeavyHalberdMode.SWORD) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        if (mode != HeavyHalberdMode.TRIDENT) {
            return InteractionResult.PASS;
        }
        if (EnchantmentHelper.getTridentSpinAttackStrength(stack, player) > 0.0F && !player.isInWaterOrRain()) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        int willDamage = super.damageItem(stack, amount, entity, onBroken);
        int damageValue = stack.getDamageValue();
        int maxDamage = stack.getMaxDamage();
        if (damageValue + willDamage >= maxDamage - 1) {
            willDamage = maxDamage - damageValue - 1;
        }
        return willDamage;
    }

    protected static boolean isTooDamagedToUse(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (getMode(stack) != HeavyHalberdMode.MACE) return;
        if (!(attacker instanceof ServerPlayer player) || !MaceItem.canSmashAttack(player)) return;
        final ServerLevel level = (ServerLevel) attacker.level();
        if (player.isIgnoringFallDamageFromCurrentImpulse() && player.currentImpulseImpactPos != null) {
            if (player.currentImpulseImpactPos.y > player.position().y) {
                player.setIgnoreFallDamageFromCurrentImpulse(true, player.position());
            }
        } else {
            player.setIgnoreFallDamageFromCurrentImpulse(true, player.position());
        }

        player.setDeltaMovement(player.getDeltaMovement().with(Direction.Axis.Y, 0.01F));
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        if (target.onGround()) {
            player.setSpawnExtraParticlesOnFall(true);
            SoundEvent soundEvent = player.fallDistance > 5.0F ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND;
            level.playSound(
                null, player.getX(), player.getY(), player.getZ(), soundEvent, player.getSoundSource(), 1.0F, 1.0F
            );
        } else {
            level.playSound(
                null, player.getX(), player.getY(), player.getZ(), SoundEvents.MACE_SMASH_AIR, player.getSoundSource(), 1.0F, 1.0F
            );
        }

        MaceItem.knockback(level, player, target);
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (getMode(stack) == HeavyHalberdMode.MACE && MaceItem.canSmashAttack(attacker)) {
            attacker.resetFallDistance();
        }
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        if (!(source.getDirectEntity() instanceof LivingEntity entity)) return 0.0F;
        if (getMode(entity.getWeaponItem()) != HeavyHalberdMode.MACE) return 0.0F;
        if (!MaceItem.canSmashAttack(entity)) return 0.0F;

        float firstMaxHeight = 3.0F;
        float secondMaxHeight = 8.0F;
        float fallDistance = (float) entity.fallDistance;

        float damageBonus;
        if (fallDistance <= firstMaxHeight) {
            damageBonus = 4.0F * fallDistance;
        } else if (fallDistance <= secondMaxHeight) {
            damageBonus = 12.0F + 2.0F * (fallDistance - firstMaxHeight);
        } else {
            damageBonus = 22.0F + fallDistance - secondMaxHeight;
        }

        return entity.level() instanceof ServerLevel level
            ? damageBonus + EnchantmentHelper.modifyFallBasedDamage(level, entity.getWeaponItem(), target, source, 0.0F) * fallDistance
            : damageBonus;
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        ThrownHeavyHalberdEntity thrown = this.createThrown(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1));
        thrown.pickup = AbstractArrow.Pickup.ALLOWED;
        return thrown;
    }

    public abstract ThrownHeavyHalberdEntity createThrown(Level level, LivingEntity shooter, ItemStack pickupItemStack);

    public abstract ThrownHeavyHalberdEntity createThrown(Level level, double x, double y, double z, ItemStack pickupItemStack);

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
        return switch (getMode(stack)) {
            case TRIDENT -> ItemAbilities.DEFAULT_TRIDENT_ACTIONS.contains(itemAbility);
            case SWORD -> itemAbility == ItemAbilities.SWORD_SWEEP;
            default -> false;
        };
    }

    public static boolean isEnchantmentActive(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.is(enchantment.value().definition().supportedItems());
    }

    public static class HeavyHalberdHolder extends Holder.Reference<Item> {
        public HeavyHalberdHolder(
            Type type,
            HolderOwner<Item> owner,
            @Nullable ResourceKey<Item> key,
            @Nullable Item value
        ) {
            super(type, owner, key, value);
        }

        public boolean is(HeavyHalberdMode mode, TagKey<Item> tag) {
            return isModeEnabled(mode, tag) && super.is(tag);
        }

        private static boolean isModeEnabled(HeavyHalberdMode mode, TagKey<Item> tag) {
            if (tag.equals(ItemTags.SWORDS) || tag.equals(ItemTags.SWEEPING_ENCHANTABLE)) {
                return mode == HeavyHalberdMode.SWORD;
            }
            if (tag.equals(ItemTags.SPEARS) || tag.equals(ItemTags.LUNGE_ENCHANTABLE)) {
                return mode == HeavyHalberdMode.SPEAR;
            }
            if (tag.equals(ItemTags.MELEE_WEAPON_ENCHANTABLE)
                || tag.equals(ItemTags.SHARP_WEAPON_ENCHANTABLE)) {
                return mode == HeavyHalberdMode.SWORD || mode == HeavyHalberdMode.SPEAR;
            }
            if (tag.equals(ItemTags.FIRE_ASPECT_ENCHANTABLE) || tag.equals(ItemTags.WEAPON_ENCHANTABLE)) {
                return mode == HeavyHalberdMode.SWORD
                       || mode == HeavyHalberdMode.SPEAR
                       || mode == HeavyHalberdMode.MACE;
            }
            if (tag.equals(ItemTags.TRIDENT_ENCHANTABLE)) {
                return mode == HeavyHalberdMode.TRIDENT;
            }
            if (tag.equals(ItemTags.MACE_ENCHANTABLE)) {
                return mode == HeavyHalberdMode.MACE;
            }
            return true;
        }
    }
}
