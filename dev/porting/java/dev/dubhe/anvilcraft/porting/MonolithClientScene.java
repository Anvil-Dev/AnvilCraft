package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.MonolithCoreBlockEntity;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

import java.util.Objects;
import java.util.Set;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class MonolithClientScene {
    private static final int[] AGES = {20, 50, 65, 80, 95, 100};
    private static int stage;
    private static boolean requested;
    private static volatile boolean ready;
    @javax.annotation.Nullable
    private static volatile RuntimeException failure;
    private static long next;
    private static long deadline;
    private static boolean capture;
    private static boolean interacted;

    @SubscribeEvent
    public static void before(RenderFrameEvent.Pre event) {
        if (Boolean.getBoolean("anvilcraft.portMonolithScene") && ready && Minecraft.getInstance().level != null) {
            var client = Minecraft.getInstance();
            client.level.setTimeFromServer(500);
            client.level.clockManager().handleUpdates(500, java.util.Map.of(
                client.level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.WORLD_CLOCK)
                    .getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD),
                new net.minecraft.world.clock.ClockNetworkState(6000, 0, 0)));
            client.level.environmentAttributes().invalidateTickCache();
        }
    }

    public static void frame(Minecraft client) {
        if (failure != null) throw failure;
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Monolith scene timed out");
        client.options.pauseOnLostFocus = false;
        client.options.hideGui = true;
        if (stage == AGES.length) {
            AnvilCraft.LOGGER.info("PORT_MONOLITH_VISUAL_PASSED: six small/giant offering frames");
            client.stop();
            return;
        }
        if (!requested) {
            requested = true;
            ready = false;
            client.setScreen(null);
            Objects.requireNonNull(client.getSingleplayerServer()).execute(() -> {
                try {
                    var server = Objects.requireNonNull(client.getSingleplayerServer());
                    var level = server.overworld();
                    for (int x = 2; x <= 20; x++) {
                        for (int z = 2; z <= 16; z++) level.setBlock(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState(), 2);
                    }
                    var small = new BlockPos(8, 81, 8);
                    var giant = new BlockPos(14, 80, 8);
                    level.setBlock(small, ModBlocks.MONOLITH_CORE.getDefaultState(), 2);
                    for (var part : Cube3x3PartHalf.values()) {
                        level.setBlock(giant.offset(part.getOffset()), ModBlocks.GIANT_MONOLITH_CORE.get()
                            .placedState(part, ModBlocks.GIANT_MONOLITH_CORE.getDefaultState()), 2);
                    }
                    for (int y = 1; y <= 5; y++) {
                        level.setBlock(small.above(y), ModBlocks.MONOLITH_LINE.getDefaultState(), 2);
                        level.setBlock(giant.above(y + 2), ModBlocks.GIANT_MONOLITH_LINE.getDefaultState(), 2);
                    }
                    var player = server.getPlayerList().getPlayers().getFirst();
                    if (!interacted) {
                        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                        offer(level, player, small, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ANVIL, 3),
                            dev.dubhe.anvilcraft.init.item.ModItems.GUIDE_BOOK.asItem());
                        offer(level, player, giant, new net.minecraft.world.item.ItemStack(ModBlocks.GIANT_ANVIL.asItem(), 2),
                            net.minecraft.world.item.Items.WRITTEN_BOOK);
                        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                        interacted = true;
                        AnvilCraft.LOGGER.info("PORT_MONOLITH_ONLINE_REWARD_PASSED: ordinary guide and giant knowledge book");
                    }
                    for (var pos : new BlockPos[]{small, giant.above()}) {
                        var core = (MonolithCoreBlockEntity) Objects.requireNonNull(level.getBlockEntity(pos));
                        var offering = core.isGiant() ? ModBlocks.GIANT_ANVIL.getDefaultState()
                            .setValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                            .setValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER) : Blocks.ANVIL.defaultBlockState();
                        var tag = new CompoundTag();
                        tag.store("Offering", BlockState.CODEC, offering);
                        tag.putLong("OfferingStart", 500 - AGES[stage]);
                        tag.putInt("LineHeight", 5);
                        core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
                        level.sendBlockUpdated(pos, core.getBlockState(), core.getBlockState(), 2);
                    }
                    player.setNoGravity(true);
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    player.teleportTo(level, 22, 86, 20, Set.<Relative>of(), 140, 12, false);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick freeze");
                    ready = true;
                } catch (RuntimeException exception) {
                    failure = exception;
                }
            });
            next = System.currentTimeMillis() + 3500;
            return;
        }
        if (!ready || capture || client.screen != null || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        AnvilCraft.LOGGER.info("PORT_MONOLITH_SAMPLE: age={}", AGES[stage]);
        capture = true;
        Screenshot.grab(client.gameDirectory, "monolith-26.1-" + AGES[stage] + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                stage++;
                capture = false;
                requested = false;
            }));
    }

    private static void offer(net.minecraft.server.level.ServerLevel level, net.minecraft.server.level.ServerPlayer player,
                              BlockPos pos, net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item reward) {
        int count = stack.count();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        final int before = player.getInventory().countItem(reward);
        var state = level.getBlockState(pos);
        state.useItemOn(stack, level, player, net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),
                net.minecraft.core.Direction.NORTH, pos, false));
        if (stack.count() != count - 1) throw new IllegalStateException("Offering did not consume exactly one item");
        var corePos = state.is(ModBlocks.GIANT_MONOLITH_CORE.get())
            ? ModBlocks.GIANT_MONOLITH_CORE.get().getMainPartPos(pos, state) : pos;
        var core = (MonolithCoreBlockEntity) Objects.requireNonNull(level.getBlockEntity(corePos));
        var tag = core.saveWithoutMetadata(level.registryAccess());
        tag.putLong("OfferingStart", level.getGameTime() - MonolithCoreBlockEntity.OFFERING_TICKS);
        core.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));
        core.tick();
        core.tick();
        if (player.getInventory().countItem(reward) != before + 1) throw new IllegalStateException("Online reward lost or duplicated");
    }

}
