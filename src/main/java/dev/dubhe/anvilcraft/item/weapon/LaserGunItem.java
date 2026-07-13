package dev.dubhe.anvilcraft.item.weapon;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.entity.WeaponBeamEntity;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import dev.dubhe.anvilcraft.util.WeaponRaycastUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LaserGunItem extends EnergyWeaponItem {
    private static final Map<UUID, LaserState> STATES = new ConcurrentHashMap<>();
    private static final int[] DAMAGE = {12, 28, 60, 124, 252};
    private static final int[] ENERGY = {400_000, 800_000, 1_600_000, 3_200_000, 6_400_000};
    private static final int[] VISUAL_LEVEL = {1, 2, 4, 8, 16};

    public LaserGunItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!canStartUsing(player, stack, ENERGY[0])) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (!(user instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) return;
        LaserState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new LaserState());
        WeaponRaycastUtil.Ray fullRay = WeaponRaycastUtil.ray(player, 48.0);
        BlockHitResult blockHit = WeaponRaycastUtil.laserBlockHit(level, player, fullRay);
        WeaponRaycastUtil.Ray ray = new WeaponRaycastUtil.Ray(fullRay.start(), blockHit.getLocation());
        int piercing = stack.getEnchantmentLevel(
            level.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.PIERCING));
        List<LivingEntity> targets = WeaponRaycastUtil.livingEntitiesToEnd(
            level, player, ray, Math.min(5, piercing) + 1);
        Vec3 end = ray.end();
        int visualStage = targets.isEmpty() ? 0 : Math.min(4, state.targetTicks / 100);
        Vec3 visualStart = WeaponRaycastUtil.visualStart(player, WeaponRaycastUtil.MUZZLE_RIGHT_OFFSET);
        WeaponBeamEntity.showContinuous(
            level, visualStart, end, WeaponBeamEntity.LASER, VISUAL_LEVEL[visualStage], player);

        if (!targets.isEmpty()) {
            state.resetMining();
            hurtTargets(serverLevel, player, stack, targets, state);
            return;
        }
        state.resetTarget();
        mine(serverLevel, player, stack, blockHit, state);
    }

    private static void hurtTargets(
        ServerLevel level, ServerPlayer player, ItemStack stack, List<LivingEntity> targets, LaserState state
    ) {
        UUID first = targets.getFirst().getUUID();
        if (!first.equals(state.target)) {
            state.target = first;
            state.targetTicks = 0;
        }
        state.targetTicks++;
        int quickCharge = stack.getEnchantmentLevel(
            level.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.QUICK_CHARGE));
        int period = 20 - Math.min(quickCharge, 10);
        if (state.targetTicks % period != 0) return;
        int stage = Math.min(4, state.targetTicks / 100);
        if (!((EnergyWeaponItem) stack.getItem()).consumeEnergy(player, stack, ENERGY[stage], 80_000_000)) return;

        if (stage >= 3) {
            player.igniteForSeconds(5.0F);
            player.hurt(stage == 3 ? player.damageSources().onFire() : player.damageSources().lava(), 4.0F);
        }
        for (LivingEntity target : targets) {
            DamageSource source = ModDamageTypes.laser(level, player);
            if (target.hurt(source, DAMAGE[stage])) {
                EnchantmentHelper.doPostAttackEffectsWithItemSource(
                    level, target, source, stack);
            }
        }
    }

    private static void mine(
        ServerLevel level, ServerPlayer player, ItemStack stack, BlockHitResult hit, LaserState state
    ) {
        if (!state.vein.isEmpty() && !hit.getBlockPos().equals(state.miningAnchor)) state.resetMining();
        if (state.vein.isEmpty()) {
            BlockPos origin = hit.getBlockPos();
            BlockState ore = level.getBlockState(origin);
            if (!ore.is(Tags.Blocks.ORES)) {
                state.miningTicks = 0;
                state.miningAnchor = null;
                state.idleTicks++;
                if (state.idleTicks % 20 == 0) {
                    ((EnergyWeaponItem) stack.getItem()).consumeEnergy(player, stack, 400_000, 80_000_000);
                }
                return;
            }
            state.idleTicks = 0;
            state.miningAnchor = origin.immutable();
            state.vein.addAll(findVein(level, origin, ore, AnvilCraft.CONFIG.laserOreClusterMaxSize, player.position()));
        }
        state.miningTicks++;
        if (state.miningTicks % miningPeriod(level, stack) != 0 || state.vein.isEmpty()) return;
        if (!((EnergyWeaponItem) stack.getItem()).consumeEnergy(player, stack, 400_000, 80_000_000)) return;
        BlockPos pos = state.vein.removeFirst();
        BlockState ore = level.getBlockState(pos);
        if (!ore.is(Tags.Blocks.ORES)) return;
        for (ItemStack drop : BreakBlockUtil.dropWithTool(level, pos, stack)) {
            player.getInventory().placeItemBackInInventory(drop);
        }
        BlockState replacement = ore.is(Blocks.ANCIENT_DEBRIS)
            ? Blocks.NETHERRACK.defaultBlockState()
            : ore.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)
                ? Blocks.DEEPSLATE.defaultBlockState()
                : ore.is(Tags.Blocks.ORES_IN_GROUND_NETHERRACK)
                    ? Blocks.NETHERRACK.defaultBlockState()
                    : Blocks.STONE.defaultBlockState();
        level.setBlockAndUpdate(pos, replacement);
    }

    public static int miningPeriod(Level level, ItemStack stack) {
        int efficiency = stack.getEnchantmentLevel(
            level.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY));
        return 20 - Math.min(efficiency, 19);
    }

    public static List<BlockPos> findVein(
        Level level, BlockPos origin, BlockState ore, int limit, Vec3 playerPosition
    ) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> found = new ArrayList<>();
        queue.add(origin);
        while (!queue.isEmpty() && found.size() < limit) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos) || !level.getBlockState(pos).is(ore.getBlock())) continue;
            found.add(pos.immutable());
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x != 0 || y != 0 || z != 0) queue.add(pos.offset(x, y, z));
                    }
                }
            }
        }
        found.sort(Comparator.comparingDouble((BlockPos pos) -> pos.distToCenterSqr(playerPosition)).reversed());
        return found;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        STATES.remove(entity.getUUID());
    }

    @Override
    protected void stopForInsufficientPower(Player player, ItemStack weapon) {
        STATES.remove(player.getUUID());
        super.stopForInsufficientPower(player, weapon);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72_000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    private static final class LaserState {
        private UUID target;
        private int targetTicks;
        private int miningTicks;
        private int idleTicks;
        private BlockPos miningAnchor;
        private final Deque<BlockPos> vein = new ArrayDeque<>();

        private void resetTarget() {
            target = null;
            targetTicks = 0;
        }

        private void resetMining() {
            miningTicks = 0;
            idleTicks = 0;
            miningAnchor = null;
            vein.clear();
        }
    }
}
