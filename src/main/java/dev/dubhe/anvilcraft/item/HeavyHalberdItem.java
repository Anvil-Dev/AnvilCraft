package dev.dubhe.anvilcraft.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.client.renderer.item.ItemUseAnimationTransform;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentTags;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;

public abstract class HeavyHalberdItem extends TieredItem implements ProjectileItem {
    public static final int TRIDENT_MODE = 0;
    public static final int SPEAR_MODE = 1;
    public static final int SWORD_MODE = 2;
    public static final int MACE_MODE = 3;

    public HeavyHalberdItem(Tier tier, Properties properties) {
        super(
            tier,
            properties
                .component(DataComponents.TOOL, createToolProperties(tier))
                .rarity(Rarity.EPIC)
        );
    }

    public static ItemAttributeModifiers createAttributes(Tier tier, float attackDamage, float attackSpeed) {
        return ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID, attackDamage + tier.getAttackDamageBonus(), AttributeModifier.Operation.ADD_VALUE),
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

    public static Tool createToolProperties(Tier tier) {
        ArrayList<Tool.Rule> rules = new ArrayList<>();
        rules.addAll(SwordItem.createToolProperties().rules());
        rules.addAll(tier.createToolProperties(BlockTags.MINEABLE_WITH_AXE).rules());
        return new Tool(rules, 1.0F, 2);
    }

