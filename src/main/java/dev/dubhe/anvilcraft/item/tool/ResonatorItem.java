package dev.dubhe.anvilcraft.item.tool;

import dev.dubhe.anvilcraft.api.tooltip.providers.IItemTooltipProvider;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentTags;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.network.ResonanceMiningEffectPacket;
import dev.dubhe.anvilcraft.network.WeaponChargeProgressPacket;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;

@Getter
public abstract class ResonatorItem extends Item implements IItemTooltipProvider {
    private static final int USE_DURATION = 72000;
    private static final int STANDARD_RESONANCE_MINING_TICKS = 20;
    private static final int STANDARD_RESONANCE_MINING_DURABILITY_COST = 128;
    private final Map<LivingEntity, MiningTarget> clientMiningTargets = new WeakHashMap<>();
    private final Map<LivingEntity, MiningTarget> serverMiningTargets = new WeakHashMap<>();
    private final ToolMaterial material;
    private final float attackDamage;

    public ResonatorItem(ToolMaterial material, float attackDamage, float attackSpeed, Properties properties) {
        super(
            properties
                .attributes(ResonatorItem.createAttributes(material, attackDamage, attackSpeed))
                .component(DataComponents.TOOL, ResonatorItem.createToolProperties(material, true))
                .component(DataComponents.WEAPON, new Weapon(2, 0.0F))
                .durability(material.durability()).repairable(material.repairItems()).enchantable(material.enchantmentValue())
        );
        this.material = material;
        this.attackDamage = attackDamage;
    }

    @Override
    public void appendItemTooltip(
        ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag tooltipFlag
    ) {
        builder.accept(Component.translatable("tooltip.anvilcraft.resonator.mining_desc",
            Component.keybind("key.anvilcraft.switch_tool_mode")).withStyle(ChatFormatting.GRAY));
    }

