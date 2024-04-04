package dev.dubhe.anvilcraft.init;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.dubhe.anvilcraft.block.entity.AutoCrafterBlockEntity;
import dev.dubhe.anvilcraft.block.entity.ChuteBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRATE;

public class ModBlockEntities {
    public static final BlockEntityEntry<? extends BlockEntity> AUTO_CRAFTER = REGISTRATE
            .blockEntity("auto_crafter", AutoCrafterBlockEntity::new)
            .validBlock(ModBlocks.AUTO_CRAFTER)
            .register();

    public static final BlockEntityEntry<? extends BlockEntity> CHUTE = REGISTRATE
            .blockEntity("chute", ChuteBlockEntity::new)
            .validBlock(ModBlocks.CHUTE)
            .register();

    public static void register() {
    }
}
