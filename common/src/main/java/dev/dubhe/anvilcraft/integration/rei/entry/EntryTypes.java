package dev.dubhe.anvilcraft.integration.rei.entry;

import me.shedaniel.rei.api.common.entry.type.EntryType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * REI
 */
public interface EntryTypes {
    EntryType<BlockState> BLOCK = EntryType.deferred(new ResourceLocation("block_state"));
}
