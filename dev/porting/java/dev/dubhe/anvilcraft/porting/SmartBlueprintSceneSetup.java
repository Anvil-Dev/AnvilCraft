package dev.dubhe.anvilcraft.porting;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.SmartBlockPlacerBlockEntity;
import dev.dubhe.anvilcraft.building.StructureSnapshot;
import dev.dubhe.anvilcraft.building.StructureSnapshotCodec;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.nio.file.Files;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SmartBlueprintSceneSetup {
    public static void verify(dev.dubhe.anvilcraft.util.LevelLike preview) {
        int colored = 0;
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 7; z++) {
                    var state = preview.getBlockState(new BlockPos(x, y, z));
                    if (state.is(Blocks.RED_CONCRETE) || state.is(Blocks.BLUE_CONCRETE) || state.is(Blocks.YELLOW_CONCRETE)) colored++;
                }
            }
        }
        if (colored != 3) throw new IllegalStateException("Blueprint preview block count: " + colored);
        AnvilCraft.LOGGER.info("PORT_SMART_BLUEPRINT_PREVIEW_PASSED: three reference blocks in native blueprint view");
    }

    public static void install(MinecraftServer server, SmartBlockPlacerBlockEntity placer) {
        try {
            var id = UUID.randomUUID();
            var name = "smart_parity_" + id + ".nbt";
            var file = server.getWorldPath(LevelResource.ROOT).resolve("anvilcraft/structures").resolve(name);
            var snapshot = new StructureSnapshot(new Vec3i(3, 2, 1),
                List.of(Blocks.RED_CONCRETE.defaultBlockState(), Blocks.BLUE_CONCRETE.defaultBlockState(),
                    Blocks.YELLOW_CONCRETE.defaultBlockState()),
                List.of(new StructureSnapshot.BlockEntry(BlockPos.ZERO, 0, Optional.empty()),
                    new StructureSnapshot.BlockEntry(new BlockPos(2, 0, 0), 1, Optional.empty()),
                    new StructureSnapshot.BlockEntry(new BlockPos(2, 1, 0), 2, Optional.empty())), List.of());
            Files.createDirectories(file.getParent());
            NbtIo.writeCompressed(StructureSnapshotCodec.write(snapshot), file);
            var disk = ModItems.STRUCTURE_DISK.asStack();
            disk.set(ModComponents.STRUCTURE_DISK_DATA,
                new StructureDiskData(name, "Direction parity", id, Direction.EAST, 3, 2, 1, false, false));
            placer.getBlueprintItemHandler().set(0, ItemResource.of(disk), 1);
            if (!placer.hasBlueprint()) throw new IllegalStateException("Smart placer rejected the reference disk");
            AnvilCraft.LOGGER.info("PORT_SMART_BLUEPRINT_INSTALLED: east-facing 3x2x1 disk, auto rotation disabled");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
