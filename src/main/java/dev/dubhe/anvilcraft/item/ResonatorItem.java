package dev.dubhe.anvilcraft.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.client.renderer.item.ItemUseAnimationTransform;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentTags;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.network.ResonanceMiningEffectPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.HumanoidArm;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public abstract class ResonatorItem extends TieredItem {
    public static final int AUTO_MODE = 0;
    public static final int AXE_MODE = 1;
    public static final int SHOVEL_MODE = 2;
    public static final int HOE_MODE = 3;
    public static final int PICKAXE_MODE = 4;
    private static final int USE_DURATION = 72000;
    private static final int STANDARD_RESONANCE_MINING_TICKS = 20;
    private static final int STANDARD_RESONANCE_MINING_DURABILITY_COST = 128;

    private final Map<LivingEntity, MiningTarget> clientMiningTargets = new WeakHashMap<>();
    private final Map<LivingEntity, MiningTarget> serverMiningTargets = new WeakHashMap<>();

    public ResonatorItem(Tier tier, Properties properties) {
        super(
            tier,
            properties.component(DataComponents.TOOL, createToolProperties(tier))
        );
    }

    @Override
    @SuppressWarnings("removal")
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
                return ItemUseAnimationTransform.applyCrossbowCharge(
                    poseStack,
                    player,
                    arm,
                    stack,
                    partialTick,
                    equipProgress,
                    resonanceMiningTicks()
                );
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(
            Component.translatable(
                "tooltip.anvilcraft.resonator.mining_desc",
                Component.keybind("key.anvilcraft.switch_tool_mode")
            ).withStyle(ChatFormatting.GRAY)
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
                for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
                    if (!entry.modifier().is(BASE_ATTACK_DAMAGE_ID)) {
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

    protected int resonanceMiningTicks() {
        return STANDARD_RESONANCE_MINING_TICKS;
    }

    protected int resonanceMiningDurabilityCost() {
        return STANDARD_RESONANCE_MINING_DURABILITY_COST;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!stack.has(DataComponents.UNBREAKABLE)) checkTooDamaged(this.getTier(), stack);
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

    private boolean canStartResonanceMining(ItemStack stack) {
        if (isTooDamagedToUse(stack)) return false;
        int durabilityCost = resonanceMiningDurabilityCost();
        return durabilityCost <= 0 || stack.getMaxDamage() - stack.getDamageValue() > durabilityCost;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (getMode(stack) == AUTO_MODE && canStartResonanceMining(stack)) {
            return InteractionResultHolder.fail(stack);
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (getMode(stack) != AUTO_MODE || !canStartResonanceMining(stack)) {
            return super.onItemUseFirst(stack, context);
        }
        return startResonanceMining(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        int mode = ResonatorItem.getMode(stack);
        return switch (mode) {
            case AUTO_MODE -> canStartResonanceMining(stack)
                ? startResonanceMining(context)
                : InteractionResult.PASS;
            case AXE_MODE -> this.useOnAsAxe(context);
            case SHOVEL_MODE -> this.useOnAsShovel(context);
            case HOE_MODE -> this.useOnAsHoe(context);
            case PICKAXE_MODE -> this.useOnAsPickaxe(context);
            default -> super.useOn(context);
        };
    }

    private InteractionResult startResonanceMining(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!canResonanceMine(level.getBlockState(pos), level, pos)) return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        BlockHitResult hitResult = new BlockHitResult(
            context.getClickLocation(),
            context.getClickedFace(),
            pos.immutable(),
            context.isInside()
        );
        MiningTarget target = new MiningTarget(hitResult, context.getHand(), getEffectPositions(level, pos));
        miningTargets(level).put(player, target);
        player.startUsingItem(context.getHand());
        sendMiningEffects(level, target.effectPositions(), resonanceMiningTicks() + 2);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return getMode(stack) == AUTO_MODE ? USE_DURATION : super.getUseDuration(stack, entity);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return getMode(stack) == AUTO_MODE ? UseAnim.CROSSBOW : UseAnim.NONE;
    }

    public static float resonanceMiningProgress(Level level, Player player, float partialTick) {
        if (!(player.getUseItem().getItem() instanceof ResonatorItem resonator)) return -1.0F;
        if (!resonator.miningTargets(level).containsKey(player)) return -1.0F;

        ItemStack stack = player.getUseItem();
        int elapsedTicks = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
        return Math.min(1.0F, (elapsedTicks + partialTick) / resonator.resonanceMiningTicks());
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        Map<LivingEntity, MiningTarget> targets = miningTargets(level);
        MiningTarget target = targets.get(livingEntity);
        if (target == null) {
            if (!level.isClientSide) livingEntity.stopUsingItem();
            return;
        }

        BlockHitResult hit = getTargetedBlock(livingEntity);
        if (hit == null || !target.hitPos().equals(hit.getBlockPos()) || !canStartResonanceMining(stack)) {
            stopResonanceMining(level, livingEntity, target);
            return;
        }

        BlockState state = level.getBlockState(target.hitPos());
        if (!canResonanceMine(state, level, target.hitPos())) {
            stopResonanceMining(level, livingEntity, target);
            return;
        }

        int elapsedTicks = getUseDuration(stack, livingEntity) - remainingUseDuration;
        if (!level.isClientSide && elapsedTicks % 3 == 0) {
            float pitch = 0.75f + 0.04f * elapsedTicks;
            level.playSound(null, target.hitPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8f, pitch);
        }
        if (elapsedTicks < resonanceMiningTicks()) return;

        int damageBeforeMining = stack.getDamageValue();
        boolean destroyed = livingEntity instanceof ServerPlayer player
            && player.gameMode.destroyBlock(target.hitPos());
        targets.remove(livingEntity);
        if (destroyed) {
            consumeResonanceMiningDurability(stack, damageBeforeMining);
            level.playSound(null, target.hitPos(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
        }
        sendMiningEffects(level, target.effectPositions(), 0);
        livingEntity.stopUsingItem();
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        MiningTarget target = miningTargets(level).remove(livingEntity);
        if (target == null) return;

        sendMiningEffects(level, target.effectPositions(), 0);
        int elapsedTicks = getUseDuration(stack, livingEntity) - remainingUseDuration;
        if (elapsedTicks >= resonanceMiningTicks() || !(livingEntity instanceof Player player)) return;

        BlockHitResult hit = getTargetedBlock(player);
        if (hit == null || !target.hitPos().equals(hit.getBlockPos())) return;
        AnvilHammerItem.interactWithBlock(
            player,
            target.hitPos(),
            level,
            stack,
            target.hand(),
            target.hitResult()
        );
    }

    private void consumeResonanceMiningDurability(ItemStack stack, int damageBeforeMining) {
        int durabilityCost = resonanceMiningDurabilityCost();
        if (durabilityCost <= 0) return;
        // 直接写入损伤值，避免耐久附魔改变本次消耗。
        stack.setDamageValue(damageBeforeMining + durabilityCost);
    }

    private Map<LivingEntity, MiningTarget> miningTargets(Level level) {
        return level.isClientSide ? this.clientMiningTargets : this.serverMiningTargets;
    }

    public static boolean isResonanceMining(Level level, Player player, BlockPos pos) {
        if (!(player.getUseItem().getItem() instanceof ResonatorItem resonator)) return false;
        MiningTarget target = resonator.miningTargets(level).get(player);
        return target != null && target.hitPos().equals(pos);
    }

    private void stopResonanceMining(Level level, LivingEntity livingEntity, MiningTarget target) {
        miningTargets(level).remove(livingEntity);
        sendMiningEffects(level, target.effectPositions(), 0);
        livingEntity.stopUsingItem();
    }

    private static List<BlockPos> getEffectPositions(Level level, BlockPos hitPos) {
        BlockState state = level.getBlockState(hitPos);
        if (state.is(ModBlocks.LARGE_CAKE)) return List.of(hitPos.immutable());
        if (!(state.getBlock() instanceof AbstractMultiPartBlock<?> multiPartBlock)) {
            return List.of(hitPos.immutable());
        }

        return getMultiPartEffectPositions(level, hitPos, state, multiPartBlock);
    }

    private static <P extends Enum<P>> List<BlockPos> getMultiPartEffectPositions(
        Level level,
        BlockPos hitPos,
        BlockState hitState,
        AbstractMultiPartBlock<P> multiPartBlock
    ) {
        List<BlockPos> positions = new ArrayList<>();
        for (P part : multiPartBlock.getParts()) {
            BlockPos partPos = hitPos.offset(multiPartBlock.offsetFrom(hitState, part));
            if (level.getBlockState(partPos).is(multiPartBlock)) positions.add(partPos.immutable());
        }
        return positions.isEmpty() ? List.of(hitPos.immutable()) : List.copyOf(positions);
    }

    private static void sendMiningEffects(Level level, List<BlockPos> positions, int durationTicks) {
        for (BlockPos pos : positions) {
            sendMiningEffect(level, pos, durationTicks);
        }
    }

    private static void sendMiningEffect(Level level, BlockPos pos, int durationTicks) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel,
            new ChunkPos(pos),
            new ResonanceMiningEffectPacket(pos, durationTicks)
        );
    }

    private static BlockHitResult getTargetedBlock(LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) return null;
        HitResult hit = player.pick(player.blockInteractionRange(), 0.0f, false);
        return hit.getType() == HitResult.Type.BLOCK ? (BlockHitResult) hit : null;
    }

    static boolean canResonanceMine(BlockState state, Level level, BlockPos pos) {
        if (state.isAir()) return false;
        return state.getDestroySpeed(level, pos) >= 0.0f;
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

    public static void setMode(Player player, InteractionHand hand, @Range(from = 0, to = 4) int mode) {
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

    private record MiningTarget(
        BlockHitResult hitResult,
        InteractionHand hand,
        List<BlockPos> effectPositions
    ) {
        private BlockPos hitPos() {
            return hitResult.getBlockPos();
        }
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
