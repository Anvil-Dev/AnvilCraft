package dev.dubhe.anvilcraft.client.support;

import dev.anvilcraft.lib.v2.rendering.preview.gui.StructureGuiProviders;
import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.building.StoragePreviewSnapshot;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import dev.dubhe.anvilcraft.saved.storage.IStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** 手册与机器共用的存储界面提供器，仅从结构快照读取内容。 */
public final class StorageStructurePreview {
    private StorageStructurePreview() {
    }

    public static void register() {
        for (Block block : List.of(ModBlocks.CRATE.get(), ModBlocks.LARGE_CRATE.get(),
            ModBlocks.SHULKER_CONTAINER.get(), ModBlocks.HYPERDIMENSION_STORAGE_STATION.get())) {
            StructureGuiProviders.register(BuiltInRegistries.BLOCK.getKey(block), StorageStructurePreview::create);
        }
    }

    private static StorageScreen create(StructureGuiProviders.Context context) {
        var state = context.level().getBlockState(context.pos());
        Block block = state.getBlock();
        BlockPos pos = block instanceof AbstractMultiPartBlock<?> multi ? multi.getMainPartPos(context.pos(), state) : context.pos();
        IStorageType<?> type = block == ModBlocks.CRATE.get() ? ModStorageTypes.CRATE.get()
            : block == ModBlocks.LARGE_CRATE.get() ? ModStorageTypes.LARGE_CRATE.get()
            : block == ModBlocks.SHULKER_CONTAINER.get() ? ModStorageTypes.SHULKER_CONTAINER.get() : ModStorageTypes.HYPERDIMENSION.get();
        CompoundTag data = context.level().getStructureBlockEntityData(pos);
        var snapshot = StoragePreviewSnapshot.read(data == null ? new CompoundTag() : data, context.level().registryAccess(), type);
        return new StorageScreen(context.player(), pos,
            block instanceof CrateBlock ? CrateBlock.displayName(state) : block.getName(), snapshot);
    }
}
