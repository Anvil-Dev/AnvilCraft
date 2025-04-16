package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.hammer.HammerManager;
import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.network.RocketJumpPacket;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AnvilHammerItem extends Item implements Equipable {
    public static final Property<?>[] SUPPORTED_PROPERTIES = {
        BlockStateProperties.FACING,
        BlockStateProperties.FACING_HOPPER,
        BlockStateProperties.HORIZONTAL_FACING,
        BlockStateProperties.ORIENTATION,
        BlockStateProperties.AXIS,
        BlockStateProperties.HORIZONTAL_AXIS
    };
    private static final List<Predicate<Player>> isWearingPredicates = new ArrayList<>();
    private final ItemAttributeModifiers modifiers;

    static {
        isWearingPredicates.add(player -> player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof AnvilHammerItem);
    }

    /**
     * 初始化铁砧锤
     *
     * @param properties 物品属性
     */
    public AnvilHammerItem(Item.Properties properties) {
        super(properties);
        modifiers = ItemAttributeModifiers.builder()
            .add(
                Attributes.ATTACK_DAMAGE,
                new AttributeModifier(
                    BASE_ATTACK_DAMAGE_ID,
                    getAttackDamageModifierAmount(),
                    AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.MAINHAND
            ).add(
                Attributes.ATTACK_SPEED,
                new AttributeModifier(
                    BASE_ATTACK_SPEED_ID,
                    -3F,
                    AttributeModifier.Operation.ADD_VALUE
                ),
                EquipmentSlotGroup.MAINHAND
            ).build();
    }

    protected float getAttackDamageModifierAmount() {
        return 5;
    }

    public Block getAnvil() {
        return Blocks.ANVIL;
    }

    private static void breakBlock(ServerPlayer player, BlockPos pos, ServerLevel level, ItemStack tool) {
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!state.is(ModBlockTags.HAMMER_REMOVABLE) && !(block instanceof IHammerRemovable)) return;
        if (block instanceof AbstractMultiPartBlock<?> multiplePartBlock) {
            BlockPos posMainPart = multiplePartBlock.getMainPartPos(pos, state);
            BlockState stateMainPart = level.getBlockState(posMainPart);
            if (stateMainPart.is(block)) {
                pos = posMainPart;
                state = stateMainPart;
            }
        } else if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)
            && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            BlockPos posMainPart = pos.below();
            BlockState stateMainPart = level.getBlockState(posMainPart);
            if (stateMainPart.is(block)) {
                pos = posMainPart;
                state = stateMainPart;
            }
        }
        BlockPos posToRemove = pos;
        List<ItemStack> drops = player.isCreative() ? List.of() : BreakBlockUtil.dropSilkTouch(level, pos);
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

    /**
     * 检查是否可以使用铁砧锤
     */
    public static boolean ableToUseAnvilHammer(Level level, BlockPos blockPos, Player player) {
        if (player.isShiftKeyDown()) return true;
        BlockState state = level.getBlockState(blockPos);
        if (state.is(ModBlockTags.ANVIL_HAMMER_BLACKLIST)) return false;
        if (state.getBlock() instanceof IHammerChangeable hammerChangeable) {
            return hammerChangeable.checkBlockState(state);
        }
        return true;
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
        for (Property<?> supportedProperty : SUPPORTED_PROPERTIES) {
            if (state.hasProperty(supportedProperty)) {
                return supportedProperty;
            }
        }
        return null;
    }

    public static boolean dropAnvil(@Nullable Player player, Level level, BlockPos blockPos) {
        if (player == null || level.isClientSide) return false;
        ItemStack itemStack = player.getItemInHand(player.getUsedItemHand());
        Item item = itemStack.getItem();
        if (!(item instanceof AnvilHammerItem anvilHammerItem)) return false;
        if (player.getCooldowns().isOnCooldown(anvilHammerItem)) {
            return false;
        }
        player.getCooldowns().addCooldown(itemStack.getItem(), 5);
        FallingBlockEntity dummyAnvilEntity = new FallingBlockEntity(EntityType.FALLING_BLOCK, level);
        dummyAnvilEntity.blockState = anvilHammerItem.getAnvil().defaultBlockState();
        AnvilFallOnLandEvent event = new AnvilFallOnLandEvent(
            level, blockPos.above(), dummyAnvilEntity, player.fallDistance
        );
        NeoForge.EVENT_BUS.post(event);
        level.playSound(null, blockPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1f, 1f);
        itemStack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        return true;
    }

    /**
     * 右键方块
     */
    public static void useBlock(
        ServerPlayer player, BlockPos blockPos, ServerLevel level, ItemStack anvilHammer) {
        if (rocketJump(player, level, blockPos)) return;
        if (!player.getAbilities().mayBuild) return;
        if (player.isShiftKeyDown()) {
            breakBlock(player, blockPos, level, anvilHammer);
            return;
        }
        Block block = level.getBlockState(blockPos).getBlock();
        HammerManager.getChange(block).change(player, blockPos, level, anvilHammer);
    }

    private static boolean rocketJump(@Nullable ServerPlayer serverPlayer, ServerLevel level, BlockPos blockPos) {
        if (serverPlayer == null) return false;
        ItemStack itemStack = serverPlayer.getInventory().offhand.getFirst();
        if (!itemStack.is(Items.FIREWORK_ROCKET)) return false;
        if (!itemStack.has(DataComponents.FIREWORKS)) return false;
        int i = itemStack.get(DataComponents.FIREWORKS).flightDuration();
        if (serverPlayer.getRotationVector().x > 70) {
            if (!serverPlayer.getAbilities().instabuild) itemStack.shrink(1);
            double power = i * 0.75 + 0.5;
            serverPlayer.setDeltaMovement(0, power, 0);
            PacketDistributor.sendToPlayer(serverPlayer, new RocketJumpPacket(power));
            level.sendParticles(
                ParticleTypes.FIREWORK,
                serverPlayer.getX(),
                serverPlayer.getY(),
                serverPlayer.getZ(),
                20,
                0,
                0.5,
                0,
                0.05);
            level.playSound(null, blockPos, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.AMBIENT, 3.0f, 1.0f);
            return true;
        }
        return false;
    }


    @Override
    public boolean canAttackBlock(
        BlockState state, Level level, BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 1.0f;
    }

    @Override
    public boolean mineBlock(
        ItemStack stack,
        Level level,
        BlockState state,
        BlockPos pos,
        LivingEntity miningEntity) {
        if (state.getDestroySpeed(level, pos) != 0.0f) {
            stack.hurtAndBreak(2, miningEntity, LivingEntity.getSlotForHand(miningEntity.getUsedItemHand()));
        }
        return true;
    }

    protected float calculateFallDamageBonus(float fallDistance) {
        return Math.min(fallDistance * 2, 40);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, LivingEntity.getSlotForHand(target.getUsedItemHand()));
        float damageBonus = calculateFallDamageBonus(attacker.fallDistance);
        Level level = target.level();
        if (level instanceof ServerLevel serverLevel) {
            EnchantmentHelper.modifyFallBasedDamage(
                serverLevel,
                stack,
                attacker,
                level.damageSources().anvil(attacker),
                damageBonus
            );
        }
        target.hurt(target.level().damageSources().anvil(attacker), damageBonus);
        if (attacker.fallDistance >= 3) {
            attacker.level()
                .playSound(
                    null,
                    BlockPos.containing(attacker.position()),
                    SoundEvents.ANVIL_LAND,
                    SoundSource.BLOCKS,
                    1f,
                    attacker.fallDistance > 17 ? (float) 0.5 : 1 - attacker.fallDistance / 35);
        }
        return true;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack pStack, BlockState pState) {
        return false;
    }

    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        return modifiers;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    public static void addIsWearingPredicate(Predicate<Player> predicate) {
        isWearingPredicates.add(predicate);
    }

    public static boolean isWearing(Player player) {
        for (Predicate<Player> it : isWearingPredicates) {
            if (it.test(player)) {
                return true;
            }
        }
        return false;
    }
}
