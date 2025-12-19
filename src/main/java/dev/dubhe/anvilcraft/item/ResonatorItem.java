package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentTags;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class ResonatorItem extends TieredItem {
    public static final int AUTO_MODE = 0;
    public static final int AXE_MODE = 1;
    public static final int SHOVEL_MODE = 2;
    public static final int HOE_MODE = 3;
    public static final int PICKAXE_MODE = 4;

    public ResonatorItem(Tier tier, Properties properties) {
        super(
            tier,
            properties
                .component(DataComponents.TOOL, createToolProperties(tier))
                .fireResistant()
        );
    }

    private boolean isTranscendence(ItemStack stack) {
        return stack.is(ModItems.TRANSCENDENCE_RESONATOR);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        if (isTranscendence(stack)) {
            tooltipComponents.add(Component.translatable("tooltip.anvilcraft.resonator.mining_desc")
                .withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.anvilcraft.resonator.desc")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
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

    public static Tool createToolProperties(Tier tier) {
        List<Tool.Rule> rules = new ArrayList<>(SwordItem.createToolProperties().rules());
        rules.add(Tool.Rule.overrideSpeed(BlockTags.LEAVES, 15.0F));
        rules.add(Tool.Rule.overrideSpeed(BlockTags.WOOL, 5.0F));
        rules.add(Tool.Rule.overrideSpeed(List.of(Blocks.VINE, Blocks.GLOW_LICHEN), 2.0F));
        rules.add(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, tier.getSpeed()));
        rules.add(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_HOE, tier.getSpeed()));
        rules.add(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, tier.getSpeed()));
        rules.add(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, tier.getSpeed()));
        return new Tool(List.copyOf(rules), tier.getSpeed(), 1);
    }

    public static Tool createToolProperties(@Range(from = 0, to = 4) int mode, Tier tier) {
        return switch (mode) {
            case AUTO_MODE -> createToolProperties(tier);
            case AXE_MODE -> new Tool(
                List.of(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_AXE, tier.getSpeed())), 1.0f, 1);
            case SHOVEL_MODE -> new Tool(
                List.of(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_SHOVEL, tier.getSpeed())), 1.0f, 1);
            case HOE_MODE -> new Tool(
                List.of(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_HOE, tier.getSpeed())), 1.0f, 1);
            case PICKAXE_MODE -> new Tool(
                List.of(Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, tier.getSpeed())), 1.0f, 1);
            default -> throw new IllegalStateException("Unexpected mode: " + mode);
        };
    }

    public static int getMode(ItemStack stack) {
        return Math.clamp(stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT).value(), 0, 4);
    }

    public static void checkTooDamaged(Tier tier, ItemStack stack) {
        Item item = stack.getItem();
        if (!(item instanceof ResonatorItem resonator)) return;
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
                            resonator.getBaseAttackDamage() + tier.getAttackDamageBonus(),
                            AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                    );
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
            }
            if (!stack.has(DataComponents.TOOL)) {
                stack.set(
                    DataComponents.TOOL,
                    createToolProperties(ResonatorItem.getMode(stack), tier)
                );
            }
        }
    }

    protected abstract double getBaseAttackDamage();

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        checkTooDamaged(this.getTier(), stack);
    }

    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (isTooDamagedToUse(stack)) return 1.0f;
        Tool tool = stack.get(DataComponents.TOOL);
        return tool != null ? tool.getMiningSpeed(state) : this.getTier().getSpeed();
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
    }

    protected static boolean isTooDamagedToUse(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        int mode = ResonatorItem.getMode(stack);
        return switch (mode) {
            case AUTO_MODE -> {
                if (isTranscendence(stack) && !isTooDamagedToUse(stack)) {
                    Player player = context.getPlayer();
                    if (player != null) {
                        player.startUsingItem(context.getHand());
                        yield InteractionResult.CONSUME;
                    }
                }
                yield InteractionResult.PASS;
            }
            case AXE_MODE -> this.useOnAsAxe(context);
            case SHOVEL_MODE -> this.useOnAsShovel(context);
            case HOE_MODE -> this.useOnAsHoe(context);
            case PICKAXE_MODE -> this.useOnAsPickaxe(context);
            default -> super.useOn(context);
        };
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (level.isClientSide || !(livingEntity instanceof ServerPlayer player)) return;

        // 0.5秒 = 10 ticks
        if (getUseDuration(stack, livingEntity) - remainingUseDuration >= 10) {
            // 获取视线方块
            if (player.pick(player.blockInteractionRange(), 0f, false) instanceof BlockHitResult hit) {
                BlockPos pos = hit.getBlockPos();
                BlockState state = level.getBlockState(pos);
                // 检查是否可破坏 (硬度 >= 0)
                if (state.getDestroySpeed(level, pos) >= 0) {
                    Block.dropResources(state, level, pos, level.getBlockEntity(pos), player, stack);
                    level.destroyBlock(pos, false);
                    stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                }
            }
            // 停止使用
            player.stopUsingItem();
        }
    }

    public InteractionResult useOnAsAxe(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (
            context.getHand().equals(InteractionHand.MAIN_HAND)
            && player.getOffhandItem().is(Items.SHIELD)
            && !player.isSecondaryUseActive()
        ) {
            return InteractionResult.PASS;
        }
        Optional<BlockState> optional = Optional.<BlockState>empty()
            .or(() -> {
                Optional<BlockState> optional1 = Optional.ofNullable(state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false));
                optional1.ifPresent(it -> level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F));
                return optional1;
            })
            .or(() -> {
                Optional<BlockState> optional1 = Optional.ofNullable(state.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, false));
                optional1.ifPresent(it -> {
                    level.playSound(player, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.levelEvent(player, 3005, pos, 0);
                });
                return optional1;
            })
            .or(() -> {
                Optional<BlockState> optional1 = Optional.ofNullable(state.getToolModifiedState(context, ItemAbilities.AXE_WAX_OFF, false));
                optional1.ifPresent(it -> {
                    level.playSound(player, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.levelEvent(player, 3004, pos, 0);
                });
                return optional1;
            });
        if (optional.isEmpty()) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        if (player instanceof ServerPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer) player, pos, stack);
        }

        level.setBlock(pos, optional.get(), 11);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, optional.get()));
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResult useOnAsShovel(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (context.getClickedFace() == Direction.DOWN) return InteractionResult.PASS;
        Player player = context.getPlayer();
        BlockState finalState = state;
        Optional<BlockState> optional = Optional.<BlockState>empty()
            .or(() -> {
                Optional<BlockState> optional1 = Optional.ofNullable(
                    finalState.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false));
                optional1.ifPresent(it -> {
                    if (level.getBlockState(pos.above()).isAir()) {
                        level.playSound(player, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                });
                return optional1;
            })
            .or(() -> {
                Optional<BlockState> optional1 = Optional.ofNullable(
                    finalState.getToolModifiedState(context, ItemAbilities.SHOVEL_DOUSE, false));
                optional1.ifPresent(it -> {
                    if (!level.isClientSide()) {
                        level.levelEvent(null, 1009, pos, 0);
                    }
                });
                return optional1;
            });
        if (optional.isEmpty()) return InteractionResult.PASS;

        if (level.isClientSide) return InteractionResult.sidedSuccess(true);
        state = optional.get();
        level.setBlock(pos, state, 11);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
        if (player != null) {
            context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
        }
        return InteractionResult.sidedSuccess(false);
    }

    public InteractionResult useOnAsHoe(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos).getToolModifiedState(context, ItemAbilities.HOE_TILL, false);
        Consumer<UseOnContext> contextConsumer =
            state == null ? null : HoeItem.changeIntoState(state);
        if (contextConsumer == null) return InteractionResult.PASS;

        Player player = context.getPlayer();
        level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        if (level.isClientSide) return InteractionResult.sidedSuccess(true);

        contextConsumer.accept(context);
        if (player != null) {
            context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
        }
        return InteractionResult.sidedSuccess(false);
    }

    @SuppressWarnings("unused")
    public InteractionResult useOnAsPickaxe(UseOnContext context) {
        return InteractionResult.PASS;
    }

    public static void set(ServerPlayer player, InteractionHand hand, @Range(from = 0, to = 4) int mode) {
        ItemStack resonator = player.getItemInHand(hand);
        if (!resonator.is(ModItemTags.RESONATOR)) return;
        Item item = resonator.getItem();
        if (!(item instanceof TieredItem resonatorItem)) return;
        resonator.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(mode));
        resonator.set(DataComponents.TOOL, createToolProperties(mode, resonatorItem.getTier()));
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return switch (ResonatorItem.getMode(stack)) {
            case AXE_MODE -> ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility);
            case SHOVEL_MODE -> ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(itemAbility);
            case HOE_MODE -> ItemAbilities.DEFAULT_HOE_ACTIONS.contains(itemAbility);
            case PICKAXE_MODE -> ItemAbilities.DEFAULT_PICKAXE_ACTIONS.contains(itemAbility);
            default -> false;
        };
    }

    public static class ResonatorHolder extends Holder.Reference<Item> {
        public ResonatorHolder(Holder.Reference.Type type, HolderOwner<Item> owner, ResourceKey<Item> key, Item value) {
            super(type, owner, key, value);
        }

        public boolean is(int mode, TagKey<Item> tagKey) {
            if (mode == AUTO_MODE) return super.is(tagKey);
            return switch (tagKey) {
                case TagKey<Item> tag when tag.equals(ItemTags.AXES) -> super.is(tag) && mode == AXE_MODE;
                case TagKey<Item> tag when tag.equals(ItemTags.SHOVELS) -> super.is(tag) && mode == SHOVEL_MODE;
                case TagKey<Item> tag when tag.equals(ItemTags.HOES) -> super.is(tag) && mode == HOE_MODE;
                case TagKey<Item> tag when tag.equals(ItemTags.PICKAXES) -> super.is(tag) && mode == PICKAXE_MODE;
                default -> super.is(tagKey);
            };
        }

        public boolean is(int mode, HolderSet<Item> holders) {
            if (mode == AUTO_MODE) return holders.contains(this);
            return switch (holders) {
                case HolderSet.Named<Item> h when h.key().equals(ItemTags.AXES) -> h.contains(this) && mode == AXE_MODE;
                case HolderSet.Named<Item> h when h.key().equals(ItemTags.SHOVELS) -> h.contains(this) && mode == SHOVEL_MODE;
                case HolderSet.Named<Item> h when h.key().equals(ItemTags.HOES) -> h.contains(this) && mode == HOE_MODE;
                case HolderSet.Named<Item> h when h.key().equals(ItemTags.PICKAXES) -> h.contains(this) && mode == PICKAXE_MODE;
                default -> holders.contains(this);
            };
        }
    }
}
