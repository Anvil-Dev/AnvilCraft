package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.cfa.item.CelestialForgingAnvilBlockItem;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.LiquidCoverage;
import dev.dubhe.anvilcraft.block.entity.celestial.RingType;
import dev.dubhe.anvilcraft.block.entity.celestial.RockyPlanetData;
import dev.dubhe.anvilcraft.block.entity.celestial.SpecialCelestialBodyData;
import dev.dubhe.anvilcraft.block.entity.celestial.StarData;
import dev.dubhe.anvilcraft.block.entity.celestial.Temperature;
import dev.dubhe.anvilcraft.client.renderer.item.CelestialForgingAnvilItemRenderer;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;

import java.util.ArrayList;
import java.util.List;

public final class CelestialAnvilItemClientScene {
    private static final boolean STELLAR = Boolean.getBoolean("anvilcraft.portStellarScene");
    private static final String[] STAR_NAMES = {"M", "K", "G", "F", "A", "B", "O", "White dwarf", "Neutron", "Black hole", "Brown dwarf"};
    private static final List<ItemStack> ITEMS = new ArrayList<>();
    private static int stage;
    private static net.minecraft.core.BlockPos musicPos;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean ready;
    private static volatile boolean reloaded;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("CFA item stage " + stage);
        client.player.setNoGravity(true);
        client.player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        client.player.setPos(8.5, 85, 12.5);
        if (capturing || System.currentTimeMillis() < next) return;
        client.options.guiScale().set(2);
        client.options.fov().set(70);
        client.options.fovEffectScale().set(0.0);
        client.level.setTimeFromServer(500);
        client.options.bobView().set(false);
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        if (stage == 0) {
            client.getSingleplayerServer().execute(() -> {
                var server = client.getSingleplayerServer();
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 8.5 85 12.5 180 15");
                var player = server.getPlayerList().getPlayers().getFirst();
                player.setNoGravity(true);
                player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
                ready = true;
            });
            prepare(client);
            client.setScreen(new Gallery());
            advance(1);
            return;
        }
        if (!ready) return;
        switch (stage) {
            case 1 -> capture(client, "gallery", 2);
            case 2 -> {
                client.setScreen(null);
                supply(client, 1, false, false);
                advance(3);
            }
            case 3 -> {
                if (Boolean.getBoolean("anvilcraft.portCfaMusicScene") && musicPos == null) {
                    musicPos = client.player.blockPosition();
                    dev.dubhe.anvilcraft.client.event.QuenchedOutMusicHandler.start(musicPos);
                }
                capture(client, "main-hand", 4);
            }
            case 4 -> {
                supply(client, 2, true, false);
                advance(5);
            }
            case 5 -> {
                if (Boolean.getBoolean("anvilcraft.portCfaMusicScene")) verifyMusic(client);
                capture(client, "off-hand", 6);
            }
            case 6 -> {
                supply(client, 2, false, true);
                client.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                advance(7);
            }
            case 7 -> {
                if (!client.player.getItemBySlot(EquipmentSlot.HEAD).is(ModBlocks.CELESTIAL_FORGING_ANVIL.asItem())) return;
                capture(client, "head", 8);
            }
            case 8 -> {
                client.options.setCameraType(CameraType.FIRST_PERSON);
                client.setScreen(new Gallery());
                client.reloadResourcePacks().whenComplete((ignored, error) -> client.execute(() -> {
                    if (error != null) throw new IllegalStateException(error);
                    reloaded = true;
                }));
                advance(9);
            }
            case 9 -> {
                if (!reloaded) return;
                verify();
                capture(client, "reloaded", 10);
            }
            case 10 -> {
                AnvilCraft.CLIENT_CONFIG.planetAtmosphereRenderingMode =
                    dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.CelestialRenderingMode.VANILLA;
                if (STELLAR) {
                    AnvilCraft.CLIENT_CONFIG.stellarRenderingMode =
                        dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.CelestialRenderingMode.VANILLA;
                }
                advance(11);
            }
            case 11 -> capture(client, "vanilla-atmosphere", 12);
            case 12 -> {
                AnvilCraft.CLIENT_CONFIG.planetAtmosphereRenderingMode =
                    dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.CelestialRenderingMode.STANDARD;
                if (STELLAR) {
                    AnvilCraft.CLIENT_CONFIG.stellarRenderingMode =
                        dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.CelestialRenderingMode.STANDARD;
                }
                if (STELLAR && !Boolean.getBoolean("anvilcraft.portCfaItemReference")) {
                    failStellarPipeline();
                    advance(13);
                    return;
                }
                finish(client);
            }
            case 13 -> capture(client, "forced-fallback", 14);
            case 14 -> {
                reloaded = false;
                client.reloadResourcePacks().whenComplete((ignored, error) -> client.execute(() -> {
                    if (error != null) throw new IllegalStateException(error);
                    reloaded = true;
                }));
                advance(15);
            }
            case 15 -> {
                if (reloaded) capture(client, "recovered", 16);
            }
            case 16 -> finish(client);
            default -> throw new IllegalStateException("Unknown CFA item stage");
        }
    }

    private static void verifyMusic(Minecraft client) {
        try {
            var field = dev.dubhe.anvilcraft.client.event.QuenchedOutMusicHandler.class.getDeclaredField("ACTIVE");
            field.setAccessible(true);
            var active = (java.util.Map<?, ?>) field.get(null);
            var sound = (net.minecraft.client.resources.sounds.AbstractTickableSoundInstance) active.get(musicPos);
            if (sound == null || !client.getSoundManager().isActive(sound)) throw new IllegalStateException("CFA music did not play");
            dev.dubhe.anvilcraft.client.event.QuenchedOutMusicHandler.stop(musicPos);
            if (!sound.isStopped() || active.containsKey(musicPos)) throw new IllegalStateException("CFA music did not stop");
            AnvilCraft.LOGGER.info("PORT_CFA_MUSIC_PASSED: native sound started and stopped");
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void finish(Minecraft client) {
        AnvilCraft.LOGGER.info("PORT_CFA_ITEM_RENDER_PASSED: body data, fitting, head parts, hands and reload");
        client.stop();
    }

    private static void failStellarPipeline() {
        try {
            var type = dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.StellarEmissionRenderer.class;
            var failed = type.getDeclaredField("failed");
            failed.setAccessible(true);
            failed.setBoolean(null, true);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void prepare(Minecraft client) {
        if (STELLAR) {
            prepareStars();
            verify();
            return;
        }
        ITEMS.add(ModBlocks.CELESTIAL_FORGING_ANVIL.asStack());
        ITEMS.add(item(new RockyPlanetData(CelestialBodyClass.ROCKY_MED_LIQUID, false, LiquidCoverage.MEDIUM,
            Temperature.MILD, RingType.NONE, 32, 2, 3, 0, 2, 0), 42));
        ITEMS.add(item(new RockyPlanetData(CelestialBodyClass.ROCKY_HIGH_LIQUID, true, LiquidCoverage.HIGH,
            Temperature.MILD, RingType.STRONG, 48, 4, 3, 23.5F, 3, 1), 73));
        ITEMS.add(item(new StarData(CelestialBodyClass.G_MAIN, 32, 255, 220, 160, 10, 2, 0, 64, null), 15));
        ITEMS.add(item(new StarData(CelestialBodyClass.BLACK_HOLE, 48, 0, 0, 0, 15, 2, 0, 128, null), 64));
        ITEMS.add(item(new SpecialCelestialBodyData("anvilcraft:flesh", "Flesh", 16, 18, 2, 0,
            Temperature.MILD, true, LiquidCoverage.HIGH, false, true, "planet_flesh", null), 91));
        var profile = new CompoundTag();
        profile.putString("name", client.player.getGameProfile().name());
        profile.store("id", UUIDUtil.CODEC, client.player.getUUID());
        ITEMS.add(item(SpecialCelestialBodyData.fromPlayerHead(profile, 16), 81));
        verify();
    }

    private static void prepareStars() {
        var classes = new CelestialBodyClass[]{CelestialBodyClass.M_MAIN, CelestialBodyClass.K_MAIN,
            CelestialBodyClass.G_MAIN, CelestialBodyClass.F_MAIN, CelestialBodyClass.A_MAIN, CelestialBodyClass.B_MAIN,
            CelestialBodyClass.O_MAIN, CelestialBodyClass.WHITE_DWARF, CelestialBodyClass.NEUTRON_STAR, CelestialBodyClass.BLACK_HOLE};
        for (var type : classes) {
            float temperature = dev.dubhe.anvilcraft.block.entity.celestial.StellarVisualState.temperatureForSurfaceClass(type, 32);
            int rgb = dev.dubhe.anvilcraft.block.entity.celestial.StellarVisualState.colorForTemperature(temperature);
            if (type == CelestialBodyClass.BLACK_HOLE) rgb = 0;
            ITEMS.add(item(new StarData(type, 32, (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 10, 0, 0, 32, null), 15));
            AnvilCraft.LOGGER.info("PORT_STELLAR_SAMPLE {}: temperature={}, color={}, exposure={}", type, temperature, rgb,
                dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.StellarRadiance.exposure(temperature, 1, 1));
        }
        ITEMS.add(item(new dev.dubhe.anvilcraft.block.entity.celestial.GiantPlanetData(CelestialBodyClass.BROWN_DWARF,
            dev.dubhe.anvilcraft.block.entity.celestial.PressureType.GAS,
            dev.dubhe.anvilcraft.block.entity.celestial.WindSpeed.HIGH, RingType.NONE, 32, 1, 1, 10, 0, 0, true), 15));
    }

    private static ItemStack item(CelestialBodyData body, long seed) {
        final var stack = ModBlocks.CELESTIAL_FORGING_ANVIL.asStack();
        var tag = new CompoundTag();
        tag.put("celestialBody", body.toTag());
        tag.putLong("bodySeed", seed);
        tag.putBoolean("isAmplify", body instanceof StarData);
        CelestialForgingAnvilBlockItem.saveRenderData(tag, 500);
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        output.store(tag);
        BlockItem.setBlockEntityData(stack, ModBlockEntities.CELESTIAL_FORGING_ANVIL.get(), output);
        return stack;
    }

    private static void verify() {
        var bodyRenderer = new CelestialForgingAnvilItemRenderer(false);
        var headRenderer = new CelestialForgingAnvilItemRenderer(true);
        for (int index = 1; index < ITEMS.size(); index++) {
            var body = bodyRenderer.extractArgument(ITEMS.get(index));
            var head = headRenderer.extractArgument(ITEMS.get(index));
            if (body == null || body.state().getEffectiveBodyData() == null || head == null || head.parts().size() != 18
                || head.state().isAnimating() || !head.state().isCanRenderBody()) {
                throw new IllegalStateException("Invalid stored CFA item state " + index);
            }
            double extent = Math.max(body.bounds().getXsize(), Math.max(body.bounds().getYsize(), body.bounds().getZsize()));
            if (!(extent > 0) || !Double.isFinite(extent)) throw new IllegalStateException("Invalid celestial fit");
            AnvilCraft.LOGGER.info("PORT_CFA_ITEM_FIT {}: {}", index, body.bounds());
        }
    }

    private static void supply(Minecraft client, int index, boolean offhand, boolean head) {
        client.getSingleplayerServer().execute(() -> {
            var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
            player.setItemInHand(InteractionHand.MAIN_HAND, !head && !offhand ? ITEMS.get(index).copy() : ItemStack.EMPTY);
            player.setItemInHand(InteractionHand.OFF_HAND, !head && offhand ? ITEMS.get(index).copy() : ItemStack.EMPTY);
            player.setItemSlot(EquipmentSlot.HEAD, head ? ITEMS.get(index).copy() : ItemStack.EMPTY);
            player.inventoryMenu.broadcastChanges();
        });
    }

    private static boolean atmosphereReady() {
        try {
            var type = STELLAR ? dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.StellarEmissionRenderer.class
                : dev.dubhe.anvilcraft.client.renderer.blockentity.celestial.PlanetAtmosphereRenderer.class;
            var checked = type.getDeclaredField("checked");
            var failed = type.getDeclaredField("failed");
            checked.setAccessible(true);
            failed.setAccessible(true);
            if (failed.getBoolean(null)) throw new IllegalStateException("Native atmosphere pipeline entered fallback");
            return checked.getBoolean(null);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void advance(int value) {
        stage = value;
        next = System.currentTimeMillis() + 1400;
    }

    private static void capture(Minecraft client, String name, int nextStage) {
        if (client.getOverlay() != null) {
            next = System.currentTimeMillis() + 500;
            return;
        }
        boolean fallback = name.equals("vanilla-atmosphere") || name.equals("forced-fallback");
        if (!Boolean.getBoolean("anvilcraft.portCfaItemReference") && !fallback && !atmosphereReady()) return;
        AnvilCraft.LOGGER.info("PORT_CFA_VIEW {}: position={}, flying={}", name,
            client.player.position(), client.player.getAbilities().flying);
        capturing = true;
        String fileName = (STELLAR ? "stellar-26.1-" : "cfa-item-26.1-") + name + ".png";
        Screenshot.grab(client.gameDirectory, fileName, client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(nextStage);
            }));
    }

    private static final class Gallery extends Screen {
        private Gallery() {
            super(Component.literal("CFA item previews"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF252525);
            String[] names = STELLAR ? STAR_NAMES : new String[]{"Empty", "Rocky", "Atmosphere", "Star", "Black hole", "Flesh", "Head"};
            for (int index = 0; index < ITEMS.size(); index++) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(30 + (index % 7) * 84, (STELLAR ? 60 + (index / 7) * 150 : 90));
                graphics.pose().scale(3);
                graphics.item(ITEMS.get(index), 0, 0);
                graphics.pose().popMatrix();
                graphics.text(this.font, names[index], 30 + (index % 7) * 84, (STELLAR ? 130 + (index / 7) * 150 : 160), -1, false);
            }
        }
    }
}
