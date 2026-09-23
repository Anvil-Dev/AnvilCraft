package dev.dubhe.anvilcraft.client.building;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.building.BlueprintPlacement;
import dev.dubhe.anvilcraft.building.BuildingEntityTransform;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWNativeWin32;
import org.lwjgl.system.windows.User32;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class BuildingRodClientScene {
    private static final BlockPos FIRST = new BlockPos(8, 81, 8);
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean prepared;
    private static volatile Throwable failure;
    private static volatile Path blueprintFile;
    private static BlueprintPlacement confirmed;
    private static int fixedCells;
    private static int fixedEnergy;
    private static volatile int standId = -1;

    public static void frame(Minecraft client) {
        if (deadline == 0) deadline = System.currentTimeMillis() + 180000;
        if (failure != null || System.currentTimeMillis() > deadline) {
            throw new IllegalStateException("Building client stage " + stage, failure);
        }
        if (client.screen != null || capturing || System.currentTimeMillis() < next) return;
        if (!client.isWindowActive()) GLFW.glfwFocusWindow(client.getWindow().handle());
        client.player.setNoGravity(true);
        client.options.hideGui = false;
        client.getToastManager().clear();
        client.gui.getChat().clearMessages(false);
        switch (stage) {
            case 0 -> {
                AnvilCraft.CLIENT_CONFIG.buildingRodControls = AnvilCraftClientConfig.BuildingRodControls.OPTIMIZED;
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var server = client.getSingleplayerServer();
                        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tick unfreeze");
                        var player = server.getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.setNoGravity(true);
                        player.setPos(8.5, 82, 12);
                        player.getInventory().clearContent();
                        var rod = ModItems.BUILDING_ROD.asStack();
                        rod.set(ModComponents.STORED_ENERGY, new StoredEnergy(10000));
                        player.setItemInHand(InteractionHand.MAIN_HAND, rod);
                        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE, 16));
                        player.inventoryMenu.broadcastChanges();
                        prepared = true;
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                advance(1);
            }
            case 1 -> {
                if (!prepared || !client.player.getMainHandItem().is(ModItems.BUILDING_ROD)
                    || client.player.getOffhandItem().getCount() != 16) return;
                client.player.setPos(8.5, 82, 12);
                aim(client, Vec3.atBottomCenterOf(FIRST));
                advance(22);
            }
            case 2 -> {
                check(BuildingRodClient.target != null && BuildingRodClient.target.equals(FIRST), "Collider first target");
                verifyCore(client);
                client.options.keyUse.setDown(true);
                use(client);
                check(FIRST.equals(BuildingRodClient.first), "Press starts source selection");
                aim(client, Vec3.atBottomCenterOf(FIRST.east(2)));
                advance(3);
            }
            case 3 -> {
                var bounds = BuildingRodClient.selectionBounds();
                check(bounds != null && bounds.getXSpan() == 3 && bounds.getYSpan() == 1 && bounds.getZSpan() == 1,
                    "Drag selection dimensions");
                capture(client, "selection", 4);
            }
            case 4 -> {
                client.options.keyUse.setDown(false);
                advance(5);
            }
            case 5 -> {
                if (!client.level.getBlockState(FIRST.east(2)).is(Blocks.STONE)) return;
                check(BuildingRodClient.first == null && count(client) == 13 && energy(client) == 9700, "Release commits once");
                check(!((ItemStack) field(BuildingRodItemRenderer.class, "pushedPayload")).isEmpty(),
                    "Result packet starts payload animation");
                capture(client, "placed", 6);
            }
            case 6 -> {
                if (!client.isWindowActive()) return;
                key(client, GLFW.GLFW_KEY_Z, GLFW.GLFW_MOD_CONTROL);
                advance(7);
            }
            case 7 -> {
                if (!client.level.getBlockState(FIRST).isAir() || count(client) != 16) return;
                check(energy(client) == 9700, "Keyboard undo preserves consumed energy");
                aim(client, Vec3.atBottomCenterOf(FIRST));
                advance(26);
            }
            case 8 -> {
                if (!prepared || BuildingRodClient.snapshot == null) return;
                client.player.setPos(8.5, 84, 14);
                client.options.fov().set(90);
                aim(client, Vec3.atBottomCenterOf(FIRST));
                advance(9);
            }
            case 9 -> {
                check(field(BuildingRodRenderer.class, "mesh") != null, "GPU projection mesh built");
                check(!((List<?>) field(BuildingRodRenderer.class, "ENTITIES")).isEmpty(), "Block entity preview present");
                check(((List<?>) field(BuildingRodRenderer.class, "PREVIEW_ENTITIES")).size() == 1, "Entity preview present");
                capture(client, "blueprint", 10);
            }
            case 10 -> {
                client.options.keyUse.setDown(true);
                use(client);
                advance(11);
            }
            case 11 -> {
                client.options.keyUse.setDown(false);
                advance(12);
            }
            case 12 -> {
                check(BuildingRodClient.locked && BuildingRodClient.first == null, "Blueprint lock after release");
                if (!client.isWindowActive()) return;
                var before = BuildingRodClient.placement;
                key(client, GLFW.GLFW_KEY_EQUAL, 0);
                check(BuildingRodClient.placement.rotation() != before.rotation(), "Native keyboard hook rotates blueprint");
                key(client, GLFW.GLFW_KEY_BACKSLASH, 0);
                check(BuildingRodClient.placement.mirror() != before.mirror(), "Native keyboard hook mirrors blueprint");
                advance(13);
            }
            case 13 -> capture(client, "locked-rotated", 14);
            case 14 -> {
                confirmed = BuildingRodClient.placement;
                client.options.keyUse.setDown(true);
                use(client);
                advance(15);
            }
            case 15 -> {
                client.options.keyUse.setDown(false);
                BlockPos solid = confirmed.worldOf(BlockPos.ZERO);
                if (!client.level.getBlockState(solid).is(Blocks.STONE)) return;
                check(!BuildingRodClient.locked, "Completion packet unlocks projection");
                AnvilCraft.CLIENT_CONFIG.buildingRodControls = AnvilCraftClientConfig.BuildingRodControls.TRADITIONAL;
                client.player.setPos(-8.5, 84, 12);
                aim(client, new Vec3(-8.5, 81, 6.5));
                advance(16);
            }
            case 16 -> {
                if (BuildingRodClient.snapshot == null) return;
                client.options.keyUse.setDown(true);
                use(client);
                advance(17);
            }
            case 17 -> {
                client.options.keyUse.setDown(false);
                advance(18);
            }
            case 18 -> {
                check(BuildingRodTraditionalControls.isActive() && BuildingRodClient.locked
                    && BuildingRodTraditionalControls.selectedTool() == BuildingRodTraditionalControls.Tool.CONFIRM,
                    "Traditional lock and tool");
                capture(client, "traditional", 31);
            }
            case 19 -> {
                BuildingRodClient.cancel();
                check(!BuildingRodTraditionalControls.isActive() && !BuildingRodClient.locked, "Cancel clears both control modes");
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    var rod = player.getMainHandItem();
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE, 16));
                    player.setItemInHand(InteractionHand.OFF_HAND, rod);
                    player.inventoryMenu.broadcastChanges();
                });
                advance(20);
            }
            case 20 -> {
                if (!client.player.getOffhandItem().is(ModItems.BUILDING_ROD)) return;
                check(BuildingRodItemRenderer.hideHand(client.player, InteractionHand.MAIN_HAND), "Offhand rod hides material hand");
                capture(client, "offhand", 35);
            }
            case 21 -> {
                try {
                    Files.deleteIfExists(blueprintFile);
                } catch (Exception error) {
                    throw new IllegalStateException(error);
                }
                AnvilCraft.LOGGER.info("PORT_BUILDING_CLIENT_PASSED: selection, fixed-distance, Control release/click, undo, "
                    + "ghost blocks/fluid/BER/entity, lock, rotation, mirror, completion, traditional scrolling, "
                    + "offhand, last material, carried claw, third-person, mining beam");
                client.stop();
            }
            case 22 -> {
                client.options.keyUse.setDown(true);
                use(client);
                check(FIRST.equals(BuildingRodClient.first), "Ctrl scenario starts from collider target");
                nativeModifier(client, false, true);
                advance(23);
            }
            case 23 -> {
                check((Boolean) field(BuildingRodClient.class, "distanceHeld"), "Native Control key fixes distance");
                client.options.keyUse.setDown(false);
                aim(client, new Vec3(8.5, client.player.getEyeY(), 8));
                advance(24);
            }
            case 24 -> {
                check(BuildingRodClient.first != null, "Releasing use while Control held preserves selection");
                double before = (Double) field(BuildingRodClient.class, "previewDistance");
                key(client, GLFW.GLFW_KEY_UP, GLFW.GLFW_MOD_CONTROL);
                check((Double) field(BuildingRodClient.class, "previewDistance") > before, "Arrow adjusts fixed distance");
                nativeModifier(client, false, false);
                advance(25);
            }
            case 25 -> {
                check(BuildingRodClient.first == null && count(client) == 16 && energy(client) == 10000,
                    "Control release cancels without placement or cost");
                aim(client, Vec3.atBottomCenterOf(FIRST));
                advance(2);
            }
            case 26 -> {
                client.options.keyUse.setDown(true);
                use(client);
                nativeModifier(client, false, true);
                advance(27);
            }
            case 27 -> {
                check((Boolean) field(BuildingRodClient.class, "distanceHeld"), "Second fixed-distance selection");
                client.options.keyUse.setDown(false);
                advance(28);
            }
            case 28 -> {
                fixedCells = BuildingRodClient.placementPreview().size();
                fixedEnergy = energy(client);
                check(fixedCells > 0 && BuildingRodClient.first != null, "Fixed-distance selection remains ready");
                client.options.keyUse.setDown(true);
                use(client);
                nativeModifier(client, false, false);
                advance(29);
            }
            case 29 -> {
                client.options.keyUse.setDown(false);
                if (energy(client) != fixedEnergy - fixedCells * 100) return;
                check(BuildingRodClient.first == null && count(client) == 16 - fixedCells,
                    "Right click confirms fixed-distance selection exactly once");
                key(client, GLFW.GLFW_KEY_Z, GLFW.GLFW_MOD_CONTROL);
                advance(30);
            }
            case 30 -> {
                if (count(client) != 16 || !client.level.getBlockState(FIRST).isAir()) return;
                prepared = false;
                giveBlueprint(client);
                advance(8);
            }
            case 31 -> {
                nativeModifier(client, false, true);
                advance(32);
            }
            case 32 -> {
                var event = new InputEvent.MouseScrollingEvent(0, 1, false, false, false, 0, 0);
                BuildingRodClient.scroll(event);
                check(event.isCanceled() && BuildingRodTraditionalControls.selectedTool() == BuildingRodTraditionalControls.Tool.LAYER_UP,
                    "Control-scroll selects previous traditional tool");
                nativeModifier(client, false, false);
                nativeModifier(client, true, true);
                advance(33);
            }
            case 33 -> {
                var event = new InputEvent.MouseScrollingEvent(0, 1, false, false, false, 0, 0);
                BuildingRodClient.scroll(event);
                check(event.isCanceled() && BuildingRodClient.layer == 0, "Alt-scroll applies selected layer tool");
                nativeModifier(client, true, false);
                advance(34);
            }
            case 34 -> {
                var event = new InputEvent.MouseScrollingEvent(0, 1, false, false, false, 0, 0);
                BuildingRodClient.scroll(event);
                check(!event.isCanceled(), "Unmodified scroll remains hotbar input");
                capture(client, "traditional-layer", 19);
            }
            case 35 -> {
                check(((ItemStack) field(BuildingRodItemRenderer.class, "pushedPayload")).isEmpty(), "No previous offhand push");
                client.getSingleplayerServer().execute(() -> {
                    try {
                        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                        player.setGameMode(GameType.SURVIVAL);
                        player.getMainHandItem().setCount(1);
                        var pos = new BlockPos(-8, 81, 6);
                        var result = player.getMainHandItem().useOn(new UseOnContext(player.level(), player, InteractionHand.MAIN_HAND,
                            player.getMainHandItem(), new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false)));
                        check(result.consumesAction(), "Ordinary placement succeeds with offhand rod");
                        player.inventoryMenu.broadcastChanges();
                    } catch (Throwable error) {
                        failure = error;
                    }
                });
                stage = 36;
                next = 0;
            }
            case 36 -> {
                var pushed = (ItemStack) field(BuildingRodItemRenderer.class, "pushedPayload");
                if (!client.player.getMainHandItem().isEmpty() || pushed.isEmpty()) return;
                check(pushed.is(Items.STONE) && pushed.getCount() == 1, "Last consumed material retained by ordinary-placement feedback");
                capture(client, "offhand-last-material", 37);
            }
            case 37 -> {
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.getInventory().setItem(9, player.getOffhandItem());
                    player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                    player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
                    player.inventoryMenu.broadcastChanges();
                });
                advance(38);
            }
            case 38 -> {
                if (!BuildingRodItemRenderer.usesToolClaw(client.player)) return;
                capture(client, "carried-claw", 39);
            }
            case 39 -> {
                client.getSingleplayerServer().execute(() -> {
                    var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    player.inventoryMenu.broadcastChanges();
                });
                advance(40);
            }
            case 40 -> {
                if (!client.player.getMainHandItem().isEmpty()) return;
                check(BuildingRodItemRenderer.usesToolClaw(client.player), "Carried rod keeps empty-hand claw");
                capture(client, "carried-empty-claw", 41);
            }
            case 41 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.getInventory().clearContent();
                    player.inventoryMenu.broadcastChanges();
                    var stand = EntityType.ARMOR_STAND.create(player.level(), EntitySpawnReason.LOAD);
                    stand.setPos(-7.5, 82, 6.5);
                    stand.setShowArms(true);
                    stand.setNoGravity(true);
                    var rod = ModItems.BUILDING_ROD.asStack();
                    rod.set(ModComponents.STORED_ENERGY, new StoredEnergy(10000));
                    stand.setItemSlot(EquipmentSlot.MAINHAND, rod);
                    stand.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.STONE));
                    player.level().addFreshEntity(stand);
                    standId = stand.getId();
                });
                aim(client, new Vec3(-7.5, 83, 6.5));
                advance(42);
            }
            case 42 -> {
                var candidate = client.level.getEntity(standId);
                if (!(candidate instanceof ArmorStand stand) || !stand.getMainHandItem().is(ModItems.BUILDING_ROD)) return;
                var state = (ArmedEntityRenderState) client.getEntityRenderDispatcher().extractEntity(stand, 0);
                check(state.rightHandItemState instanceof BuildingRodHandItemState
                    && (Boolean) field(state.rightHandItemState, "custom") && state.leftHandItemState.isEmpty(),
                    "Native third-person state renders rod and hides material hand");
                capture(client, "third-person-main", 43);
            }
            case 43 -> {
                client.getSingleplayerServer().execute(() -> {
                    var stand = (ArmorStand) client.getSingleplayerServer().overworld().getEntity(standId);
                    var rod = stand.getMainHandItem();
                    stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE));
                    stand.setItemSlot(EquipmentSlot.OFFHAND, rod);
                });
                advance(44);
            }
            case 44 -> {
                var candidate = client.level.getEntity(standId);
                if (!(candidate instanceof ArmorStand stand) || !stand.getOffhandItem().is(ModItems.BUILDING_ROD)) return;
                var state = (ArmedEntityRenderState) client.getEntityRenderDispatcher().extractEntity(stand, 0);
                check((Boolean) field(state.leftHandItemState, "custom") && state.rightHandItemState.isEmpty(),
                    "Native third-person offhand rod retains payload and hides main material");
                capture(client, "third-person-off", 45);
            }
            case 45 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    server.overworld().getEntity(standId).discard();
                    var player = server.getPlayerList().getPlayers().getFirst();
                    player.setGameMode(GameType.CREATIVE);
                    var rod = ModItems.BUILDING_ROD.asStack();
                    rod.set(ModComponents.STORED_ENERGY, new StoredEnergy(10000));
                    player.setItemInHand(InteractionHand.MAIN_HAND, rod);
                    player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.STONE));
                    player.level().setBlockAndUpdate(new BlockPos(-8, 82, 6), Blocks.BEDROCK.defaultBlockState());
                    player.inventoryMenu.broadcastChanges();
                });
                aim(client, new Vec3(-7.5, 82.5, 6.5));
                advance(46);
            }
            case 46 -> {
                if (!client.player.isCreative() || !client.player.getMainHandItem().is(ModItems.BUILDING_ROD)) return;
                client.options.keyAttack.setDown(true);
                BuildingRodClient.interaction(
                    new InputEvent.InteractionKeyMappingTriggered(0, client.options.keyAttack, InteractionHand.MAIN_HAND));
                advance(47);
            }
            case 47 -> {
                check(field(BuildingRodItemRenderer.class, "attackTarget") != null, "Creative attack tracks actual block hit");
                capture(client, "mining-beam", 48);
            }
            case 48 -> {
                client.options.keyAttack.setDown(false);
                advance(21);
            }
            default -> throw new IllegalStateException("Unknown building scene stage");
        }
    }

    private static void giveBlueprint(Minecraft client) {
        client.getSingleplayerServer().execute(() -> {
            try {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setGameMode(GameType.CREATIVE);
                var uuid = UUID.randomUUID();
                final String file = "controls_" + uuid + ".nbt";
                final List<BlockState> palette = List.of(Blocks.STONE.defaultBlockState(), Blocks.GLASS.defaultBlockState(),
                    Blocks.CHEST.defaultBlockState(), Blocks.WATER.defaultBlockState(), Blocks.TORCH.defaultBlockState());
                var entries = new ArrayList<StructureSnapshot.BlockEntry>();
                entries.add(new StructureSnapshot.BlockEntry(BlockPos.ZERO, 0, Optional.empty()));
                entries.add(new StructureSnapshot.BlockEntry(new BlockPos(1, 0, 0), 1, Optional.empty()));
                entries.add(new StructureSnapshot.BlockEntry(new BlockPos(2, 0, 0), 2, Optional.empty()));
                entries.add(new StructureSnapshot.BlockEntry(new BlockPos(0, 0, 1), 3, Optional.empty()));
                entries.add(new StructureSnapshot.BlockEntry(new BlockPos(0, 1, 0), 4, Optional.empty()));
                var tag = new CompoundTag();
                tag.putString("id", "minecraft:armor_stand");
                tag = BuildingEntityTransform.withWorldPos(tag, new Vec3(2.5, 1, 0.5));
                var entity = new StructureSnapshot.EntityEntry(new Vec3(2.5, 1, 0.5), new BlockPos(2, 1, 0), tag);
                var snapshot = new StructureSnapshot(new Vec3i(3, 3, 2), palette, entries, List.of(entity));
                blueprintFile = client.getSingleplayerServer().getWorldPath(LevelResource.ROOT)
                    .resolve("anvilcraft/structures").resolve(file);
                Files.createDirectories(blueprintFile.getParent());
                NbtIo.writeCompressed(StructureSnapshotCodec.write(snapshot), blueprintFile);
                var disk = ModItems.STRUCTURE_DISK.asStack();
                disk.set(ModComponents.STRUCTURE_DISK_DATA, new StructureDiskData(file, "Client blueprint", uuid,
                    Direction.NORTH, 3, 3, 2, false, false));
                player.setItemInHand(InteractionHand.OFF_HAND, disk);
                player.inventoryMenu.broadcastChanges();
                prepared = true;
            } catch (Throwable error) {
                failure = error;
            }
        });
    }

    private static void verifyCore(Minecraft client) {
        var model = new BuildingRodModelParts.ModelState();
        client.getItemModelResolver().updateForTopItem(model, client.player.getMainHandItem(),
            ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, client.level, client.player, 0);
        var layers = (List<?>) field(model, "layers");
        long cores = layers.stream().map(layer -> (net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState) layer)
            .flatMap(layer -> layer.prepareQuadList().stream()).filter(BuildingRodModelParts::isCore).count();
        check(cores == 6, "Native baked model preserves the six floating-core faces");
    }

    private static void nativeModifier(Minecraft client, boolean alt, boolean down) {
        long window = GLFWNativeWin32.glfwGetWin32Window(client.getWindow().handle());
        int message = alt ? down ? User32.WM_SYSKEYDOWN : User32.WM_SYSKEYUP : down ? User32.WM_KEYDOWN : User32.WM_KEYUP;
        long scan = alt ? 0x38L : 0x1DL;
        long flags = 1 | (scan << 16) | (down ? 0 : 0xC0000000L);
        check(User32.PostMessage(null, window, message, alt ? User32.VK_MENU : User32.VK_CONTROL, flags),
            "Keyboard message to the owned test window");
    }

    private static void key(Minecraft client, int key, int modifiers) {
        try {
            var method = KeyboardHandler.class.getDeclaredMethod("keyPress", long.class, int.class, KeyEvent.class);
            method.setAccessible(true);
            var event = new KeyEvent(key, 0, modifiers);
            method.invoke(client.keyboardHandler, client.getWindow().handle(), GLFW.GLFW_PRESS, event);
            method.invoke(client.keyboardHandler, client.getWindow().handle(), GLFW.GLFW_RELEASE, event);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static Object field(Object target, String name) {
        try {
            Class<?> type = target instanceof Class<?> value ? value : target.getClass();
            var field = type.getDeclaredField(name);
            field.setAccessible(true);
            return field.get(target instanceof Class<?> ? null : target);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void use(Minecraft client) {
        BuildingRodClient.interaction(
            new InputEvent.InteractionKeyMappingTriggered(1, client.options.keyUse, InteractionHand.MAIN_HAND));
    }

    private static void aim(Minecraft client, Vec3 point) {
        Vec3 delta = point.subtract(client.player.getEyePosition());
        client.player.setYRot((float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90);
        client.player.setXRot((float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
    }

    private static int count(Minecraft client) {
        return PocketInventory.carriedItems(client.player).stream()
            .filter(stack -> stack.is(Items.STONE)).mapToInt(ItemStack::getCount).sum();
    }

    private static int energy(Minecraft client) {
        return client.player.getMainHandItem().getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 900;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "building-client-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }
}
