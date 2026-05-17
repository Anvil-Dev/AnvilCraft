package dev.dubhe.anvilcraft.item.tool;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.MagnetUtil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Range;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class MultitoolItem extends Item {
    public static final int ALL_MODE = 0;
    public static final int SHEARS_MODE = 1;
    public static final int FLINT_AND_STEEL_MODE = 2;
    public static final int BRUSH_MODE = 3;
    public static final int SPYGLASS_MODE = 4;
    public static final int MAGNET_MODE = 5;
    public static final int FISHING_ROD_MODE = 6;
    public static final int CARROT_ON_A_STICK_MODE = 7;
    public static final int WARPED_FUNGUS_ON_A_STICK_MODE = 8;

    public MultitoolItem(Properties properties) {
        super(properties.enchantable(1));
    }

    public static boolean isHolding(Player player, Item item) {
        return MultitoolItem.getMode(player.getMainHandItem()).isActing(item)
               || MultitoolItem.getMode(player.getOffhandItem()).isActing(item);
    }

    public static boolean isHolding(Player player, MultitoolMode mode) {
        return MultitoolItem.getMode(player.getMainHandItem()) == mode || MultitoolItem.getMode(player.getOffhandItem()) == mode;
    }

    public static boolean isActingAs(ItemStack stack, MultitoolMode mode) {
        return MultitoolItem.getMode(stack) == mode;
    }

    public static MultitoolMode getMode(ItemInstance item) {
        return item.getOrDefault(ModComponents.MULTITOOL_MODE, MultitoolMode.ALL);
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (MultitoolItem.isActingAs(stack, MultitoolMode.SPYGLASS)) {
            this.stopUsing(livingEntity);
            return true;
        }
        return super.releaseUsing(stack, level, livingEntity, timeCharged);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (MultitoolItem.isActingAs(stack, MultitoolMode.SPYGLASS)) {
            this.stopUsing(livingEntity);
            return stack;
        }
        return super.finishUsingItem(stack, level, livingEntity);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        return switch (MultitoolItem.getMode(player.getItemInHand(usedHand))) {
            case SPYGLASS -> {
                player.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 1.0F);
                player.awardStat(Stats.ITEM_USED.get(this));
                yield ItemUtils.startUsingInstantly(level, player, usedHand);
            }
            case MAGNET -> this.useAsMagnet(level, player, usedHand);
            case FISHING_ROD -> this.useAsFishingRod(level, player, usedHand);
            case CARROT_ON_A_STICK -> this.useAsCarrotOnAStick(level, player, usedHand);
            case WARPED_FUNGUS_ON_A_STICK -> this.useAsWarpedFungusOnAStick(level, player, usedHand);
            case ALL -> {
                // noinspection deprecation
                player.hurtOrSimulate(level.damageSources().playerAttack(player), 1);
                yield InteractionResult.PASS;
            }
            default -> super.use(level, player, usedHand);
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return switch (MultitoolItem.getMode(context.getItemInHand())) {
            case SHEARS -> this.useOnAsShears(context);
            case FLINT_AND_STEEL -> this.useOnAsFlintAndSteel(context);
            case BRUSH -> this.useOnAsBrush(context);
            case MAGNET -> this.useOnAsMagnet(context);
            default -> super.useOn(context);
        };
    }

    @Override
    public boolean canPerformAction(ItemInstance stack, ItemAbility itemAbility) {
        return switch (MultitoolItem.getMode(stack)) {
            case SHEARS -> ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(itemAbility);
            case FLINT_AND_STEEL -> ItemAbilities.DEFAULT_FLINT_ACTIONS.contains(itemAbility);
            case BRUSH -> ItemAbilities.DEFAULT_BRUSH_ACTIONS.contains(itemAbility);
            case FISHING_ROD -> ItemAbilities.DEFAULT_FISHING_ROD_ACTIONS.contains(itemAbility);
            default -> super.canPerformAction(stack, itemAbility);
        };
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return switch (MultitoolItem.getMode(stack)) {
            case BRUSH -> ItemUseAnimation.BRUSH;
            case SPYGLASS -> ItemUseAnimation.SPYGLASS;
            default -> super.getUseAnimation(stack);
        };
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return switch (MultitoolItem.getMode(stack)) {
            case BRUSH -> 200;
            case SPYGLASS -> 1200;
            default -> super.getUseDuration(stack, entity);
        };
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (MultitoolItem.isActingAs(stack, MultitoolMode.BRUSH)) {
            this.onUseTickAsBrush(level, livingEntity, stack, remainingUseDuration);
        } else {
            super.onUseTick(level, livingEntity, stack, remainingUseDuration);
        }
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if (MultitoolItem.isActingAs(stack, MultitoolMode.SHEARS)) {
            return this.mineBlockAsShears(stack, level, state, miningEntity);
        } else {
            return super.mineBlock(stack, level, state, pos, miningEntity);
        }
    }

    @Override
    public InteractionResult interactLivingEntity(
        ItemStack stack,
        Player player,
        LivingEntity interactionTarget,
        InteractionHand usedHand
    ) {
        if (MultitoolItem.isActingAs(stack, MultitoolMode.SHEARS)) {
            return this.interactLivingEntityAsShears(stack, player, interactionTarget, usedHand);
        } else {
            return super.interactLivingEntity(stack, player, interactionTarget, usedHand);
        }
    }

    private InteractionResult useAsMagnet(Level level, Player player, InteractionHand usedHand) {
        return MagnetUtil.magnetizeItems(level, player, usedHand);
    }

    private InteractionResult useAsFishingRod(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (player.fishing != null) {
            if (!level.isClientSide()) {
                int i = player.fishing.retrieve(itemstack);
                ItemStack original = itemstack.copy();
                itemstack.hurtAndBreak(i, player, usedHand);
                if (itemstack.isEmpty()) {
                    net.neoforged.neoforge.event.EventHooks.onPlayerDestroyItem(player, original, usedHand);
                }
            }

            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.NEUTRAL,
                1.0F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
            );
            player.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
        } else {
            level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.FISHING_BOBBER_THROW,
                SoundSource.NEUTRAL,
                0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
            );
            if (level instanceof ServerLevel serverlevel) {
                int j = (int) (EnchantmentHelper.getFishingTimeReduction(serverlevel, itemstack, player) * 20.0F);
                int k = EnchantmentHelper.getFishingLuckBonus(serverlevel, itemstack, player);
                level.addFreshEntity(new FishingHook(player, level, k, j));
            }

            player.awardStat(Stats.ITEM_USED.get(this));
            player.gameEvent(GameEvent.ITEM_INTERACT_START);
        }
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult useAsCarrotOnAStick(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        if (!level.isClientSide()) {
            Entity entity = player.getControlledVehicle();
            if (player.isPassenger() && entity instanceof ItemSteerable itemSteerable) {
                if (entity.getType() == EntityType.PIG && itemSteerable.boost()) {
                    itemStack.hurtAndBreak(7, player, usedHand);
                    return InteractionResult.SUCCESS;
                }
            }
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResult.PASS;
    }

    private InteractionResult useAsWarpedFungusOnAStick(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        if (!level.isClientSide()) {
            Entity entity = player.getControlledVehicle();
            if (player.isPassenger() && entity instanceof ItemSteerable itemSteerable) {
                if (entity.getType() == EntityType.STRIDER && itemSteerable.boost()) {
                    itemStack.hurtAndBreak(1, player, usedHand);
                    return InteractionResult.SUCCESS;
                }
            }
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResult.PASS;
    }

    public InteractionResult useOnAsShears(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        BlockState blockstate1 = blockstate.getToolModifiedState(context, ItemAbilities.SHEARS_TRIM, false);
        if (blockstate1 != null) {
            Player player = context.getPlayer();
            ItemStack itemstack = context.getItemInHand();
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, blockpos, itemstack);
            }
            level.setBlockAndUpdate(blockpos, blockstate1);
            level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(context.getPlayer(), blockstate1));
            if (player != null) {
                itemstack.hurtAndBreak(1, player, context.getHand());
            }

            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
        return super.useOn(context);
    }

    public InteractionResult useOnAsFlintAndSteel(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        BlockState blockstate2 = blockstate.getToolModifiedState(context, ItemAbilities.FIRESTARTER_LIGHT, false);
        if (blockstate2 == null) {
            BlockPos blockpos1 = blockpos.relative(context.getClickedFace());
            if (BaseFireBlock.canBePlacedAt(level, blockpos1, context.getHorizontalDirection())) {
                level.playSound(
                    player,
                    blockpos1,
                    SoundEvents.FLINTANDSTEEL_USE,
                    SoundSource.BLOCKS,
                    1.0F,
                    level.getRandom().nextFloat() * 0.4F + 0.8F
                );
                BlockState blockstate1 = BaseFireBlock.getState(level, blockpos1);
                level.setBlock(blockpos1, blockstate1, 11);
                level.gameEvent(player, GameEvent.BLOCK_PLACE, blockpos);
                ItemStack itemstack = context.getItemInHand();
                if (player instanceof ServerPlayer serverPlayer) {
                    CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, blockpos1, itemstack);
                    itemstack.hurtAndBreak(1, player, context.getHand());
                }

                return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.FAIL;
        } else {
            level.playSound(
                player,
                blockpos,
                SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.BLOCKS,
                1.0F,
                level.getRandom().nextFloat() * 0.4F + 0.8F
            );
            level.setBlock(blockpos, blockstate2, 11);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, blockpos);
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, context.getHand());
            }

            return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
        }
    }

    public InteractionResult useOnAsBrush(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && this.calculateHitResult(player).getType() == HitResult.Type.BLOCK) {
            player.startUsingItem(context.getHand());
        }

        return InteractionResult.CONSUME;
    }

    public InteractionResult useOnAsMagnet(UseOnContext context) {
        return MagnetUtil.placeMagnetizedNode(context);
    }

    private HitResult calculateHitResult(Player player) {
        return ProjectileUtil.getHitResultOnViewVector(
            player,
            entity -> !entity.isSpectator() && player.isPickable(),
            player.blockInteractionRange()
        );
    }

    private void spawnDustParticles(Level level, BlockHitResult hitResult, BlockState state, Vec3 pos, HumanoidArm arm) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        int j = level.getRandom().nextInt(7, 12);
        BlockParticleOption blockparticleoption = new BlockParticleOption(ParticleTypes.BLOCK, state);
        Direction direction = hitResult.getDirection();
        DustParticlesDelta dustparticlesdelta = DustParticlesDelta.fromDirection(pos, direction);
        Vec3 vec3 = hitResult.getLocation();
        for (int k = 0; k < j; ++k) {
            level.addParticle(
                blockparticleoption,
                vec3.x - (double) (direction == Direction.WEST ? 1.0E-6F : 0.0F),
                vec3.y,
                vec3.z - (double) (direction == Direction.NORTH ? 1.0E-6F : 0.0F),
                dustparticlesdelta.xd() * (double) i * (double) 3.0F * level.getRandom().nextDouble(),
                0.0F,
                dustparticlesdelta.zd() * (double) i * (double) 3.0F * level.getRandom().nextDouble()
            );
        }
    }

    private void onUseTickAsBrush(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (remainingUseDuration < 0 || !(livingEntity instanceof Player player)) {
            livingEntity.releaseUsingItem();
            return;
        }
        HitResult result = this.calculateHitResult(player);
        if (!(result instanceof BlockHitResult blockhitresult)) {
            livingEntity.releaseUsingItem();
            return;
        }
        if (result.getType() != HitResult.Type.BLOCK) {
            livingEntity.releaseUsingItem();
            return;
        }
        int i = this.getUseDuration(stack, livingEntity) - remainingUseDuration + 1;
        if (i % 10 != 5) {
            livingEntity.releaseUsingItem();
            return;
        }
        BlockPos pos = blockhitresult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        HumanoidArm arm = livingEntity.getUsedItemHand() == InteractionHand.MAIN_HAND
                          ? player.getMainArm()
                          : player.getMainArm().getOpposite();
        if (state.shouldSpawnTerrainParticles() && state.getRenderShape() != RenderShape.INVISIBLE) {
            this.spawnDustParticles(level, blockhitresult, state, livingEntity.getViewVector(0.0F), arm);
        }

        SoundEvent event;
        if (state.getBlock() instanceof BrushableBlock brushableBlock) {
            event = brushableBlock.getBrushSound();
        } else {
            event = SoundEvents.BRUSH_GENERIC;
        }

        level.playSound(player, pos, event, SoundSource.BLOCKS);
        if (!(level instanceof ServerLevel serverLevel)) return;
        if (!(level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable)) return;
        if (!brushable.brush(level.getGameTime(), serverLevel, player, blockhitresult.getDirection(), stack)) return;
        EquipmentSlot equipmentslot = stack.equals(player.getItemBySlot(EquipmentSlot.OFFHAND))
                                      ? EquipmentSlot.OFFHAND
                                      : EquipmentSlot.MAINHAND;
        stack.hurtAndBreak(1, livingEntity, equipmentslot);
    }

    private boolean mineBlockAsShears(ItemStack stack, Level level, BlockState state, LivingEntity miningEntity) {
        if (!level.isClientSide() && !state.is(BlockTags.FIRE)) stack.hurtAndBreak(1, miningEntity, EquipmentSlot.MAINHAND);
        return state.is(BlockTags.LEAVES)
               || state.is(Blocks.COBWEB)
               || state.is(Blocks.SHORT_GRASS)
               || state.is(Blocks.FERN)
               || state.is(Blocks.DEAD_BUSH)
               || state.is(Blocks.HANGING_ROOTS)
               || state.is(Blocks.VINE)
               || state.is(Blocks.TRIPWIRE)
               || state.is(BlockTags.WOOL);
    }

    private InteractionResult interactLivingEntityAsShears(
        ItemStack stack,
        Player player,
        LivingEntity interactionTarget,
        InteractionHand usedHand
    ) {
        if (interactionTarget instanceof IShearable target) {
            BlockPos pos = interactionTarget.blockPosition();
            boolean isClient = interactionTarget.level().isClientSide();
            if (target.isShearable(player, stack, interactionTarget.level(), pos)) {
                List<ItemStack> drops = target.onSheared(player, stack, interactionTarget.level(), pos);
                if (interactionTarget.level() instanceof ServerLevel serverLevel) {
                    for (ItemStack drop : drops) {
                        target.spawnShearedDrop(serverLevel, pos, drop);
                    }
                }

                interactionTarget.gameEvent(GameEvent.SHEAR, player);
                if (!isClient) {
                    stack.hurtAndBreak(1, player, usedHand);
                }

                return isClient ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
            }
        }

        return InteractionResult.PASS;
    }

    public static void setMode(Player player, InteractionHand hand, @Range(from = 0, to = 8) int mode) {
        ItemStack item = player.getItemInHand(hand);
        if (!item.is(ModItems.MULTITOOL_ITEM)) {
            return;
        }
        item.remove(DataComponents.TOOL);
        item.set(ModComponents.MULTITOOL_MODE, MultitoolMode.values()[mode]);
        if (mode == SHEARS_MODE) item.set(DataComponents.TOOL, ShearsItem.createToolProperties());
    }

    private void stopUsing(LivingEntity entity) {
        entity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0F, 1.0F);
    }

    record DustParticlesDelta(double xd, double yd, double zd) {
        public static DustParticlesDelta fromDirection(Vec3 pos, Direction direction) {
            return switch (direction) {
                case DOWN, UP -> new DustParticlesDelta(pos.z(), 0.0F, -pos.x());
                case NORTH -> new DustParticlesDelta(1.0F, 0.0F, -0.1);
                case SOUTH -> new DustParticlesDelta(-1.0F, 0.0F, 0.1);
                case WEST -> new DustParticlesDelta(-0.1, 0.0F, -1.0F);
                case EAST -> new DustParticlesDelta(0.1, 0.0F, 1.0F);
            };
        }
    }

    public static class MultitoolHolder extends Holder.Reference<Item> {
        public MultitoolHolder(Type type, HolderOwner<Item> owner, @Nullable ResourceKey<Item> key, @Nullable Item value) {
            super(type, owner, key, value);
        }

        public boolean is(int mode, TagKey<Item> tagKey) {
            return switch (tagKey) {
                case TagKey<Item> tag when tag.equals(Tags.Items.TOOLS_SHEAR) -> super.is(tag) && mode == SHEARS_MODE;
                case TagKey<Item> tag when tag.equals(ItemTags.CREEPER_IGNITERS) -> super.is(tag) && mode == FLINT_AND_STEEL_MODE;
                case TagKey<Item> tag when tag.equals(Tags.Items.TOOLS_IGNITER) -> super.is(tag) && mode == FLINT_AND_STEEL_MODE;
                case TagKey<Item> tag when tag.equals(Tags.Items.TOOLS_BRUSH) -> super.is(tag) && mode == BRUSH_MODE;
                case TagKey<Item> tag when tag.equals(Tags.Items.TOOLS_FISHING_ROD) -> super.is(tag) && mode == FISHING_ROD_MODE;
                case TagKey<Item> tag when tag.equals(ItemTags.STRIDER_TEMPT_ITEMS) -> super.is(tag)
                                                                                       && mode == WARPED_FUNGUS_ON_A_STICK_MODE;
                default -> super.is(tagKey);
            };
        }

        public boolean is(int mode, HolderSet<Item> holders) {
            return switch (holders) {
                case HolderSet.Named<Item> holderSet when holderSet.key().equals(Tags.Items.TOOLS_SHEAR) ->
                holderSet.contains(this) && mode == SHEARS_MODE;
                case HolderSet.Named<Item> holderSet when holderSet.key().equals(ItemTags.CREEPER_IGNITERS) ->
                holderSet.contains(this) && mode == FLINT_AND_STEEL_MODE;
                case HolderSet.Named<Item> holderSet when holderSet.key().equals(Tags.Items.TOOLS_IGNITER) ->
                holderSet.contains(this) && mode == FLINT_AND_STEEL_MODE;
                case HolderSet.Named<Item> holderSet when holderSet.key().equals(Tags.Items.TOOLS_BRUSH) ->
                holderSet.contains(this) && mode == BRUSH_MODE;
                case HolderSet.Named<Item> holderSet when holderSet.key().equals(Tags.Items.TOOLS_FISHING_ROD) ->
                holderSet.contains(this) && mode == FISHING_ROD_MODE;
                case HolderSet.Named<Item> holderSet when holderSet.key().equals(ItemTags.STRIDER_TEMPT_ITEMS) ->
                holderSet.contains(this) && mode == WARPED_FUNGUS_ON_A_STICK_MODE;
                default -> holders.contains(this);
            };
        }
    }
}
