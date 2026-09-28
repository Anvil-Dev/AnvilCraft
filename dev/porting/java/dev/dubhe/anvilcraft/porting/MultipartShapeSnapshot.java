package dev.dubhe.anvilcraft.porting;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MultipartShapeSnapshot {
    public static void capture(Minecraft client) {
        var saved = client.player.getMainHandItem();
        client.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        final var entity = CollisionContext.of(client.player);
        client.player.setItemInHand(InteractionHand.MAIN_HAND, ModBlocks.GIANT_ANVIL.asStack());
        final var anvil = CollisionContext.of(client.player);
        client.player.setItemInHand(InteractionHand.MAIN_HAND, ModBlocks.ACCELERATION_RING.asStack());
        final var ring = CollisionContext.of(client.player);
        client.player.setItemInHand(InteractionHand.MAIN_HAND, saved);
        var pos = new BlockPos(0, 160, 0);
        Map<String, Integer> ids = new LinkedHashMap<>();
        JsonArray shapes = new JsonArray();
        JsonObject states = new JsonObject();
        for (var block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof AbstractMultiPartBlock<?> multipart)) continue;
            for (var state : block.getStateDefinition().getPossibleStates()) {
                JsonObject data = new JsonObject();
                data.addProperty("part", shape(multipart.getPartShape(state), ids, shapes));
                data.addProperty("whole", shape(multipart.getMultiPartShape(state), ids, shapes));
                data.addProperty("outline", shape(state.getShape(client.level, pos, entity), ids, shapes));
                data.addProperty("collision", shape(state.getCollisionShape(client.level, pos, CollisionContext.empty()), ids, shapes));
                data.addProperty("entity_collision", shape(state.getCollisionShape(client.level, pos, entity), ids, shapes));
                data.addProperty("occlusion", shape(state.canOcclude() ? state.getOcclusionShape() : Shapes.empty(), ids, shapes));
                data.addProperty("anvil_placement", shape(state.getShape(client.level, pos, anvil), ids, shapes));
                data.addProperty("ring_placement", shape(state.getShape(client.level, pos, ring), ids, shapes));
                data.addProperty("skylight", state.propagatesSkylightDown());
                if (multipart.getMultiPartShape(state) != multipart.getMultiPartShape(state)) {
                    throw new IllegalStateException("Multipart union is not cached: " + state);
                }
                states.add(key(state), data);
            }
        }
        JsonObject result = new JsonObject();
        result.add("shapes", shapes);
        result.add("states", states);
        try {
            String name = Boolean.getBoolean("anvilcraft.portSodiumScene") ? "multipart-shapes-sodium.json" : "multipart-shapes-26.1.json";
            Files.writeString(client.gameDirectory.toPath().resolve(name), result.toString());
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
        AnvilCraft.LOGGER.info("PORT_MULTIPART_SHAPES_CAPTURED: {} states, {} unique shapes", states.size(), shapes.size());
    }

    private static String key(BlockState state) {
        StringBuilder key = new StringBuilder(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        state.getProperties().stream().sorted(Comparator.comparing(Property::getName))
            .forEach(property -> key.append(';').append(property.getName()).append('=').append(value(state, property)));
        return key.toString();
    }

    private static <T extends Comparable<T>> String value(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    private static int shape(VoxelShape shape, Map<String, Integer> ids, JsonArray shapes) {
        JsonArray boxes = new JsonArray();
        shape.toAabbs().stream().sorted(Comparator.comparingDouble((AABB box) -> box.minX).thenComparingDouble(box -> box.minY)
            .thenComparingDouble(box -> box.minZ).thenComparingDouble(box -> box.maxX).thenComparingDouble(box -> box.maxY)
            .thenComparingDouble(box -> box.maxZ)).forEach(box -> {
                JsonArray coordinates = new JsonArray();
                for (double value : new double[]{box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ}) coordinates.add(value);
                boxes.add(coordinates);
            });
        return ids.computeIfAbsent(boxes.toString(), ignored -> {
            int id = shapes.size();
            shapes.add(boxes);
            return id;
        });
    }
}
