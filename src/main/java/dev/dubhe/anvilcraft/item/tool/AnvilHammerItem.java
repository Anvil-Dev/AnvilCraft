package dev.dubhe.anvilcraft.item.tool;

import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.api.hammer.HammerManager;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.EmberAnvilMenu;
import dev.dubhe.anvilcraft.inventory.FrostAnvilMenu;
import dev.dubhe.anvilcraft.inventory.OpenedHammerSource;
import dev.dubhe.anvilcraft.inventory.PortableAnvilMenu;
import dev.dubhe.anvilcraft.inventory.RoyalAnvilMenu;
import dev.dubhe.anvilcraft.inventory.TranscendenceAnvilMenu;
import dev.dubhe.anvilcraft.mixin.invoker.BlockBehaviourInvoker;
import dev.dubhe.anvilcraft.network.RocketJumpPacket;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import dev.dubhe.anvilcraft.util.EntityUtil;
import dev.dubhe.anvilcraft.util.InfiniteFluidTankBreakProtection;
import dev.dubhe.anvilcraft.util.MultiPartBlockUtil;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class AnvilHammerItem extends Item {
    public static final int PORTABLE_ANVIL_USE_TICKS = 40;
    public static final Property<?>[] SUPPORTED_PROPERTIES = {
        BlockStateProperties.FACING,
        BlockStateProperties.FACING_HOPPER,
        BlockStateProperties.HORIZONTAL_FACING,
        BlockStateProperties.ORIENTATION,
        BlockStateProperties.AXIS,
        BlockStateProperties.HORIZONTAL_AXIS
    };
    private static final List<Predicate<Player>> IS_WEARING_PREDICATES = new ArrayList<>();
    public static boolean goggleEnabled = false;

    static {
        AnvilHammerItem.IS_WEARING_PREDICATES.add(player -> player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof AnvilHammerItem);
    }

    private final ItemAttributeModifiers modifiers;

    /// 初始化铁砧锤
    ///
    /// @param properties 物品属性
    public AnvilHammerItem(Item.Properties properties) {
        super(properties
            .equippableUnswappable(EquipmentSlot.HEAD)
            .component(
                DataComponents.TOOL,
                new Tool(List.of(), 1, 1, false)
            )
        );
        this.modifiers = ItemAttributeModifiers.builder().add(
            Attributes.ATTACK_DAMAGE, new AttributeModifier(
                Item.BASE_ATTACK_DAMAGE_ID, this.getAttackDamageModifierAmount(),
                AttributeModifier.Operation.ADD_VALUE
            ), EquipmentSlotGroup.MAINHAND
        ).add(
            Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -3F, AttributeModifier.Operation.ADD_VALUE),
            EquipmentSlotGroup.MAINHAND
        ).build();
    }

    private static void breakBlock(ServerPlayer player, BlockPos pos, ServerLevel level, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!state.is(ModBlockTags.HAMMER_REMOVABLE) && !(block instanceof IHammerRemovable)) return;
        pos = MultiPartBlockUtil.getChainableMainPartPos(level, pos);
        if (InfiniteFluidTankBreakProtection.isProtected(level, pos)) {
            InfiniteFluidTankBreakProtection.showToolBreakDenied(player);
            return;
        }
        state = level.getBlockState(pos);
        block = state.getBlock();
        BlockPos posToRemove = pos;
        BreakBlockEvent breakEvent = new BreakBlockEvent(level, posToRemove, state, player);
        NeoForge.EVENT_BUS.post(breakEvent);
        if (breakEvent.isCanceled()) return;
        final List<ItemStack> drops = player.isCreative() ? List.of() : BreakBlockUtil.dropSilkTouch(level, pos);
        block.playerWillDestroy(level, posToRemove, state, player);
        level.destroyBlock(posToRemove, false);
        if (player.isCreative()) return;
        if (!player.isAlive() && player.hasDisconnected()) {
            drops.forEach(drop -> Block.popResource(level, posToRemove, drop));
            state.spawnAfterBreak(level, posToRemove, tool, true);
            return;
        }
        drops.forEach(drop -> player.getInventory().placeItemBackInInventory(drop));
        state.spawnAfterBreak(level, posToRemove, tool, true);
    }

    /// 检查是否可以使用铁砧锤
    public static boolean ableToUseAnvilHammer(Level level, BlockPos blockPos, Player player) {
        if (player.isShiftKeyDown()) return true;
        BlockState state = level.getBlockState(blockPos);
        if (state.is(ModBlockTags.ANVIL_HAMMER_BLACKLIST)) return false;
        if (state.getBlock() instanceof IHammerChangeable hammerChangeable) {
            return hammerChangeable.checkBlockState(state);
        }
        return AnvilHammerItem.findModifyableProperty(state) != null;
    }

    /**
     * 判断是否应放行副手方块放置。
     *
     * <p>主手持有铁砧锤、副手持有可放置方块时，对既不能交互、又不能被锤子修改或拆除的普通方块，
     * 放行原版副手方块放置，使其优先于长按右键打开便携铁砧菜单。
     */
    public static boolean shouldPlaceOffhandBlock(Player player, Level level, BlockHitResult hit) {
        ItemStack offhand = player.getOffhandItem();
        if (offhand.isEmpty() || offhand.is(ModItemTags.ANVIL_HAMMER)) return false;
        if (!(offhand.getItem() instanceof BlockItem)) return false;
        BlockState state = level.getBlockState(hit.getBlockPos());
        if (state.is(BlockTags.CAULDRONS) || state.is(ModBlockTags.ANVIL_HAMMER_BLACKLIST)) return false;
        if (state.is(ModBlockTags.HAMMER_REMOVABLE) || state.getBlock() instanceof IHammerRemovable) return false;
        return findModifyableProperty(state) == null;
    }

    @Nullable
    public static Property<?> findModifyableProperty(BlockState state) {
        Property<?> result = null;
        if (state.getBlock() instanceof IHammerChangeable changeable) {
            result = changeable.getChangeableProperty(state);
        }
        if (result != null) {
            return result;
        }
        for (Property<?> supportedProperty : AnvilHammerItem.SUPPORTED_PROPERTIES) {
            if (state.hasProperty(supportedProperty)) {
                return supportedProperty;
            }
        }
        return null;
    }

    public static boolean dropAnvil(@Nullable Player player, Level level, BlockPos blockPos) {
        if (player == null || level.isClientSide()) return false;
        ItemStack stack = player.getMainHandItem();
        Item item = stack.getItem();
        if (!(item instanceof AnvilHammerItem anvilHammerItem)) return false;
        if (player.getCooldowns().isOnCooldown(stack)) return false;
        player.getCooldowns().addCooldown(stack, 5);
        FallingBlockEntity dummyAnvilEntity = new FallingBlockEntity(EntityType.FALLING_BLOCK, level);
        dummyAnvilEntity.blockState = anvilHammerItem.getAnvil().defaultBlockState();
        if (level instanceof ServerLevel serverLevel) {
            AnvilEvent.OnLand event = new AnvilEvent.OnLand(serverLevel, blockPos.above(), dummyAnvilEntity, player.fallDistance);
            NeoForge.EVENT_BUS.post(event);
        }
        level.playSound(null, blockPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1F, 1F);
        stack.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
        TriggerUtil.anvilHammerClickBlock(level, blockPos, "left_click");
        return true;
    }

    public static void openPortableAnvil(Player player, int inventorySlot) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        OpenedHammerSource source = OpenedHammerSource.fromInventory(serverPlayer.getInventory(), inventorySlot);
        AnvilHammerItem.openPortableAnvil(serverPlayer, source);
    }

    private static void openPortableAnvil(ServerPlayer serverPlayer, @Nullable OpenedHammerSource source) {
        if (source == null) return;
        if (!serverPlayer.containerMenu.getCarried().isEmpty()) return;
        if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) {
            serverPlayer.closeContainer();
        }
        MenuProvider provider = new SimpleMenuProvider(
            (id, playerInventory, menuPlayer) -> AnvilHammerItem.createPortableAnvilMenu(id, playerInventory, source),
            Component.translatable("container.repair")
        );
        ModMenuTypes.open(serverPlayer, provider);
    }

    public static void openPortableAnvilFromMenuSlot(Player player, int menuSlotId) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!serverPlayer.containerMenu.getCarried().isEmpty()) return;
        if (menuSlotId < 0 || menuSlotId >= serverPlayer.containerMenu.slots.size()) return;
        Slot slot = serverPlayer.containerMenu.getSlot(menuSlotId);
        OpenedHammerSource source = OpenedHammerSource.fromMenuSlot(slot, serverPlayer.getInventory());
        AnvilHammerItem.openPortableAnvil(serverPlayer, source);
    }

    private static AbstractContainerMenu createPortableAnvilMenu(
        int id,
        Inventory playerInventory,
        OpenedHammerSource source
    ) {
        Item hammerItem = source.openedHammerItem();
        if (hammerItem == ModItems.ROYAL_ANVIL_HAMMER.get()) {
            return new RoyalAnvilMenu(id, playerInventory, ContainerLevelAccess.NULL, source);
        }
        if (hammerItem == ModItems.EMBER_ANVIL_HAMMER.get()) {
            return new EmberAnvilMenu(id, playerInventory, ContainerLevelAccess.NULL, source);
        }
        if (hammerItem == ModItems.FROST_ANVIL_HAMMER.get()) {
            return new FrostAnvilMenu(id, playerInventory, ContainerLevelAccess.NULL, source);
        }
        if (hammerItem == ModItems.TRANSCENDENCE_ANVIL_HAMMER.get()) {
            return new TranscendenceAnvilMenu(id, playerInventory, ContainerLevelAccess.NULL, source);
        }
        return new PortableAnvilMenu(id, playerInventory, ContainerLevelAccess.NULL, source);
    }

    /// 右键方块
    public static void useBlock(
        ServerPlayer player, BlockPos blockPos, ServerLevel level, ItemStack anvilHammer, InteractionHand hand,
        BlockHitResult result
    ) {
        if (AnvilHammerItem.rocketJump(player, level, result)) return;
        if (!level.mayInteract(player, blockPos)) return;
        if (!player.getAbilities().mayBuild) return;
        if (player.isShiftKeyDown()) {
            TriggerUtil.anvilHammerClickBlock(level, blockPos, "shift_right_click");
            AnvilHammerItem.breakBlock(player, blockPos, level, anvilHammer);
            return;
        }
        TriggerUtil.anvilHammerClickBlock(level, blockPos, "right_click");
        if (AnvilHammerItem.interactWithBlock(player, blockPos, level, anvilHammer, hand, result)) return;
        HammerManager.getChange(level.getBlockState(blockPos).getBlock()).change(player, blockPos, level, anvilHammer);
    }

    public static boolean interactWithBlock(
        Player player,
        BlockPos blockPos,
        Level level,
        ItemStack anvilHammer,
        InteractionHand hand,
        BlockHitResult result
    ) {
        BlockState state = level.getBlockState(blockPos);
        Block block = state.getBlock();
        MenuProvider provider = ((BlockBehaviourInvoker) block).invokeGetMenuProvider(state, level, blockPos);
        if (provider != null) {
            if (player instanceof ServerPlayer serverPlayer) {
                ModMenuTypes.open(serverPlayer, provider, blockPos);
            }
            return true;
        }
        InteractionResult useItemInteractionResult = state.useItemOn(anvilHammer, level, player, hand, result);
        if (useItemInteractionResult.equals(InteractionResult.TRY_WITH_EMPTY_HAND)) {
            return state.useWithoutItem(level, player, result) != InteractionResult.PASS;
        } else {
            return !useItemInteractionResult.equals(InteractionResult.PASS);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        player.startUsingItem(usedHand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (!level.isClientSide() && livingEntity instanceof ServerPlayer player) {
            int slot = player.getUsedItemHand() == InteractionHand.MAIN_HAND
                       ? player.getInventory().getSelectedSlot()
                       : Inventory.SLOT_OFFHAND;
            AnvilHammerItem.openPortableAnvil(player, slot);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return AnvilHammerItem.PORTABLE_ANVIL_USE_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    private static boolean rocketJump(
        @Nullable ServerPlayer serverPlayer,
        ServerLevel level,
        BlockHitResult result
    ) {
        if (!AnvilHammerItem.canRocketJump(serverPlayer)) return false;
        ItemStack stack = serverPlayer.getOffhandItem();
        Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
        int i = Objects.requireNonNull(fireworks).flightDuration();
        Vec3 location = result.getLocation();
        Vec3 offset = Vec3.atLowerCornerOf(result.getDirection().getUnitVec3i()).scale(0.15);
        FireworkRocketEntity rocket = new FireworkRocketEntity(
            level,
            serverPlayer,
            location.x + offset.x,
            location.y + offset.y,
            location.z + offset.z,
            stack
        );
        level.addFreshEntity(rocket);
        if (!serverPlayer.getAbilities().instabuild) stack.shrink(1);
        double power = i * 0.75 + 0.5;
        Vec3 movement = serverPlayer.getDeltaMovement();
        serverPlayer.setDeltaMovement(movement.x, power, movement.z);
        PacketDistributor.sendToPlayer(serverPlayer, new RocketJumpPacket(power));
        return true;
    }

    public static boolean canRocketJump(@Nullable Player player) {
        if (player == null) return false;
        ItemStack stack = player.getOffhandItem();
        if (!stack.is(Items.FIREWORK_ROCKET)) return false;
        if (!stack.has(DataComponents.FIREWORKS)) return false;
        return player.getRotationVector().x > 70;
    }

    public static void addIsWearingPredicate(Predicate<Player> predicate) {
        AnvilHammerItem.IS_WEARING_PREDICATES.add(predicate);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isWearing(Player player) {
        for (var it : AnvilHammerItem.IS_WEARING_PREDICATES) {
            if (it.test(player)) return true;
        }
        return false;
    }

    // @OnlyIn(Dist.CLIENT)
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean shouldRenderEffect(Player player) {
        return switch (AnvilCraftClient.CONFIG.goggleMode) {
            case ALWAYS_SHOW -> true;
            case WEARING_HAMMER -> AnvilHammerItem.isWearing(player);
            case HOLDING_HAMMER -> AnvilHammerItem.isHolding(player);
            case WEARING_OR_HOLDING_HAMMER -> AnvilHammerItem.isWearing(player) || AnvilHammerItem.isHolding(player);
            case TOGGLE_WITH_KEY -> AnvilHammerItem.goggleEnabled;
        };
    }

    private static boolean isHolding(Player player) {
        return player.getMainHandItem().getItem() instanceof AnvilHammerItem
            || player.getOffhandItem().getItem() instanceof AnvilHammerItem;
    }

    protected float getAttackDamageModifierAmount() {
        return 5;
    }

    public Block getAnvil() {
        return Blocks.ANVIL;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 0.0F;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miningEntity) {
        return false;
    }

    protected float calculateFallDamageBonus(float fallDistance) {
        return Math.min(fallDistance * 2, 40);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, target.getUsedItemHand());
        float damageBonus = this.calculateFallDamageBonus((float) attacker.fallDistance);
        Level level = target.level();
        if (level instanceof ServerLevel serverLevel) {
            EnchantmentHelper.modifyFallBasedDamage(serverLevel, stack, attacker, level.damageSources().anvil(attacker), damageBonus);
        }
        EntityUtil.hurtOrSimulate(target, target.level().damageSources().anvil(attacker), damageBonus);
        if (attacker.fallDistance >= 3) {
            attacker.level().playSound(
                null,
                BlockPos.containing(attacker.position()),
                SoundEvents.ANVIL_LAND,
                SoundSource.BLOCKS,
                1F,
                attacker.fallDistance > 17 ? 0.5F : 1 - (float) attacker.fallDistance / 35
            );
        }
        if (level instanceof ServerLevel serverLevel) {
            if (target.killedEntity(serverLevel, attacker, serverLevel.damageSources().mobAttack(attacker))) {
                TriggerUtil.killedEntityByAnvilHammer(serverLevel, BlockPos.containing(target.position()), target);
            }
        }
        TriggerUtil.anvilHammerHurtEntity(level, BlockPos.containing(target.position()), damageBonus);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return false;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return this.modifiers;
    }
}
