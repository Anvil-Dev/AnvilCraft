package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.HyperdimensionUploaderBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import mezz.jei.common.Internal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class UploaderClientScene {
    private static final BlockPos POS = new BlockPos(0, 162, 0);
    private static final UUID FIRST = UUID.randomUUID();
    private static final UUID SECOND = UUID.randomUUID();
    private static int stage;
    private static long next;
    private static long deadline;
    private static boolean capturing;
    private static volatile boolean uploaded;
    private static boolean sawBuffer;

    public static void frame(Minecraft client) {
        if (stage == 8 && client.level.getBlockEntity(POS) instanceof HyperdimensionUploaderBlockEntity visible
            && !visible.isBufferEmpty()) sawBuffer = true;
        if (deadline == 0) deadline = System.currentTimeMillis() + 150000;
        if (System.currentTimeMillis() > deadline) throw new IllegalStateException("Uploader stage " + stage);
        if (capturing || client.getOverlay() != null || System.currentTimeMillis() < next) return;
        switch (stage) {
            case 0 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    var level = server.overworld();
                    for (int x = -3; x <= 3; x++) {
                        for (int z = -3; z <= 4; z++) {
                            level.setBlockAndUpdate(new BlockPos(x, 161, z), Blocks.STONE.defaultBlockState());
                        }
                    }
                    level.setBlockAndUpdate(POS, ModBlocks.SINGULARITY_CRYSTAL.getDefaultState());
                    Storages.get().getOrCreate(FIRST, HyperdimensionStorage.class);
                    Storages.get().getOrCreate(SECOND, HyperdimensionStorage.class);
                    bindHand(client, FIRST);
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamemode creative @a");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "tp @a 0.5 162 3.5 180 20");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set noon");
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "weather clear");
                });
                client.options.hideGui = true;
                client.options.guiScale().set(2);
                client.options.fov().set(60);
                client.resizeGui();
                client.setScreen(null);
                advance(1);
            }
            case 1 -> {
                if (!client.level.getBlockState(POS).is(ModBlocks.SINGULARITY_CRYSTAL) || !holds(client, FIRST)
                    || client.player.distanceToSqr(0.5, 162, 3.5) > 1) return;
                advance(2);
                use(client);
            }
            case 2 -> {
                if (!(client.level.getBlockEntity(POS) instanceof HyperdimensionUploaderBlockEntity entity)
                    || !FIRST.equals(entity.getStorageId())) return;
                advance(20);
            }
            case 20 -> capture(client, "day", 3);
            case 3 -> {
                client.getSingleplayerServer().execute(() -> {
                    var server = client.getSingleplayerServer();
                    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "time set midnight");
                });
                advance(4);
            }
            case 4 -> capture(client, "night", 5);
            case 5 -> {
                client.getSingleplayerServer().execute(() -> bindHand(client, SECOND));
                advance(6);
            }
            case 6 -> {
                if (!holds(client, SECOND)) return;
                advance(7);
                use(client);
            }
            case 7 -> {
                var entity = (HyperdimensionUploaderBlockEntity) client.level.getBlockEntity(POS);
                if (!SECOND.equals(entity.getStorageId())) return;
                client.getSingleplayerServer().execute(() -> {
                    var serverEntity = (HyperdimensionUploaderBlockEntity) client.getSingleplayerServer().overworld().getBlockEntity(POS);
                    serverEntity.getBuffer().set(0, ItemResource.of(Items.GOLD_INGOT), 64);
                    serverEntity.getBuffer().set(1, ItemResource.of(Items.GOLD_INGOT), 64);
                    serverEntity.getBuffer().set(2, ItemResource.of(Items.GOLD_INGOT), 22);
                });
                advance(8);
            }
            case 8 -> {
                if (!uploaded) {
                    client.getSingleplayerServer().execute(() -> {
                        var first = Storages.get().get(FIRST, HyperdimensionStorage.class).orElseThrow();
                        var second = Storages.get().get(SECOND, HyperdimensionStorage.class).orElseThrow();
                        uploaded = first.getItems().getTypeCount() == 0 && second.getItems().getAmountAsLong(0) == 150;
                    });
                    next = System.currentTimeMillis() + 250;
                    return;
                }
                if (!((HyperdimensionUploaderBlockEntity) client.level.getBlockEntity(POS)).isBufferEmpty()) return;
                if (!sawBuffer) throw new IllegalStateException("Populated buffer never synchronized to the client");
                client.setScreen(new Preview());
                advance(9);
            }
            case 9 -> capture(client, "item", 10);
            case 10 -> {
                var runtime = Internal.getJeiRuntime();
                var manager = runtime.getRecipeManager();
                var recipe = manager.createRecipeLookup(AnvilCraftJeiPlugin.USE_ITEM_ON_BLOCK).get()
                    .filter(value -> value.outputBlock() == ModBlocks.HYPERDIMENSION_UPLOADER.get()).findFirst().orElseThrow();
                if (recipe.inputBlock() != ModBlocks.SINGULARITY_CRYSTAL.get()
                    || recipe.item() != ModItems.HYPERDIMENSION_TERMINAL.get()) {
                    throw new IllegalStateException("Uploader conversion recipe ingredients");
                }
                advance(11);
                runtime.getRecipesGui().showRecipes(manager.getRecipeCategory(AnvilCraftJeiPlugin.USE_ITEM_ON_BLOCK),
                    List.of(recipe), List.of());
            }
            case 11 -> capture(client, "jei", 12);
            case 12 -> {
                AnvilCraft.LOGGER.info("PORT_UPLOADER_CLIENT_PASSED: "
                    + "actual conversion/rebind, UUID and buffer sync, global upload, models and JEI");
                stage = 13;
                client.stop();
            }
            default -> {
            }
        }
    }

    private static void bindHand(Minecraft client, UUID id) {
        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
        var terminal = ModItems.HYPERDIMENSION_TERMINAL.asStack();
        terminal.set(ModComponents.TERMINAL_BINDING, new TerminalBinding(Optional.of(id)));
        player.setItemInHand(InteractionHand.MAIN_HAND, terminal);
        player.setNoGravity(true);
        player.containerMenu.broadcastChanges();
    }

    private static boolean holds(Minecraft client, UUID id) {
        var binding = client.player.getMainHandItem().get(ModComponents.TERMINAL_BINDING);
        return binding != null && binding.id().filter(id::equals).isPresent();
    }

    private static void use(Minecraft client) {
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
            new BlockHitResult(Vec3.atCenterOf(POS).add(0, 0.5, 0), Direction.UP, POS, false));
    }

    private static void advance(int target) {
        stage = target;
        next = System.currentTimeMillis() + 2500;
    }

    private static void capture(Minecraft client, String name, int target) {
        capturing = true;
        Screenshot.grab(client.gameDirectory, "uploader-26.1-" + name + ".png", client.getMainRenderTarget(), 1,
            message -> client.execute(() -> {
                capturing = false;
                advance(target);
            }));
    }

    private static final class Preview extends Screen {
        private Preview() {
            super(Component.literal("Uploader"));
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xFF303030);
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.width / 2.0F - 32, this.height / 2.0F - 32);
            graphics.pose().scale(4, 4);
            graphics.item(ModBlocks.HYPERDIMENSION_UPLOADER.asStack(), 0, 0);
            graphics.pose().popMatrix();
        }
    }
}
