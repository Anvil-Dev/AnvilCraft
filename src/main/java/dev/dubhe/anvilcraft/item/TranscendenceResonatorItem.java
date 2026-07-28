package dev.dubhe.anvilcraft.item;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.client.renderer.item.ItemUseAnimationTransform;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.item.property.component.Providence;
import dev.dubhe.anvilcraft.network.ResonanceMiningEffectPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public class TranscendenceResonatorItem extends ResonatorItem {
    public static final Component NAME = Component.translatable("item.anvilcraft.transcendence_resonator");
    public static final int RESONANCE_MINING_TICKS = 10;
    private static final int USE_DURATION = 72000;

    private final Map<LivingEntity, MiningTarget> clientMiningTargets = new WeakHashMap<>();
    private final Map<LivingEntity, MiningTarget> serverMiningTargets = new WeakHashMap<>();

    public TranscendenceResonatorItem(Properties properties) {
        super(
            ModTiers.TRANSCENDIUM,
            properties.fireResistant()
                .attributes(ResonatorItem.createAttributes(ModTiers.TRANSCENDIUM, 17, -3f))
                .component(ModComponents.MULTIPHASE, Multiphase.create())
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
                    RESONANCE_MINING_TICKS
                );
            }
        });
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

        BlockHitResult hitResult = new BlockHitResult(
            context.getClickLocation(),
            context.getClickedFace(),
            pos.immutable(),
            context.isInside()
        );
        MiningTarget target = new MiningTarget(hitResult, context.getHand(), getEffectPositions(level, pos));
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
    public UseAnim getUseAnimation(ItemStack stack) {
        return getMode(stack) == AUTO_MODE ? UseAnim.CROSSBOW : UseAnim.NONE;
    }

    public static float resonanceMiningProgress(Level level, Player player, float partialTick) {
        if (!(player.getUseItem().getItem() instanceof TranscendenceResonatorItem resonator)) return -1.0F;
        if (!resonator.miningTargets(level).containsKey(player)) return -1.0F;

        ItemStack stack = player.getUseItem();
        int elapsedTicks = stack.getUseDuration(player) - player.getUseItemRemainingTicks();
        return Math.min(1.0F, (elapsedTicks + partialTick) / RESONANCE_MINING_TICKS);
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

        boolean destroyed = livingEntity instanceof ServerPlayer player
            && player.gameMode.destroyBlock(target.hitPos());
        targets.remove(livingEntity);
        if (destroyed) {
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
        if (elapsedTicks >= RESONANCE_MINING_TICKS || !(livingEntity instanceof Player player)) return;

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

    private Map<LivingEntity, MiningTarget> miningTargets(Level level) {
        return level.isClientSide ? this.clientMiningTargets : this.serverMiningTargets;
    }

    public static boolean isResonanceMining(Level level, Player player, BlockPos pos) {
        if (!(player.getUseItem().getItem() instanceof TranscendenceResonatorItem resonator)) return false;
        MiningTarget target = resonator.miningTargets(level).get(player);
        return target != null && target.hitPos().equals(pos);
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
        return state.getDestroySpeed(level, pos) >= 0.0f;
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

}
