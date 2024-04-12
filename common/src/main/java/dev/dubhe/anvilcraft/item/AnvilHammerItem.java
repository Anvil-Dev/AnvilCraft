package dev.dubhe.anvilcraft.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.api.hammer.HammerManager;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Vanishable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class AnvilHammerItem extends Item implements Vanishable {
    private long lastDropAnvilTime = 0;
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    /**
     * 初始化铁砧锤
     *
     * @param properties 物品属性
     */
    public AnvilHammerItem(Item.Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(
            Attributes.ATTACK_DAMAGE,
            new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 5, AttributeModifier.Operation.ADDITION)
        );
        builder.put(
            Attributes.ATTACK_SPEED,
            new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -3F, AttributeModifier.Operation.ADDITION)
        );
        this.defaultModifiers = builder.build();
    }

    private void breakBlock(Player player, BlockPos blockPos, @NotNull Level level) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        BlockState blockState = level.getBlockState(blockPos);
        Block block = blockState.getBlock();
        if (!blockState.is(ModBlockTags.HAMMER_REMOVABLE) && !(block instanceof IHammerRemovable)) return;
        level.destroyBlock(blockPos, false);
        if (!serverPlayer.isAlive() && serverPlayer.hasDisconnected()) {
            serverPlayer.drop(new ItemStack(block.asItem()), false);
            return;
        }
        if (serverPlayer.isCreative()) return;
        serverPlayer.getInventory().placeItemBackInInventory(new ItemStack(block.asItem()));
    }

    private boolean changeBlockState(Player player, BlockPos blockPos, @NotNull Level level, ItemStack anvilHammer) {
        if (level.isClientSide) return false;
        Block block = level.getBlockState(blockPos).getBlock();
        return HammerManager.getChange(block).change(player, blockPos, level, anvilHammer);
    }

    private void dropAnvil(Player player, Level level, BlockPos blockPos) {
        if (player == null || level.isClientSide) return;
        if (System.currentTimeMillis() - lastDropAnvilTime <= 150) return;
        lastDropAnvilTime = System.currentTimeMillis();
        AnvilFallOnLandEvent event = new AnvilFallOnLandEvent(
            level, blockPos.above(), new FallingBlockEntity(EntityType.FALLING_BLOCK, level), player.fallDistance
        );
        AnvilCraft.EVENT_BUS.post(event);
        level.playSound(null, blockPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1f, 1f);
        ItemStack itemStack = player.getItemInHand(player.getUsedItemHand());
        if (itemStack.getItem() instanceof AnvilHammerItem) {
            itemStack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        BlockPos blockPos = context.getClickedPos();
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        return InteractionResult.sidedSuccess(changeBlockState(player, blockPos, level, context.getItemInHand()));
    }

    /**
     * 左键方块
     *
     * @param player   玩家
     * @param blockPos 位置
     * @param level    世界
     */
    public void useAttackBlock(Player player, BlockPos blockPos, Level level) {
        if (player == null || level.isClientSide) return;
        if (player.isShiftKeyDown()) {
            breakBlock(player, blockPos, level);
        } else {
            dropAnvil(player, level, blockPos);
        }
    }

    @Override
    public boolean canAttackBlock(
        @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player
    ) {
        return !player.isCreative();
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
        return 1.0f;
    }

    @Override
    public boolean mineBlock(
        @NotNull ItemStack stack,
        @NotNull Level level,
        @NotNull BlockState state,
        @NotNull BlockPos pos,
        @NotNull LivingEntity miningEntity
    ) {
        if (state.getDestroySpeed(level, pos) != 0.0f) {
            stack.hurtAndBreak(2, miningEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return true;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        float damageBonus = attacker.fallDistance > 17 ? 34 : attacker.fallDistance * 2;
        target.hurt(target.level().damageSources().anvil(attacker), damageBonus);
        return true;
    }

    @Override
    public boolean isCorrectToolForDrops(@NotNull BlockState block) {
        return false;
    }

    @Override
    public @NotNull Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(@NotNull EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND) {
            return this.defaultModifiers;
        }
        return super.getDefaultAttributeModifiers(slot);
    }
}
