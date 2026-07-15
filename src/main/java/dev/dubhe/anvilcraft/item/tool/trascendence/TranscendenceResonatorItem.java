package dev.dubhe.anvilcraft.item.tool.trascendence;

import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModToolMaterials;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.item.tool.ResonateMode;
import dev.dubhe.anvilcraft.item.tool.ResonatorItem;
import dev.dubhe.anvilcraft.network.ResonanceMiningEffectPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

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
            ModToolMaterials.TRANSCENDIUM,
            17,
            -3F,
            properties.fireResistant()
                .component(ModComponents.MULTIPHASE, Multiphase.create())
                .component(DataComponents.ITEM_NAME, Multiphase.firstPhaseName(NAME))
                .component(ModComponents.ETERNAL, Eternal.DEFAULT)
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE)
                .component(ModComponents.PROVIDENCE, Unit.INSTANCE)
                .component(ModComponents.FEROCIOUS, Ferocious.DEFAULT)
        );
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (ResonatorItem.getMode(stack) == ResonateMode.AUTO && !isTooDamagedToUse(stack)) {
            return InteractionResult.FAIL;
        }
        return super.use(level, player, usedHand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (ResonatorItem.getMode(stack) != ResonateMode.AUTO || isTooDamagedToUse(stack)) {
            return super.onItemUseFirst(stack, context);
        }
        return this.startResonanceMining(context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (ResonatorItem.getMode(stack) != ResonateMode.AUTO || isTooDamagedToUse(stack)) {
            return super.useOn(context);
        }
        return this.startResonanceMining(context);
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
        this.miningTargets(level).put(player, target);
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
        Map<LivingEntity, MiningTarget> targets = this.miningTargets(level);
        MiningTarget target = targets.get(livingEntity);
        if (target == null) {
            if (!level.isClientSide()) livingEntity.stopUsingItem();
            return;
        }

        BlockHitResult hit = getTargetedBlock(livingEntity);
        if (hit == null || !target.hitPos().equals(hit.getBlockPos())) {
            this.stopResonanceMining(level, livingEntity, target);
            return;
        }

        BlockState state = level.getBlockState(target.hitPos());
        if (!canResonanceMine(state, level, target.hitPos())) {
            this.stopResonanceMining(level, livingEntity, target);
            return;
        }

        int elapsedTicks = this.getUseDuration(stack, livingEntity) - remainingUseDuration;
        if (!level.isClientSide() && elapsedTicks % 3 == 0) {
            float pitch = 0.75F + 0.04F * elapsedTicks;
            level.playSound(null, target.hitPos(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.8F, pitch);
        }
        if (elapsedTicks < RESONANCE_MINING_TICKS) return;

        targets.remove(livingEntity);
        if (livingEntity instanceof ServerPlayer player && player.gameMode.destroyBlock(target.hitPos())) {
            level.playSound(null, target.hitPos(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.BLOCKS, 1.0F, 0.7F);
        }
        sendMiningEffects(level, target.effectPositions(), 0);
        livingEntity.stopUsingItem();
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int remainingUseDuration) {
        MiningTarget target = this.miningTargets(level).remove(livingEntity);
        if (target == null) return false;

        sendMiningEffects(level, target.effectPositions(), 0);
        int elapsedTicks = this.getUseDuration(stack, livingEntity) - remainingUseDuration;
        if (elapsedTicks >= RESONANCE_MINING_TICKS || !(livingEntity instanceof ServerPlayer player)) return true;

        BlockHitResult hit = getTargetedBlock(player);
        if (hit == null || !target.hitPos().equals(hit.getBlockPos())) return true;
        AnvilHammerItem.useBlock(player, target.hitPos(), player.level(), stack, target.hand(), target.hitResult());
        return true;
    }

    private Map<LivingEntity, MiningTarget> miningTargets(Level level) {
        return level.isClientSide() ? this.clientMiningTargets : this.serverMiningTargets;
    }

    private void stopResonanceMining(Level level, LivingEntity livingEntity, MiningTarget target) {
        this.miningTargets(level).remove(livingEntity);
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
            ChunkPos.containing(pos),
            new ResonanceMiningEffectPacket(pos, durationTicks)
        );
    }

    private static @Nullable BlockHitResult getTargetedBlock(LivingEntity livingEntity) {
        if (!(livingEntity instanceof Player player)) return null;
        HitResult hit = player.pick(player.blockInteractionRange(), 0.0F, false);
        return hit.getType() == HitResult.Type.BLOCK ? (BlockHitResult) hit : null;
    }

    private static boolean canResonanceMine(BlockState state, Level level, BlockPos pos) {
        if (state.isAir()) return false;
        return state.getDestroySpeed(level, pos) >= 0.0F;
    }

    private record MiningTarget(BlockHitResult hitResult, InteractionHand hand, List<BlockPos> effectPositions) {
        private BlockPos hitPos() {
            return this.hitResult.getBlockPos();
        }
    }
}
