package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IMultipleResult;
import dev.dubhe.anvilcraft.api.item.property.Merciless;
import dev.dubhe.anvilcraft.entity.ThrownHeavyHalberdEntity;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModEnchantmentTags;
import dev.dubhe.anvilcraft.recipe.multiple.MultipleToOneSmithingRecipeInput;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Position;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class HeavyHalberdItem extends TieredItem implements ProjectileItem, IMultipleResult {
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

    public static void checkTooDamaged(Tier tier, ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof HeavyHalberdItem heavyHalberd)) return;
        if (isTooDamagedToUse(stack)) {
            if (stack.has(ModComponents.MERCILESS)) {
                stack.set(ModComponents.MERCILESS, Merciless.DISABLED);
            }
            if (stack.has(DataComponents.ENCHANTMENTS) && !stack.has(ModComponents.MERCILESS)) {
                ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemEnchantments enchantmentsStored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemEnchantments.Mutable enchantmentsMutable = new ItemEnchantments.Mutable(enchantments);
                ItemEnchantments.Mutable storedMutable = new ItemEnchantments.Mutable(enchantmentsStored);
                for (Object2IntMap.Entry<Holder<Enchantment>> enchantment : enchantments.entrySet()) {
                    Holder<Enchantment> enchantmentHolder = enchantment.getKey();
                    if (enchantmentHolder.is(ModEnchantmentTags.DISABLED_PASSED)) continue;
                    int enchantmentLevel = enchantment.getIntValue();
                    int enchantmentStoredLevel = enchantmentsStored.getLevel(enchantmentHolder);
                    if (enchantmentLevel == enchantmentStoredLevel) {
                        storedMutable.set(enchantmentHolder, enchantmentLevel + 1);
                    } else if (enchantmentLevel > enchantmentStoredLevel) {
                        storedMutable.set(enchantmentHolder, enchantmentLevel);
                    }
                    enchantmentsMutable.removeIf(holder -> holder.equals(enchantmentHolder));
                }
                stack.set(DataComponents.STORED_ENCHANTMENTS, storedMutable.toImmutable());
                stack.set(DataComponents.ENCHANTMENTS, enchantmentsMutable.toImmutable());
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
            if (stack.has(DataComponents.STORED_ENCHANTMENTS) && !stack.has(ModComponents.MERCILESS)) {
                ItemEnchantments enchantmentsStored = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);
                for (Object2IntMap.Entry<Holder<Enchantment>> enchantmentStored : enchantmentsStored.entrySet()) {
                    Holder<Enchantment> enchantmentStoredHolder = enchantmentStored.getKey();
                    int enchantmentStoredLevel = enchantmentStored.getIntValue();
                    int enchantmentLevel = enchantments.getLevel(enchantmentStoredHolder);
                    if (enchantmentStoredLevel == enchantmentLevel) {
                        mutable.set(enchantmentStoredHolder, enchantmentStoredLevel + 1);
                    } else if (enchantmentStoredLevel > enchantmentLevel) {
                        mutable.set(enchantmentStoredHolder, enchantmentStoredLevel);
                    }
                }
                stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
                stack.remove(DataComponents.STORED_ENCHANTMENTS);
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
        ItemProperties.register(
            this,
            ResourceLocation.withDefaultNamespace("throwing"),
            (stack, level, entity, data) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
        );
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        checkTooDamaged(this.getTier(), stack);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public ItemStack assemble(int id, MultipleToOneSmithingRecipeInput input, HolderLookup.Provider registries) {
        if (id == 0) {
            ItemStack defaultStack = this.getDefaultInstance();

            Object2IntMap<Holder<Enchantment>> enchantments = new Object2IntArrayMap<>();
            for (int i = 0; i < 4; i++) {
                ItemStack inputStack = input.getInputItem(i);
                DataComponentType<ItemEnchantments> type = EnchantmentHelper.getComponentType(defaultStack);
                if (inputStack.getOrDefault(ModComponents.MERCILESS, Merciless.DISABLED).enabled()) {
                    type = DataComponents.STORED_ENCHANTMENTS;
                }
                for (var entry : inputStack.getOrDefault(type, ItemEnchantments.EMPTY).entrySet()) {
                    enchantments.mergeInt(entry.getKey(), entry.getIntValue(), Integer::max);
                }
            }

            DataComponentType<ItemEnchantments> type = EnchantmentHelper.getComponentType(defaultStack);
            if (defaultStack.getOrDefault(ModComponents.MERCILESS, Merciless.DISABLED).enabled()) {
                type = DataComponents.STORED_ENCHANTMENTS;
            }
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(defaultStack.getOrDefault(type, ItemEnchantments.EMPTY));
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.object2IntEntrySet()) {
                mutable.set(entry.getKey(), entry.getIntValue());
            }
            defaultStack.set(type, mutable.toImmutable());

            return defaultStack;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Returns the action that specifies what animation to play when the item is being used.
     */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    /**
     * Called when the player stops using an Item (stops holding the right mouse button).
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entityLiving, int timeLeft) {
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
        float yRot = player.getYRot();
        float xRot = player.getXRot();
        float xDelta = -Mth.sin(yRot * (float) (Math.PI / 180.0)) * Mth.cos(xRot * (float) (Math.PI / 180.0));
        float yDelta = -Mth.sin(xRot * (float) (Math.PI / 180.0));
        float zDelta = Mth.cos(yRot * (float) (Math.PI / 180.0)) * Mth.cos(xRot * (float) (Math.PI / 180.0));
        float fixer = Mth.sqrt(xDelta * xDelta + yDelta * yDelta + zDelta * zDelta);
        xDelta *= spinStrength / fixer;
        yDelta *= spinStrength / fixer;
        zDelta *= spinStrength / fixer;
        player.push(xDelta, yDelta, zDelta);
        player.startAutoSpinAttack(20, 8.0F, stack);
        if (player.onGround()) {
            float yFix = 1.1999999F;
            player.move(MoverType.SELF, new Vec3(0.0, yFix, 0.0));
        }

        level.playSound(null, player, soundEvent.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        if (isTooDamagedToUse(itemstack)) {
            return InteractionResultHolder.fail(itemstack);
        } else if (EnchantmentHelper.getTridentSpinAttackStrength(itemstack, player) > 0.0F && !player.isInWaterOrRain()) {
            return InteractionResultHolder.fail(itemstack);
        } else {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(itemstack);
        }
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
        ServerLevel level = (ServerLevel) attacker.level();
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
        return ItemAbilities.DEFAULT_TRIDENT_ACTIONS.contains(itemAbility) || ItemAbilities.DEFAULT_SWORD_ACTIONS.contains(itemAbility);
    }
}