    public static int getMode(ItemStack stack) {
        return Math.clamp(
            stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT).value(),
            TRIDENT_MODE,
            MACE_MODE
        );
    }

    public static void setMode(Player player, InteractionHand hand, @Range(from = TRIDENT_MODE, to = MACE_MODE) int mode) {
        ItemStack heavyHalberd = player.getItemInHand(hand);
        if (!(heavyHalberd.getItem() instanceof HeavyHalberdItem)) return;
        heavyHalberd.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(Math.clamp(mode, TRIDENT_MODE, MACE_MODE)));
    }

    public static void checkTooDamaged(Tier tier, ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof HeavyHalberdItem heavyHalberd)) return;
        if (isTooDamagedToUse(stack)) {
            if (stack.has(DataComponents.ENCHANTMENTS)) {
                ItemEnchantments enchs = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemEnchantments disabledEnchs = stack.getOrDefault(ModComponents.DISABLED_ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemEnchantments.Mutable enchsMut = new ItemEnchantments.Mutable(enchs);
                ItemEnchantments.Mutable disabledEnchsMut = new ItemEnchantments.Mutable(disabledEnchs);
                for (Iterator<Holder<Enchantment>> it = enchs.keySet().iterator(); it.hasNext(); ) {
                    Holder<Enchantment> enchantment = it.next();

                    if (enchantment.is(ModEnchantmentTags.DISABLED_PASSED)) continue;

                    int level = enchs.getLevel(enchantment);
                    int storedLevel = disabledEnchs.getLevel(enchantment);
                    if (level == storedLevel) {
                        level++;
                    } else {
                        level = Math.max(level, storedLevel);
                    }
                    enchsMut.set(enchantment, level);
                    it.remove();
                }
                stack.set(DataComponents.ENCHANTMENTS, enchsMut.toImmutable());
                stack.set(ModComponents.DISABLED_ENCHANTMENTS, disabledEnchsMut.toImmutable());
            }
            if (stack.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
                ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
                for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
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
                    enchsMut.set(enchantment, enchs.getLevel(enchantment));
                }
                stack.set(DataComponents.ENCHANTMENTS, enchsMut.toImmutable());
                stack.set(ModComponents.DISABLED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            }
            if (stack.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
                ItemAttributeModifiers modifiers = stack.getAttributeModifiers()
                    .withModifierAdded(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                            BASE_ATTACK_DAMAGE_ID,
                            heavyHalberd.getBaseAttackDamage() + tier.getAttackDamageBonus(),
                            AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND
                    );
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
            }
            if (!stack.has(DataComponents.TOOL)) {
                stack.set(DataComponents.TOOL, createToolProperties(tier));
            }
        }
    }

    @Override
    @SuppressWarnings({"removal"})
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(
                PoseStack poseStack,
                LocalPlayer player,
                HumanoidArm arm,
                ItemStack stack,
                float partialTick,
                float equipProgress,
                float swingProgress
            ) {
                if (getMode(stack) != SWORD_MODE) return false;
                return ItemUseAnimationTransform.applySwordBlock(poseStack, player, arm, equipProgress);
            }
        });
        ItemProperties.register(
            this,
            ResourceLocation.withDefaultNamespace("throwing"),
            (stack, level, entity, data) -> entity != null
                && getMode(stack) == TRIDENT_MODE
                && entity.isUsingItem()
                && entity.getUseItem() == stack ? 1.0F : 0.0F
        );
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(
            Component.translatable(
                "tooltip.anvilcraft.heavy_halberd.desc",
                Component.keybind("key.anvilcraft.switch_tool_mode")
            ).withStyle(ChatFormatting.GRAY)
        );
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!stack.has(DataComponents.UNBREAKABLE)) checkTooDamaged(this.getTier(), stack);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    /**
     * Returns the action that specifies what animation to play when the item is being used.
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return switch (getMode(stack)) {
            case TRIDENT_MODE -> UseAnim.SPEAR;
            case SWORD_MODE -> UseAnim.BLOCK;
            default -> UseAnim.NONE;
        };
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return getMode(stack) == TRIDENT_MODE || getMode(stack) == SWORD_MODE ? 72000 : 0;
    }

    /**
     * Called when the player stops using an Item (stops holding the right mouse button).
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
        if (getMode(stack) != TRIDENT_MODE) return;
        if (!(entityLiving instanceof Player player)) return;
        int i = this.getUseDuration(stack, entityLiving) - timeLeft;
        if (i < 10) return;
        float spinStrength = EnchantmentHelper.getTridentSpinAttackStrength(stack, player);
        if (spinStrength > 0.0F && !player.isInWaterOrRain()) return;
        if (isTooDamagedToUse(stack)) return;
        Holder<SoundEvent> soundEvent = EnchantmentHelper.pickHighestLevel(stack, EnchantmentEffectComponents.TRIDENT_SOUND)
            .orElse(SoundEvents.TRIDENT_THROW);
        if (!level.isClientSide) {
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(entityLiving.getUsedItemHand()));
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
        if (spinStrength <= 0.0F) return;
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
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (isTooDamagedToUse(itemstack)) {
            return InteractionResultHolder.fail(itemstack);
        }
        if (getMode(itemstack) == SWORD_MODE) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(itemstack);
        }
        if (getMode(itemstack) != TRIDENT_MODE) {
            return InteractionResultHolder.pass(itemstack);
        }
        if (EnchantmentHelper.getTridentSpinAttackStrength(itemstack, player) > 0.0F && !player.isInWaterOrRain()) {
            return InteractionResultHolder.fail(itemstack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemstack);
    }

    @Override
    public int getEnchantmentLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return isEnchantmentActive(stack, enchantment) ? stack.getTagEnchantments().getLevel(enchantment) : 0;
    }

    @Override
    public ItemEnchantments getAllEnchantments(ItemStack stack, HolderLookup.RegistryLookup<Enchantment> lookup) {
        ItemEnchantments enchantments = stack.getTagEnchantments();
        ItemEnchantments.Mutable activeEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            if (isEnchantmentActive(stack, enchantment)) {
                activeEnchantments.set(enchantment, enchantments.getLevel(enchantment));
            }
        }
        return activeEnchantments.toImmutable();
    }

    private static boolean isEnchantmentActive(ItemStack stack, Holder<Enchantment> enchantment) {
        if (enchantment.is(Enchantments.SWEEPING_EDGE)) {
            // 横扫之刃是剑模式专属附魔，其他模式不生效
            return getMode(stack) == SWORD_MODE && stack.is(enchantment.value().definition().supportedItems());
        }
        return stack.is(enchantment.value().definition().supportedItems());
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        int willDamage = super.damageItem(stack, amount, entity, onBroken);
        return stack.getDamageValue() - willDamage >= stack.getMaxDamage() - 1 ? 0 : willDamage;
    }

    protected static boolean isTooDamagedToUse(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof ServerPlayer player) || !MaceItem.canSmashAttack(player)) return true;
        final ServerLevel level = (ServerLevel) attacker.level();
        if (player.isIgnoringFallDamageFromCurrentImpulse() && player.currentImpulseImpactPos != null) {
            if (player.currentImpulseImpactPos.y > player.position().y) {
                player.currentImpulseImpactPos = player.position();
            }
        } else {
            player.currentImpulseImpactPos = player.position();
        }

        player.setIgnoreFallDamageFromCurrentImpulse(true);
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
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        if (MaceItem.canSmashAttack(attacker)) {
            attacker.resetFallDistance();
        }
    }

    @Override
    public float getAttackDamageBonus(Entity target, float damage, DamageSource source) {
        if (!(source.getDirectEntity() instanceof LivingEntity entity)) return 0.0F;
        if (!MaceItem.canSmashAttack(entity)) return 0.0F;

        float firstMaxHeight = 3.0F;
        float secondMaxHeight = 8.0F;
        float fallDistance = entity.fallDistance;

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

    protected abstract double getBaseAttackDamage();

    public abstract ThrownHeavyHalberdEntity createThrown(Level level, LivingEntity shooter, ItemStack pickupItemStack);

    public abstract ThrownHeavyHalberdEntity createThrown(Level level, double x, double y, double z, ItemStack pickupItemStack);

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return switch (getMode(stack)) {
            case TRIDENT_MODE -> ItemAbilities.DEFAULT_TRIDENT_ACTIONS.contains(itemAbility);
            case SWORD_MODE -> ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(itemAbility)
                               || ItemAbilities.DEFAULT_SHIELD_ACTIONS.contains(itemAbility);
            default -> false;
        };
    }

    public static class HeavyHalberdHolder extends Holder.Reference<Item> {
        public HeavyHalberdHolder(Holder.Reference.Type type, HolderOwner<Item> owner, ResourceKey<Item> key, Item value) {
            super(type, owner, key, value);
        }

        public boolean is(int mode, TagKey<Item> tagKey) {
            return isModeEnabled(mode, tagKey) && super.is(tagKey);
        }

        public boolean is(int mode, HolderSet<Item> holders) {
            if (holders instanceof HolderSet.Named<Item> named) {
                return isModeEnabled(mode, named.key()) && holders.contains(this);
            }
            return holders.contains(this);
        }

        private static boolean isModeEnabled(int mode, TagKey<Item> tagKey) {
            if (tagKey.equals(ItemTags.SWORDS)) {
                return mode == SWORD_MODE;
            }
            if (tagKey.equals(ItemTags.TRIDENT_ENCHANTABLE)) {
                return mode == TRIDENT_MODE;
            }
            if (tagKey.equals(ItemTags.MACE_ENCHANTABLE)) {
                return mode == MACE_MODE;
            }
            if (tagKey.equals(ItemTags.SWORD_ENCHANTABLE)
                || tagKey.equals(ItemTags.SHARP_WEAPON_ENCHANTABLE)
                || tagKey.equals(ItemTags.FIRE_ASPECT_ENCHANTABLE)
                || tagKey.equals(ItemTags.WEAPON_ENCHANTABLE)
            ) {
                // 通用近战武器附魔（抢夺、锋利、火焰附加等）在所有近战模式下生效
                return mode == TRIDENT_MODE || mode == SPEAR_MODE || mode == SWORD_MODE || mode == MACE_MODE;
            }
            return true;
        }
    }
}
