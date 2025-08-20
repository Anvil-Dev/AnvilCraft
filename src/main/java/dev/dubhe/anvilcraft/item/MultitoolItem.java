package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.UseMagnetEvent;
import dev.dubhe.anvilcraft.api.item.IMultipleResult;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.init.ModEntities;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.recipe.multiple.MultipleToOneSmithingRecipeInput;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.common.IShearable;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MultitoolItem extends Item implements IMultipleResult {
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
        super(properties);
    }

    @SuppressWarnings("removal")
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ItemProperties.register(
            this, ResourceLocation.withDefaultNamespace("cast"), (itemStack, level, entity, value) -> {
                if (entity == null) {
                    return 0.0f;
                } else {
                    boolean flag = itemStack.is(entity.getMainHandItem().getItem());
                    boolean flag1 = itemStack.is(entity.getOffhandItem().getItem());
                    if (entity.getMainHandItem().getItem() instanceof MultitoolItem
                    && getMode(entity.getMainHandItem()) == FISHING_ROD_MODE) {
                        flag1 = false;
                    }
                    return (flag || flag1) && entity instanceof Player && ((Player) entity).fishing != null ? 1.0f : 0.0f;
                }
            });
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        if (getMode(stack) == SPYGLASS_MODE) {
            this.stopUsing(livingEntity);
        } else {
            super.releaseUsing(stack, level, livingEntity, timeCharged);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (getMode(stack) == SPYGLASS_MODE) {
            this.stopUsing(livingEntity);
            return stack;
        } else {
            return super.finishUsingItem(stack, level, livingEntity);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        return switch (getMode(player.getItemInHand(usedHand))) {
            case SPYGLASS_MODE -> {
                player.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 1.0F);
                player.awardStat(Stats.ITEM_USED.get(this));
                yield  ItemUtils.startUsingInstantly(level, player, usedHand);
            }
            case MAGNET_MODE -> useAsMagnet(level, player, usedHand);
            case FISHING_ROD_MODE -> useAsFishingRod(level, player, usedHand);
            case CARROT_ON_A_STICK_MODE -> useAsCarrotOnAStick(level, player, usedHand);
            case WARPED_FUNGUS_ON_A_STICK_MODE -> useAsWarpedFungusOnAStick(level, player, usedHand);
            case ALL_MODE -> {
                player.hurt(level.damageSources().playerAttack(player), 1);
                yield InteractionResultHolder.pass(player.getItemInHand(usedHand));
            }
            default -> super.use(level, player, usedHand);
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return switch (context.getItemInHand().getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT).value()) {
            case SHEARS_MODE -> useOnAsShears(context);
            case FLINT_AND_STEEL_MODE -> useOnAsFlintAndSteel(context);
            case BRUSH_MODE -> useOnAsBrush(context);
            case MAGNET_MODE -> useOnAsMagnet(context);
            default -> super.useOn(context);
        };
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility itemAbility) {
        return switch (getMode(stack)) {
            case SHEARS_MODE -> ItemAbilities.DEFAULT_SHEARS_ACTIONS.contains(itemAbility);
            case FLINT_AND_STEEL_MODE -> ItemAbilities.DEFAULT_FLINT_ACTIONS.contains(itemAbility);
            case BRUSH_MODE -> ItemAbilities.DEFAULT_BRUSH_ACTIONS.contains(itemAbility);
            case FISHING_ROD_MODE -> ItemAbilities.DEFAULT_FISHING_ROD_ACTIONS.contains(itemAbility);
            default -> super.canPerformAction(stack, itemAbility);
        };
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return switch (getMode(stack)) {
            case BRUSH_MODE -> UseAnim.BRUSH;
            case SPYGLASS_MODE -> UseAnim.SPYGLASS;
            default -> super.getUseAnimation(stack);
        };
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return switch (getMode(stack)) {
            case BRUSH_MODE -> 200;
            case SPYGLASS_MODE -> 1200;
            default -> super.getUseDuration(stack, entity);
        };
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (getMode(stack) == BRUSH_MODE) {
            onUseTickAsBrush(level, livingEntity, stack, remainingUseDuration);
        } else {
            super.onUseTick(level, livingEntity, stack, remainingUseDuration);
        }
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        if (getMode(stack) == SHEARS_MODE) {
            return mineBlockAsShears(stack, level, state, miningEntity);
        } else {
            return super.mineBlock(stack, level, state, pos, miningEntity);
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (getMode(stack) == SHEARS_MODE) {
            return interactLivingEntityAsShears(stack, player, interactionTarget, usedHand);
        } else {
            return super.interactLivingEntity(stack, player, interactionTarget, usedHand);
        }
    }

    @Override
    public ItemStack assemble(int id, MultipleToOneSmithingRecipeInput input, HolderLookup.Provider registries) {
        if (id == 0) {
            ItemStack defaultInstance = getDefaultInstance();
            DataComponentType<ItemEnchantments> type = EnchantmentHelper.getComponentType(defaultInstance);
            Map<Holder<Enchantment>, Integer> enchantments = new Object2IntArrayMap<>();
            for (int i = 0; i < 8; i++) {
                ItemStack inputItem = input.getInputItem(i);
                for (var entry : inputItem.getOrDefault(type, ItemEnchantments.EMPTY).entrySet()) {
                    enchantments.merge(entry.getKey(), entry.getIntValue(), Integer::max);
                }
            }
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(defaultInstance.getOrDefault(type, ItemEnchantments.EMPTY));
            for (var entry : enchantments.entrySet()) {
                mutable.set(entry.getKey(), entry.getValue());
            }
            defaultInstance.set(type, mutable.toImmutable());
            return defaultInstance;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public int getEnchantmentValue(ItemStack stack) {
        return 1;
    }

    private InteractionResultHolder<ItemStack> useAsMagnet(Level level, Player player, InteractionHand usedHand) {
        if (player.isShiftKeyDown()) return InteractionResultHolder.pass(player.getItemInHand(usedHand));
        ItemStack item = player.getItemInHand(usedHand);
        double radius = AnvilCraft.config.magnetItemAttractsRadius;
        UseMagnetEvent event = new UseMagnetEvent(level, player, radius);
        ModLoader.postEvent(event);
        if (event.isCanceled()) return InteractionResultHolder.pass(item);
        radius = event.getAttractRadius();
        AABB aabb = new AABB(
            player.position().add(-radius, -radius, -radius),
            player.position().add(radius, radius, radius));
        level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Entity::isAlive)
            .forEach(e -> e.moveTo(player.position()));
        item.hurtAndBreak(1, player, LivingEntity.getSlotForHand(usedHand));
        player.getCooldowns().addCooldown(this, 5);
        return InteractionResultHolder.sidedSuccess(item, level.isClientSide());
    }

    private InteractionResultHolder<ItemStack> useAsFishingRod(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemstack = player.getItemInHand(usedHand);
        if (player.fishing != null) {
            if (!level.isClientSide) {
                int i = player.fishing.retrieve(itemstack);
                ItemStack original = itemstack.copy();
                itemstack.hurtAndBreak(i, player, LivingEntity.getSlotForHand(usedHand));
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

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    private InteractionResultHolder<ItemStack> useAsCarrotOnAStick(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(itemStack);
        } else {
            Entity entity = player.getControlledVehicle();
            if (player.isPassenger() && entity instanceof ItemSteerable itemSteerable) {
                if (entity.getType() == EntityType.PIG && itemSteerable.boost()) {
                    EquipmentSlot equipmentslot = LivingEntity.getSlotForHand(usedHand);
                    itemStack.hurtAndBreak(7, player, equipmentslot);
                    return InteractionResultHolder.success(itemStack);
                }
            }

            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.pass(itemStack);
        }
    }

    private InteractionResultHolder<ItemStack> useAsWarpedFungusOnAStick(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);
        if (level.isClientSide) {
            return InteractionResultHolder.pass(itemStack);
        } else {
            Entity entity = player.getControlledVehicle();
            if (player.isPassenger() && entity instanceof ItemSteerable itemSteerable) {
                if (entity.getType() == EntityType.STRIDER && itemSteerable.boost()) {
                    EquipmentSlot equipmentslot = LivingEntity.getSlotForHand(usedHand);
                    itemStack.hurtAndBreak(1, player, equipmentslot);
                    return InteractionResultHolder.success(itemStack);
                }
            }

            player.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.pass(itemStack);
        }
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
                itemstack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            }

            return InteractionResult.sidedSuccess(level.isClientSide);
        } else {
            return super.useOn(context);
        }
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
                level.playSound(player, blockpos1, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
                BlockState blockstate1 = BaseFireBlock.getState(level, blockpos1);
                level.setBlock(blockpos1, blockstate1, 11);
                level.gameEvent(player, GameEvent.BLOCK_PLACE, blockpos);
                ItemStack itemstack = context.getItemInHand();
                if (player instanceof ServerPlayer serverPlayer) {
                    CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, blockpos1, itemstack);
                    itemstack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
                }

                return InteractionResult.sidedSuccess(level.isClientSide());
            } else {
                return InteractionResult.FAIL;
            }
        } else {
            level.playSound(player, blockpos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
            level.setBlock(blockpos, blockstate2, 11);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, blockpos);
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
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
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();
        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) return InteractionResult.PASS;
        double maxY = blockState.getCollisionShape(level, pos).max(Direction.Axis.Y, 0.5, 0.5);
        if (!blockState.getBlock().properties().hasCollision && maxY == 0) return InteractionResult.PASS;
        for (MagnetizedNodeEntity entity : level.getEntities(ModEntities.MAGNETIZED_NODE.get(), new AABB(pos).setMaxY(pos.getY() + 1.1), EntitySelector.NO_SPECTATORS)) {
            if (entity.blockPos.equals(pos)) return InteractionResult.PASS;
        }
        Vec3 nodePos = pos.getBottomCenter().add(0, maxY, 0);
        MagnetizedNodeEntity magnetizedNodeEntity = new MagnetizedNodeEntity(level, nodePos, pos);
        level.addFreshEntity(magnetizedNodeEntity);
        player.getCooldowns().addCooldown(this, 5);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private HitResult calculateHitResult(Player player) {
        return ProjectileUtil.getHitResultOnViewVector(player,
            (entity) -> !entity.isSpectator() && player.isPickable(),
            player.blockInteractionRange());
    }

    private void spawnDustParticles(Level level, BlockHitResult hitResult, BlockState state, Vec3 pos, HumanoidArm arm) {
        int i = arm == HumanoidArm.RIGHT ? 1 : -1;
        int j = level.getRandom().nextInt(7, 12);
        BlockParticleOption blockparticleoption = new BlockParticleOption(ParticleTypes.BLOCK, state);
        Direction direction = hitResult.getDirection();
        DustParticlesDelta dustparticlesdelta = DustParticlesDelta.fromDirection(pos, direction);
        Vec3 vec3 = hitResult.getLocation();
        for (int k = 0; k < j; ++k) {
            level.addParticle(blockparticleoption, vec3.x - (double) (direction == Direction.WEST ? 1.0E-6F : 0.0F), vec3.y, vec3.z - (double) (direction == Direction.NORTH ? 1.0E-6F : 0.0F), dustparticlesdelta.xd() * (double) i * (double) 3.0F * level.getRandom().nextDouble(), 0.0F, dustparticlesdelta.zd() * (double) i * (double) 3.0F * level.getRandom().nextDouble());
        }
    }

    private void onUseTickAsBrush(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (remainingUseDuration >= 0 && livingEntity instanceof Player player) {
            HitResult hitresult = this.calculateHitResult(player);
            if (hitresult instanceof BlockHitResult blockhitresult) {
                if (hitresult.getType() == HitResult.Type.BLOCK) {
                    int i = this.getUseDuration(stack, livingEntity) - remainingUseDuration + 1;
                    boolean flag = i % 10 == 5;
                    if (flag) {
                        BlockPos blockpos = blockhitresult.getBlockPos();
                        BlockState blockstate = level.getBlockState(blockpos);
                        HumanoidArm humanoidarm = livingEntity.getUsedItemHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
                        if (blockstate.shouldSpawnTerrainParticles() && blockstate.getRenderShape() != RenderShape.INVISIBLE) {
                            this.spawnDustParticles(level, blockhitresult, blockstate, livingEntity.getViewVector(0.0F), humanoidarm);
                        }

                        Block var15 = blockstate.getBlock();
                        SoundEvent soundevent;
                        if (var15 instanceof BrushableBlock brushableBlock) {
                            soundevent = brushableBlock.getBrushSound();
                        } else {
                            soundevent = SoundEvents.BRUSH_GENERIC;
                        }

                        level.playSound(player, blockpos, soundevent, SoundSource.BLOCKS);
                        if (!level.isClientSide()) {
                            BlockEntity var18 = level.getBlockEntity(blockpos);
                            if (var18 instanceof BrushableBlockEntity brushableBlockEntity) {
                                boolean flag1 = brushableBlockEntity.brush(level.getGameTime(), player, blockhitresult.getDirection());
                                if (flag1) {
                                    EquipmentSlot equipmentslot = stack.equals(player.getItemBySlot(EquipmentSlot.OFFHAND)) ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                                    stack.hurtAndBreak(1, livingEntity, equipmentslot);
                                }
                            }
                        }
                    }
                    return;
                }
            }

            livingEntity.releaseUsingItem();
        } else {
            livingEntity.releaseUsingItem();
        }
    }

    private boolean mineBlockAsShears(ItemStack stack, Level level, BlockState state, LivingEntity miningEntity) {
        if (!level.isClientSide && !state.is(BlockTags.FIRE)) {
            stack.hurtAndBreak(1, miningEntity, EquipmentSlot.MAINHAND);
        }
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

    private InteractionResult interactLivingEntityAsShears(ItemStack stack, Player player, LivingEntity interactionTarget, InteractionHand usedHand) {
        if (interactionTarget instanceof IShearable target) {
            BlockPos pos = interactionTarget.blockPosition();
            boolean isClient = interactionTarget.level().isClientSide();
            if (target.isShearable(player, stack, interactionTarget.level(), pos)) {
                List<ItemStack> drops = target.onSheared(player, stack, interactionTarget.level(), pos);
                if (!isClient) {
                    for (ItemStack drop : drops) {
                        target.spawnShearedDrop(interactionTarget.level(), pos, drop);
                    }
                }

                interactionTarget.gameEvent(GameEvent.SHEAR, player);
                if (!isClient) {
                    stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(usedHand));
                }

                return InteractionResult.sidedSuccess(isClient);
            }
        }

        return InteractionResult.PASS;
    }

    public static int getMode(ItemStack item) {
        return Math.clamp(item.getOrDefault(DataComponents.CUSTOM_MODEL_DATA, CustomModelData.DEFAULT).value(), 0, 8);
    }

    public static void setMode(Player player, InteractionHand hand, @Range(from = 0, to = 8) int mode) {
        ItemStack item = player.getItemInHand(hand);
        if (!item.is(ModItems.MULTITOOL_ITEM)) {
            return;
        }
        item.remove(DataComponents.TOOL);
        item.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(mode));
        if (mode == SHEARS_MODE) {
            item.set(DataComponents.TOOL, ShearsItem.createToolProperties());
        }
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
                case TagKey<Item> tag when tag.equals(Tags.Items.TOOLS_SHEAR)       -> super.is(tag) && mode == SHEARS_MODE;
                case TagKey<Item> tag when tag.equals(ItemTags.CREEPER_IGNITERS)    -> super.is(tag) && mode == FLINT_AND_STEEL_MODE;
                case TagKey<Item> tag when tag.equals(Tags.Items.TOOLS_IGNITER)     -> super.is(tag) && mode == FLINT_AND_STEEL_MODE;
                case TagKey<Item> tag when tag.equals(Tags.Items.TOOLS_BRUSH)       -> super.is(tag) && mode == BRUSH_MODE;
                case TagKey<Item> tag when tag.equals(Tags.Items.TOOLS_FISHING_ROD) -> super.is(tag) && mode == FISHING_ROD_MODE;
                case TagKey<Item> tag when tag.equals(ItemTags.STRIDER_TEMPT_ITEMS) -> super.is(tag) && mode == WARPED_FUNGUS_ON_A_STICK_MODE;
                default -> super.is(tagKey);
            };
        }

        public boolean is(int mode, HolderSet<Item> holders) {
            return switch (holders) {
                case HolderSet.Named<Item> holderSet when holderSet.key().equals(Tags.Items.TOOLS_SHEAR)       -> holderSet.contains(this) && mode == SHEARS_MODE;
                case HolderSet.Named<Item> holderSet when holderSet.key().equals(ItemTags.CREEPER_IGNITERS)    -> holderSet.contains(this) && mode == FLINT_AND_STEEL_MODE;
                case HolderSet.Named<Item> holderSet when holderSet.key().equals(Tags.Items.TOOLS_IGNITER)     -> holderSet.contains(this) && mode == FLINT_AND_STEEL_MODE;
                case HolderSet.Named<Item> holderSet when holderSet.key().equals(Tags.Items.TOOLS_BRUSH)       -> holderSet.contains(this) && mode == BRUSH_MODE;
                case HolderSet.Named<Item> holderSet when holderSet.key().equals(Tags.Items.TOOLS_FISHING_ROD) -> holderSet.contains(this) && mode == FISHING_ROD_MODE;
                case HolderSet.Named<Item> holderSet when holderSet.key().equals(ItemTags.STRIDER_TEMPT_ITEMS) -> holderSet.contains(this) && mode == WARPED_FUNGUS_ON_A_STICK_MODE;
                default -> holders.contains(this);
            };
        }
    }
}
