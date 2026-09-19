package dev.dubhe.anvilcraft.building;

import dev.dubhe.anvilcraft.network.BuildingRodObstructionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BuildingRodObstructions {
    private BuildingRodObstructions() {
    }

    static Set<Entity> find(Level level, List<BuildingRodService.Cell> cells, CollisionContext context) {
        Set<Entity> blockers = new LinkedHashSet<>();
        for (var cell : cells) {
            var shape = cell.state().getCollisionShape(level, cell.pos(), context);
            if (shape.isEmpty()) continue;
            shape = shape.move(cell.pos().getX(), cell.pos().getY(), cell.pos().getZ());
            for (Entity entity : level.getEntities((Entity) null, shape.bounds())) {
                if (!entity.isRemoved() && entity.blocksBuilding
                    && Shapes.joinIsNotEmpty(shape, Shapes.create(entity.getBoundingBox()), BooleanOp.AND)) {
                    blockers.add(entity);
                }
            }
        }
        return blockers;
    }

    static boolean reject(ServerPlayer player, List<BuildingRodService.Cell> cells) {
        Set<Entity> blockers = find(player.level(), cells, CollisionContext.of(player));
        if (blockers.isEmpty()) return false;
        PacketDistributor.sendToPlayer(player, new BuildingRodObstructionPacket(blockers.stream().map(Entity::getId).toList()));
        BuildingRodService.message(player, "blocked");
        return true;
    }
}
