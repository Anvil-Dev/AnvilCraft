package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.item.property.component.Providence;
import dev.dubhe.anvilcraft.network.ResonanceMiningEffectPacket;
import dev.dubhe.anvilcraft.saved.multiphase.Multiphase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class TranscendenceResonatorItem extends ResonatorItem {
    public static final Component NAME = Component.translatable("item.anvilcraft.transcendence_resonator");
    private static final int RESONANCE_MINING_TICKS = 10;
    private static final int USE_DURATION = 72000;

    private final Map<LivingEntity, MiningTarget> clientMiningTargets = new WeakHashMap<>();
    private final Map<LivingEntity, MiningTarget> serverMiningTargets = new WeakHashMap<>();

    public TranscendenceResonatorItem(Properties properties) {
        super(
            ModTiers.TRANSCENDIUM,
            properties.fireResistant()
                .attributes(ResonatorItem.createAttributes(ModTiers.TRANSCENDIUM, 17, -3f))
                .component(ModComponents.MULTIPHASE, new MultiphaseRef())
                .component(DataComponents.ITEM_NAME, Multiphase.firstPhaseName(NAME))
                .component(ModComponents.ETERNAL, Eternal.INSTANCE)
                .component(DataComponents.UNBREAKABLE, new Unbreakable(true))
                .component(ModComponents.PROVIDENCE, Providence.INSTANCE)
                .component(ModComponents.FEROCIOUS, Ferocious.DEFAULT)
        );
    }

    @Override
    protected double getBaseAttackDamage() {
        return 17;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (getMode(stack) == AUTO_MODE && !isTooDamagedToUse(stack)) return InteractionResultHolder.fail(stack);
        return super.use(level, player, usedHand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (getMode(stack) != AUTO_MODE || isTooDamagedToUse(stack)) {
            return super.onItemUseFirst(stack, context);
        }
        return startResonanceMining(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (getMode(stack) != AUTO_MODE || isTooDamagedToUse(stack)) return super.useOn(context);
        return startResonanceMining(context);
    }

    private InteractionResult startResonanceMining(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!canResonanceMine(level.getBlockState(pos), level, pos)) return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        MiningTarget target = new MiningTarget(pos.immutable(), getEffectPositions(level, pos));
        miningTargets(level).put(player, target);
        player.startUsingItem(context.getHand());
        sendMiningEffects(level, target.effectPositions(), RESONANCE_MINING_TICKS + 2);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return USE_DURATION;
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
        if (hit == null || !target.hitPos().equals(hit.getBlockPos())) {
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
        if (elapsedTicks < RESONANCE_MINING_TICKS) return;

        targets.remove(livingEntity);
        if (livingEntity instanceof ServerPlayer player && player.gameMode.destroyBlock(target.hitPos())) {
            level.playSound(null, target.hitPos(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0f, 0.7f);
        }
        sendMiningEffects(level, target.effectPositions(), 0);
        livingEntity.stopUsingItem();
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeCharged) {
        MiningTarget target = miningTargets(level).remove(livingEntity);
        if (target != null) sendMiningEffects(level, target.effectPositions(), 0);
    }

    private Map<LivingEntity, MiningTarget> miningTargets(Level level) {
        return level.isClientSide ? this.clientMiningTargets : this.serverMiningTargets;
    }

    private void stopResonanceMining(Level level, LivingEntity livingEntity, MiningTarget target) {
        miningTargets(level).remove(livingEntity);
        if (target != null) sendMiningEffects(level, target.effectPositions(), 0);
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
        return state.is(Blocks.BEDROCK) || state.getDestroySpeed(level, pos) >= 0.0f;
    }

    private record MiningTarget(BlockPos hitPos, List<BlockPos> effectPositions) {
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        // TODO: 兼容性支持结束后将以下检测代码删除
        if (stack.has(ModComponents.MERCILESS)) {
            stack.set(ModComponents.MERCILESS, null);
        }
        if (stack.has(ModComponents.MERCILESS_ENCHANTMENTS)) {
            ItemEnchantments merciless = stack.get(ModComponents.MERCILESS_ENCHANTMENTS);
            ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(stack.get(DataComponents.ENCHANTMENTS));
            for (Holder<Enchantment> mercilessEnch : merciless.keySet()) {
                int mercilessLevel = merciless.getLevel(mercilessEnch);
                int enchLevel = enchantments.getLevel(mercilessEnch);
                if (enchLevel == mercilessLevel) {
                    enchLevel++;
                } else {
                    enchLevel = Math.max(mercilessLevel, enchLevel);
                }
                enchantments.set(mercilessEnch, enchLevel);
            }
            stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        }
    }
}