    public static ItemAttributeModifiers createAttributes(ToolMaterial material, float attackDamage, float attackSpeed) {
        return ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    Item.BASE_ATTACK_DAMAGE_ID, attackDamage + material.attackDamageBonus(), AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            )
            .build();
    }

    @SuppressWarnings("deprecation")
    public static Tool createToolProperties(ToolMaterial material, boolean isBootstrap) {
        HolderGetter<Block> lookup = isBootstrap
                                     ? BuiltInRegistries.acquireBootstrapRegistrationLookup(BuiltInRegistries.BLOCK)
                                     : BuiltInRegistries.BLOCK;
        List<Tool.Rule> rules = new ArrayList<>();
        rules.add(Tool.Rule.minesAndDrops(HolderSet.direct(Blocks.COBWEB.builtInRegistryHolder()), 15.0F));
        rules.add(Tool.Rule.overrideSpeed(lookup.getOrThrow(BlockTags.SWORD_INSTANTLY_MINES), Float.MAX_VALUE));
        rules.add(Tool.Rule.overrideSpeed(lookup.getOrThrow(BlockTags.SWORD_EFFICIENT), 1.5F));
        rules.add(Tool.Rule.overrideSpeed(lookup.getOrThrow(BlockTags.LEAVES), 15.0F));
        rules.add(Tool.Rule.overrideSpeed(lookup.getOrThrow(BlockTags.WOOL), 5.0F));
        rules.add(Tool.Rule.overrideSpeed(
            HolderSet.direct(Blocks.VINE.builtInRegistryHolder(), Blocks.GLOW_LICHEN.builtInRegistryHolder()),
            2.0F
        ));
        rules.add(Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_AXE), material.speed()));
        rules.add(Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_HOE), material.speed()));
        rules.add(Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_PICKAXE), material.speed()));
        rules.add(Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_SHOVEL), material.speed()));
        return new Tool(List.copyOf(rules), material.speed(), 1, true);
    }

    public static Tool createToolProperties(ResonateMode mode, ToolMaterial material, HolderGetter<Block> lookup) {
        return switch (mode) {
            case AUTO -> ResonatorItem.createToolProperties(material, false);
            case AXE -> new Tool(
                List.of(Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_AXE), material.speed())),
                1.0F,
                1,
                true
            );
            case SHOVEL -> new Tool(
                List.of(Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_SHOVEL), material.speed())),
                1.0F,
                1,
                true
            );
            case HOE -> new Tool(
                List.of(Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_HOE), material.speed())),
                1.0F,
                1,
                true
            );
            case PICKAXE -> new Tool(
                List.of(Tool.Rule.minesAndDrops(lookup.getOrThrow(BlockTags.MINEABLE_WITH_PICKAXE), material.speed())),
                1.0F,
                1,
                true
            );
        };
    }

    public static ResonateMode getMode(ItemInstance stack) {
        return stack.getOrDefault(ModComponents.RESONATE_MODE, ResonateMode.AUTO);
    }

    public static void checkTooDamaged(ToolMaterial material, ItemStack stack, HolderGetter<Block> lookup) {
        Item item = stack.getItem();
        if (!(item instanceof ResonatorItem resonator)) return;
        if (ResonatorItem.isTooDamagedToUse(stack)) {
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
                    if (!entry.modifier().is(Item.BASE_ATTACK_DAMAGE_ID)) {
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
                            Item.BASE_ATTACK_DAMAGE_ID,
                            resonator.attackDamage + material.attackDamageBonus(),
                            AttributeModifier.Operation.ADD_VALUE
                        ),
                        EquipmentSlotGroup.MAINHAND
                    );
                stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
            }
            if (!stack.has(DataComponents.TOOL)) {
                stack.set(DataComponents.TOOL, ResonatorItem.createToolProperties(ResonatorItem.getMode(stack), material, lookup));
            }
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        if (stack.has(DataComponents.UNBREAKABLE)) return;
        ResonatorItem.checkTooDamaged(this.getMaterial(), stack, level.registryAccess().lookupOrThrow(Registries.BLOCK));
    }

    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (ResonatorItem.isTooDamagedToUse(stack)) return 1.0F;
        Tool tool = stack.get(DataComponents.TOOL);
        return tool != null ? tool.getMiningSpeed(state) : this.getMaterial().speed();
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
        ResonateMode mode = ResonatorItem.getMode(stack);
        return switch (mode) {
            case AUTO -> this.canStartResonanceMining(stack)
                ? this.startResonanceMining(context)
                : InteractionResult.PASS;
            case AXE -> this.useOnAsAxe(context);
            case SHOVEL -> this.useOnAsShovel(context);
            case HOE -> this.useOnAsHoe(context);
            case PICKAXE -> this.useOnAsPickaxe(context);
            default -> super.useOn(context);
        };
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return getMode(stack) == ResonateMode.AUTO ? USE_DURATION : super.getUseDuration(stack, entity);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        Map<LivingEntity, MiningTarget> targets = this.miningTargets(level);
        MiningTarget target = targets.get(livingEntity);
        if (target == null) {
            if (!level.isClientSide()) livingEntity.stopUsingItem();
            return;
        }

        BlockHitResult hit = getTargetedBlock(livingEntity);
        if (hit == null || !target.hitPos().equals(hit.getBlockPos()) || !this.canStartResonanceMining(stack)) {
            this.stopResonanceMining(level, livingEntity, target);
            return;
        }

        BlockState state = level.getBlockState(target.hitPos());
        if (!canResonanceMine(state, level, target.hitPos())) {
            this.stopResonanceMining(level, livingEntity, target);
            return;
        }

        int elapsedTicks = this.getUseDuration(stack, livingEntity) - remainingUseDuration;
        if (livingEntity instanceof ServerPlayer player) {
            WeaponChargeProgressPacket.sync(player, stack, elapsedTicks, this.resonanceMiningTicks(), false);
        }
        if (!level.isClientSide() && elapsedTicks % 3 == 0) {
            float pitch = 0.75f + 0.04f * elapsedTicks;
            level.playSound(null, target.hitPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8f, pitch);
        }
        if (elapsedTicks < this.resonanceMiningTicks()) return;

        int damageBeforeMining = stack.getDamageValue();
        boolean destroyed = livingEntity instanceof ServerPlayer player
            && player.gameMode.destroyBlock(target.hitPos());
        targets.remove(livingEntity);
        if (destroyed) {
            this.consumeResonanceMiningDurability(stack, damageBeforeMining);
            level.playSound(null, target.hitPos(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
        }
        sendMiningEffects(level, target.effectPositions(), 0);
        livingEntity.stopUsingItem();
    }

    public InteractionResult useOnAsAxe(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (
            context.getHand().equals(InteractionHand.MAIN_HAND)
            && player.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS)
            && !player.isSecondaryUseActive()
        ) {
            return InteractionResult.PASS;
        }
        Optional<BlockState> newState = Optional.<BlockState>empty()
            .or(() -> {
                Optional<BlockState> newOp = Optional.ofNullable(state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false));
                newOp.ifPresent(_ -> level.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F));
                return newOp;
            })
            .or(() -> {
                Optional<BlockState> newOp = Optional.ofNullable(state.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, false));
                newOp.ifPresent(_ -> {
                    level.playSound(player, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.levelEvent(player, 3005, pos, 0);
                    if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                        BlockPos anotherPos = ChestBlock.getConnectedBlockPos(pos, state);
                        level.gameEvent(GameEvent.BLOCK_CHANGE, anotherPos, GameEvent.Context.of(player, level.getBlockState(anotherPos)));
                        level.levelEvent(player, 3005, anotherPos, 0);
                    }
                });
                return newOp;
            })
            .or(() -> {
                Optional<BlockState> newOp = Optional.ofNullable(state.getToolModifiedState(context, ItemAbilities.AXE_WAX_OFF, false));
                newOp.ifPresent(_ -> {
                    level.playSound(player, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.levelEvent(player, 3004, pos, 0);
                    if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                        BlockPos anotherPos = ChestBlock.getConnectedBlockPos(pos, state);
                        level.gameEvent(GameEvent.BLOCK_CHANGE, anotherPos, GameEvent.Context.of(player, level.getBlockState(anotherPos)));
                        level.levelEvent(player, 3004, anotherPos, 0);
                    }
                });
                return newOp;
            });
        if (newState.isEmpty()) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();
        if (player instanceof ServerPlayer) {
            CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer) player, pos, stack);
        }

        level.setBlock(pos, newState.get(), 11);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState.get()));
        stack.hurtAndBreak(1, player, context.getHand().asEquipmentSlot());

        return InteractionResult.SUCCESS;
    }

    public InteractionResult useOnAsShovel(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (context.getClickedFace() == Direction.DOWN) return InteractionResult.PASS;
        Player player = context.getPlayer();
        Optional<BlockState> newStateOp = Optional.<BlockState>empty()
            .or(() -> {
                Optional<BlockState> newOp = Optional.ofNullable(state.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false));
                newOp.ifPresent(_ -> {
                    if (!level.getBlockState(pos.above()).isAir()) return;
                    level.playSound(player, pos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                });
                return newOp;
            })
            .or(() -> {
                Optional<BlockState> newOp = Optional.ofNullable(state.getToolModifiedState(context, ItemAbilities.SHOVEL_DOUSE, false));
                newOp.ifPresent(_ -> {
                    if (!level.isClientSide()) level.levelEvent(null, 1009, pos, 0);
                });
                return newOp;
            });
        if (newStateOp.isEmpty()) return InteractionResult.PASS;

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockState newState = newStateOp.get();
        level.setBlock(pos, newState, 11);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState));
        if (player != null) context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());

        return InteractionResult.SUCCESS;
    }

    public InteractionResult useOnAsHoe(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos).getToolModifiedState(context, ItemAbilities.HOE_TILL, false);
        if (state == null) return InteractionResult.PASS;
        Consumer<UseOnContext> action = HoeItem.changeIntoState(state);
        Player player = context.getPlayer();
        level.playSound(player, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (level.isClientSide()) return InteractionResult.SUCCESS;
        action.accept(context);
        if (player != null) {
            context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
        }
        return InteractionResult.SUCCESS;
    }

    @SuppressWarnings("unused")
    public InteractionResult useOnAsPickaxe(UseOnContext context) {
        return InteractionResult.PASS;
    }

    public static void setMode(Player player, InteractionHand hand, ResonateMode mode) {
        ItemStack resonator = player.getItemInHand(hand);
        if (!resonator.is(ModItemTags.RESONATOR)) return;
        Item item = resonator.getItem();
        if (!(item instanceof ResonatorItem resonatorItem)) return;
        resonator.set(ModComponents.RESONATE_MODE, mode);
        resonator.set(
            DataComponents.TOOL,
            ResonatorItem.createToolProperties(mode, resonatorItem.getMaterial(), player.registryAccess().lookupOrThrow(Registries.BLOCK))
        );
    }

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
        return switch (ResonatorItem.getMode(stack)) {
            case AXE -> ItemAbilities.DEFAULT_AXE_ACTIONS.contains(itemAbility);
            case SHOVEL -> ItemAbilities.DEFAULT_SHOVEL_ACTIONS.contains(itemAbility);
            case HOE -> ItemAbilities.DEFAULT_HOE_ACTIONS.contains(itemAbility);
            default -> false;
        };
    }

    public static boolean allowsToolTag(ResonateMode mode, TagKey<?> tag) {
        if (mode == ResonateMode.AUTO) return true;
        if (tag.equals(ItemTags.AXES)) return mode == ResonateMode.AXE;
        if (tag.equals(ItemTags.SHOVELS)) return mode == ResonateMode.SHOVEL;
        if (tag.equals(ItemTags.HOES)) return mode == ResonateMode.HOE;
        if (tag.equals(ItemTags.PICKAXES)) return mode == ResonateMode.PICKAXE;
        return true;
    }

    public int resonanceMiningTicks() {
        return STANDARD_RESONANCE_MINING_TICKS;
    }

    protected int resonanceMiningDurabilityCost() {
        return STANDARD_RESONANCE_MINING_DURABILITY_COST;
    }

    private boolean canStartResonanceMining(ItemStack stack) {
        if (isTooDamagedToUse(stack)) return false;
        int durabilityCost = this.resonanceMiningDurabilityCost();
        return durabilityCost <= 0 || stack.getMaxDamage() - stack.getDamageValue() > durabilityCost;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (getMode(stack) == ResonateMode.AUTO && this.canStartResonanceMining(stack)) {
            return InteractionResult.FAIL;
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (getMode(stack) != ResonateMode.AUTO || !this.canStartResonanceMining(stack)) {
            return super.onItemUseFirst(stack, context);
        }
        return this.startResonanceMining(context);
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
        this.miningTargets(level).put(player, target);
        player.startUsingItem(context.getHand());
        sendMiningEffects(level, target.effectPositions(), this.resonanceMiningTicks() + 2);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return getMode(stack) == ResonateMode.AUTO ? ItemUseAnimation.CROSSBOW : ItemUseAnimation.NONE;
    }

    public static float resonanceMiningProgress(Level level, Player player, float partialTick) {
        if (!(player.getUseItem().getItem() instanceof ResonatorItem resonator)) return -1.0F;
        if (!resonator.miningTargets(level).containsKey(player)) return -1.0F;

        ItemStack stack = player.getUseItem();
        int elapsedTicks = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
        return Math.min(1.0F, (elapsedTicks + partialTick) / resonator.resonanceMiningTicks());
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        MiningTarget target = this.miningTargets(level).remove(livingEntity);
        if (target == null) return false;

        sendMiningEffects(level, target.effectPositions(), 0);
        int elapsedTicks = this.getUseDuration(stack, livingEntity) - remainingUseDuration;
        if (elapsedTicks >= this.resonanceMiningTicks() || !(livingEntity instanceof Player player)) return false;

        BlockHitResult hit = getTargetedBlock(player);
        if (hit == null || !target.hitPos().equals(hit.getBlockPos())) return false;
        AnvilHammerItem.interactWithBlock(
            player,
            target.hitPos(),
            level,
            stack,
            target.hand(),
            target.hitResult()
        );
        return false;
    }

    private void consumeResonanceMiningDurability(ItemStack stack, int damageBeforeMining) {
        int durabilityCost = this.resonanceMiningDurabilityCost();
        if (durabilityCost <= 0) return;
        // 直接写入损伤值，避免耐久附魔改变本次消耗。
        stack.setDamageValue(damageBeforeMining + durabilityCost);
    }

    private Map<LivingEntity, MiningTarget> miningTargets(Level level) {
        return level.isClientSide() ? this.clientMiningTargets : this.serverMiningTargets;
    }

    public static boolean isResonanceMining(Level level, Player player, BlockPos pos) {
        if (!(player.getUseItem().getItem() instanceof ResonatorItem resonator)) return false;
        MiningTarget target = resonator.miningTargets(level).get(player);
        return target != null && target.hitPos().equals(pos);
    }

    private void stopResonanceMining(Level level, LivingEntity livingEntity, MiningTarget target) {
        this.miningTargets(level).remove(livingEntity);
        sendMiningEffects(level, target.effectPositions(), 0);
        livingEntity.stopUsingItem();
    }

    private static List<BlockPos> getEffectPositions(Level level, BlockPos hitPos) {
        BlockState state = level.getBlockState(hitPos);
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
            new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4),
            new ResonanceMiningEffectPacket(pos, durationTicks)
        );
    }

    private static @Nullable BlockHitResult getTargetedBlock(LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) return null;
        HitResult hit = player.pick(player.blockInteractionRange(), 0.0f, false);
        return hit.getType() == HitResult.Type.BLOCK ? (BlockHitResult) hit : null;
    }

    static boolean canResonanceMine(BlockState state, Level level, BlockPos pos) {
        if (state.isAir()) return false;
        return state.getDestroySpeed(level, pos) >= 0.0f;
    }

    private record MiningTarget(BlockHitResult hitResult, InteractionHand hand, List<BlockPos> effectPositions) {
        private BlockPos hitPos() {
            return this.hitResult.getBlockPos();
        }
    }

    public static class ResonatorHolder extends Holder.Reference<Item> {
        public ResonatorHolder(Holder.Reference.Type type, HolderOwner<Item> owner, ResourceKey<Item> key, Item value) {
            super(type, owner, key, value);
        }

        public boolean is(ResonateMode mode, TagKey<Item> tag) {
            return super.is(tag) && allowsToolTag(mode, tag);
        }

    }
}
