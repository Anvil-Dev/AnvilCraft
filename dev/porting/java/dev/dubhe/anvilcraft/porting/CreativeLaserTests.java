package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.laser.LaserComponentTypes;
import dev.dubhe.anvilcraft.block.entity.CreativeLaserBlockEntity;
import dev.dubhe.anvilcraft.block.laser.CreativeLaserBlock;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.inventory.CreativeLaserMenu;
import dev.dubhe.anvilcraft.network.CreativeLaserUpdatePacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.Map;
import java.util.function.Consumer;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public final class CreativeLaserTests {
    private static final BlockPos POS = new BlockPos(8, 7, 3);
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "port_creative_laser_settings", CreativeLaserTests::settings,
        "port_creative_laser_emission", CreativeLaserTests::emission,
        "port_creative_laser_mining", CreativeLaserTests::mining,
        "port_creative_laser_shape", CreativeLaserTests::shape
    );

    @SubscribeEvent
    public static void functions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach((name, test) -> registry.register(AnvilCraft.of(name), test)));
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var environment = event.registerEnvironment(AnvilCraft.of("port_creative_laser"));
        TESTS.forEach((name, test) -> event.registerTest(AnvilCraft.of(name), new FunctionGameTestInstance(
            ResourceKey.create(Registries.TEST_FUNCTION, AnvilCraft.of(name)),
            new TestData<>(environment, AnvilCraft.of("port_logistics_empty"), 100, 0, true))));
    }

    private static CreativeLaserBlockEntity place(GameTestHelper helper, Direction direction) {
        helper.setBlock(POS, ModBlocks.CREATIVE_LASER.getDefaultState().setValue(CreativeLaserBlock.FACING, direction));
        return helper.getBlockEntity(POS, CreativeLaserBlockEntity.class);
    }

    private static void settings(GameTestHelper h) {
        var laser = place(h, Direction.EAST);
        h.assertTrue(laser.getConfiguredLevel() == 16 && laser.getLensType() == LensType.NONE && !laser.isGamma(), "Source defaults");
        var player = h.makeMockPlayer(GameType.CREATIVE);
        new CreativeLaserUpdatePacket(64, LensType.ROYAL, true).handleOnServer(player);
        h.assertTrue(laser.getConfiguredLevel() == 16, "Update requires the creative laser menu");
        player.containerMenu = new CreativeLaserMenu(1, laser);
        for (var lens : LensType.values()) {
            for (boolean gamma : new boolean[]{false, true}) {
                for (int requested : new int[]{Integer.MIN_VALUE, -1, 0, 1, 16, 64, 65, Integer.MAX_VALUE}) {
                    var buffer = Unpooled.buffer();
                    try {
                        var packet = new CreativeLaserUpdatePacket(requested, lens, gamma);
                        CreativeLaserUpdatePacket.STREAM_CODEC.encode(buffer, packet);
                        CreativeLaserUpdatePacket.STREAM_CODEC.decode(buffer).handleOnServer(player);
                    } finally {
                        buffer.release();
                    }
                    int expected = Math.clamp(requested, 0, 64);
                    h.assertTrue(laser.getConfiguredLevel() == expected && laser.getLensType() == lens && laser.isGamma() == gamma,
                        "Packet clamps level and preserves independent lens/gamma options");
                    var saved = laser.saveWithFullMetadata(h.getLevel().registryAccess());
                    var restored = new CreativeLaserBlockEntity(laser.getType(), laser.getBlockPos(), laser.getBlockState());
                    restored.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, h.getLevel().registryAccess(), saved));
                    h.assertTrue(restored.getConfiguredLevel() == expected && restored.getLensType() == lens && restored.isGamma() == gamma,
                        "Configuration survives native ValueInput/ValueOutput save round trip");
                }
            }
        }
        var buffer = Unpooled.buffer();
        try {
            ByteBufCodecs.INT.encode(buffer, 16);
            ByteBufCodecs.STRING_UTF8.encode(buffer, "unknown_lens");
            ByteBufCodecs.BOOL.encode(buffer, false);
            h.assertTrue(CreativeLaserUpdatePacket.STREAM_CODEC.decode(buffer).lensType() == LensType.NONE, "Unknown lens fallback");
        } finally {
            buffer.release();
        }
        h.succeed();
    }

    private static void emission(GameTestHelper h) {
        for (var direction : Direction.values()) {
            var laser = place(h, direction);
            var target = POS.relative(direction, 2);
            h.setBlock(target, Blocks.BEDROCK);
            for (int strength : new int[]{1, 4, 16, 64}) {
                for (boolean gamma : new boolean[]{false, true}) {
                    laser.setConfiguredLevel(strength);
                    laser.setGamma(gamma);
                    laser.tick(h.getLevel());
                    h.assertTrue(laser.getLaserLevel() == strength && h.absolutePos(target).equals(laser.getIrradiateBlockPos()),
                        "Emission " + direction + " strength=" + strength + " gamma=" + gamma
                            + " actual=" + laser.getLaserLevel() + "/" + laser.getIrradiateBlockPos()
                            + " expected=" + h.absolutePos(target) + " signal=" + h.getLevel().hasNeighborSignal(laser.getBlockPos()));
                    h.assertTrue(laser.isEmittingGamma() == gamma && laser.getLaserMaxLength() == (gamma ? 16 : 128),
                        "Gamma type and range follow configuration");
                }
            }
            h.setBlock(target, Blocks.AIR);
            laser.setConfiguredLevel(0);
            laser.tick(h.getLevel());
            h.assertTrue(laser.getLaserLevel() == 0 && laser.getIrradiateBlockPos() == null, "Level zero cancels emission");
        }
        var laser = place(h, Direction.EAST);
        laser.setConfiguredLevel(16);
        laser.setGamma(false);
        h.setBlock(POS.east(4), Blocks.BEDROCK);
        laser.tick(h.getLevel());
        h.setBlock(POS.west(), Blocks.REDSTONE_BLOCK);
        laser.tick(h.getLevel());
        h.assertTrue(laser.getLaserLevel() == 0 && laser.getIrradiateBlockPos() == null, "Redstone disables emission");
        h.setBlock(POS.west(), Blocks.AIR);
        laser.tick(h.getLevel());
        h.assertTrue(laser.getLaserLevel() == 16, "Removing redstone restores configured emission");
        h.succeed();
    }

    private static void mining(GameTestHelper h) {
        var laser = place(h, Direction.EAST);
        laser.setLensType(LensType.ROYAL);
        h.setBlock(POS.west(), Blocks.CHEST);
        h.setBlock(POS.east(4), Blocks.DIAMOND_ORE);
        for (int i = 0; i < 20; i++) laser.tick(h.getLevel());
        h.assertTrue(h.getBlockState(POS.east(4)).is(Blocks.STONE),
            "Mining actual=" + h.getBlockState(POS.east(4)) + " hit=" + laser.getIrradiateBlockPos()
            + " level=" + laser.getLaserLevel() + " signal=" + h.getLevel().hasNeighborSignal(laser.getBlockPos()));
        var chest = h.getBlockEntity(POS.west(), ChestBlockEntity.class);
        h.assertTrue(chest.countItem(Items.DIAMOND_ORE) == 1, "Royal configured lens produces silk-touch loot into inventory");
        var mining = laser.getComponent(LaserComponentTypes.MINING);
        h.assertTrue(mining != null && !mining.specialTargets(), "Source creative lens configuration does not grant physical-lens targets");
        h.setBlock(POS.east(4), ModBlocks.VOID_STONE.get());
        for (int i = 0; i < 30; i++) laser.tick(h.getLevel());
        h.assertTrue(h.getBlockState(POS.east(4)).is(Blocks.DEEPSLATE),
            "Source ore tags select the generic deepslate recipe for void stone");
        h.succeed();
    }

    private static void shape(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.CREATIVE);
        for (var direction : Direction.values()) {
            player.setYRot(switch (direction) {
                case NORTH -> 180;
                case EAST -> -90;
                case WEST -> 90;
                default -> 0;
            });
            player.setYHeadRot(player.getYRot());
            player.setXRot(direction == Direction.UP ? -90 : direction == Direction.DOWN ? 90 : 0);
            for (boolean crouch : new boolean[]{false, true}) {
                player.setShiftKeyDown(crouch);
                var hit = new BlockHitResult(h.absolutePos(POS).getCenter(), Direction.UP, h.absolutePos(POS.below()), false);
                var context = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, ModBlocks.CREATIVE_LASER.asStack(), hit);
                var placed = ModBlocks.CREATIVE_LASER.get().getStateForPlacement(context);
                h.assertTrue(placed.getValue(CreativeLaserBlock.FACING) == (crouch ? direction.getOpposite() : direction),
                    "Normal and crouched placement follow the source looking direction");
            }
            var state = ModBlocks.CREATIVE_LASER.getDefaultState().setValue(CreativeLaserBlock.FACING, direction);
            var boxes = state.getShape(h.getLevel(), h.absolutePos(POS)).toAabbs();
            double volume = boxes.stream().mapToDouble(b -> b.getXsize() * b.getYsize() * b.getZsize()).sum();
            h.assertTrue(Math.abs(volume - 0.671875) < 1e-8, "All six rotated shapes retain source volume");
            var support = new Vec3(0.0625, 0.0625, 0.0625).with(direction.getAxis(),
                direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 0.0625 : 0.9375);
            var tip = support.with(direction.getAxis(), 1 - support.get(direction.getAxis()));
            h.assertTrue(boxes.stream().anyMatch(b -> b.contains(support)) && boxes.stream().noneMatch(b -> b.contains(tip)),
                "Full support plate faces opposite the inset laser tip");
            for (var rotation : Rotation.values()) {
                h.assertTrue(state.rotate(rotation).getValue(CreativeLaserBlock.FACING) == rotation.rotate(direction), "Rotation parity");
            }
            for (var mirror : Mirror.values()) {
                h.assertTrue(state.mirror(mirror).getValue(CreativeLaserBlock.FACING) == mirror.mirror(direction), "Mirror parity");
            }
        }
        h.succeed();
    }
}
