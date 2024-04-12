package dev.dubhe.anvilcraft.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Vanishable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class AnvilHammer extends Item implements Vanishable {
    private long lastDropAnvilTime = 0;
    private final HashSet<String> stateNames = new HashSet<>(List.of("facing", "axis", "rotation"));
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;
    public AnvilHammer(Item.Properties properties) {
        super(properties);
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 5, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -3F, AttributeModifier.Operation.ADDITION));
        this.defaultModifiers = builder.build();
    }

    private void breakBlock(Player player, BlockPos blockPos, Level level){
        if (level.isClientSide) return;
        BlockState blockState = level.getBlockState(blockPos);
        if (!blockState.is(ModBlockTags.REMOVABLE)) return;
        Block block = blockState.getBlock();
        level.destroyBlock(blockPos, false);
        if (!player.isAlive() || player instanceof ServerPlayer && ((ServerPlayer) player).hasDisconnected()) {
                player.drop(new ItemStack(block.asItem()), false);
        }
        else if (!player.isCreative()){
            Inventory inventory = player.getInventory();
            if (inventory.player instanceof ServerPlayer) {
                inventory.placeItemBackInInventory(new ItemStack(block.asItem()));
            }


        }
    }
    private boolean changeBlockState(Player player, BlockPos blockPos, Level level, ItemStack anvilHammer){
        if (level.isClientSide) return false;
        Block block = level.getBlockState(blockPos).getBlock();
        StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();
        Collection<Property<?>> collection = stateDefinition.getProperties();
        String string = BuiltInRegistries.BLOCK.getKey(block).toString();
        if (collection.isEmpty()) return false;
        CompoundTag compoundTag = anvilHammer.getOrCreateTagElement("DebugProperty");
        String string2 = compoundTag.getString(string);
        Property<?> property = stateDefinition.getProperty(string2);
            if (property == null) {
            for (Property<?> tempProperty : collection.stream().toList()) {
                if (stateNames.contains(tempProperty.getName())) {
                    property = tempProperty;
                    break;
                }
            }
            if (property == null) return false;
        }
        BlockState blockState = cycleState(level.getBlockState(blockPos), property, player.isSecondaryUseActive());
        level.setBlock(blockPos, blockState, 18);
        return true;
    }

    private static <T extends Comparable<T>> BlockState cycleState(BlockState state, Property<T> property, boolean backwards) {
        return state.setValue(property, getRelative(property.getPossibleValues(), state.getValue(property), backwards));
    }
    private static <T> T getRelative(Iterable<T> allowedValues, @Nullable T currentValue, boolean backwards) {
        return backwards ? Util.findPreviousInIterable(allowedValues, currentValue) : Util.findNextInIterable(allowedValues, currentValue);
    }
    public void dropAnvil(Player player, Level level, BlockPos blockPos) {
        if (player == null  || level.isClientSide) return;
        if (System.currentTimeMillis() - lastDropAnvilTime <= 150) return;
        lastDropAnvilTime = System.currentTimeMillis();
        AnvilFallOnLandEvent event = new AnvilFallOnLandEvent(level, blockPos.above(), new FallingBlockEntity(EntityType.FALLING_BLOCK, level), player.fallDistance);
        AnvilCraft.EVENT_BUS.post(event);
        level.playSound (null, blockPos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS,1f,1f);
        ItemStack itemStack = player.getItemInHand(player.getUsedItemHand());
        if (itemStack.getItem() instanceof AnvilHammer){
            itemStack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(player.getUsedItemHand()));
        }
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
    BlockPos blockPos = context.getClickedPos();
    Player player = context.getPlayer();
    Level level = context.getLevel();
    if (player == null) {
        return InteractionResult.FAIL;
    }
    return InteractionResult.sidedSuccess(changeBlockState(player, blockPos, level, context.getItemInHand()));
    }
    public void useAttackBlock(Player player, BlockPos blockPos, Level level) {
        if (player == null || level.isClientSide) return;
        if (player.isShiftKeyDown()) {
            breakBlock(player, blockPos, level);
        } else {
            dropAnvil(player, level, blockPos);
        }
    }
    @Override
    public boolean canAttackBlock(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, Player player) {
        return !player.isCreative();
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
        return 1.0f;
    }
    @Override
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull Level level, BlockState state, @NotNull BlockPos pos, @NotNull LivingEntity miningEntity) {
        if (state.getDestroySpeed(level, pos) != 0.0f) {
            stack.hurtAndBreak(2, miningEntity, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }
        return true;
    }
    @Override
    public boolean hurtEnemy(ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
            stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        float damageBonus = attacker.fallDistance > 17 ? 34 : attacker.fallDistance*2;
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
